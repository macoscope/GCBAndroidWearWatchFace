package com.macoscope.gcbwatchface.service;

import android.support.annotation.NonNull;
import android.util.Log;

import com.eccyan.optional.Optional;
import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.gson.Gson;
import com.macoscope.gcbwatchface.CalendarRepository;
import com.macoscope.gcbmodel.CommunicationConfig;
import com.macoscope.gcbmodel.Event;
import com.patloew.rxwear.GoogleAPIConnectionException;
import com.patloew.rxwear.RxWear;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.functions.Action1;

class SyncJob extends Job {
    private static final int MINUTES_LIMIT_FOR_UPCOMING_EVENTS = 55;
    public static final String TAG = "gcb_watchface_sync_job";
    public static final String TAG_AD_HOC = "gcb_watchface_sync_job_ad_hoc";
    public static final String KEY_CALENDAR_ID = "calendarId";

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        PersistableBundleCompat extras = params.getExtras();
        long calendarId = extras.getLong(KEY_CALENDAR_ID, -1);

        if (!isCanceled()) {
            CalendarRepository calendarRepository = new CalendarRepository(getContext().getContentResolver());
            if (calendarId != -1) {
                Optional<List<Event>> eventsOptional = calendarRepository.getEvents(calendarId,
                        MINUTES_LIMIT_FOR_UPCOMING_EVENTS, TimeUnit.MINUTES);
                if (eventsOptional.isPresent()) {
                    sendEvents(eventsOptional.get());
                    return Result.SUCCESS;
                }
            }
            sendEvents(new ArrayList<Event>());
        }
        return Result.SUCCESS;
    }

    private void sendEvents(List<Event> eventList) {
        RxWear.init(getContext());
        Gson gson = new Gson();
        final String eventsJson = gson.toJson(eventList);
        RxWear.Message.SendDataMap.toAllRemoteNodes(CommunicationConfig.EVENTS_LIST_PATH)
                .putString(CommunicationConfig.EVENTS_LIST_DATA_KEY, eventsJson)
                .toObservable().subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                Log.d(getParams().getTag(), "JSON send to watch: " + eventsJson);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                if (throwable instanceof GoogleAPIConnectionException) {
                    Log.v(TAG, "Android Wear app is not installed");
                } else {
                    Log.v(TAG, "Update events error", throwable);
                }
            }
        });
    }


}
