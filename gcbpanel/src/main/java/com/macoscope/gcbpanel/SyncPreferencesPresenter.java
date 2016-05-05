package com.macoscope.gcbpanel;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;

import java.util.Arrays;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static android.app.Activity.RESULT_OK;

public class SyncPreferencesPresenter {

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;


    static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY};

    private SyncPreferencesView syncPreferencesView;
    private GoogleAccountCredential googleAccountCredential;
    private Context context;
    private Preference accountPreference;

    public SyncPreferencesPresenter(SyncPreferencesView syncPreferencesView, Context context, Preference accountPreference) {
        this.syncPreferencesView = syncPreferencesView;
        this.context = context;
        this.accountPreference = accountPreference;
        initCredentials();
        setAccountPreferenceClickListener(accountPreference);
    }

    private void chooseAccountIfGooglePlayServicesAvailable() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else {
            chooseAccount();
        }
    }

    private void setAccountPreferenceClickListener(Preference accountPreference){
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
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    context,
                    "This app needs to access your Google account.",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    syncPreferencesView.showMessage(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
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
}
