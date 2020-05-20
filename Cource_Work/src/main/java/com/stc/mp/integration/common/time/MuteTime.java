package com.stc.mp.integration.common.time;

import lombok.Data;

import java.util.Date;

@Data
public class MuteTime implements Comparable, Cloneable {

    private long start;
    private long stop;

    @Override
    public MuteTime clone() {
        MuteTime mt = new MuteTime();
        mt.start = start;
        mt.stop = stop;
        return mt;
    }

    @Override
    public String toString() {
        return start > 3600 * 1000 ? //date or sec from beginning of file
                "Mute: ".concat(new Date(start).toString()).concat(" Unmute: ").concat(new Date(stop).toString()) :
                "Mute: ".concat(Float.toString((float) start / 1000)).concat(" Unmute: ").concat(Float.toString((float) stop / 1000));
    }

    @Override
    public int compareTo(Object o) {
        return (int) (getStart() - ((MuteTime) o).getStart());
    }

}
