package org.jlab.ersap.coda.engines;

import org.jlab.coda.jevio.EvioBank;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.epsci.ersap.base.ErsapUtil;
import org.jlab.epsci.ersap.engine.Engine;
import org.jlab.epsci.ersap.engine.EngineData;
import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.ersap.coda.support.VAdcHit;
import org.jlab.ersap.coda.types.EvioDataType;
import org.jlab.ersap.coda.types.JavaObjectType;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2021, Jefferson Science Associates, all rights reserved.
 * See LICENSE.txt file.
 * Thomas Jefferson National Accelerator Facility
 * Experimental Physics Software and Computing Infrastructure Group
 * 12000, Jefferson Ave, Newport News, VA 23606
 * Phone : (757)-269-7100
 *
 * @author gurjyan on 5/6/22
 * @project ersap-coda
 */
public class FAdcIdEngine implements Engine {

    private static int[] slotMap = {0, 10, 13, 9, 14, 8, 15, 7, 16, 6, 17, 5, 18, 4, 19, 3, 20};

    private static String C_WINDOW = "s_window";
    private long tDelta; // time window to correlate hits as candidate for an event, i.e. coincident
    private static String S_STEP = "s_step";
    private int stepSize;
    private static String S_HITS = "s_hits";
    private int nHitsInSWindow;

    private static String T_SLOT = "t_slot";
    private int tSlot;
    private static String T_CHANNEL = "t_channel";
    private int tChannel;
    private static String BC_SLOT = "bc_slot";
    private int bcSlot;
    private static String BC_CHANNEL = "bc_channel";
    private int bcChannel;

    private static String BC_QMIN = "bc_qmin";
    private int bcQmin;
    private static String BC_QMAX = "bc_qmax";
    private int bcQmax;

    private ArrayList<String> centerBlocks = new ArrayList<>();

    private AtomicInteger totalFrames = new AtomicInteger(0);
    private AtomicInteger emptyFrames = new AtomicInteger(0);
    private AtomicInteger identifiedEvents = new AtomicInteger(0);

    @Override
    public EngineData configure(EngineData engineData) {
        if (engineData.getMimeType().equalsIgnoreCase(EngineDataType.JSON.mimeType())) {
            String source = (String) engineData.getData();
            JSONObject data = new JSONObject(source);
            tDelta = data.has(C_WINDOW) ? data.getLong(C_WINDOW) : 0;
            stepSize = data.has(S_STEP) ? data.getInt(S_STEP) : 1;
            nHitsInSWindow = data.has(S_HITS) ? data.getInt(S_HITS) : 3;
            tSlot = data.has(T_SLOT) ? data.getInt(T_SLOT) : 0;
            tChannel = data.has(T_CHANNEL) ? data.getInt(T_CHANNEL) : 0;
            bcSlot = data.has(BC_SLOT) ? data.getInt(BC_SLOT) : 0;
            bcChannel = data.has(BC_CHANNEL) ? data.getInt(BC_CHANNEL) : 0;
            bcQmin = data.has(BC_QMIN) ? data.getInt(BC_QMIN) : 0;
            bcQmax = data.has(BC_QMAX) ? data.getInt(BC_QMAX) : 8000;
        }
//        centerBlocks.add("1-17-6");
        centerBlocks.add("1-17-7");
//        centerBlocks.add("1-17-8");
        centerBlocks.add("1-17-11");
        centerBlocks.add("1-17-12");
        centerBlocks.add("1-19-0");
//        centerBlocks.add("1-19-3");
        centerBlocks.add("1-19-4");
//        centerBlocks.add("1-19-5");
        return null;
    }

    @Override
    public EngineData execute(EngineData engineData) {
        totalFrames.incrementAndGet();
        // reset hit bins and hit start times
        long tStart, tEnd;
        boolean foundTrigger = false;
        boolean foundCenter = false;
        tStart = 0;
        tEnd = 0;

        EngineData out = new EngineData();
        List<VAdcHit> x = new ArrayList<>();
        out.setData(JavaObjectType.JOBJ, x);
        String dataType = engineData.getMimeType();
        if (dataType.equals(EvioDataType.EVIO.mimeType())) {
            EvioEvent ev = (EvioEvent) engineData.getData();
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
                List<VAdcHit> data = new ArrayList<>();
                VAdcHit sum = new VAdcHit(0, 0, 0, 0, 0);
                // Skip over SIB by starting at 1
                for (int k = 1; k < kids; k++) {
                    EvioBank dataBank = (EvioBank) rocTSB.getChildAt(k);
                    // Ignore the data type (currently the improper value of 0xf).
                    // Just get the data as bytes
                    int payloadId = dataBank.getHeader().getTag();
                    int slt = getSlot(payloadId);
                    byte[] byteData = dataBank.getRawBytes();

                    if (byteData.length > 0) {
                        ///////////////////////////////
                        // define the hits for a slot in the VTP frame
                        ArrayList<Long> times = new ArrayList<>();
                        IntBuffer intBuf =
                                ByteBuffer.wrap(byteData)
                                        .order(ByteOrder.BIG_ENDIAN)
                                        .asIntBuffer();
                        int[] pData = new int[intBuf.remaining()];
                        intBuf.get(pData);
                        for (int i : pData) {
                            int q = (i >> 0) & 0x1FFF;
                            int channel = (i >> 13) & 0x000F;
                            long v = ((i >> 17) & 0x3FFF) * 4;
//            long ht = frame_time_ns + v; // actual time
                            long ht = v; // time within the frame
                            if (tSlot > 0 && tChannel > 0 &&
                                    slt == tSlot && channel == tChannel) {
                                foundTrigger = true;
                            } else if (bcSlot > 0 && bcChannel > 0
                                    && slt == bcSlot && channel == bcChannel
                                    && q >= bcQmin && q <= bcQmax) {
                                foundCenter = true;
                            }
                            times.add(ht);
                            data.add(new VAdcHit(1, slt, channel, q, ht));
                        }
                        tStart = Collections.min(times);
                        tEnd = Collections.max(times);
                        /////////////////////////////////
                    }
                }

                if (!data.isEmpty()) {

                    // sliding window technique
                    if (tDelta > 0) {
                        if (tStart < tEnd) {
                            int step = 0;
                            long tee;
                            long newTStart = 0;
                            List<VAdcHit> event = new ArrayList<>();
                            do {
                                int q = 0;
                                int slt = -1,cht = -1;

                                if (newTStart > 0) {
                                    tStart = newTStart + stepSize;
                                    step = 0;
                                    newTStart = 0;
                                }
                                final long ts = tStart + ((long) step * stepSize);
                                final long te = ts + tDelta;
                                tee = te;
                                step++;
                                List<VAdcHit> slice = data.stream()
                                        .filter(e -> (e.getTime() >= ts) && (e.getTime() <= te))
                                        .collect(Collectors.toList());

                                if (slice.size() > nHitsInSWindow) {
                                    // see if we find duplicate hits
                                    long dup = slice.stream()
                                            .filter(i -> Collections.frequency(slice, i) > 1)
                                            .count();
                                    // if no duplicates found we take a window with the maximum hits
                                    if (dup == 0) {
                                        if(foundCenter) {
                                            // get max charge in the sliding window
                                            for(VAdcHit vk: slice){
                                                if(vk.getCharge() >= q) {
                                                    q = sum.getCharge();
                                                    slt  = vk.getSlot();
                                                    cht = vk.getChannel();
                                                }
                                            }
                                            if (slt == bcSlot && cht == bcChannel) {
                                                event.addAll(slice);
                                                newTStart = tee;
                                                identifiedEvents.incrementAndGet();
                                            }
                                        }
//                                        if (tSlot > 0 && tChannel > 0 &&
//                                                bcSlot > 0 && bcChannel > 0 &&
//                                                foundTrigger && foundCenter) {
//                                            event.addAll(slice);
//                                            newTStart = tee;
//                                        } else if (tSlot > 0 && tChannel > 0 &&
//                                                bcSlot == 0 && bcChannel == 0 &&
//                                                foundTrigger) {
//                                            event.addAll(slice);
//                                            newTStart = tee;
//                                        } else if (tSlot == 0 && tChannel == 0 &&
//                                                bcSlot > 0 && bcChannel > 0 &&
//                                                foundCenter) {
//                                            event.addAll(slice);
//                                            newTStart = tee;
//                                            identifiedEvents.incrementAndGet();
//                                        } else if (tSlot == 0 && tChannel == 0 &&
//                                                bcSlot == 0 && bcChannel == 0) {
//                                            event.addAll(slice);
//                                            newTStart = tee;
//                                        }
                                    }
                                }
                            } while (tee <= tEnd);

                            if (!event.isEmpty()) {
                                for (VAdcHit v : event) {
                                    if (centerBlocks.contains(v.getName().trim())) {
                                        sum.setCharge(sum.getCharge() + v.getCharge());
                                    }
                                }
                                event.add(sum);
                                out.setData(JavaObjectType.JOBJ, event);
                            }
                        }
                    } else {
                        // no software trigger, i.e. sliding window
                        if (tSlot > 0 && tChannel > 0 &&
                                bcSlot > 0 && bcChannel > 0 &&
                                foundTrigger && foundCenter) {
                            out.setData(JavaObjectType.JOBJ, data);
                        } else if (tSlot > 0 && tChannel > 0 &&
                                bcSlot == 0 && bcChannel == 0 &&
                                foundTrigger) {
                            out.setData(JavaObjectType.JOBJ, data);
                        } else if (tSlot == 0 && tChannel == 0 &&
                                bcSlot > 0 && bcChannel > 0 &&
                                foundCenter) {
                            out.setData(JavaObjectType.JOBJ, data);
                        } else if (tSlot == 0 && tChannel == 0 &&
                                bcSlot == 0 && bcChannel == 0) {
                            out.setData(JavaObjectType.JOBJ, data);
                        }
                    }
                } else {
                    emptyFrames.incrementAndGet();
                }
            }
        }

        System.out.println("Total Frames = " + totalFrames.get()
                + "; Empty Frames = " + emptyFrames.get()
                + "; Total Triggers = " + identifiedEvents.get()
                + "; Identified " + (double) identifiedEvents.get() / ((totalFrames.get() - emptyFrames.get())) * 100 * 1.0 + "%");
        return out;
    }

    private int getSlot(int payloadId) {
        return slotMap[payloadId];
    }

    @Override
    public EngineData executeGroup(Set<EngineData> set) {
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
