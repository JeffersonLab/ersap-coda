package org.jlab.ersap.coda.support;

import java.util.Objects;

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
public class VAdcHit {
    private int crate;
    private int slot;
    private int channel;
    private int charge;
    private long time;

    public VAdcHit(int crate, int slot, int channel, int charge, long time) {
        this.crate = crate;
        this.slot = slot;
        this.channel = channel;
        this.charge = charge;
        this.time = time;
    }

    public int getCrate() {
        return crate;
    }

    public int getSlot() {
        return slot;
    }

    public int getChannel() {
        return channel;
    }

    public int getCharge() {
        return charge;
    }

    public void setCharge(int charge) {
        this.charge = charge;
    }

    public long getTime() {
        return time;
    }

    public String getName() {
        return crate+"-"+slot+"-"+channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VAdcHit)) return false;
        VAdcHit vAdcHit = (VAdcHit) o;
        return getCrate() == vAdcHit.getCrate() && getSlot() == vAdcHit.getSlot() && getChannel() == vAdcHit.getChannel();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCrate(), getSlot(), getChannel());
    }

    @Override
    public String toString() {
        return "AdcHit{" +
                "crate=" + crate +
                ", slot=" + slot +
                ", channel=" + channel +
                ", charge=" + charge +
                ", time=" + time +
                '}';
    }
}

