package com.macoscope.gcbwatchface;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.Snackbar;

import com.google.android.gms.common.GoogleApiAvailability;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class DataSyncPreferenceFragment extends PreferenceFragment implements SyncPreferencesView,
        EasyPermissions.PermissionCallbacks {

    private SyncPreferencesPresenter syncPreferencesPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_data_sync);
        ListPreference syncFrequency = (ListPreference) findPreference(getString(R.string.pref_key_sync_frequency));
        Preference accountPreference = findPreference(getString(R.string.pref_key_sync_account));
        ListPreference calendarPreferenceList = (ListPreference) findPreference(getString(R.string.pref_key_sync_calendar));
        syncPreferencesPresenter = new SyncPreferencesPresenter(this, getActivity(), syncFrequency, accountPreference,
                calendarPreferenceList, findPreference(getString(R.string.pref_key_sync_now)));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        syncPreferencesPresenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        syncPreferencesPresenter.onDestroy();
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    public void showGooglePlayServicesAvailabilityErrorDialog(int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                getActivity(),
                connectionStatusCode,
                SyncPreferencesPresenter.REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    public void showMessage(String message) {
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void showMessage(int resourceId) {
        showMessage(getString(resourceId));
    }


    @Override
    public void startRequestAccountActivityForResult(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void requestEasyPermissions(int requestCode, String permission, int messageResourceId) {
        EasyPermissions.requestPermissions(this, getString(messageResourceId), requestCode, permission);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        syncPreferencesPresenter.onPermissionsGranted(requestCode, perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        syncPreferencesPresenter.onPermissionsDenied(requestCode, perms);
    }


}
