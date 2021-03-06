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

/**
 * Copyright (c) 2021, Jefferson Science Associates, all rights reserved.
 * See LICENSE.txt file.
 * Thomas Jefferson National Accelerator Facility
 * Experimental Physics Software and Computing Infrastructure Group
 * 12000, Jefferson Ave, Newport News, VA 23606
 * Phone : (757)-269-7100
 *
 * @author gurjyan on 5/1/22
 * @project ersap-coda
 */
public class FAdcHitsEngine implements Engine {

    private static int[] slotMap = {0, 10, 13, 9, 14, 8, 15, 7, 16, 6, 17, 5, 18, 4, 19, 3, 20};
    private boolean foundTrigger = false;
    private boolean foundCenter = false;

    private static String T_SLOT = "t_slot";
    private int tSlot;
    private static String T_CHANNEL = "t_channel";
    private int tChannel;
    private static String BC_SLOT = "bc_slot";
    private int bcSlot;
    private static String BC_CHANNEL = "bc_channel";
    private int bcChannel;

    private static String MIN_Q = "min_q";
    private int minQ;
    private static String MAX_Q = "max_q";
    private int maxQ;

    @Override
    public EngineData configure(EngineData engineData) {
        if (engineData.getMimeType().equalsIgnoreCase(EngineDataType.JSON.mimeType())) {
            String source = (String) engineData.getData();
            JSONObject data = new JSONObject(source);
            tSlot = data.has(T_SLOT) ? data.getInt(T_SLOT) : 0;
            tChannel = data.has(T_CHANNEL) ? data.getInt(T_CHANNEL) : 0;
            bcSlot = data.has(BC_SLOT) ? data.getInt(BC_SLOT) : 0;
            bcChannel = data.has(BC_CHANNEL) ? data.getInt(BC_CHANNEL) : 0;
            minQ = data.has(MIN_Q) ? data.getInt(MIN_Q) : 0;
            maxQ = data.has(MAX_Q) ? data.getInt(MAX_Q) : 9000;
        }
        return null;
    }

    @Override
    public EngineData execute(EngineData engineData) {
        foundTrigger = false;
        foundCenter = false;
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
                // Skip over SIB by starting at 1
                for (int k = 1; k < kids; k++) {
                    EvioBank dataBank = (EvioBank) rocTSB.getChildAt(k);
                    // Ignore the data type (currently the improper value of 0xf).
                    // Just get the data as bytes
                    int payloadId = dataBank.getHeader().getTag();
                    int slt = getSlot(payloadId);
                    byte[] byteData = dataBank.getRawBytes();

                    if (byteData.length > 0) {
                        // define the hits for a slot in the VTP frame
                        fADCPayloadDecoder(data, timestamp, slt, byteData);
                    }
                }
                if(!data.isEmpty()){
                    if(tSlot > 0 && tChannel > 0 &&
                            bcSlot > 0 && bcChannel > 0 &&
                            foundTrigger && foundCenter){
                        out.setData(JavaObjectType.JOBJ, data);
                    } else if(tSlot > 0 && tChannel > 0 &&
                            bcSlot == 0 && bcChannel == 0 &&
                            foundTrigger ) {
                        out.setData(JavaObjectType.JOBJ, data);
                    } else if(tSlot == 0 && tChannel == 0 &&
                            bcSlot > 0 && bcChannel > 0 &&
                            foundCenter ) {
                        out.setData(JavaObjectType.JOBJ, data);
                    } else if (tSlot == 0 && tChannel == 0 &&
                            bcSlot == 0 && bcChannel == 0) {
                        out.setData(JavaObjectType.JOBJ, data);
                    }
                }
                return out;
            }
        }
        return out;
    }

    /**
     * Finds the hits (channel, charge and time) reported by the fADC in a specific slot
     *
     * @param data
     * @param frame_time_ns
     * @param slot
     * @param ba
     */
    private void fADCPayloadDecoder(List<VAdcHit> data,
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
//            long ht = frame_time_ns + v;
            long ht = v;
            if (tSlot > 0 && tChannel > 0 &&
                    slot == tSlot && channel == tChannel) {
                foundTrigger = true;
            } else if (bcSlot > 0 && bcChannel > 0
                    && slot == bcSlot && channel == bcChannel) {
                if (q >= minQ && q <= maxQ) {
                    foundCenter = true;
                }
            }
            data.add(new VAdcHit(1, slot, channel, q, ht));
        }
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


