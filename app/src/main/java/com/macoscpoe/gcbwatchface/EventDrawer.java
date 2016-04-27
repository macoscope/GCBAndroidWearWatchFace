package com.macoscpoe.gcbwatchface;

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

public class EventDrawer implements Drawer{
    private static final int MAX_TITLE_LINES = 2;

    private TextPaint startsInTextPaint;
    private TextPaint minutesTextPaint;
    private TextView eventNameTextView;
    private Paint bitmapPaint;

    private String startsIn;

    private int startsInHeight;
    private int startInMinutesHeight;
    private int startInMinutesPadding;

    private Resources resources;

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

    public void draw(EventViewModel eventViewModel, Canvas canvas, float radius, float centerX, float centerY,
                     long timeInMillis) {
        drawEventName(canvas, bitmapPaint, radius, eventViewModel.getName(), centerX, centerY);
        canvas.drawText(startsIn, centerX, centerY + startsInHeight, startsInTextPaint);
        canvas.drawText(eventViewModel.getMinutesToEvent(resources, timeInMillis), centerX, centerY +
                        startsInHeight + startInMinutesPadding + startInMinutesHeight, minutesTextPaint);
    }

    /**
     * Draw event name above inner oval diameter.

     */
    private void drawEventName(Canvas canvas, Paint bitmapPaint, float radius, CharSequence eventName,
                               float centerX, float centerY) {
        eventNameTextView.setText(eventName);
        eventNameTextView.setDrawingCacheEnabled(true);
        eventNameTextView.measure(View.MeasureSpec.makeMeasureSpec(1, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        int measuredHeight = eventNameTextView.getMeasuredHeight();
        int desiredWidth = (int) Math.sqrt(Math.pow(radius, 2) - Math.pow(measuredHeight, 2)) * 2;
        eventNameTextView.measure(View.MeasureSpec.makeMeasureSpec(desiredWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        eventNameTextView.layout(0, 0, eventNameTextView.getMeasuredWidth(), eventNameTextView.getMeasuredHeight());
        if (eventNameTextView.getDrawingCache() != null) {
            canvas.drawBitmap(eventNameTextView.getDrawingCache(), centerX - eventNameTextView.getMeasuredWidth() / 2,
                    centerY - eventNameTextView.getMeasuredHeight(), bitmapPaint);
        }
        eventNameTextView.setDrawingCacheEnabled(false);
    }

    public void setAmbientMode(boolean ambientModeOn){
        startsInTextPaint.setAntiAlias(!ambientModeOn);
        minutesTextPaint.setAntiAlias(!ambientModeOn);
    }
}
