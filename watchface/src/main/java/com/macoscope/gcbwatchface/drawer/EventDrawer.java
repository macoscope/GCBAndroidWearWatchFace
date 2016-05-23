package com.macoscope.gcbwatchface.drawer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.macoscope.gcbwatchface.ColorPalette;
import com.macoscope.gcbwatchface.R;
import com.macoscope.gcbwatchface.formatter.EventFormatter;
import com.macoscope.gcbwatchface.util.MeasureUtil;

public class EventDrawer implements Drawer {
    private static final int MAX_TITLE_LINES = 2;
    private static final int MAX_CALENDAR_NAME_LINES = 1;
    private TextPaint startsInTextPaint;
    private TextPaint minutesTextPaint;
    private TextView eventNameTextView;
    private TextView eventCalendarTextView;
    private Paint bitmapPaint;

    private String startsIn;

    private int startsInHeight;
    private int startInMinutesHeight;
    private int startInMinutesPadding;

    private int calendarNameOffset;

    private Resources resources;
    private int eventNameHeight;
    private int desiredEventNameWidth;
    private int calendarDesiredWidth;

    public EventDrawer(Context context, Typeface typefaceLight, ColorPalette colorPalette, Paint bitmapPaint) {
        this.bitmapPaint = bitmapPaint;
        this.resources = context.getResources();
        this.startInMinutesPadding = MeasureUtil.getDimensionToPixel(resources, R.dimen.start_in_minutes_padding);
        this.startsIn = resources.getString(R.string.starts_in);

        initEventNameTextView(context, colorPalette);
        initStartsInTextPaint(colorPalette, MeasureUtil.getDimensionToPixel(resources, R.dimen.event_starts_in_font));
        String minutesString = resources.getQuantityString(R.plurals.minutes, MeasureUtil.ALL_DIGITS,
                MeasureUtil.ALL_DIGITS);
        initMinutesTextPaint(typefaceLight, colorPalette, MeasureUtil.getDimensionToPixel(resources,
                R.dimen.minutes_to_event_font), minutesString);
        initEventCalendarTextView(context, colorPalette);
        //XXX must be initialized after paints measurements
        this.calendarNameOffset = startsInHeight + startInMinutesPadding + startInMinutesHeight + MeasureUtil
                .getDimensionToPixel(resources, R.dimen.calendar_name_padding);
    }

    private void initEventNameTextView(Context context, ColorPalette colorPalette) {
        eventNameTextView = new TextView(context);
        eventNameTextView.setTypeface(Typeface.DEFAULT);
        eventNameTextView.setTextSize(context.getResources().getDimension(R.dimen.event_name_font));
        eventNameTextView.setTextColor(colorPalette.colorWhite);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        eventNameTextView.setLayoutParams(layoutParams);
        eventNameTextView.setLines(MAX_TITLE_LINES);
        eventNameTextView.setEllipsize(TextUtils.TruncateAt.END);
        eventNameTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        eventNameTextView.setGravity(Gravity.BOTTOM);
    }

    private void initEventCalendarTextView(Context context, ColorPalette colorPalette) {
        eventCalendarTextView = new TextView(context);
        eventCalendarTextView.setTypeface(Typeface.DEFAULT);
        eventCalendarTextView.setTextSize(context.getResources().getDimension(R.dimen.event_calendar_font));
        eventCalendarTextView.setTextColor(colorPalette.colorWhite);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        eventCalendarTextView.setLayoutParams(layoutParams);
        eventCalendarTextView.setLines(MAX_CALENDAR_NAME_LINES);
        eventCalendarTextView.setEllipsize(TextUtils.TruncateAt.END);
        eventCalendarTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    private void initStartsInTextPaint(ColorPalette colorPalette, float textSize) {
        startsInTextPaint = new TextPaint();
        startsInTextPaint.setAntiAlias(true);
        startsInTextPaint.setColor(colorPalette.colorGray);
        startsInTextPaint.setStyle(Paint.Style.FILL);
        startsInTextPaint.setTextSize(textSize);
        startsInTextPaint.setTextAlign(Paint.Align.CENTER);
        Rect textBounds = new Rect();
        startsInTextPaint.getTextBounds(startsIn, 0, startsIn.length(), textBounds);
        startsInHeight = textBounds.height();
    }

    private void initMinutesTextPaint(Typeface typefaceLight, ColorPalette colorPalette, float textSize, String
            minutesString) {
        minutesTextPaint = new TextPaint();
        minutesTextPaint.setAntiAlias(true);
        minutesTextPaint.setColor(colorPalette.colorWhite);
        minutesTextPaint.setStyle(Paint.Style.FILL);
        minutesTextPaint.setTextSize(textSize);
        minutesTextPaint.setTextAlign(Paint.Align.CENTER);
        minutesTextPaint.setTypeface(typefaceLight);
        Rect textBounds = new Rect();
        minutesTextPaint.getTextBounds(minutesString, 0, minutesString.length(), textBounds);
        startInMinutesHeight = textBounds.height();
    }

    public void measure(float radius){
        eventNameHeight = measureTextViewHeight(eventNameTextView);
        desiredEventNameWidth = (int) Math.sqrt(radius * radius - Math.pow(eventNameHeight, 2)) * 2;
        applyDesiredWidth(eventNameTextView, desiredEventNameWidth);

        int calendarMeasuredHeight = measureTextViewHeight(eventCalendarTextView);
        float top = calendarNameOffset + calendarMeasuredHeight;
        calendarDesiredWidth = (int) Math.sqrt(radius * radius + top * top);
        applyDesiredWidth(eventCalendarTextView, calendarDesiredWidth);
    }

    public void draw(EventFormatter eventFormatter, Canvas canvas, float centerX, float centerY, long timeInMillis) {
        drawEventName(canvas, bitmapPaint, eventFormatter.getName(), centerX, centerY);
        drawEventCalendarName(canvas, bitmapPaint, eventFormatter.getCalendarName(), centerX, centerY);
        canvas.drawText(startsIn, centerX, centerY + startsInHeight, startsInTextPaint);
        canvas.drawText(eventFormatter.getMinutesToEventString(resources, timeInMillis), centerX, centerY +
                startsInHeight + startInMinutesPadding + startInMinutesHeight, minutesTextPaint);
    }
    
    /**
     * Draw event name above inner oval diameter.
     */
    private void drawEventName(Canvas canvas, Paint bitmapPaint, CharSequence eventName,
                               float centerX, float centerY) {
        eventNameTextView.setText(eventName);
        eventNameTextView.setDrawingCacheEnabled(true);
        if (eventNameTextView.getDrawingCache() != null) {
            canvas.drawBitmap(eventNameTextView.getDrawingCache(), centerX - desiredEventNameWidth / 2,
                    centerY - eventNameHeight, bitmapPaint);
        }
        eventNameTextView.setDrawingCacheEnabled(false);
    }

    private void drawEventCalendarName(Canvas canvas, Paint bitmapPaint, CharSequence calendarName,
                                       float centerX, float centerY) {
        eventCalendarTextView.setText(calendarName);
        eventCalendarTextView.setDrawingCacheEnabled(true);

        if (eventCalendarTextView.getDrawingCache() != null) {
            canvas.drawBitmap(eventCalendarTextView.getDrawingCache(), centerX - calendarDesiredWidth / 2,
                    centerY + calendarNameOffset, bitmapPaint);
        }
        eventCalendarTextView.setDrawingCacheEnabled(false);
    }

    private int measureTextViewHeight(TextView textView) {
        textView.measure(View.MeasureSpec.makeMeasureSpec(1, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return textView.getMeasuredHeight();
    }

    private void applyDesiredWidth(TextView textView, int desiredWidth) {
        textView.measure(View.MeasureSpec.makeMeasureSpec(desiredWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        textView.layout(0, 0, textView.getMeasuredWidth(), textView.getMeasuredHeight());
    }

    public void setAmbientMode(boolean ambientModeOn) {
        startsInTextPaint.setAntiAlias(!ambientModeOn);
        minutesTextPaint.setAntiAlias(!ambientModeOn);
    }
}
