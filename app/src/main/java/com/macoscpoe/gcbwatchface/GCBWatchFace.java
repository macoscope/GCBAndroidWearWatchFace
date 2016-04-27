package com.macoscpoe.gcbwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class GCBWatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = 1000;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private static final EventViewModel EVENT;

    static {
        long eventTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(26);
        EVENT = new EventViewModel("Lorem ipsum dolor sit amet, consectetur adipiscing elit.", new Date(eventTime), "Warsaw");
    }

    @Override
    public GCBWatchFaceEngine onCreateEngine() {
        return new GCBWatchFaceEngine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<GCBWatchFaceEngine> weakEngineReference;

        public EngineHandler(GCBWatchFaceEngine reference) {
            weakEngineReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            GCBWatchFaceEngine engine = weakEngineReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class GCBWatchFaceEngine extends CanvasWatchFaceService.Engine {
        private static final int PIECES_GAP = 8;
        private static final String MINUTES_FONT_FAMILY = "sans-serif-light";

        /**
         * Positions tweaked for best gradient position on canvas rotation
         */
        private final float[] OVAL_GRADIENT_POSITION = new float[]{0f, 0.495f, 0.495f, 0.995f, 0.995f};

        private static final int MAX_TITLE_LINES = 2;

        private final Handler updateTimeHandler = new EngineHandler(this);
        private boolean registeredTimeZoneReceiver = false;
        private boolean ambientMode;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean lowBitAmbient;
        private boolean drawInEventMode = false;

        private ColorPalette colorPalette;

        private Calendar time;
        private Bitmap faceBitmap;
        private Canvas faceCanvas;
        private Canvas indicatorCanvas;

        private Paint bitmapPaint;
        private Paint arcPaint;
        private Paint innerOvalPaint;
        private Paint gradientPaint;
        private Paint innerArcPaint;
        private TextPaint startsInTextPaint;
        private TextPaint minutesTextPaint;

        private RectF oval;
        private RectF innerOval;
        private RectF arcRect;

        private float strokeSize;
        private float innerStrokeSize;
        private float padding;
        private float ovalsGap;

        private int startsInHeight;
        private int startInMinutesHeight;
        private int startInMinutesPadding;

        private String startsIn;

        private TextView eventNameTextView;
        private Typeface typefaceLight;

        private HourDrawer hourDrawer;

        private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TimeZone timeZone = TimeZone.getTimeZone(intent.getStringExtra("time-zone"));
                time.setTimeZone(timeZone);
                time.setTimeInMillis(System.currentTimeMillis());
                if (hourDrawer != null) {
                    hourDrawer.setTimeZone(timeZone);
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setUpWatchFaceStyle();
            initTime();
            Context context = GCBWatchFace.this.getApplicationContext();
            initResources(context);
            initEventNameTextView(context);
            initDrawers();
            initPaints();
            initRectangles();
        }

        private void initDrawers() {
            hourDrawer = new HourDrawer(colorPalette, typefaceLight, getDimensionToPixel(R.dimen.hour_text_size));
        }

        private void initResources(Context context) {
            colorPalette = new ColorPalette(context);

            strokeSize = getDimensionToPixel(R.dimen.outside_oval_stroke);
            innerStrokeSize = getDimensionToPixel(R.dimen.inner_oval_stroke);
            padding = getDimensionToPixel(R.dimen.face_padding);
            ovalsGap = getDimensionToPixel(R.dimen.ovals_gap);
            startInMinutesPadding = getDimensionToPixel(R.dimen.start_in_minutes_padding);

            startsIn = getString(R.string.starts_in);

            typefaceLight = Typeface.create(MINUTES_FONT_FAMILY, Typeface.NORMAL);
            if (typefaceLight == null) {
                typefaceLight = Typeface.DEFAULT;
            }
        }

        private void initEventNameTextView(Context context) {
            eventNameTextView = new TextView(context);
            eventNameTextView.setTypeface(Typeface.DEFAULT);
            eventNameTextView.setTextSize(getResources().getDimension(R.dimen.event_name_font));
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

        private void initRectangles() {
            oval = new RectF();
            arcRect = new RectF();
            innerOval = new RectF();
        }

        private void setUpWatchFaceStyle() {
            setWatchFaceStyle(new WatchFaceStyle.Builder(GCBWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
        }

        private void initTime() {
            time = new GregorianCalendar();
        }

        private void initPaints() {
            initGradientPaint();
            initInactivePiecesPaint();
            initInnerOvalPaint();
            initBitmapPaint();
            initInactiveInnerPiecesPaint();
            initStartsInTextPaint();
            initMinutesTextPaint();
            setAntiAliasForPaints(true);
        }

        private int getDimensionToPixel(int id) {
            float scale = getResources().getDisplayMetrics().density;
            // Convert the dps to pixels, based on density scale
            return (int) (getResources().getDimension(id) * scale + 0.5f);
        }


        private void initGradientPaint() {
            gradientPaint = new Paint();
            // gradient set in onDraw
            gradientPaint.setStrokeWidth(strokeSize);
            gradientPaint.setStyle(Paint.Style.STROKE);
        }

        private void initInactivePiecesPaint() {
            arcPaint = new Paint();
            arcPaint.setColor(colorPalette.colorNeutral);
            arcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        }

        private void initInnerOvalPaint() {
            innerOvalPaint = new Paint();
            innerOvalPaint.setColor(colorPalette.colorWhite);
            innerOvalPaint.setStrokeWidth(innerStrokeSize);
            innerOvalPaint.setStyle(Paint.Style.STROKE);
        }

        private void initBitmapPaint() {
            bitmapPaint = new Paint();
            bitmapPaint.setFilterBitmap(true);
        }


        private void initInactiveInnerPiecesPaint() {
            innerArcPaint = new Paint();
            innerArcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            innerArcPaint.setColor(Color.TRANSPARENT);
        }

        private void initStartsInTextPaint() {
            startsInTextPaint = new TextPaint();
            startsInTextPaint.setColor(colorPalette.colorGray);
            startsInTextPaint.setStyle(Paint.Style.FILL);
            startsInTextPaint.setTextSize(getDimensionToPixel(R.dimen.event_starts_in_font));
            startsInTextPaint.setTextAlign(Paint.Align.CENTER);
            Rect textBounds = new Rect();
            startsInTextPaint.getTextBounds(startsIn, 0, startsIn.length(), textBounds);
            startsInHeight = textBounds.height();
        }

        private void initMinutesTextPaint() {
            minutesTextPaint = new TextPaint();
            minutesTextPaint.setColor(colorPalette.colorWhite);
            minutesTextPaint.setStyle(Paint.Style.FILL);
            minutesTextPaint.setTextSize(getDimensionToPixel(R.dimen.minutes_to_event_font));
            minutesTextPaint.setTextAlign(Paint.Align.CENTER);
            minutesTextPaint.setTypeface(typefaceLight);
            Rect textBounds = new Rect();
            String minutesString = getResources().getQuantityString(R.plurals.minutes, MeasureUtil.ALL_DIGITS,
                    MeasureUtil.ALL_DIGITS);
            minutesTextPaint.getTextBounds(minutesString, 0, minutesString.length(), textBounds);
            startInMinutesHeight = textBounds.height();
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            lowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (ambientMode != inAmbientMode) {
                ambientMode = inAmbientMode;
                if (lowBitAmbient) {
                    hourDrawer.setAmbientMode(inAmbientMode);
                    setAntiAliasForPaints(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void setAntiAliasForPaints(boolean antiAliasOn) {
            bitmapPaint.setAntiAlias(antiAliasOn);
            arcPaint.setAntiAlias(antiAliasOn);
            innerOvalPaint.setAntiAlias(antiAliasOn);
            gradientPaint.setAntiAlias(antiAliasOn);
            innerArcPaint.setAntiAlias(antiAliasOn);
            startsInTextPaint.setAntiAlias(antiAliasOn);
            minutesTextPaint.setAntiAlias(antiAliasOn);
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    drawInEventMode = !drawInEventMode;
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            time.setTimeInMillis(System.currentTimeMillis());
            canvas.drawColor(colorPalette.backgroundColor);

            float centerX = bounds.centerX();
            float centerY = bounds.centerY();
            int minutes = time.get(Calendar.MINUTE);

            initWatchFaceBitmap(bounds, strokeSize);

            if (drawInEventMode) {
                drawEventName(canvas, innerOval, EVENT.getName(), centerX, centerY);
                canvas.drawText(startsIn, centerX, centerY + startsInHeight, startsInTextPaint);
                canvas.drawText(EVENT.getMinutesToEvent(getResources(), time.getTimeInMillis()), centerX, centerY +
                                startsInHeight + startInMinutesPadding + startInMinutesHeight,
                        minutesTextPaint);
            } else {
                hourDrawer.draw(canvas, time, centerX, centerY);
            }

            drawWatchFace(faceBitmap, bounds, strokeSize, minutes);

            drawMeetingIndicator(faceBitmap, EVENT.getHourMinutes());

            canvas.drawBitmap(faceBitmap, padding - strokeSize, padding - strokeSize, bitmapPaint);


        }

        private void initWatchFaceBitmap(Rect bounds, float stroke) {
            int width = (int) (bounds.width() - padding * 2 + stroke * 2);
            int height = (int) (bounds.height() - padding * 2 + stroke * 2);
            if (faceBitmap == null) {
                faceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
        }

        private void drawMeetingIndicator(Bitmap faceBitmap, int minutes) {
            float ovalsPadding = innerStrokeSize / 2 + ovalsGap + strokeSize / 2;
            innerOval.set(oval.left + ovalsPadding, oval.top + ovalsPadding, oval.right - ovalsPadding,
                    oval.bottom - ovalsPadding);

            if (indicatorCanvas == null) {
                indicatorCanvas = new Canvas(faceBitmap);
            }

            float piece = (float) (Math.PI * innerOval.width() / 12);
            float gap = PIECES_GAP;
            if (innerOvalPaint.getPathEffect() == null) {
                innerOvalPaint.setPathEffect(getDashedStrokeEffect(piece, gap));
            }

            float ovalRotation = (gap / 2 * 30) / piece;

            indicatorCanvas.save();
            indicatorCanvas.rotate(ovalRotation, innerOval.centerX(), innerOval.centerY());
            indicatorCanvas.drawOval(innerOval, innerOvalPaint);
            indicatorCanvas.restore();


            arcRect = new RectF(innerOval.left - innerStrokeSize, innerOval.top - innerStrokeSize,
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

        private DashPathEffect getDashedStrokeEffect(float line, float gap) {
            return new DashPathEffect(new float[]{line - gap, gap}, 0);
        }

        private void drawWatchFace(Bitmap faceBitmap, Rect bounds, float stroke, int minutes) {
            int width = (int) (bounds.width() - padding * 2 + stroke * 2);
            int height = (int) (bounds.height() - padding * 2 + stroke * 2);
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
            float gap = PIECES_GAP;
            if (gradientPaint.getPathEffect() == null) {
                gradientPaint.setPathEffect(getDashedStrokeEffect(piece, gap));
            }
            //count rotation to have gaps in dashed stroke on hours place
            float ovalRotation = (gap / 2 * 30) / piece - 90;

            faceCanvas.save();
            faceCanvas.rotate(ovalRotation, centerX, centerY);
            faceCanvas.drawOval(oval, gradientPaint);
            faceCanvas.restore();
            int angle;
            if (minutes >= 30) {
                angle = (minutes * 6 / 30) * 30;
            } else if (minutes == 0) {
                angle = -330;
            } else {
                angle = -((59 - minutes) * 6 / 30) * 30;
            }

            arcRect = new RectF(oval.left - padding, oval.top - padding, oval.right + padding, oval.bottom
                    + padding);
            faceCanvas.drawArc(arcRect, -90, angle, true, arcPaint);
        }

        /**
         * Draw event name above inner oval diameter.
         *
         * @param centerX - bounds center x
         * @param centerY - bounds center y
         */
        private void drawEventName(Canvas canvas, RectF innerOval, CharSequence eventName, float centerX, float centerY) {
            eventNameTextView.setText(eventName);
            eventNameTextView.setDrawingCacheEnabled(true);
            eventNameTextView.measure(MeasureSpec.makeMeasureSpec(1, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

            int measuredHeight = eventNameTextView.getMeasuredHeight();
            float radius = innerOval.width() / 2;
            int desiredWidth = (int) Math.sqrt(Math.pow(radius, 2) - Math.pow(measuredHeight, 2)) * 2;
            eventNameTextView.measure(MeasureSpec.makeMeasureSpec(desiredWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            eventNameTextView.layout(0, 0, eventNameTextView.getMeasuredWidth(), eventNameTextView.getMeasuredHeight());
            if (eventNameTextView.getDrawingCache() != null) {
                canvas.drawBitmap(eventNameTextView.getDrawingCache(), centerX - eventNameTextView.getMeasuredWidth() / 2, centerY -
                                eventNameTextView.getMeasuredHeight(),
                        bitmapPaint);
            }
            eventNameTextView.setDrawingCacheEnabled(false);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                TimeZone defaultZone = TimeZone.getDefault();
                time.setTimeZone(defaultZone);
                time.setTimeInMillis(System.currentTimeMillis());
                hourDrawer.setTimeZone(defaultZone);
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            GCBWatchFace.this.registerReceiver(timeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return;
            }
            registeredTimeZoneReceiver = false;
            GCBWatchFace.this.unregisterReceiver(timeZoneReceiver);
        }

        /**
         * Starts the {@link #updateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #updateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
