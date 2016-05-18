package com.macoscope.gcbwatchface.drawer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.VisibleForTesting;

import com.macoscope.gcbwatchface.ColorPalette;
import com.macoscope.gcbwatchface.util.DashedCirclePaintWrapper;

public class EventIndicatorDrawer implements Drawer {
    private static final float ARC_MASK_SWAP_ANGLE = 330;
    private Paint innerOvalPaint;
    private Paint innerArcPaint;
    private DashedCirclePaintWrapper dashedCirclePaintWrapper;
    private Canvas indicatorCanvas;
    private float innerStrokeSize;
    private RectF innerOval;
    private RectF arcRect;
    private float ovalRotation;

    public EventIndicatorDrawer(ColorPalette colorPalette, float innerStrokeSize) {
        this.innerStrokeSize = innerStrokeSize;
        this.arcRect = new RectF();

        initInnerOvalPaint(colorPalette, innerStrokeSize);
        initInactiveInnerPiecesPaint();

    }

    private void initInnerOvalPaint(ColorPalette colorPalette, float innerStrokeSize) {
        innerOvalPaint = new Paint();
        innerOvalPaint.setAntiAlias(true);
        innerOvalPaint.setColor(colorPalette.colorWhite);
        innerOvalPaint.setStrokeWidth(innerStrokeSize);
        innerOvalPaint.setStyle(Paint.Style.STROKE);
        dashedCirclePaintWrapper = new DashedCirclePaintWrapper(innerOvalPaint);
    }

    private void initInactiveInnerPiecesPaint() {
        innerArcPaint = new Paint();
        innerArcPaint.setAntiAlias(true);
        innerArcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        innerArcPaint.setColor(Color.TRANSPARENT);
    }


    public void measure(Bitmap faceBitmap, RectF innerOval) {
        this.innerOval = innerOval;

        indicatorCanvas = new Canvas(faceBitmap);
        dashedCirclePaintWrapper.onDiameterChange(innerOval.width());
        ovalRotation = dashedCirclePaintWrapper.getRotation(0);
        arcRect.set(innerOval.left - innerStrokeSize, innerOval.top - innerStrokeSize,
                innerOval.right + innerStrokeSize, innerOval.bottom + innerStrokeSize);

    }

    public void draw(int minutes) {
        indicatorCanvas.save();
        indicatorCanvas.rotate(ovalRotation, innerOval.centerX(), innerOval.centerY());
        indicatorCanvas.drawOval(innerOval, innerOvalPaint);
        indicatorCanvas.restore();
        indicatorCanvas.drawArc(arcRect, getStartAngle(minutes), ARC_MASK_SWAP_ANGLE, true, innerArcPaint);
    }

    public void clearIndication() {
        indicatorCanvas.drawOval(arcRect, innerArcPaint);
    }

    //TODO Test it
    @VisibleForTesting
    private float getStartAngle(int minutes) {
        return minutes == 0 ? -60 : (minutes * 6 / 30) * 30 - 60;
    }

    public void setAmbientMode(boolean ambientModeOn) {
        innerOvalPaint.setAntiAlias(!ambientModeOn);
        innerArcPaint.setAntiAlias(!ambientModeOn);
    }
}
