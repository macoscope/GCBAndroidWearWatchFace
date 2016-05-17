package com.macoscope.gcbwtachface.service;

import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.gson.Gson;
import com.macoscope.gcbwtachface.CalendarRepository;
import com.macoscpoe.gcbmodel.CommunicationConfig;
import com.macoscpoe.gcbmodel.Event;
import com.patloew.rxwear.GoogleAPIConnectionException;
import com.patloew.rxwear.RxWear;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.functions.Action1;

public class SyncJob extends Job {
    public static final String TAG = "gcb_watchface_sync_job";
    public static final String TAG_AD_HOC = "gcb_watchface_sync_job_ad_hoc";
    public static final String KEY_CALENDAR_ID = "calendarId";
    public static final String KEY_MINUTES = "minutes";
    private Gson gson;
    private CalendarRepository calendarRepository;

    @NonNull
    @Override
    protected Result onRunJob(Params params) {

        PersistableBundleCompat extras = params.getExtras();
        long calendarId = extras.getLong(KEY_CALENDAR_ID, -1);
        long minutes = extras.getLong(KEY_MINUTES, -1);

        if (!isCanceled()) {
            calendarRepository = new CalendarRepository(getContext().getContentResolver());
            List<Event> events = calendarId == -1
                    ? new ArrayList<Event>()
                    : calendarRepository.getEvents(calendarId, minutes, TimeUnit.MINUTES);
            sendEvents(events);
        }
        return Result.SUCCESS;
    }

    private void sendEvents(List<Event> eventList) {
        RxWear.init(getContext());
        gson = new Gson();
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
