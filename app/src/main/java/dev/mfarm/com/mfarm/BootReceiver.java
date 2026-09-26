package dev.mfarm.com.mfarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Restores vaccination alarms after reboot, timezone changes, and app updates.
 * Android drops inexact/exact alarms across these events.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            AlarmScheduler.reschedulePending(context.getApplicationContext());
        }
    }
}
