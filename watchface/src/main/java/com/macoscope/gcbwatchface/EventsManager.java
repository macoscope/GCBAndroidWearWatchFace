package com.macoscope.gcbwatchface;


import android.content.Context;

import com.eccyan.optional.Optional;
import com.google.android.gms.wearable.DataMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.macoscpoe.gcbmodel.CommunicationConfig;
import com.macoscpoe.gcbmodel.Event;
import com.patloew.rxwear.RxWear;
import com.patloew.rxwear.transformers.MessageEventGetDataMap;

import java.lang.reflect.Type;
import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

public class EventsManager {
    private CompositeSubscription compositeSubscription;
    private Gson gson;
    private Type eventListType = new TypeToken<List<Event>>(){}.getType();
    private List<Event> events;

    public EventsManager(Context context) {
        RxWear.init(context);
        this.gson = new Gson();
    }

    public void registerMessageListener(final EventsListChangeListener listener) {
        unregisterMessageListener();
        compositeSubscription = new CompositeSubscription();
        Subscription subscription = RxWear.Message.listen()
                .compose(MessageEventGetDataMap.filterByPath(CommunicationConfig.EVENTS_LIST_PATH))
                .map(new Func1<DataMap, List<Event>>() {
                    @Override
                    public List<Event> call(DataMap dataMap) {
                        String json = dataMap.getString(CommunicationConfig.EVENTS_LIST_DATA_KEY);
                        return gson.fromJson(json, eventListType);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Event>>() {
                    @Override
                    public void call(List<Event> events) {
                        eventsLoaded(events);
                        listener.onEventsListChanged();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        listener.onEventsLoadFailure();
                    }
                });
        compositeSubscription.add(subscription);
    }

    private void eventsLoaded(List<Event> events) {
        this.events = events;
    }

    public void unregisterMessageListener() {
        if (compositeSubscription != null) {
            compositeSubscription.unsubscribe();
        }
    }

    public Optional<Event> getUpcomingEvent() {
        if (events == null || events.size() == 0) {
            return Optional.empty();
        } else {
            Event event = events.get(0);
            if (event.isValid()) {
                return Optional.of(event);
            } else {
                events.remove(0);
                return getUpcomingEvent();
            }
        }
    }
}
