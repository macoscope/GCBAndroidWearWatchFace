package com.macoscope.gcbwatchface;


import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.eccyan.optional.Optional;
import com.google.android.gms.wearable.DataMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.macoscope.gcbmodel.CommunicationConfig;
import com.macoscope.gcbmodel.Event;
import com.patloew.rxwear.RxWear;
import com.patloew.rxwear.transformers.MessageEventGetDataMap;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

public class EventsManager {
    private CompositeSubscription compositeSubscription;
    private Gson gson;
    private Type eventListType = new TypeToken<List<Event>>() {}.getType();
    private List<Event> events;

    private Func1 dataMapToEventsListFunction = new Func1<DataMap, List<Event>>() {
        @Override
        public List<Event> call(DataMap dataMap) {
            String json = dataMap.getString(CommunicationConfig.EVENTS_LIST_DATA_KEY);
            if (TextUtils.isEmpty(json)) {
                return new ArrayList<>();
            } else {
                return gson.fromJson(json, eventListType);
            }
        }
    };

    private static Comparator<Event> eventComparator = new Comparator<Event>() {
        @Override
        public int compare(Event lhs, Event rhs) {
            if(lhs.getStartDate() < rhs.getStartDate()){
                return -1;
            } else if(lhs.getStartDate() > rhs.getStartDate()){
                return 1;
            } else {
                return 0;
            }
        }
    };
    @VisibleForTesting
    private EventsManager(){}

    public EventsManager(Context context) {
        RxWear.init(context);
        this.gson = new Gson();
    }

    public void registerMessageListener(final EventsListChangeListener listener) {
        unregisterMessageListener();
        compositeSubscription = new CompositeSubscription();
        Subscription subscription = RxWear.Message.listen()
                .compose(MessageEventGetDataMap.filterByPath(CommunicationConfig.EVENTS_LIST_PATH))
                .map(dataMapToEventsListFunction)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Event>>() {
                    @Override
                    public void call(List<Event> events) {
                        Collections.sort(events, eventComparator);
                        setEvents(events);
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

    private void setEvents(List<Event> events) {
        this.events = events;
    }

    public void unregisterMessageListener() {
        if (compositeSubscription != null) {
            compositeSubscription.unsubscribe();
        }
    }

    public Optional<Event> getUpcomingEvent(long currentTime) {
        if (events == null || events.size() == 0) {
            return Optional.empty();
        } else {
            Event event = events.get(0);
            if (event.isValidRelativeTo(currentTime)) {
                return Optional.of(event);
            } else {
                while (hasValidEvent(currentTime)){
                    events.remove(0);
                }
                if(events.size() > 0){
                    return Optional.of(events.get(0));
                } else {
                    return Optional.empty();
                }
            }
        }
    }

    private boolean hasValidEvent(long currentTime){
        return events.size() > 0 && !events.get(0).isValidRelativeTo(currentTime);
    }
}
