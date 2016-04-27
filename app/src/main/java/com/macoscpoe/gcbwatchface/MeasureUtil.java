package com.macoscpoe.gcbwatchface;


import android.content.res.Resources;

public class MeasureUtil {
    public static final int ALL_DIGITS = 1234567890;

    public static final int PIECES_GAP = 8;

    public static int getDimensionToPixel(Resources resources, int id) {
        float scale = resources.getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (resources.getDimension(id) * scale + 0.5f);
    }
}
