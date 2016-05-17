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
import com.macoscope.gcbwatchface.MeasureUtil;
import com.macoscope.gcbwatchface.PathEffectUtil;

public class EventIndicatorDrawer implements Drawer {
    private static final float ARC_MASK_SWAP_ANGLE = 330;
    private Paint innerOvalPaint;
    private Paint innerArcPaint;
    private Canvas indicatorCanvas;
    private float ovalsPadding;
    private float innerStrokeSize;

    public EventIndicatorDrawer(ColorPalette colorPalette, float innerStrokeSize, float outerStroke, float
            ovalsGap) {
        this.innerStrokeSize = innerStrokeSize;
        ovalsPadding = innerStrokeSize / 2 + ovalsGap + outerStroke / 2;
        initInnerOvalPaint(colorPalette, innerStrokeSize);
        initInactiveInnerPiecesPaint();
    }

    private void initInnerOvalPaint(ColorPalette colorPalette, float innerStrokeSize) {
        innerOvalPaint = new Paint();
        innerOvalPaint.setAntiAlias(true);
        innerOvalPaint.setColor(colorPalette.colorWhite);
        innerOvalPaint.setStrokeWidth(innerStrokeSize);
        innerOvalPaint.setStyle(Paint.Style.STROKE);
    }

    private void initInactiveInnerPiecesPaint() {
        innerArcPaint = new Paint();
        innerArcPaint.setAntiAlias(true);
        innerArcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        innerArcPaint.setColor(Color.TRANSPARENT);
    }

    private void initIndicationCanvas(Bitmap bitmap) {
        if (indicatorCanvas == null) {
            indicatorCanvas = new Canvas(bitmap);
        }
    }

    public void draw(Bitmap faceBitmap, int minutes, RectF oval, RectF innerOval, RectF arcRect) {

        initIndicationCanvas(faceBitmap);

        innerOval.set(oval.left + ovalsPadding, oval.top + ovalsPadding, oval.right - ovalsPadding,
                oval.bottom - ovalsPadding);

        float piece = (float) (Math.PI * innerOval.width() / 12);
        float gap = MeasureUtil.PIECES_GAP;
        if (innerOvalPaint.getPathEffect() == null) {
            innerOvalPaint.setPathEffect(PathEffectUtil.getDashedStrokeEffect(piece, gap));
        }

        float ovalRotation = (gap / 2 * 30) / piece;

        indicatorCanvas.save();
        indicatorCanvas.rotate(ovalRotation, innerOval.centerX(), innerOval.centerY());
        indicatorCanvas.drawOval(innerOval, innerOvalPaint);
        indicatorCanvas.restore();

        arcRect.set(innerOval.left - innerStrokeSize, innerOval.top - innerStrokeSize,
                innerOval.right + innerStrokeSize, innerOval.bottom + innerStrokeSize);

        indicatorCanvas.drawArc(arcRect, getStartAngle(minutes), ARC_MASK_SWAP_ANGLE, true, innerArcPaint);
    }

    public void clearIndication(Bitmap faceBitmap, RectF oval, RectF arcRect) {
        initIndicationCanvas(faceBitmap);

        arcRect.set(oval.left + ovalsPadding - innerStrokeSize, oval.top + ovalsPadding - innerStrokeSize,
                oval.right - ovalsPadding + innerStrokeSize, oval.bottom - ovalsPadding + innerStrokeSize);

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
