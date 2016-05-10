package com.macoscope.gcbwatchface;

import android.content.Context;
import android.os.PowerManager;

import com.eccyan.optional.Optional;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class LoadCalendarEventsTask {

    private static final String WAKE_LOCK_TAG = "GCBWatchFaceWakeLock";
    private CalendarUseCase calendarUseCase;

    private int calendarId;
    private EventsLoadedListener eventsLoadedListener;
    private Subscription subscription;

    private PowerManager.WakeLock mWakeLock;
    private PowerManager powerManager;

    public LoadCalendarEventsTask(Context context, EventsLoadedListener eventsLoadedListener) {
        this.eventsLoadedListener = eventsLoadedListener;
        this.calendarUseCase = new CalendarUseCase(context.getContentResolver());
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    public void setCalendarId(int calendarId) {
        this.calendarId = calendarId;
    }

    public void execute(int calendarId, long timeInterval, TimeUnit timeUnit) {
        calendarUseCase.logCalendars();
        unsubscribe();
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        mWakeLock.acquire();
        subscription = calendarUseCase.getEvents(calendarId, timeInterval, timeUnit)
                .observeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        releaseWakeLock();
                    }
                })
                .subscribe(new Action1<Optional<List<EventModel>>>() {
                    @Override
                    public void call(Optional<List<EventModel>> listOptional) {
                        if(listOptional.isPresent()){
                            eventsLoadedListener.onEventsLoaded(listOptional.get());
                        }else{
                            eventsLoadedListener.onNoEventsLoaded();
                        }
                        releaseWakeLock();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        releaseWakeLock();
                    }
                });
    }

    private void unsubscribe() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
        releaseWakeLock();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    public void cancel() {
        unsubscribe();
    }
}
