package org.jlab.ersap.coda.engines;

import org.jlab.epsci.ersap.engine.EngineDataType;
import org.jlab.epsci.ersap.std.services.AbstractEventWriterService;
import org.jlab.epsci.ersap.std.services.EventWriterException;
import org.jlab.ersap.coda.support.ManualHistogram;
import org.jlab.ersap.coda.support.VAdcHit;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/**
 * Copyright (c) 2021, Jefferson Science Associates, all rights reserved.
 * See LICENSE.txt file.
 * Thomas Jefferson National Accelerator Facility
 * Experimental Physics Software and Computing Infrastructure Group
 * 12000, Jefferson Ave, Newport News, VA 23606
 * Phone : (757)-269-7100
 *
 * @author gurjyan on 5/5/22
 * @project ersap-coda
 */
public class ManualHistogramEngine extends AbstractEventWriterService<FileWriter> {
    private ManualHistogram manHist;
 private Scanner scanner = new Scanner(System.in);
    @Override
    protected FileWriter createWriter(Path path, JSONObject jsonObject) throws EventWriterException {
        manHist = new ManualHistogram();
        try {
            return new FileWriter(path.toString());
        } catch (IOException e) {
            throw new EventWriterException(e);
        }
    }

    @Override
    protected void closeWriter() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void writeEvent(Object o) throws EventWriterException {
        List<VAdcHit> h = (List<VAdcHit>) o;
        manHist.reset();
        for (VAdcHit v : h) {
            manHist.update(v.getName().trim(), v);
        }
        scanner.nextLine();
    }

    @Override
    protected EngineDataType getDataType() {
        return null;
    }
}