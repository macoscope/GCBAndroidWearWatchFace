package com.macoscope.gcbpanel;

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

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

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
        setBindPreferenceSummariesToValues();
        initCredentials();
        initCalendarUseCase();
        loadAvailableCalendarIds();
        setAccountPreferenceClickListener(accountPreference);
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
        calendarUseCase = new CalendarUseCase(googleAccountCredential);
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
                        loadAvailableCalendarIds();
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

    private void loadAvailableCalendarIds() {
        enableCalendarsList(false);
        if (!TextUtils.isEmpty(googleAccountCredential.getSelectedAccountName())) {
            Subscription subscription = calendarUseCase.getPreferenceListCalendarsArrays().subscribeOn(Schedulers.io())
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
