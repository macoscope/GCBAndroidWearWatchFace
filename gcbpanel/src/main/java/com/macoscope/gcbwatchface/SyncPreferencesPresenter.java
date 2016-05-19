package com.macoscope.gcbwatchface;

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
import com.macoscope.gcbwatchface.service.SyncJobScheduler;

import java.util.Arrays;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static android.app.Activity.RESULT_OK;

public class SyncPreferencesPresenter {

    static final int REQUEST_ACCOUNT_PICKER = 100;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 101;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 102;
    static final int REQUEST_PERMISSION_READ_CALENDAR = 103;

    private static final long DEFAULT_SYNC_INTERVAL_MINUTES = 55;

    private static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY};
    private static final CharSequence[] EMPTY_CHARS_ARRAY = new CharSequence[]{};

    private SyncPreferencesView syncPreferencesView;
    private GoogleAccountCredential googleAccountCredential;
    private Context context;
    private Preference accountPreference;
    private ListPreference calendarListPreference;
    private ListPreference syncFrequencyPreference;
    private CalendarUseCase calendarUseCase;
    private CompositeSubscription compositeSubscription;
    private SyncJobScheduler syncJobScheduler;
    private SharedPreferences sharedPreferences;

    private String noneSelection;

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
                        ListPreference listPreference = (ListPreference) preference;
                        setSummaryFromSelectedValue(listPreference, stringValue, noneSelection);
                    } else {
                        preference.setSummary(stringValue);
                    }
                    return true;
                }
            };

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    String calendarPreferenceKey = calendarListPreference.getKey();
                    String syncFrequencyPreferenceKey = syncFrequencyPreference.getKey();
                    if (calendarPreferenceKey.equals(key) || syncFrequencyPreferenceKey.equals(key)) {

                        String calendarId = sharedPreferences.getString(calendarPreferenceKey, "");
                        String syncFrequency = sharedPreferences.getString(syncFrequencyPreferenceKey, "");

                        scheduleUpdateJob(calendarId, syncFrequency);
                    } else if (accountPreference.getKey().equals(key)) {
                        clearCalendarsPreference();
                    }
                }
            };

    public SyncPreferencesPresenter(SyncPreferencesView syncPreferencesView, Context context,
                                    final ListPreference syncFrequencyPreference, final Preference accountPreference,
                                    final ListPreference calendarListPreference, Preference syncNowPreference) {
        this.syncPreferencesView = syncPreferencesView;
        this.context = context;
        this.accountPreference = accountPreference;
        this.calendarListPreference = calendarListPreference;
        this.syncFrequencyPreference = syncFrequencyPreference;
        this.compositeSubscription = new CompositeSubscription();
        this.syncJobScheduler = new SyncJobScheduler();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.noneSelection = context.getString(R.string.none_selected);
        setBindPreferenceSummariesToValues();
        setSharedPreferenceChangeListener();
        initCredentials();
        initCalendarUseCase();
        loadAvailableCalendarsIfPermissionsGranted(true);
        setAccountPreferenceClickListener(accountPreference);
        bindClickListenerForRefresh(syncNowPreference);
    }

    private void bindClickListenerForRefresh(Preference preference) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                forceRefreshIfParametersStored();
                return true;
            }
        });
    }

    private void forceRefreshIfParametersStored() {
        String calendarPreferenceKey = calendarListPreference.getKey();
        String syncFrequencyPreferenceKey = syncFrequencyPreference.getKey();
        if (sharedPreferences.contains(calendarPreferenceKey) && sharedPreferences.contains(syncFrequencyPreferenceKey)) {
            String calendarId = sharedPreferences.getString(calendarPreferenceKey, "");
            String syncFrequency = sharedPreferences.getString(syncFrequencyPreferenceKey, "");
            scheduleUpdateJob(calendarId, syncFrequency);
        } else {
            syncPreferencesView.showMessage(R.string.pref_sync_now_error);
        }
    }

    public void onDestroy() {
        compositeSubscription.unsubscribe();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    private void scheduleUpdateJob(String calendarIdString, String syncFrequencyString) {
        long calendarId = -1;
        long syncFrequency = DEFAULT_SYNC_INTERVAL_MINUTES;

        try {
            calendarId = Long.parseLong(calendarIdString);
            syncFrequency = Long.parseLong(syncFrequencyString);
        } catch (NumberFormatException numberFormatException) {
            // Do nothing, use default values it this case. Watch will receive empty events list because of empty
            // events list for selected account.
        }

        syncJobScheduler.scheduleNewSyncJob(calendarId, syncFrequency);
    }

    private void cancelScheduledJobs() {
        syncJobScheduler.cancelAllScheduledJobs();
    }

    private void setBindPreferenceSummariesToValues() {
        bindPreferenceSummaryToValue(syncFrequencyPreference);
        bindPreferenceSummaryToValue(accountPreference);
        bindPreferenceSummaryToValue(calendarListPreference);
    }

    private void setSharedPreferenceChangeListener() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
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
                sharedPreferences.getString(preference.getKey(), ""));
    }

    private void setSummaryFromSelectedValue(ListPreference listPreference, CharSequence newValue, CharSequence empty) {
        String value = newValue.toString();
        int index = listPreference.findIndexOfValue(value);
        // Set the summary to reflect the new value.
        listPreference.setSummary(index >= 0 ? listPreference.getEntries()[index] : empty);
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
        String accountName = sharedPreferences.getString(accountPreference.getKey(), null);
        if (accountName != null) {
            googleAccountCredential.setSelectedAccountName(accountName);
        }
    }

    private void restoreCalendarsSummary() {
        String calendarListKey = calendarListPreference.getKey();
        if (sharedPreferences.contains(calendarListKey)) {
            setSummaryFromSelectedValue(calendarListPreference, sharedPreferences.getString(calendarListKey, ""),
                    noneSelection);
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
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(accountPreference.getKey(), accountName);
                        editor.commit();
                        accountPreference.setSummary(accountName);
                        googleAccountCredential.setSelectedAccountName(accountName);
                        loadAvailableCalendarsIfPermissionsGranted(false);
                    }
                }
                break;
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_READ_CALENDAR)
    private void loadAvailableCalendarsIfPermissionsGranted(boolean initial) {
        if (EasyPermissions.hasPermissions(context, Manifest.permission.READ_CALENDAR)) {
            loadAvailableCalendars(initial);
        } else {
            EasyPermissions.requestPermissions(
                    context,
                    context.getString(R.string.read_calendar_permissions),
                    REQUEST_PERMISSION_READ_CALENDAR,
                    Manifest.permission.READ_CALENDAR);
        }
    }


    private void loadAvailableCalendars(final boolean initial) {
        enableCalendarsList(false);
        if (!TextUtils.isEmpty(googleAccountCredential.getSelectedAccountName())) {
            Subscription subscription = calendarUseCase
                    .getCalendarsPreferenceList(googleAccountCredential.getSelectedAccountName())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Optional<Pair<CharSequence[], CharSequence[]>>>() {
                        @Override
                        public void call(Optional<Pair<CharSequence[], CharSequence[]>> pairOptional) {
                            if (pairOptional.isPresent()) {
                                bindCalendarsToPreferenceList(pairOptional.get());
                                if(initial) {
                                    restoreCalendarsSummary();
                                }
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
        cancelScheduledJobs();
    }

    private void handleFetchCalendarsError() {
        syncPreferencesView.showMessage(R.string.error_load_calendars);
        clearCalendarsPreference();
        cancelScheduledJobs();
    }

    private void enableCalendarsList(boolean enable) {
        calendarListPreference.setEnabled(enable);
        calendarListPreference.setSelectable(enable);
    }

    private void clearCalendarsPreference() {
        setCalendarsPreference(EMPTY_CHARS_ARRAY, EMPTY_CHARS_ARRAY);
        calendarListPreference.setSummary(R.string.none_selected);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(calendarListPreference.getKey()).commit();
    }

    private void setCalendarsPreference(CharSequence[] entries, CharSequence[] values) {
        calendarListPreference.setEntries(entries);
        calendarListPreference.setEntryValues(values);
    }
}
