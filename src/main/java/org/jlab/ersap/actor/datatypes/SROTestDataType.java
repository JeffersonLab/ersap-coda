package org.jlab.ersap.actor.datatypes;

import org.jlab.epsci.ersap.base.error.ErsapException;
import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.epsci.ersap.engine.ErsapSerializer;
import org.jlab.ersap.actor.coda.proc.RocTimeFrameBank;
import org.jlab.ersap.actor.coda.proc.EtEvent;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

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
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(bos);
                    out.writeObject(sroData);
                    out.flush();
                    byte[] bytes = bos.toByteArray();
                    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
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
                    ObjectInputStream in = new ObjectInputStream(bis);
                    List<List<RocTimeFrameBank>> timeFrames = (List<List<RocTimeFrameBank>>) in.readObject();
                    
                    // Create an EtEvent to wrap the timeFrames for consistency
                    EtEvent etEvent = new EtEvent();
                    for (List<RocTimeFrameBank> timeFrame : timeFrames) {
                        etEvent.addTimeFrame(timeFrame);
                    }
                    return etEvent;
                } catch (IOException | ClassNotFoundException e) {
                    throw new ErsapException("Failed to deserialize SRO data", e);
                }
            }
        });

    }
}
