/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.macoscpoe.gcbwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.text.format.Time;
import android.view.SurfaceHolder;

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
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<GCBWatchFace.Engine> mWeakReference;

        public EngineHandler(GCBWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            GCBWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private static final int COLOR_SOFT_BLUE =  0xFF6195e4;
        private static final int COLOR_GREEN_BLUE = 0xFF22daa8;
        private static final int COLOR_BLUSH =      0xFFea997d;
        private static final int COLOR_LIPSTICK =   0xFFe3325c;
        private static final int COLOR_WHITE =      0xFFFFFFFF;
        private static final int COLOR_NEUTRAL =    0xFF1a1a1a;
        private static final int COLOR_HOUR =       0xFFFFFFFF;

        private final int[] OVAL_GRADIENT = new int[]{COLOR_GREEN_BLUE, COLOR_SOFT_BLUE, COLOR_BLUSH,
                COLOR_LIPSTICK};

        private final float[] OVAL_GRADIENT_POSITION = new float[]{0f, 0.49f, 0.5f, 1f};

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mHandPaint;
        boolean mAmbient;
        GregorianCalendar time;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                time.setTimeZone(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
                time.setTime(new Date());
            }
        };

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        private Paint debugPaint;
        private RectF oval;
        private Paint ovalPaint;
        private Paint arcPaint;
        private RectF innerOval;
        private Paint innerOvalPaint;
        private int eventMinutes;
        private Paint gradientPaint;
        private TextPaint hourTextPaint;
        private RectF maskArc;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(GCBWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            Resources resources = GCBWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mHandPaint = new Paint();
            mHandPaint.setColor(resources.getColor(R.color.analog_hands));
            mHandPaint.setStrokeWidth(getDimensionSize(R.dimen.analog_hand_stroke));
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);

            gradientPaint = new Paint();
            // gradient set in onDraw
            gradientPaint.setStrokeWidth(getDimensionSize(R.dimen.outside_oval_stroke));
            gradientPaint.setAntiAlias(true);
            gradientPaint.setStyle(Paint.Style.STROKE);

            hourTextPaint = new TextPaint();
            hourTextPaint.setTextSize(getDimensionSize(R.dimen.hour_text_size));
            hourTextPaint.setColor(COLOR_HOUR);
            hourTextPaint.setAntiAlias(true);
            hourTextPaint.setTextAlign(Paint.Align.CENTER);

            arcPaint = new Paint();
            arcPaint.setColor(COLOR_NEUTRAL);

            innerOvalPaint = new Paint();
            innerOvalPaint.setColor(COLOR_WHITE);
            innerOvalPaint.setStrokeWidth(getDimensionSize(R.dimen.inner_oval_stroke));

            debugPaint = new Paint();
            debugPaint.setColor(Color.RED);

            oval = new RectF();
            maskArc = new RectF();
            innerOval = new RectF();

            time = new GregorianCalendar();
        }

        private float getDimensionSize(int id){
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
                    mHandPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        boolean drawDebugGrid = true;

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
//                    mTapCount++;
//                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
//                            R.color.background : R.color.background2));
                    drawDebugGrid = !drawDebugGrid;
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
//            mTime.setToNow();
            time.setTime(new Date());
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);
            }

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = bounds.centerX();
            float centerY = bounds.centerY();

            int seconds = time.get(Calendar.SECOND);
            float secRot = seconds / 30f * (float) Math.PI;
            int minutes = time.get(Calendar.MINUTE);
            int hour = time.get(Calendar.HOUR_OF_DAY);
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((hour + (minutes / 60f)) / 6f) * (float) Math.PI;
            float arcAngle = seconds * 6;


            float secLength = centerX - 20;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            if (!mAmbient) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mHandPaint);
            }

            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
//            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mHandPaint);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
//            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHandPaint);

            float padding = 30;
            oval.set(bounds.left + padding, bounds.top + padding, bounds.right - padding, bounds.bottom - padding);
            float arc = (float) (Math.PI * oval.width() / 12);
            float gap = 8;
            DashPathEffect dashPathEffect = new DashPathEffect(new float[]{arc - gap, gap}, 1);
//            ovalPaint.setPathEffect(dashPathEffect);
            innerOvalPaint.setPathEffect(dashPathEffect);
            gradientPaint.setPathEffect(dashPathEffect);
            //count rotation to have gaps in dashed stroke on hours
            float ovalRotation = (gap / 2 * 30) / arc - 90;
            if (gradientPaint.getShader() == null) {
                gradientPaint.setShader(new SweepGradient(centerX, centerY, OVAL_GRADIENT, OVAL_GRADIENT_POSITION));
            }

            canvas.save();
            canvas.rotate(ovalRotation, centerX, centerY);
            canvas.drawOval(oval, gradientPaint);
            canvas.restore();

            int angle = (int) (arcAngle / 30) * 30;
            canvas.drawArc(oval, -90, angle, true, arcPaint);

            if (drawDebugGrid) {
                canvas.drawLine(0, centerY, bounds.right, centerY, debugPaint);
                canvas.drawLine(centerX, 0, centerX, bounds.bottom, debugPaint);
            }

            canvas.drawText(getHourToDisplay(time), centerX, centerY, hourTextPaint);
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
