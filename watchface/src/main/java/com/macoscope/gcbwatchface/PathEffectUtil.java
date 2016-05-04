package com.macoscope.gcbwatchface;

import android.graphics.DashPathEffect;

public class PathEffectUtil {
    public static DashPathEffect getDashedStrokeEffect(float line, float gap) {
        return new DashPathEffect(new float[]{line - gap, gap}, 0);
    }
}
