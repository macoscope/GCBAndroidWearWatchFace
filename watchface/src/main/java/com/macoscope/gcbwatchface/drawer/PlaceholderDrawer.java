package com.macoscope.gcbwatchface.drawer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.macoscope.gcbwatchface.ColorPalette;

public class PlaceholderDrawer implements Drawer {

    private ColorPalette colorPalette;
    private float ovalsSummaryGap;
    private TextPaint textPaint;
    private String message;
    private StaticLayout staticLayout;
    private int layoutWidth = -1;
    private boolean needNewStaticLayout = true;

    public PlaceholderDrawer(ColorPalette colorPalette, float textSize, String message, float innerStrokeSize,
                             float outerStroke, float ovalsGap) {
        this.colorPalette = colorPalette;
        this.ovalsSummaryGap = 2 * innerStrokeSize + 2 * outerStroke + 2 * ovalsGap;
        initTextPaint(textSize);
        this.message = message;
    }

    public void setMessage(String message) {
        this.message = message;
        needNewStaticLayout = !message.equals(this.message);
    }

    private void initTextPaint(float textSize) {
        textPaint = new TextPaint();
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setColor(colorPalette.colorWhite);
    }

    private int getLayoutWidth(float boundsWidth) {
        if (layoutWidth == -1) {
            layoutWidth = (int) ((boundsWidth - ovalsSummaryGap) / Math.sqrt(2));
        }
        return layoutWidth;
    }

    public void draw(Canvas canvas, float boundsWidth, float centerX, float centerY) {
        if (needNewStaticLayout) {
            staticLayout = new StaticLayout(message, textPaint, getLayoutWidth(boundsWidth),
                    Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
            needNewStaticLayout = false;
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
