package com.macoscope.gcbpanel;

import android.content.Intent;

public interface SyncPreferencesView {
    void showGooglePlayServicesAvailabilityErrorDialog(int connectionStatusCode);
    void showMessage(String message);
    void startRequestAccountActivityForResult(Intent intent, int requestCode);
}
