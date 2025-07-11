package org.jlab.ersap.actor.coda.engine;

import org.jlab.coda.jevio.EvioEvent;
import org.jlab.epsci.ersap.base.ErsapUtil;
import org.jlab.epsci.ersap.engine.Engine;
import org.jlab.epsci.ersap.engine.EngineData;
import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.ersap.actor.coda.proc.Awtbc;
import org.jlab.ersap.actor.coda.proc.EtEvent;
import org.jlab.ersap.actor.coda.proc.EvioEventParser;
import org.jlab.ersap.actor.datatypes.JavaObjectType;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Copyright (c) 2021, Jefferson Science Associates, all rights reserved.
 * See LICENSE.txt file.
 * Thomas Jefferson National Accelerator Facility
 * Experimental Physics Software and Computing Infrastructure Group
 * 12000, Jefferson Ave, Newport News, VA 23606
 * Phone : (757)-269-7100
 *
 * @author gurjyan on 2/9/23
 * {@code} ersap-actor
 * The EventIdentificationEngine class is responsible for identifying events in a single VTP time window.
 * It utilizes sliding window algorithm to define an event based on the FADC hit multiplicity.
 */
public class EventIdentificationEngine implements Engine {
    private EvioEventParser parser;
    private Awtbc awtbc;
    private static final String SLIDING_WINDOW = "sliding_window";
    private long slidingWindow = 40; // in nanoseconds
    private static final String MULTIPLICITY = "multiplicity";
    private int multiplicity = 2; // number of hits within the sliding window
    private static final String STREAMSOURCE = "stream_source";
    private String streamSource;
    private boolean isSourceEt;
    private static final String VERBOSE = "verbose";
    private String verbose;


    @Override
    public EngineData configure(EngineData engineData) {
        if (engineData.getMimeType().equalsIgnoreCase(EngineDataType.JSON.mimeType())) {
            String source = (String) engineData.getData();
            JSONObject data = new JSONObject(source);
            slidingWindow = data.has(SLIDING_WINDOW) ? data.getLong(SLIDING_WINDOW) : 40;
            multiplicity = data.has(MULTIPLICITY) ? data.getInt(MULTIPLICITY) : 2;
            streamSource = data.has(STREAMSOURCE) ? data.getString(STREAMSOURCE) : "et";
            verbose = data.has(VERBOSE) ? data.getString(VERBOSE) : "no";
        }
        if(streamSource.trim().equalsIgnoreCase("et")){
            isSourceEt = true;
        } else if (streamSource.trim().equalsIgnoreCase("file")){
            isSourceEt = false;
        }
        awtbc = new Awtbc(multiplicity, slidingWindow, false);
        if (verbose.trim().equalsIgnoreCase("yes")) {
            parser = new EvioEventParser(true);
        } else {
            parser = new EvioEventParser(false);
        }

        return null;
    }

    @Override
    public EngineData execute(EngineData engineData) {
        if(isSourceEt) {
            return executeETEvent(engineData);
        } else {
            return executeFileEvent(engineData);
        }
    }

    private EngineData executeFileEvent(EngineData engineData) {
        EngineData out = new EngineData();
        EtEvent data;

        // Decoding
        try {
            data = parser.parseFileEvent((EvioEvent)engineData.getData());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

            // Clustering
//        for (RocTimeSliceBanks rsb : data) {
//            result.addAll(awtbc.findCluster(rsb.getHits()));
//        }
//        System.out.println("DDD============DDD");
//        for (IStreamItem evt : result) {
//            System.out.println(evt);
//        }
//        System.out.println("DDD============DDD");

        out.setData(JavaObjectType.JOBJ, data);
        return out;
    }

   private EngineData executeETEvent(EngineData engineData) {
        EngineData out = new EngineData();

       EtEvent data;

        // Decoding
        try {
            data = parser.parseEtEvent((ByteBuffer)engineData.getData());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        out.setData(JavaObjectType.JOBJ, data);
        return out;
    }

    @Override
    public EngineData executeGroup(Set<EngineData> set) {
        return null;
    }

    @Override
    public Set<EngineDataType> getInputDataTypes () {
        return ErsapUtil.buildDataTypes(JavaObjectType.JOBJ,
                EngineDataType.JSON);
    }

    @Override
    public Set<EngineDataType> getOutputDataTypes () {
        return ErsapUtil.buildDataTypes(JavaObjectType.JOBJ);
    }

    @Override
    public Set<String> getStates () {
        return null;
    }

    @Override
    public String getDescription () {
        return "fADC data decoder and event identification. EVIO data format, supports file and ET sources ";
    }

    @Override
    public String getVersion () {
        return "v1.0";
    }

    @Override
    public String getAuthor () {
        return "gurjyan";
    }

    @Override
    public void reset () {

    }

    @Override
    public void destroy () {
    }
}
