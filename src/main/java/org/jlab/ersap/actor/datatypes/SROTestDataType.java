package org.jlab.ersap.actor.datatypes;

import org.jlab.epsci.ersap.base.error.ErsapException;
import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.epsci.ersap.engine.ErsapSerializer;
import org.jlab.ersap.actor.coda.proc.RocTimeFrameBank;

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
                List<List<RocTimeFrameBank>> sroData = (List<List<RocTimeFrameBank>>) data;
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
                    return (List<List<RocTimeFrameBank>>) in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new ErsapException("Failed to deserialize SRO data", e);
                }
            }
        });

    }
}
