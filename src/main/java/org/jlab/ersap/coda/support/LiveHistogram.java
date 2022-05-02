package org.jlab.ersap.coda.support;

import twig.data.H1F;
import twig.data.H2F;
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
    private Map<String, H1F> histograms2 = new HashMap<>();
    private H2F h;

    public LiveHistogram(String frameTitle, ArrayList<String> histTitles,
                         ArrayList<String> histTitles2,
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
            H1F hist = new H1F(s, histBins, histMin, histMax);
            hist.setTitleX(s);
            histograms.put(s, hist);
            c.region().draw(hist);
        }
        frame.setVisible(true);

        if(histTitles2!=null) {
            JFrame frame2 = new JFrame(frameTitle);
            frame2.setSize(frameWidth, frameHeight);
            JPanel panel2 = new JPanel();
            GridLayout gl2 = new GridLayout(gridSize, gridSize);
            gl2.setHgap(10);
            gl2.setVgap(10);
            panel2.setLayout(gl2);
            frame2.getContentPane().add(panel2);

            // create canvases with associated histograms,
            // and add them to the panel
            for (String s : histTitles2) {
                TGDataCanvas c = new TGDataCanvas();
                c.setAxisFont(new Font("Avenir", Font.PLAIN, 6));
                panel2.add(c);
                c.initTimer(600);
                H1F hist = new H1F(s, histBins, histMin, histMax);
                hist.setTitleX(s);
                histograms2.put(s, hist);
                c.region().draw(hist);
            }
            frame2.setVisible(true);
        }

        JFrame frame3 = new JFrame( "ERSAP" );
        TGDataCanvas cc = new TGDataCanvas();

        frame3.add(cc);
        frame3.setSize(600, 600);

        cc.initTimer(600);
        h = new H2F("channel vs hitTime",1024,0,66000, 100, 0,33);

        cc.region().draw(h);
        frame3.setVisible(true);
    }

    public void update (String name, VAdcHit v) {
        if(histograms.containsKey(name)){
            histograms.get(name).fill(v.getCharge());
            h.fill(v.getTime(), v.getChannel());
        } else if(histograms2.containsKey(name)){
            histograms2.get(name).fill(v.getCharge());
            h.fill(v.getTime(), v.getChannel()+16);
        }
    }
}

