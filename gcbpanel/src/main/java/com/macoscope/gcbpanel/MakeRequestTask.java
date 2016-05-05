package com.macoscope.gcbpanel;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An asynchronous task that handles the Google Calendar API call.
 * Placing the API calls in their own task ensures the UI stays responsive.
 */
class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
    private CalendarActivity calendarActivity;
    private com.google.api.services.calendar.Calendar mService = null;
    private Exception mLastError = null;

    public MakeRequestTask(CalendarActivity calendarActivity, GoogleAccountCredential credential) {
        this.calendarActivity = calendarActivity;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Google Calendar API Android Quickstart")
                .build();
    }

    /**
     * Background task to call Google Calendar API.
     *
     * @param params no parameters needed for this task.
     */
    @Override
    protected List<String> doInBackground(Void... params) {
        try {
            return getDataFromApi();
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    /**
     * Fetch a list of the next 10 events from the specific calendar.
     *
     * @return List of Strings describing returned events.
     * @throws IOException
     */
    private List<String> getDataFromApi() throws IOException {
        // List the next 10 events from the primary calendar.
        DateTime now = new DateTime(System.currentTimeMillis());
        List<String> eventStrings = new ArrayList<>();
        mService.calendarList().list();
        Events events = mService.events().list(CalendarActivity.CALENDAR_ID)
                .setMaxResults(CalendarActivity.CALENDAR_MAX_EVENTS)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
        List<Event> items = events.getItems();

        for (Event event : items) {
            DateTime start = event.getStart().getDateTime();
            if (start == null) {
                // All-day events don't have start times, so just use
                // the start date.
                start = event.getStart().getDate();
            }
            eventStrings.add(
                    String.format("%s (%s)", event.getSummary(), start));
        }
        return eventStrings;
    }


    @Override
    protected void onPreExecute() {
//        calendarActivity.mOutputText.setText("");
        calendarActivity.mProgress.show();
    }

    @Override
    protected void onPostExecute(List<String> output) {
        calendarActivity.mProgress.hide();
        if (output == null || output.size() == 0) {
//            calendarActivity.mOutputText.setText("No results returned.");
        } else {
            output.add(0, "Data retrieved using the Google Calendar API:");
//            calendarActivity.mOutputText.setText(TextUtils.join("\n", output));
        }
    }

    @Override
    protected void onCancelled() {
        calendarActivity.mProgress.hide();
        if (mLastError != null) {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                calendarActivity.showGooglePlayServicesAvailabilityErrorDialog(
                        ((GooglePlayServicesAvailabilityIOException) mLastError)
                                .getConnectionStatusCode());
            } else if (mLastError instanceof UserRecoverableAuthIOException) {
                calendarActivity.startActivityForResult(
                        ((UserRecoverableAuthIOException) mLastError).getIntent(),
                        CalendarActivity.REQUEST_AUTHORIZATION);
            } else {
//                calendarActivity.mOutputText.setText("The following error occurred:\n"
//                        + mLastError.getMessage());
            }
        } else {
//            calendarActivity.mOutputText.setText("Request cancelled.");
        }
    }
}
