package org.jlab.ersap.actor.datatypes;

import org.jlab.epsci.ersap.base.error.ErsapException;
import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.epsci.ersap.engine.ErsapSerializer;
import org.jlab.ersap.actor.coda.proc.RocTimeFrameBank;
import org.jlab.ersap.actor.coda.proc.EtEvent;
import org.jlab.ersap.actor.coda.proc.FADCHit;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.ArrayList;

/**
 * A ERSAP engine data-type for a List<List<RocTimeFrameBank>>
 */
public final class SROTestDataType extends EngineDataType {

    private static final String MIME_TYPE = "binary/sro-data";

    public static final SROTestDataType INSTANCE = new SROTestDataType();


    private SROTestDataType() {
        super(MIME_TYPE, new ErsapSerializer() {

            @Override
            public ByteBuffer write(Object data) throws ErsapException {
                List<List<RocTimeFrameBank>> sroData;
                System.out.println("DDD ===> "+ data.getClass().getName());
                // Handle both EtEvent and direct List<List<RocTimeFrameBank>>
                if (data instanceof EtEvent) {
                    EtEvent etEvent = (EtEvent) data;
                    sroData = etEvent.getTimeFrames();
                } else {
                    sroData = (List<List<RocTimeFrameBank>>) data;
                }
                
                try {
                    // Use the same binary format as C++ for compatibility
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(bos);
                    
                    // Write outer list size
                    System.out.println("DDD-Java: Writing outer size: " + sroData.size());
                    out.writeInt(sroData.size());
                    
                    for (List<RocTimeFrameBank> sublist : sroData) {
                        // Write inner list size
                        System.out.println("DDD-Java: Writing inner size: " + sublist.size());
                        out.writeInt(sublist.size());
                        
                        for (RocTimeFrameBank frame : sublist) {
                            // Write frame data
                            out.writeInt(frame.getRocID());
                            out.writeInt(frame.getFrameNumber());
                            out.writeLong(frame.getTimeStamp());
                            
                            // Write hits
                            List<FADCHit> hits = frame.getHits();
                            out.writeInt(hits.size());
                            
                            for (FADCHit hit : hits) {
                                out.writeInt(hit.crate());
                                out.writeInt(hit.slot());
                                out.writeInt(hit.channel());
                                out.writeInt(hit.charge());
                                out.writeLong(hit.time());
                            }
                        }
                    }
                    
                    out.flush();
                    byte[] bytes = bos.toByteArray();
                    System.out.println("DDD-Java: Total bytes written: " + bytes.length);
                    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
                    buffer.put(bytes);
                    buffer.flip();
                    return buffer;
                } catch (IOException e) {
                    throw new ErsapException("Failed to serialize SRO data", e);
                }
            }

            @Override
            public Object read(ByteBuffer buffer) throws ErsapException {
                try {
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                    DataInputStream in = new DataInputStream(bis);
                    
                    // Read outer list size
                    int outerSize = in.readInt();
                    List<List<RocTimeFrameBank>> timeFrames = new ArrayList<>();
                    
                    for (int o = 0; o < outerSize; ++o) {
                        // Read inner list size
                        int innerSize = in.readInt();
                        List<RocTimeFrameBank> sublist = new ArrayList<>();
                        
                        for (int f = 0; f < innerSize; ++f) {
                            RocTimeFrameBank frame = new RocTimeFrameBank();
                            frame.setRocID(in.readInt());
                            frame.setFrameNumber(in.readInt());
                            frame.setTimeStamp(in.readLong());
                            
                            // Read hits
                            int hitCount = in.readInt();
                            for (int h = 0; h < hitCount; ++h) {
                                int crate = in.readInt();
                                int slot = in.readInt();
                                int channel = in.readInt();
                                int charge = in.readInt();
                                long time = in.readLong();
                                frame.addHit(new FADCHit(crate, slot, channel, charge, time));
                            }
                            sublist.add(frame);
                        }
                        timeFrames.add(sublist);
                    }
                    
                    // Create an EtEvent to wrap the timeFrames for consistency
                    EtEvent etEvent = new EtEvent();
                    for (List<RocTimeFrameBank> timeFrame : timeFrames) {
                        etEvent.addTimeFrame(timeFrame);
                    }
                    return etEvent;
                } catch (IOException e) {
                    throw new ErsapException("Failed to deserialize SRO data", e);
                }
            }
        });

    }
}
