package com.macoscope.gcbwatchface;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;

import java.util.Calendar;
import java.util.TimeZone;

public class HourDrawer implements Drawer{

    private ColorPalette colorPalette;
    private Paint hourTextPaint;
    private int hourHeight;
    private HourFormatter hourFormatter;

    public HourDrawer(ColorPalette colorPalette, Typeface typeface, float textSize) {
        this.colorPalette = colorPalette;
        hourFormatter = new HourFormatter();
        initHourTextPaint(typeface, textSize);
    }

    private void initHourTextPaint(Typeface typeface, float textSize) {
        hourTextPaint = new TextPaint();
        hourTextPaint.setTextSize(textSize);
        hourTextPaint.setTextAlign(Paint.Align.CENTER);
        hourTextPaint.setTypeface(typeface);
        hourTextPaint.setAntiAlias(true);
        Rect textBounds = new Rect();
        String digits = String.format("%d", MeasureUtil.ALL_DIGITS);
        hourTextPaint.getTextBounds(digits, 0, digits.length(), textBounds);
        hourHeight = textBounds.height();
    }

    public void draw(Canvas canvas, Calendar time, float centerX, float centerY) {
        hourTextPaint.setColor(colorPalette.getHourColor(time.get(Calendar.MINUTE)));
        canvas.drawText(hourFormatter.getHourToDisplay(time), centerX, centerY + hourHeight / 2, hourTextPaint);
    }

    public void setTimeZone(TimeZone timeZone){
        hourFormatter.setTimeZone(timeZone);
    }


    public void setAmbientMode(boolean ambientModeOn){
        hourTextPaint.setAntiAlias(!ambientModeOn);
    }
}
