package com.macoscope.gcbwatchface;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

public class EventIndicatorDrawer implements Drawer{

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

    public void draw(Bitmap faceBitmap,  int minutes, RectF oval, RectF innerOval, RectF arcRect) {

        innerOval.set(oval.left + ovalsPadding, oval.top + ovalsPadding, oval.right - ovalsPadding,
                oval.bottom - ovalsPadding);

        if (indicatorCanvas == null) {
            indicatorCanvas = new Canvas(faceBitmap);
        }

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

        float startAngle;
        float angle = 330;

        if (minutes == 0) {
            startAngle = -60;
        } else {
            startAngle = (minutes * 6 / 30) * 30 - 60;
        }
        indicatorCanvas.drawArc(arcRect, startAngle, angle, true, innerArcPaint);
    }

    public void setAmbientMode(boolean ambientModeOn) {
        innerOvalPaint.setAntiAlias(!ambientModeOn);
        innerArcPaint.setAntiAlias(!ambientModeOn);
    }
}
