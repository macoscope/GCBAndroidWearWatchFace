package com.macoscope.gcbwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import com.eccyan.optional.Optional;
import com.macoscope.gcbwatchface.drawer.EventDrawer;
import com.macoscope.gcbwatchface.drawer.EventIndicatorDrawer;
import com.macoscope.gcbwatchface.drawer.FaceDrawer;
import com.macoscope.gcbwatchface.drawer.HourDrawer;
import com.macoscope.gcbwatchface.drawer.PlaceholderDrawer;
import com.macoscope.gcbwatchface.formatter.EventFormatter;
import com.macoscpoe.gcbmodel.Event;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class GCBWatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a minute
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = 1000;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    /**
     * Handler message id for updating the event.
     */
    private static final int MSG_UPDATE_EVENT = 1;

    @Override
    public GCBWatchFaceEngine onCreateEngine() {
        return new GCBWatchFaceEngine();
    }

    private class GCBWatchFaceEngine extends CanvasWatchFaceService.Engine {
        private static final String MINUTES_FONT_FAMILY = "sans-serif-light";

        private class EngineHandler extends Handler {

            private final WeakReference<GCBWatchFaceEngine> weakEngineReference;

            public EngineHandler(GCBWatchFaceEngine engineReference) {
                weakEngineReference = new WeakReference<>(engineReference);
            }

            @Override
            public void handleMessage(Message msg) {
                GCBWatchFaceEngine engine = weakEngineReference.get();
                if (engine != null) {
                    switch (msg.what) {
                        case MSG_UPDATE_TIME: {
                            engine.handleUpdateTimeMessage();
                            break;
                        }
                        case MSG_UPDATE_EVENT: {
                            engine.updateEvent();
                            break;
                        }
                    }
                }
            }
        }
        private final Handler engineHandler = new EngineHandler(this);
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
        private Calendar formatterCalendar;
        private Bitmap faceBitmap;
        private Paint bitmapPaint;

        private RectF outerOval;
        private RectF innerOval;
        private RectF arcRect;

        private float strokeSize;
        private float padding;

        private Typeface typefaceLight;

        private HourDrawer hourDrawer;
        private EventDrawer eventDrawer;
        private FaceDrawer faceDrawer;
        private EventIndicatorDrawer indicatorDrawer;
        private PlaceholderDrawer placeholderDrawer;

        private EventFormatter eventFormatter;
        private EventsManager eventsManager;

        private BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TimeZone timeZone = TimeZone.getTimeZone(intent.getStringExtra("time-zone"));
                time.setTimeZone(timeZone);
                time.setTimeInMillis(System.currentTimeMillis());
                if (hourDrawer != null) {
                    hourDrawer.setTimeZone(timeZone);
                }
                formatterCalendar.setTimeZone(timeZone);
            }
        };

        private EventsListChangeListener eventsListListener = new EventsListChangeListener() {
            @Override
            public void onEventsListChanged() {
                updateEvent();
            }

            @Override
            public void onEventsLoadFailure() {

            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setUpWatchFaceStyle();
            initCalendars();
            initEventFormatter();
            Context context = GCBWatchFace.this.getApplicationContext();
            initResources(context);
            initPaints();
            initRectangles();
            initDrawers(context);
            initAndRegisterEventsManager(context);
        }

        private void initAndRegisterEventsManager(Context context) {
            eventsManager = new EventsManager(context);
            eventsManager.registerMessageListener(eventsListListener);
        }

        private void initEventFormatter() {
            eventFormatter = new EventFormatter(formatterCalendar);
        }

        private void initResources(Context context) {
            colorPalette = new ColorPalette(context);

            strokeSize = MeasureUtil.getDimensionToPixel(getResources(), R.dimen.outside_oval_stroke);
            padding = MeasureUtil.getDimensionToPixel(getResources(), R.dimen.face_padding);

            typefaceLight = Typeface.create(MINUTES_FONT_FAMILY, Typeface.NORMAL);
            if (typefaceLight == null) {
                typefaceLight = Typeface.DEFAULT;
            }

        }

        private void initDrawers(Context context) {
            hourDrawer = new HourDrawer(colorPalette, typefaceLight, MeasureUtil.getDimensionToPixel(getResources(),
                    R.dimen.hour_text_size));
            eventDrawer = new EventDrawer(context, typefaceLight, colorPalette, bitmapPaint);
            faceDrawer = new FaceDrawer(colorPalette, padding, strokeSize);
            indicatorDrawer = new EventIndicatorDrawer(colorPalette,
                    MeasureUtil.getDimensionToPixel(getResources(), R.dimen.inner_oval_stroke),
                    strokeSize,
                    MeasureUtil.getDimensionToPixel(getResources(), R.dimen.ovals_gap));
            placeholderDrawer = new PlaceholderDrawer(colorPalette, MeasureUtil.getDimensionToPixel(getResources(),
                    R.dimen.permissions_not_granted), "No events loaded", MeasureUtil.getDimensionToPixel
                    (getResources(), R.dimen.inner_oval_stroke), strokeSize, MeasureUtil.getDimensionToPixel
                    (getResources(), R.dimen.ovals_gap));
        }

        private void initRectangles() {
            outerOval = new RectF();
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

        private void initCalendars() {
            time = new GregorianCalendar();
            formatterCalendar = new GregorianCalendar();
        }

        private void initPaints() {
            initBitmapPaint();
        }

        private void initBitmapPaint() {
            bitmapPaint = new Paint();
            bitmapPaint.setAntiAlias(true);
        }

        @Override
        public void onDestroy() {
            eventsManager.unregisterMessageListener();
            engineHandler.removeMessages(MSG_UPDATE_TIME);
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
                    faceDrawer.setAmbientMode(inAmbientMode);
                    hourDrawer.setAmbientMode(inAmbientMode);
                    eventDrawer.setAmbientMode(inAmbientMode);
                    indicatorDrawer.setAmbientMode(inAmbientMode);
                    bitmapPaint.setAntiAlias(!inAmbientMode);
                    placeholderDrawer.setAmbientMode(inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
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
                if (eventFormatter.hasValidEvent()) {
                    eventDrawer.draw(eventFormatter, canvas, innerOval.width() / 2, centerX, centerY, time.getTimeInMillis());
                } else {
                    placeholderDrawer.draw(canvas, bounds.width(), centerX, centerY);
                }
            } else {
                hourDrawer.draw(canvas, time, centerX, centerY);
            }
            faceDrawer.draw(faceBitmap, outerOval, arcRect, bounds.width(), bounds.height(), minutes);
            if (eventFormatter.hasValidEvent()) {
                indicatorDrawer.draw(faceBitmap, eventFormatter.getHourMinutes(), outerOval, innerOval, arcRect);
            }

            canvas.drawBitmap(faceBitmap, padding - strokeSize, padding - strokeSize, bitmapPaint);

        }

        private void initWatchFaceBitmap(Rect bounds, float stroke) {
            int width = (int) (bounds.width() - padding * 2 + stroke * 2);
            int height = (int) (bounds.height() - padding * 2 + stroke * 2);
            if (faceBitmap == null) {
                faceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerTimeZoneChangeReceiver();
                // Update time zone in case it changed while we weren't visible.
                updateTimeZone();

            } else {
                unregisterTimeZoneReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void updateTimeZone() {
            TimeZone defaultZone = TimeZone.getDefault();
            time.setTimeZone(defaultZone);
            time.setTimeInMillis(System.currentTimeMillis());
            hourDrawer.setTimeZone(defaultZone);
        }

        private void registerTimeZoneChangeReceiver() {
            if (!registeredTimeZoneReceiver) {
                registeredTimeZoneReceiver = true;
                IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                registerReceiver(timeZoneReceiver, filter);
            }
        }

        private void unregisterTimeZoneReceiver() {
            if (registeredTimeZoneReceiver) {
                registeredTimeZoneReceiver = false;
                unregisterReceiver(timeZoneReceiver);
            }
        }

        /**
         * Starts the {@link #engineHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            engineHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                engineHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #engineHandler} timer should be running. The timer should
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
                engineHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        public void updateEvent() {
            Optional<Event> upcomingEvent = eventsManager.getUpcomingEvent();
            if(upcomingEvent.isPresent()) {
                Event event = upcomingEvent.get();
                eventFormatter.setEvent(event);
                long delay = event.getStartDate() - System.currentTimeMillis();
                engineHandler.sendEmptyMessageDelayed(MSG_UPDATE_EVENT, delay);
            } else {
                eventFormatter.clearEvent();
            }
            invalidate();
        }
    }
}
