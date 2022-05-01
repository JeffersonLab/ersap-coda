package org.jlab.ersap.coda.support;

import twig.data.H1F;
import twig.graphics.TGDataCanvas;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
public class LiveHistogram {

    private Map<String, H1F> histograms = new HashMap<>();

    public LiveHistogram(String frameTitle, ArrayList<String> histTitles,
                         int gridSize, int frameWidth, int frameHeight,
                         int histBins, double histMin, double histMax) {

        JFrame frame = new JFrame(frameTitle);
        frame.setSize(frameWidth, frameHeight);
        JPanel panel = new JPanel();
        GridLayout gl = new GridLayout(gridSize,gridSize);
        gl.setHgap(10);
        gl.setVgap(10);
        panel.setLayout(gl);
        frame.getContentPane().add(panel);

        // create canvases with associated histograms,
        // and add them to the panel
        for(String s: histTitles){
            TGDataCanvas c = new TGDataCanvas();
            c.setAxisFont(new Font("Avenir",Font.PLAIN,6));
            panel.add(c);
            c.initTimer(600);
            System.out.println("DDD histogram = *"+s+"*");
            H1F hist = new H1F(s, histBins, histMin, histMax);
            hist.setTitleX(s);
            histograms.put(s.trim(), hist);
            c.region().draw(hist);
        }
        frame.setVisible(true);
    }

    public void update (String name, int value) {
        if(histograms.containsKey(name)){
            histograms.get(name).fill(value);
        }
    }
}

