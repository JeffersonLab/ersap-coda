package org.jlab.ersap.coda.engines;

import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.epsci.ersap.std.services.AbstractEventWriterService;
import org.jlab.epsci.ersap.std.services.EventWriterException;
import org.jlab.ersap.coda.support.LiveHistogram;
import org.jlab.ersap.coda.support.VAdcHit;
import org.jlab.ersap.coda.types.JavaObjectType;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
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
public class AggStoreHistogramEngine extends AbstractEventWriterService<FileWriter> {
    private static String FRAME_TITLE = "frame_title";
    private String frameTitle;
    private static String FRAME_WIDTH = "frame_width";
    private int frameWidth;
    private static String FRAME_HEIGHT = "frame_height";
    private int frameHeight;
    private static String HIST_TITLES = "hist_titles";
    private ArrayList<String> histTitles;
    private static String HIST_TITLES2 = "hist_titles2";
    private ArrayList<String> histTitles2;
    private static String HIST_BINS = "hist_bins";
    private int histBins;
    private static String HIST_MIN = "hist_min";
    private double histMin;
    private static String HIST_MAX = "hist_max";
    private double histMax;
    private static String GRID_SIZE = "grid_size";
    private int gridSize;
    private static String SCATTER_RESET = "scatter_reset";
    private boolean scatterReset;

    private LiveHistogram liveHist;

    @Override
    protected FileWriter createWriter(Path file, JSONObject opts)
            throws EventWriterException {
        if (opts.has(FRAME_TITLE)) {
            frameTitle = opts.getString(FRAME_TITLE);
        }
        if (opts.has(FRAME_WIDTH)) {
            frameWidth = opts.getInt(FRAME_WIDTH);
        }
        if (opts.has(FRAME_HEIGHT)) {
            frameHeight = opts.getInt(FRAME_HEIGHT);
        }
        if (opts.has(HIST_TITLES)) {
            histTitles = new ArrayList<>();
            String ht = opts.getString(HIST_TITLES);
            StringTokenizer st = new StringTokenizer(ht, ",");
            while (st.hasMoreTokens()) {
                histTitles.add(st.nextToken().trim());
            }
        }
        if (opts.has(HIST_TITLES2)) {
            histTitles2 = new ArrayList<>();
            String ht = opts.getString(HIST_TITLES2);
            StringTokenizer st = new StringTokenizer(ht, ",");
            while (st.hasMoreTokens()) {
                histTitles2.add(st.nextToken().trim());
            }
        }
        if (opts.has(HIST_BINS)) {
            histBins = opts.getInt(HIST_BINS);
        }
        if (opts.has(HIST_MIN)) {
            histMin = opts.getDouble(HIST_MIN);
        }
        if (opts.has(HIST_MAX)) {
            histMax = opts.getDouble(HIST_MAX);
        }
        if (opts.has(GRID_SIZE)) {
            gridSize = opts.getInt(GRID_SIZE);
        }

        if (opts.has(SCATTER_RESET)) {
            scatterReset = true;
        }

        liveHist = new LiveHistogram(frameTitle, histTitles, histTitles2, gridSize,
                frameWidth, frameHeight, histBins, histMin, histMax);


        try {
            return new FileWriter(file.toString());
        } catch (IOException e) {
            throw new EventWriterException(e);
        }
    }

    @Override
    protected void closeWriter() {
        try {
            liveHist.writeHist();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void writeEvent(Object event) throws EventWriterException {
        List<VAdcHit> h = (List<VAdcHit>) event;
        if(!h.isEmpty()) {
            if (scatterReset) liveHist.resetScatter();
            for (VAdcHit v : h) {
                liveHist.update(v.getName().trim(), v);
            }
        }
    }

    @Override
    protected EngineDataType getDataType() {
        return JavaObjectType.JOBJ;
    }
}
