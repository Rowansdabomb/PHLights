package com.philips.lighting.hue.demo.huequickstartapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;
import static com.philips.lighting.hue.demo.huequickstartapp.App.CHANNEL_ID;

public class BackgroundService extends Service {
    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Intent OnIntent = new Intent(this, NotificationBroadcastReceiver.class);
        OnIntent.setAction("android.intent.action." + context.getString(R.string.TURN_ON_LIGHTS))
                .putExtra("PHLightsNotificationOnIntent", false);
        PendingIntent OnPendingIntent = PendingIntent.getBroadcast(this, 0, OnIntent, 0);

        Intent OffIntent = new Intent(this, NotificationBroadcastReceiver.class);
        OffIntent.setAction("android.intent.action." + context.getString(R.string.TURN_OFF_LIGHTS))
                .putExtra("PHLightsNotificationOffIntent", false);
        PendingIntent OffPendingIntent = PendingIntent.getBroadcast(this, 0, OffIntent, 0);

        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_layout);
        notificationLayout.setImageViewResource(R.id.notification_icon, R.drawable.ic_notification_icon);
        notificationLayout.setOnClickPendingIntent(R.id.on_button, OnPendingIntent);
        notificationLayout.setOnClickPendingIntent(R.id.off_button, OffPendingIntent);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(this.getString(R.string.app_name))
                .setContentText("content text")
                .setContent(notificationLayout)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(mainPendingIntent)
                .build();

        startForeground(1, notification);

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
