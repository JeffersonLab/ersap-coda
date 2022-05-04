package org.jlab.ersap.coda.support;

import twig.data.H1F;
import twig.data.H2F;
import twig.data.TDirectory;
import twig.graphics.TGDataCanvas;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
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
    private H2F scatter;
    private TDirectory histDir;
    private static String ERSAP_USER_DATA;

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

        JFrame frame3 = new JFrame( "ERSAP: channel vs hitTime" );
        TGDataCanvas cc = new TGDataCanvas();

        frame3.add(cc);
        frame3.setSize(600, 600);

        cc.initTimer(600);
        scatter = new H2F("cvh",100,0,70000, 100, 0,33);
        cc.region().draw(scatter);
        frame3.setVisible(true);

        // create directory
        histDir = new TDirectory();
        ERSAP_USER_DATA = System.getenv("ERSAP_USER_DATA");

    }

    public void update (String name, VAdcHit v) {
        if(histograms.containsKey(name)){
            histograms.get(name).fill(v.getCharge());
            if(histograms.containsKey(name)) {
                if (v.getSlot() == 19) {
                    scatter.fill(v.getTime(), v.getChannel() + 16);
                } else {
                    scatter.fill(v.getTime(), v.getChannel());
                }
            }
        } else if(histograms2.containsKey(name)){
            histograms2.get(name).fill(v.getCharge());
            if(histograms2.containsKey(name)) {
                if (v.getSlot() == 19) {
                    scatter.fill(v.getTime(), v.getChannel() + 16);
                } else {
                    scatter.fill(v.getTime(), v.getChannel());
                }
            }
        }
    }

    public void resetScatter(){
        scatter.reset();
    }

    public void writeHist(){
        for(H1F h1: histograms.values()) {
            histDir.add(ERSAP_USER_DATA + "/data/output", h1);
        }
        for(H1F h2: histograms2.values()) {
            histDir.add(ERSAP_USER_DATA + "/data/output", h2);
        }
        histDir.add(ERSAP_USER_DATA + "/data/output", scatter);
        histDir.write(ERSAP_USER_DATA + "/data/output/hist_desy.twig");
    }

    public void readPlotHist(){

//        TDirectory dir2 = new TDirectory("hist_desy.twig");

//        dir2.show(); // will print content of the file

//        H1F h2 = (H1F) dir2.get(ERSAP_USER_DATA + "/data/output");
//        TGCanvas c = new TGCanvas();
//        c.view().region().draw(h2);
//        c.repaint();
    }
}

