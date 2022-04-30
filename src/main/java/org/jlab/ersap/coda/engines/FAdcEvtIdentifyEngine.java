package org.jlab.ersap.coda.engines;

import org.jlab.coda.jevio.EvioBank;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.epsci.ersap.base.ErsapUtil;
import org.jlab.epsci.ersap.engine.Engine;
import org.jlab.epsci.ersap.engine.EngineData;
import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.epsci.ersap.std.services.AbstractEventWriterService;
import org.jlab.epsci.ersap.std.services.EventWriterException;
import org.jlab.ersap.coda.support.LiveHistogram;
import org.jlab.ersap.coda.support.VAdcHit;
import org.jlab.ersap.coda.types.EvioDataType;
import org.jlab.ersap.coda.types.JavaObjectType;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2021, Jefferson Science Associates, all rights reserved.
 * See LICENSE.txt file.
 * Thomas Jefferson National Accelerator Facility
 * Experimental Physics Software and Computing Infrastructure Group
 * 12000, Jefferson Ave, Newport News, VA 23606
 * Phone : (757)-269-7100
 *
 * @author gurjyan on 4/27/22
 * @project ersap-coda
 */
public class FAdcEvtIdentifyEngine implements Engine {

      private static String S_WINDOW = "sliding_widow_size";
    private int slidingWindowSize;
    private static String S_STEP = "sliding_step";
    private int stepSize;
    // expected beam_center = "crate_slot_channel"
    private static String BEAM_CENTER = "beam_center";
    private String beamCenter;
    private static String UNDEFINED = "undefined";

    private static int[] slotMap = {0, 10, 13, 9, 14, 8, 15, 7, 16, 6, 17, 5, 18, 4, 19, 3, 20};

    @Override
    public EngineData configure(EngineData input) {
        if (input.getMimeType().equalsIgnoreCase(EngineDataType.JSON.mimeType())) {
            String source = (String) input.getData();
            JSONObject data = new JSONObject(source);
            slidingWindowSize = data.has(S_WINDOW) ? data.getInt(S_WINDOW) : 16;
            stepSize = data.has(S_STEP) ? data.getInt(S_STEP) : 1;
            if(data.has(BEAM_CENTER)){
                beamCenter = data.getString(BEAM_CENTER);
            } else {
                beamCenter = UNDEFINED;
            }
        }

        return null;
    }

    @Override
    public EngineData execute(EngineData input) {

        EngineData out = new EngineData();
        Map<String, List<Integer>> x = new HashMap<>();
        out.setData(JavaObjectType.JOBJ, x);
        String dataType = input.getMimeType();
        if (dataType.equals(EvioDataType.EVIO.mimeType())) {
            EvioEvent ev = (EvioEvent) input.getData();
            int evTag = ev.getHeader().getTag();
            if (evTag == 0xffd1) {
                System.out.println("Skip over PRESTART event");
                return out;
            } else if (evTag == 0xffd2) {
                System.out.println("Skip over GO event");
                return out;
            } else if (evTag == 0xffd4) {
                System.out.println("Hit END event, quitting");
                return out;
            }

//            if (evTag == 0xff60) {
//                System.out.println("Found built streaming event");
//            }

            // Go one level down ->
            int childCount = ev.getChildCount();
            if (childCount < 2) {
                System.out.println("Problem: too few child for event (" + childCount + ")");
            }
            // First bank is Time Info Bank (TIB) with frame and timestamp
            EvioBank b = (EvioBank) ev.getChildAt(0);
            int[] intData = b.getIntData();
            int frame = intData[0];
//            long timestamp = ((((long) intData[1]) & 0x00000000ffffffffL) +
//                    (((long) intData[2]) << 32));
            long timestamp = frame * 65536L;
//            System.out.println("  Frame = " + frame + ", TS = " + timestamp);

            // Loop through all ROC Time Slice Banks (TSB) which come after TIB
            for (int j = 1; j < childCount; j++) {
                // ROC Time SLice Bank
                EvioBank rocTSB = (EvioBank) ev.getChildAt(j);
                int kids = rocTSB.getChildCount();
                if (kids < 2) {
                    System.out.println("Problem: too few child for TSB (" + childCount + ")");
                }

                // Another level down, each TSB has a Stream Info Bank (SIB) which comes first,
                // followed by data banks

                // actual container of all fADC hits in the VTP frame
                Map<Long, List<VAdcHit>> data = new HashMap<>();
                // Skip over SIB by starting at 1
                for (int k = 1; k < kids; k++) {
                    EvioBank dataBank = (EvioBank) rocTSB.getChildAt(k);
                    // Ignore the data type (currently the improper value of 0xf).
                    // Just get the data as bytes
                    int payloadId = dataBank.getHeader().getTag();
                    int slt = getSlot(payloadId);
                    byte[] byteData = dataBank.getRawBytes();

                    if(byteData.length > 0) {
                        // define the fits for a slot in the VTP frame
                        fADCPayloadDecoder(data, timestamp, slt, byteData);
                    } else {
                        System.out.println("Warning: payload ID = " + payloadId+"  bank is empty.");
                    }
                }

//                for(Long tl: data.keySet()){
//                    System.out.println("============== "+ tl + " =============");
//                    for(VAdcHit v: data .get(tl)){
//                        System.out.println(v);
//                    }
//                }
                if(!data.isEmpty()) {
                    out.setData(JavaObjectType.JOBJ, eventIdentification(data));
                    return out;
                }
            }
        }
        return input;
    }

    /**
     * Finds the hits (channel, charge and time) reported by the fADC in a specific slot
     * @param data
     * @param frame_time_ns
     * @param slot
     * @param ba
     */
    private void fADCPayloadDecoder(Map<Long, List<VAdcHit>> data,
                                    Long frame_time_ns,
                                    int slot,
                                    byte[] ba) {
        IntBuffer intBuf =
                ByteBuffer.wrap(ba)
                        .order(ByteOrder.BIG_ENDIAN)
                        .asIntBuffer();
        int[] pData = new int[intBuf.remaining()];
        intBuf.get(pData);
        for (int i : pData) {
            int q = (i >> 0) & 0x1FFF;
            int channel = (i >> 13) & 0x000F;
            long v = ((i >> 17) & 0x3FFF) * 4;
            long ht = frame_time_ns + v;
            VAdcHit ah = new VAdcHit(1, slot, channel, q, ht);
            if (data.containsKey(ht)) {
                data.get(ht).add(ah);
            } else {
                List<VAdcHit> hits = new ArrayList<>();
                hits.add(ah);
                data.put(ht, hits);
            }
        }
    }

    /**
     * Sliding window algorithm that requires single hit per channel in the sliding window,
     * and that the channel that corresponds to the beam center would have a largest charge.
     * @param hits
     * @return
     */
    private Map<String, List<Integer>> eventIdentification(Map<Long, List<VAdcHit>> hits) {
        Map<String, List<Integer>> evIdentified = new HashMap<>();
        long sTime, eTime;
        int step = 0;
        Set<Long> timeStamps = hits.keySet();
        long startFrameTime = Collections.min(timeStamps);
        long endFrameTime = Collections.max(timeStamps);
        do {
            // sliding time window leading edge
            sTime = startFrameTime + ((long) step * stepSize);
            // sliding time window trailing edge
            eTime = sTime + slidingWindowSize;
            step++;
            final long s = sTime;
            final long e = eTime;
//            System.out.println("DDD large window = "+startFrameTime+" - "+endFrameTime);
//            System.out.println("DDD sub-window = "+sTime+" - "+eTime);
            // carve the data for that window from the VTP frame
            Map<Long, List<VAdcHit>> subMap = hits.entrySet().stream()
                    .filter(x -> (x.getKey() >= s) && (x.getKey() <= e))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // event identification
            // store hits within the sliding window in a new container: name and the list of hits
            Map<String, List<Integer>> slidingWindowHits = new HashMap<>();
            for (List<VAdcHit> l : subMap.values()) {
                for (VAdcHit h : l) {
                    // the name of the channel: crate-slot-channel
                    String n = h.getCrate()
                            + "-" + h.getSlot()
                            + "-" + h.getChannel();

                    if (slidingWindowHits.containsKey(n)) {
                        slidingWindowHits.get(n).add(h.getCharge());
                    } else {
                        List<Integer> ll = new ArrayList<>();
                        ll.add(h.getCharge());
                        slidingWindowHits.put(n, ll);
                    }
                }
            }
            boolean foundEvent = true;
            Map<String, Integer> evtCandidate = new HashMap<>();
            // if there are more than 1 hit per channel fail the identification
            for (Map.Entry<String, List<Integer>> entry : slidingWindowHits.entrySet()) {
                if (entry.getValue().size() > 1) {
                    foundEvent = false;
                    evtCandidate.clear();
                    break;
                } else {
                    evtCandidate.put(entry.getKey(), entry.getValue().get(0));
                }
            }
            // require beamCenter to have the max deposited charge
            if(!beamCenter.equals(UNDEFINED)) {
                int maxValueInMap = (Collections.max(evtCandidate.values()));
                for (Map.Entry<String, Integer> entry : evtCandidate.entrySet()) {
                    if ((entry.getValue() == maxValueInMap) && !(entry.getKey().equals(beamCenter))) {
                        foundEvent = false;
                        break;
                    }
                }
            }
            if (foundEvent) {
                // add to the identified events container
                for (Map.Entry<String, Integer>  entry : evtCandidate.entrySet()) {
                    if (evIdentified.containsKey(entry.getKey())) {
                        evIdentified.get(entry.getKey()).add(entry.getValue());
                    } else {
                        List<Integer> lo = new ArrayList<>();
                        lo.add(entry.getValue());
                        evIdentified.put(entry.getKey(), lo);
                    }
                }
            }
        } while (eTime <= endFrameTime);
        return evIdentified;
    }

    private int getSlot(int payloadId) {
        return slotMap[payloadId];
    }

    @Override
    public EngineData executeGroup(Set<EngineData> inputs) {
        return null;
    }

    @Override
    public Set<EngineDataType> getInputDataTypes() {
        return ErsapUtil.buildDataTypes(EvioDataType.EVIO,
                EngineDataType.JSON);
    }

    @Override
    public Set<EngineDataType> getOutputDataTypes() {
        return ErsapUtil.buildDataTypes(EvioDataType.EVIO);
    }

    @Override
    public Set<String> getStates() {
        return null;
    }

    @Override
    public String getDescription() {
        return "fADC data decoder and event identification. EVIO data format";
    }

    @Override
    public String getVersion() {
        return "v1.0";
    }

    @Override
    public String getAuthor() {
        return "gurjyan";
    }

    @Override
    public void reset() {

    }

    @Override
    public void destroy() {

    }
}
