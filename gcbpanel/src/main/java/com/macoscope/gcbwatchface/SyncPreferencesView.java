package com.macoscope.gcbwatchface;

import android.content.Intent;

public interface SyncPreferencesView {
    void showGooglePlayServicesAvailabilityErrorDialog(int connectionStatusCode);

    void showMessage(String message);

    void showMessage(int resourceId);

    void startRequestAccountActivityForResult(Intent intent, int requestCode);

    void requestEasyPermissions(int requestCode, String permission, int messageResourceId);
}
