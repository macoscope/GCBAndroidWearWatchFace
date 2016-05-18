package com.macoscope.gcbwatchface.service;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import java.util.concurrent.TimeUnit;

public class SyncJobScheduler {

    private static final long EXECUTION_WINDOW_START = 1l;
    private static final long EXECUTION_WINDOW_END = 1000l;

    public void scheduleNewSyncJob(long calendarId, long intervalInMinutes) {
        PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putLong(SyncJob.KEY_CALENDAR_ID, calendarId);
        //Send data now
        scheduleSendDataOnceNow(extras);
        //Schedule periodic updates
        scheduleSendDataPeriodic(extras, intervalInMinutes);
    }

    private void scheduleSendDataOnceNow(PersistableBundleCompat extras) {
        new JobRequest.Builder(SyncJob.TAG_AD_HOC)
                .setExecutionWindow(EXECUTION_WINDOW_START, EXECUTION_WINDOW_END)
                .setExtras(extras)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    private void scheduleSendDataPeriodic(PersistableBundleCompat extras, long intervalInMinutes) {
        new JobRequest.Builder(SyncJob.TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(intervalInMinutes))
                .setPersisted(true)
                .setExtras(extras)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    public void cancelAllScheduledJobs() {
        JobManager.instance().cancelAllForTag(SyncJob.TAG_AD_HOC);
        JobManager.instance().cancelAllForTag(SyncJob.TAG);
    }

}
