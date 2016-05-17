package com.macoscope.gcbwtachface;

import android.content.ContentResolver;
import android.util.Pair;

import com.eccyan.optional.Optional;
import com.macoscpoe.gcbmodel.Event;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

public class CalendarUseCase {

    private CalendarRepository calendarRepository;

    private Func1 calendarsListToPreferenceArraysMapFunction =
            new Func1<Optional<List<CalendarModel>>, Optional<Pair<CharSequence[], CharSequence[]>>>() {
                @Override
                public Optional<Pair<CharSequence[], CharSequence[]>> call(Optional<List<CalendarModel>> listOptional) {
                    if (listOptional.isPresent() && !listOptional.get().isEmpty()) {
                        return getPreferenceListEntryValueArraysPair(listOptional.get());
                    } else {
                        return Optional.empty();
                    }
                }
            };

    public CalendarUseCase(ContentResolver contentResolver) {
        calendarRepository = new CalendarRepository(contentResolver);
    }

    public Observable<Optional<List<CalendarModel>>> getCalendars(final String account){
        return Observable.defer(new Func0<Observable<Optional<List<CalendarModel>>>>() {
            @Override
            public Observable<Optional<List<CalendarModel>>> call() {
                try {
                    List<CalendarModel> calendars = calendarRepository.getCalendars(account);
                    if (calendars.size() > 0) {
                        return Observable.just(Optional.of(calendars));
                    }
                } catch (Exception error) {
                    return Observable.error(error);
                }
                return Observable.just(Optional.<List<CalendarModel>>empty());
            }
        });
    }

    public Observable<Optional<Pair<CharSequence[], CharSequence[]>>> getCalendarsPreferenceList(String account){
        return getCalendars(account).map(calendarsListToPreferenceArraysMapFunction);
    }

    private Optional<Pair<CharSequence[], CharSequence[]>> getPreferenceListEntryValueArraysPair(List<CalendarModel>
                                                                                                         calendarEntries) {
        int size = calendarEntries.size();
        CharSequence[] entries = new CharSequence[size];
        CharSequence[] values = new CharSequence[size];
        for (int i = 0; i < calendarEntries.size(); i++) {
            CalendarModel calendarModel = calendarEntries.get(i);
            entries[i] = calendarModel.getDisplayName();
            values[i] = Long.toString(calendarModel.getId());
        }
        return Optional.of(new Pair<>(entries, values));
    }

}
