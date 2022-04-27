package org.jlab.ersap.coda.types;

import j4np.data.evio.EvioEvent;
import org.jlab.epsci.ersap.base.error.ErsapException;
import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.epsci.ersap.engine.ErsapSerializer;

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
public class EvioDataType {

    private EvioDataType() { }

    private static class EvioSerializer implements ErsapSerializer {

        @Override
        public ByteBuffer write(Object data) throws ErsapException {
            EvioEvent event = (EvioEvent)data;
            return event.getBuffer();
        }

        @Override
        public Object read(ByteBuffer buffer) throws ErsapException {
            return new EvioEvent(); //todo how to create EvioReader object from a byteBuffer
        }
    }

    public static final EngineDataType EVIO =
            new EngineDataType("binary/data-evio", EngineDataType.BYTES.serializer());
}
