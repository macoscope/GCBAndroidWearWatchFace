package com.macoscope.gcbwatchface;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.support.annotation.VisibleForTesting;

public class FaceDrawer implements Drawer{
    /**
     * Positions tweaked for best gradient position on canvas rotation
     */
    private final float[] OVAL_GRADIENT_POSITION = new float[]{0f, 0.495f, 0.495f, 0.995f, 0.995f};

    private Canvas faceCanvas;
    private ColorPalette colorPalette;
    private Paint gradientPaint;
    private Paint arcPaint;
    private float padding;
    private float stroke;

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
        // gradient set in onDraw
        gradientPaint.setStrokeWidth(strokeSize);
        gradientPaint.setStyle(Paint.Style.STROKE);
    }

    private void initInactivePiecesPaint() {
        arcPaint = new Paint();
        arcPaint.setAntiAlias(true);
        arcPaint.setColor(colorPalette.colorNeutral);
        arcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
    }

    public void draw(Bitmap faceBitmap, RectF oval, RectF arcRect, float boundsWidth, float boundsHeight, int
            minutes) {
        int width = (int) (boundsWidth - padding * 2 + stroke * 2);
        int height = (int) (boundsHeight - padding * 2 + stroke * 2);
        float centerX = width / 2;
        float centerY = height / 2;

        if (faceCanvas == null) {
            faceCanvas = new Canvas(faceBitmap);
        }

        if (gradientPaint.getShader() == null) {
            gradientPaint.setShader(new SweepGradient(centerX, centerY, colorPalette.ovalGradient,
                    OVAL_GRADIENT_POSITION));
        }

        oval.set(stroke, stroke, width - stroke, height - stroke);
        float piece = (float) (Math.PI * oval.width() / 12);
        float gap = MeasureUtil.PIECES_GAP;
        if (gradientPaint.getPathEffect() == null) {
            gradientPaint.setPathEffect(PathEffectUtil.getDashedStrokeEffect(piece, gap));
        }
        //count rotation to have gaps in dashed stroke on hours place
        float ovalRotation = (gap / 2 * 30) / piece - 90;

        faceCanvas.save();
        faceCanvas.rotate(ovalRotation, centerX, centerY);
        faceCanvas.drawOval(oval, gradientPaint);
        faceCanvas.restore();

        arcRect.set(oval.left - padding, oval.top - padding, oval.right + padding, oval.bottom
                + padding);
        faceCanvas.drawArc(arcRect, -90, getSwapAngle(minutes), true, arcPaint);
    }
    //TODO Test it
    @VisibleForTesting
    private int getSwapAngle(int minutes){
        if (minutes >= 30) {
            return (minutes * 6 / 30) * 30;
        } else if (minutes == 0) {
            return  -330;
        } else {
            return  -((59 - minutes) * 6 / 30) * 30;
        }
    }

    public void setAmbientMode(boolean ambientModeOn){
        gradientPaint.setAntiAlias(!ambientModeOn);
        arcPaint.setAntiAlias(!ambientModeOn);
    }
}
