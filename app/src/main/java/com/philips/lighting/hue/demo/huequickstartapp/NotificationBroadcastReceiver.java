package com.philips.lighting.hue.demo.huequickstartapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "BroadcastReceiver";
    private MainActivity mainActivity = null;

    public NotificationBroadcastReceiver() {

    }

    public NotificationBroadcastReceiver(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, intent.getAction());
        if (intent.getAction() == "android.intent.action.TURN_ON_LIGHTS") {
            context.sendBroadcast(new Intent("TURN_ON_LIGHTS"));
        } else if (intent.getAction() == "android.intent.action.TURN_OFF_LIGHTS") {
            context.sendBroadcast(new Intent("TURN_OFF_LIGHTS"));
        }
    }
}
