package com.macoscope.gcbwatchface.util;

import android.graphics.DashPathEffect;
import android.graphics.Paint;

public class DashedCirclePaintWrapper {
    /**
     * Angle between two following hours on the clock face.
     */
    private static final int HOURS_ANGLE_OFFSET = 30;
    private static final int HOURS_COUNT = 12;
    private Paint paint;
    private float piece;

    public DashedCirclePaintWrapper(Paint paintUsedToDrawing){
        paint = paintUsedToDrawing;
    }

    public void onDiameterChange(float diameter){
        piece = (float) (Math.PI * diameter / HOURS_COUNT);
        paint.setPathEffect(getDashedStrokeEffect(piece, MeasureUtil.PIECES_GAP));
    }

    /**
     * Count canvas rotation to have dashed gaps ov drawn oval in place of watch face hours
     * @param additionalDegrees - additional rotation, usually it could be -90 degrees to start drawing circle from top
     *
     * @return rotation in degrees which could be applied to {@link android.graphics.Canvas#rotate(float)} function
     */
    public float getRotation(float additionalDegrees){
        return MeasureUtil.PIECES_GAP / 2 * HOURS_ANGLE_OFFSET / piece + additionalDegrees;
    }


    private DashPathEffect getDashedStrokeEffect(float line, float gap) {
        return new DashPathEffect(new float[]{line - gap, gap}, 0);
    }
}
