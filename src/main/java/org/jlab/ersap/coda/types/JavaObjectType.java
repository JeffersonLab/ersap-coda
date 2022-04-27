package org.jlab.ersap.coda.types;

import org.jlab.epsci.ersap.base.error.ErsapException;
import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.epsci.ersap.engine.ErsapSerializer;
import org.jlab.ersap.coda.util.O2BB2O;

import java.io.IOException;
import java.nio.ByteBuffer;

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
public class JavaObjectType {
    private JavaObjectType() { }

    private static class JavaMapSerializer implements ErsapSerializer {

        @Override
        public ByteBuffer write(Object data) throws ErsapException {
            ByteBuffer bb = null;
            try {
                bb = ByteBuffer.wrap(O2BB2O.toByteArray(data));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bb;
        }

        @Override
        public Object read(ByteBuffer buffer) throws ErsapException {
            Object b = null;
            try {
                b = O2BB2O.toObject(buffer.array());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return b;
        }
    }

    public static final EngineDataType JOBJ =
            new EngineDataType("binary/data-jobj", EngineDataType.BYTES.serializer());
}
