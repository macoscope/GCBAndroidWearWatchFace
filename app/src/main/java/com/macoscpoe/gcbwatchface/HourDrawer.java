package com.macoscpoe.gcbwatchface;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class HourDrawer {

    private ColorPalette colorPalette;
    private Paint hourTextPaint;
    private int hourHeight;
    private Calendar hourCalendar;

    public HourDrawer(ColorPalette colorPalette, Typeface typeface, float textSize) {
        this.colorPalette = colorPalette;
        hourCalendar = new GregorianCalendar();
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
        canvas.drawText(getHourToDisplay(time), centerX, centerY + hourHeight / 2, hourTextPaint);
    }

    public void setTimeZone(TimeZone timeZone){
        hourCalendar.setTimeZone(timeZone);
    }

    //TODO test
    private String getHourToDisplay(Calendar calendar) {
        hourCalendar.setTimeInMillis(calendar.getTimeInMillis());
        int hour = hourCalendar.get(Calendar.HOUR_OF_DAY);
        int minutes = hourCalendar.get(Calendar.MINUTE);
        if (minutes >= 30) {
            hourCalendar.add(Calendar.HOUR_OF_DAY, 1);
            hour = hourCalendar.get(Calendar.HOUR_OF_DAY);
        }
        return String.format("%d", hour);
    }

    public void setAmbientMode(boolean ambientModeOn){
        hourTextPaint.setAntiAlias(!ambientModeOn);
    }
}
