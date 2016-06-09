package com.macoscope.gcbwatchface.drawer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.support.annotation.VisibleForTesting;

import com.macoscope.gcbwatchface.ColorPalette;
import com.macoscope.gcbwatchface.util.DashedCirclePaintWrapper;

public class FaceDrawer implements Drawer {
    /**
     * Positions tweaked for best gradient position on canvas rotation
     */
    private final float[] OVAL_GRADIENT_POSITION = new float[]{0f, 0.495f, 0.495f, 0.995f, 0.995f};
    /**
     * Rotation angle offset which must be applied as rotation to canvas if ovals should be drawn from top
     */
    private static final int OVAL_ANGLE_OFFSET = -90;

    private Canvas faceCanvas;
    private ColorPalette colorPalette;
    private Paint gradientPaint;
    private Paint arcPaint;
    private DashedCirclePaintWrapper dashedCirclePaintWrapper;
    private RectF oval;
    private RectF arcRect;
    private float padding;
    private float stroke;
    private float centerX;
    private float centerY;
    private float ovalRotation;

    @VisibleForTesting
    private FaceDrawer() {
    }

    public FaceDrawer(ColorPalette colorPalette, float padding, float strokeSize) {
        this.padding = padding;
        this.colorPalette = colorPalette;
        this.stroke = strokeSize;
        initGradientPaint(strokeSize);
        initInactivePiecesPaint();
    }

    private void initGradientPaint(float strokeSize) {
        gradientPaint = new Paint();
        gradientPaint.setAntiAlias(true);
        // gradient set in measure
        gradientPaint.setStrokeWidth(strokeSize);
        gradientPaint.setStyle(Paint.Style.STROKE);
        dashedCirclePaintWrapper = new DashedCirclePaintWrapper(gradientPaint);
    }

    private void initInactivePiecesPaint() {
        arcPaint = new Paint();
        arcPaint.setAntiAlias(true);
        arcPaint.setColor(colorPalette.colorNeutral);
        arcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
    }

    public void measure(Bitmap faceBitmap, RectF outerOval, int width, int height) {
        float ovalWidth = width - padding * 2 + stroke * 2;
        float ovalHeight = height - padding * 2 + stroke * 2;
        centerX = ovalWidth / 2;
        centerY = ovalHeight / 2;
        oval = outerOval;
        faceCanvas = new Canvas(faceBitmap);
        gradientPaint.setShader(new SweepGradient(centerX, centerY, colorPalette.ovalGradient, OVAL_GRADIENT_POSITION));

        outerOval.set(stroke, stroke, ovalWidth - stroke, ovalHeight - stroke);
        dashedCirclePaintWrapper.onDiameterChange(outerOval.width());
        ovalRotation = dashedCirclePaintWrapper.getRotation(OVAL_ANGLE_OFFSET);
        arcRect = new RectF(oval.left - padding, oval.top - padding, oval.right + padding, oval.bottom + padding);
    }

    public void draw(int minutes) {
        faceCanvas.save();
        faceCanvas.rotate(ovalRotation, centerX, centerY);
        faceCanvas.drawOval(oval, gradientPaint);
        faceCanvas.restore();
        faceCanvas.drawArc(arcRect, OVAL_ANGLE_OFFSET, getSwapAngle(minutes), true, arcPaint);
    }

    @VisibleForTesting
    private int getSwapAngle(int minutes) {
        if (minutes >= 30) {
            return (minutes * 6 / 30) * 30;
        } else if (minutes == 0) {
            return -330;
        } else {
            return -((59 - minutes) * 6 / 30) * 30;
        }
    }

    public void setAmbientMode(boolean ambientModeOn) {
        gradientPaint.setAntiAlias(!ambientModeOn);
        arcPaint.setAntiAlias(!ambientModeOn);
    }
}
