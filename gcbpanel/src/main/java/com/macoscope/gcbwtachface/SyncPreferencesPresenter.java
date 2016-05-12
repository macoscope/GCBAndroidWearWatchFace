package com.macoscope.gcbwtachface;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Pair;

import com.eccyan.optional.Optional;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.gson.Gson;
import com.patloew.rxwear.GoogleAPIConnectionException;
import com.patloew.rxwear.RxWear;

import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static android.app.Activity.RESULT_OK;

public class SyncPreferencesPresenter {

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int REQUEST_PERMISSION_READ_CALENDAR = 1004;

    private static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY};
    private static final CharSequence[] EMPTY_CHARS_ARRAY = new CharSequence[]{};

    private SyncPreferencesView syncPreferencesView;
    private GoogleAccountCredential googleAccountCredential;
    private Context context;
    private Preference accountPreference;
    private ListPreference calendarListPreference;
    private ListPreference syncFrequencyPreference;
    private CalendarUseCase calendarUseCase;
    //TODO handle unsubscribe
    private CompositeSubscription compositeSubscription;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private Preference.OnPreferenceChangeListener bindPreferenceSummaryToValueListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    String stringValue = value.toString();

                    if (preference instanceof ListPreference) {
                        // For list preferences, look up the correct display value in
                        // the preference's 'entries' list.
                        ListPreference listPreference = (ListPreference) preference;
                        int index = listPreference.findIndexOfValue(stringValue);

                        // Set the summary to reflect the new value.
                        preference.setSummary(
                                index >= 0
                                        ? listPreference.getEntries()[index]
                                        : null);


                    } else {
                        // For all other preferences, set the summary to the value's
                        // simple string representation.
                        preference.setSummary(stringValue);
                    }
                    return true;
                }
            };

    public SyncPreferencesPresenter(SyncPreferencesView syncPreferencesView, Context context,
                                    ListPreference syncFrequencyPreference, Preference accountPreference,
                                    ListPreference calendarListPreference) {
        this.syncPreferencesView = syncPreferencesView;
        this.context = context;
        this.accountPreference = accountPreference;
        this.calendarListPreference = calendarListPreference;
        this.syncFrequencyPreference = syncFrequencyPreference;
        this.compositeSubscription = new CompositeSubscription();
        RxWear.init(context);
        setBindPreferenceSummariesToValues();
        calendarListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String stringValue = newValue.toString();

                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
                if(index >= 0) {
                    long calendarId = Long.parseLong(listPreference.getEntryValues()[index].toString());
                    loadEvents(calendarId, 55);
                }

                return true;
            }
        });
        initCredentials();
        initCalendarUseCase();
        loadAvailableCalendarsIfPermissionsGranted();
        setAccountPreferenceClickListener(accountPreference);
    }

    private void loadEvents(long calendarId, long minutesInterval) {
        Subscription subscription = calendarUseCase.getEvents(calendarId, minutesInterval)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Optional<List<Event>>>() {
                    @Override
                    public void call(Optional<List<Event>> listOptional) {
                        showEvents(listOptional);
                        sendEvents(listOptional);
                    }
                });
        compositeSubscription.add(subscription);
    }


    private void showEvents(Optional<List<Event>> listOptional){
        if(listOptional.isPresent() && listOptional.get().size() > 0){
            String string = "";
            for(int i=0; i< listOptional.get().size(); i++){
                string += listOptional.get().get(i).getTitle()+ "\n";
            }
            syncPreferencesView.showMessage(string);
        } else {
            syncPreferencesView.showMessage("No events in 55min for selected calendar");
        }
    }

    private void sendEvents(Optional<List<Event>> listOptional){
        if(listOptional.isPresent() && listOptional.get().size() > 0){
            Gson gson = new Gson();
            String eventsGson = gson.toJson(listOptional.get());
            Subscription subscription = RxWear.Message.SendDataMap.toAllRemoteNodes("/eventsList")
                    .putString("eventsList", eventsGson)
                    .toObservable().subscribe(new Action1<Integer>() {
                @Override
                public void call(Integer integer) {
                    syncPreferencesView.showMessage("Events send");
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    if(throwable instanceof GoogleAPIConnectionException) {
                       syncPreferencesView.showMessage("Android Wear app is not installed");
                    } else {
                        syncPreferencesView.showMessage("Could not send message");
                    }
                }
            });
            compositeSubscription.add(subscription);
        }
    }

    private void setBindPreferenceSummariesToValues() {
        bindPreferenceSummaryToValue(syncFrequencyPreference);
        bindPreferenceSummaryToValue(accountPreference);
        bindPreferenceSummaryToValue(calendarListPreference);
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #bindPreferenceSummaryToValueListener
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(bindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        bindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private void chooseAccountIfGooglePlayServicesAvailable() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else {
            chooseAccount();
        }
    }

    private void setAccountPreferenceClickListener(Preference accountPreference) {
        accountPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                chooseAccountIfGooglePlayServicesAvailable();
                return true;
            }
        });
    }

    private void initCredentials() {
        googleAccountCredential = GoogleAccountCredential.usingOAuth2(
                context.getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        String accountName = PreferenceManager.getDefaultSharedPreferences(accountPreference.getContext())
                .getString(accountPreference.getKey(), null);
        if (accountName != null) {
            googleAccountCredential.setSelectedAccountName(accountName);
        }
    }

    private void initCalendarUseCase() {
        calendarUseCase = new CalendarUseCase(context.getContentResolver());
    }


    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(context);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(context);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            syncPreferencesView.showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(context, Manifest.permission.GET_ACCOUNTS)) {
            syncPreferencesView.startRequestAccountActivityForResult(
                    googleAccountCredential.newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER);
        } else {
            EasyPermissions.requestPermissions(
                    context,
                    context.getString(R.string.account_permissions),
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    syncPreferencesView.showMessage(R.string.error_missing_play_services);
                } else {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = PreferenceManager
                                .getDefaultSharedPreferences(accountPreference.getContext());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(accountPreference.getKey(), accountName);
                        editor.commit();
                        accountPreference.setSummary(accountName);
                        googleAccountCredential.setSelectedAccountName(accountName);
                        loadAvailableCalendarsIfPermissionsGranted();
                    }
                }
                break;
//            case REQUEST_AUTHORIZATION:
//                if (resultCode == RESULT_OK) {
//                    chooseAccountIfGooglePlayServicesAvailable();
//                }
//                break;
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_READ_CALENDAR)
    private void loadAvailableCalendarsIfPermissionsGranted() {
        if (EasyPermissions.hasPermissions(context, Manifest.permission.READ_CALENDAR)) {
            loadAvailableCalendars();
        } else {
            EasyPermissions.requestPermissions(
                    context,
                    context.getString(R.string.read_calendar_permissions),
                    REQUEST_PERMISSION_READ_CALENDAR,
                    Manifest.permission.READ_CALENDAR);
        }
    }


    private void loadAvailableCalendars() {
        enableCalendarsList(false);
        if (!TextUtils.isEmpty(googleAccountCredential.getSelectedAccountName())) {
            Subscription subscription = calendarUseCase.getCalendarsPreferenceList(googleAccountCredential.getSelectedAccountName())
                    .subscribeOn(Schedulers
                            .io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Optional<Pair<CharSequence[], CharSequence[]>>>() {
                        @Override
                        public void call(Optional<Pair<CharSequence[], CharSequence[]>> pairOptional) {
                            if (pairOptional.isPresent()) {
                                bindCalendarsToPreferenceList(pairOptional.get());
                            } else {
                                handleNoCalendarForAccount();
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            throwable.printStackTrace();
                            handleFetchCalendarsError();
                        }
                    });
            compositeSubscription.add(subscription);
        } else {
            clearCalendarsPreference();
        }
    }

    private void bindCalendarsToPreferenceList(Pair<CharSequence[], CharSequence[]> entryValuePair) {
        setCalendarsPreference(entryValuePair.first, entryValuePair.second);
        enableCalendarsList(true);
    }

    private void handleNoCalendarForAccount() {
        syncPreferencesView.showMessage(R.string.error_zero_calendars);
        clearCalendarsPreference();
    }

    private void handleFetchCalendarsError() {
        syncPreferencesView.showMessage(R.string.error_load_calendars);
        clearCalendarsPreference();
    }

    private void enableCalendarsList(boolean enable) {
        calendarListPreference.setEnabled(enable);
        calendarListPreference.setSelectable(enable);
    }

    private void clearCalendarsPreference() {
        setCalendarsPreference(EMPTY_CHARS_ARRAY, EMPTY_CHARS_ARRAY);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(calendarListPreference
                .getContext())
                .edit();
        editor.remove(calendarListPreference.getKey()).commit();
    }

    private void setCalendarsPreference(CharSequence[] entries, CharSequence[] values) {
        calendarListPreference.setSummary(R.string.no_calendar_selected);
        calendarListPreference.setEntries(entries);
        calendarListPreference.setEntryValues(values);
    }
}
