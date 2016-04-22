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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

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

    @Override
    public GCBWatchFaceEngine onCreateEngine() {
        return new GCBWatchFaceEngine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<GCBWatchFaceEngine> mWeakReference;

        public EngineHandler(GCBWatchFaceEngine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            GCBWatchFaceEngine engine = mWeakReference.get();
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
        //TODO move to resources
        private static final int COLOR_SOFT_BLUE = 0xFF6195e4;
        private static final int COLOR_GREEN_BLUE = 0xFF22daa8;
        private static final int COLOR_BLUSH = 0xFFea997d;
        private static final int COLOR_LIPSTICK = 0xFFe3325c;
        private static final int COLOR_WHITE = 0xFFFFFFFF;
        private static final int COLOR_NEUTRAL = 0xFF1a1a1a;
        private static final int COLOR_HOUR = 0xFFFFFFFF;

        private static final int PIECES_GAP = 8;

        private final int[] OVAL_GRADIENT = new int[]{COLOR_GREEN_BLUE, COLOR_SOFT_BLUE, COLOR_BLUSH,
                COLOR_LIPSTICK, COLOR_GREEN_BLUE};
        /**
         * Positions tweaked for best gradient position on canvas rotation
         */
        private final float[] OVAL_GRADIENT_POSITION = new float[]{0f, 0.495f, 0.495f, 0.995f, 0.995f};

        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mAmbient;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode. - TODO not yet handled
         */
        boolean mLowBitAmbient;
        boolean drawInDebugMode = false;

        private GregorianCalendar time;

        private Bitmap faceBitmap;
        private Canvas faceCanvas;

        private Paint bitmapPaint;
        private Paint handPaint;
        private Paint debugPaint;
        private Paint arcPaint;
        private Paint innerOvalPaint;
        private Paint gradientPaint;
        private TextPaint hourTextPaint;

        private RectF oval;
        private RectF innerOval;
        private RectF arcRect;

        private float strokeSize;
        private float innerStrokeSize;
        private int backgroundColor;
        private float padding;

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                time.setTimeZone(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
                time.setTime(new Date());
            }
        };
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setUpWatchFaceStyle();
            initTime();
            Context context = GCBWatchFace.this.getApplicationContext();
            initPaints(context);
            backgroundColor = ContextCompat.getColor(context, R.color.background);
            oval = new RectF();
            arcRect = new RectF();
            innerOval = new RectF();
            padding = getDimensionSize(R.dimen.face_padding);
        }

        private void setUpWatchFaceStyle() {
            setWatchFaceStyle(new WatchFaceStyle.Builder(GCBWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_PERSISTENT)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());
        }

        private void initTime() {
            time = new GregorianCalendar();
        }

        private void initPaints(Context context) {
            handPaint = new Paint();
            handPaint.setColor(ContextCompat.getColor(context, R.color.analog_hands));
            handPaint.setStrokeWidth(getDimensionSize(R.dimen.analog_hand_stroke));
            handPaint.setAntiAlias(true);
            handPaint.setStrokeCap(Paint.Cap.ROUND);

            gradientPaint = new Paint();
            // gradient set in onDraw
            strokeSize = getDimensionSize(R.dimen.outside_oval_stroke);
            gradientPaint.setStrokeWidth(strokeSize);
            gradientPaint.setAntiAlias(true);
            gradientPaint.setStyle(Paint.Style.STROKE);

            hourTextPaint = new TextPaint();
            hourTextPaint.setTextSize(getDimensionSize(R.dimen.hour_text_size));
            hourTextPaint.setColor(COLOR_HOUR);
            hourTextPaint.setAntiAlias(true);
            hourTextPaint.setTextAlign(Paint.Align.CENTER);

            arcPaint = new Paint();
            arcPaint.setColor(COLOR_NEUTRAL);
            arcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
            arcPaint.setAntiAlias(true);

            innerOvalPaint = new Paint();
            innerOvalPaint.setColor(COLOR_WHITE);
            innerStrokeSize = getDimensionSize(R.dimen.inner_oval_stroke);
            innerOvalPaint.setStrokeWidth(innerStrokeSize);
            innerOvalPaint.setAntiAlias(true);
            innerOvalPaint.setStyle(Paint.Style.STROKE);

            bitmapPaint = new Paint();
            bitmapPaint.setAntiAlias(true);
            bitmapPaint.setFilterBitmap(true);
            bitmapPaint.setDither(true);

            debugPaint = new Paint();
            debugPaint.setColor(Color.RED);
        }

        private float getDimensionSize(int id) {
            return getResources().getDimension(id);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
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
            handPaint.setAntiAlias(antiAliasOn);
            debugPaint.setAntiAlias(antiAliasOn);
            arcPaint.setAntiAlias(antiAliasOn);
            innerOvalPaint.setAntiAlias(antiAliasOn);
            gradientPaint.setAntiAlias(antiAliasOn);
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
                    drawInDebugMode = !drawInDebugMode;
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            time.setTime(new Date());

            canvas.drawColor(backgroundColor);

            float centerX = bounds.centerX();
            float centerY = bounds.centerY();

            int seconds = time.get(Calendar.SECOND);
            int minutes = time.get(Calendar.MINUTE);
            int hour = time.get(Calendar.HOUR_OF_DAY);

            if (drawInDebugMode) {
//                canvas.drawLine(0, centerY, bounds.right, centerY, debugPaint);
//                canvas.drawLine(centerX, 0, centerX, bounds.bottom, debugPaint);
                drawSecondsClockHand(canvas, centerX, centerY, seconds);
                drawWatchFace(canvas, bounds, strokeSize, seconds);
            } else {
                drawWatchFace(canvas, bounds, strokeSize, minutes);
            }

            drawMeetingIndicator(canvas, strokeSize);
            canvas.drawText(getHourToDisplay(time), centerX, centerY, hourTextPaint);
        }

        private void drawSecondsClockHand(Canvas canvas, float centerX, float centerY, int seconds) {
            float secLength = centerX - 30;
            float secRot = seconds / 30f * (float) Math.PI;
            float secX = (float) Math.sin(secRot) * secLength;
            float secY = (float) -Math.cos(secRot) * secLength;
            canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, handPaint);
        }

        private void drawMeetingIndicator(Canvas canvas, float stroke) {
            innerOval.set(oval.left + stroke, oval.top + stroke, oval.right - stroke,
                    oval.bottom - stroke);

            float piece = (float) (Math.PI * innerOval.width() / 12);
            float gap = PIECES_GAP;
            if (innerOvalPaint.getPathEffect() == null) {
                DashPathEffect dashPathEffect = new DashPathEffect(new float[]{piece - gap, gap}, 0);
                innerOvalPaint.setPathEffect(dashPathEffect);
            }

            float ovalRotation = (gap / 2 * 30) / piece;

            canvas.save();
            canvas.rotate(ovalRotation, innerOval.centerX(), innerOval.centerY());
            canvas.translate(padding - stroke + innerStrokeSize /2, padding - stroke - innerStrokeSize/2);
            canvas.drawOval(innerOval, innerOvalPaint);
            canvas.restore();
        }

        private void drawWatchFace(Canvas sourceCanvas, Rect bounds, float stroke, int minutes) {
            int width = (int) (bounds.width() - padding * 2 + stroke * 2);
            int height = (int) (bounds.height() - padding * 2 + stroke * 2);
            float centerX = width / 2;
            float centerY = height / 2;

            if (faceBitmap == null) {
                faceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                faceCanvas = new Canvas(faceBitmap);
                if (gradientPaint.getShader() == null) {
                    gradientPaint.setShader(new SweepGradient(centerX, centerY, OVAL_GRADIENT, OVAL_GRADIENT_POSITION));
                }
            }

            oval.set(stroke, stroke, width - stroke, height - stroke);
            float piece = (float) (Math.PI * oval.width() / 12);
            float gap = PIECES_GAP;
            if (gradientPaint.getPathEffect() == null) {
                DashPathEffect dashPathEffect = new DashPathEffect(new float[]{piece - gap, gap}, 0);
                gradientPaint.setPathEffect(dashPathEffect);
            }
            //count rotation to have gaps in dashed stroke on hours place
            float ovalRotation = (gap / 2 * 30) / piece - 90;

            faceCanvas.save();
            faceCanvas.rotate(ovalRotation, centerX, centerY);
            faceCanvas.drawOval(oval, gradientPaint);
            faceCanvas.restore();
            int angle;
            if (minutes > 30) {
                angle = (minutes * 6 / 30) * 30;
            } else {
                angle = -((60 - minutes) * 6 / 30) * 30;
            }

            arcRect = new RectF(oval.left - padding, oval.top - padding, oval.right + padding, oval.bottom
                    + padding);
            faceCanvas.drawArc(arcRect, -90, angle, true, arcPaint);

            sourceCanvas.drawBitmap(faceBitmap, padding - stroke, padding - stroke, bitmapPaint);

        }

        private String getHourToDisplay(GregorianCalendar calendar) {
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minutes = calendar.get(Calendar.MINUTE);
            if (minutes > 30) {
                calendar.add(Calendar.HOUR_OF_DAY, 1);
                hour = calendar.get(Calendar.HOUR_OF_DAY);
            }
            return String.format("%d", hour);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                time.setTimeZone(TimeZone.getDefault());
                time.setTime(new Date());
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            GCBWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            GCBWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
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
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
