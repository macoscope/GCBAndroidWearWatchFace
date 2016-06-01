package com.macoscope.gcbwatchface;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;

public class ColorPalette {
    public int backgroundColor;
    public int colorSoftBlue;
    public int colorGreenBlue;
    public int colorBlush;
    public int colorLipstick;
    public int colorWhite;
    public int colorNeutral;
    public int colorGray;
    public int[] ovalGradient;

    @VisibleForTesting
    private ColorPalette() {
    }

    public ColorPalette(Context context) {
        backgroundColor = ContextCompat.getColor(context, R.color.background);
        colorSoftBlue = ContextCompat.getColor(context, R.color.soft_blue);
        colorGreenBlue = ContextCompat.getColor(context, R.color.green_blue);
        colorBlush = ContextCompat.getColor(context, R.color.blush);
        colorLipstick = ContextCompat.getColor(context, R.color.lipstick);
        colorWhite = ContextCompat.getColor(context, R.color.white);
        colorNeutral = ContextCompat.getColor(context, R.color.neutral);
        colorGray = ContextCompat.getColor(context, R.color.grey_font);
        ovalGradient = new int[]{colorGreenBlue, colorSoftBlue, colorBlush, colorLipstick, colorGreenBlue};
    }

    public int getHourColor(int minutes) {
        return minutes < 30 ? colorGreenBlue : colorLipstick;
    }
}

