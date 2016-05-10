package com.macoscope.gcbwatchface;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

public class PermissionsDrawer implements Drawer {

    private ColorPalette colorPalette;
    private float ovalsSummaryGap;
    private TextPaint textPaint;
    private String permissions;
    private StaticLayout staticLayout;
    private int layoutWidth;

    public PermissionsDrawer(ColorPalette colorPalette, float textSize, String permissions, float innerStrokeSize, float outerStroke, float
            ovalsGap) {
        this.colorPalette = colorPalette;
        this.ovalsSummaryGap = 2 * innerStrokeSize + 2 * outerStroke + 2 * ovalsGap;
        initTextPaint(textSize);
        this.permissions = permissions;
    }

    private void initTextPaint(float textSize) {
        textPaint = new TextPaint();
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setColor(colorPalette.colorWhite);
    }

    public void draw(Canvas canvas, float boundsWidth, float centerX, float centerY) {
        if (staticLayout == null) {
            layoutWidth = (int) ((boundsWidth - ovalsSummaryGap) / Math.sqrt(2));
            staticLayout = new StaticLayout(permissions, textPaint, layoutWidth, Layout.Alignment.ALIGN_CENTER, 1, 0,
                    false);
        }
        canvas.save();
        canvas.translate(centerX, centerY - staticLayout.getHeight() / 2);
        staticLayout.draw(canvas);
        canvas.restore();
    }

    public void setAmbientMode(boolean ambientModeOn) {
        textPaint.setAntiAlias(!ambientModeOn);
    }
}
