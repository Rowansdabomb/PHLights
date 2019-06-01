package com.philips.lighting.hue.demo.huequickstartapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;

import com.philips.lighting.hue.sdk.wrapper.HueLog;
import com.philips.lighting.hue.sdk.wrapper.Persistence;
import com.philips.lighting.hue.sdk.wrapper.utilities.InitSdk;

public class App extends Application {
    public static final String CHANNEL_ID = "PHLightsServiceChannel";

    static {
        // Load the huesdk native library before calling any SDK method
        System.loadLibrary("huesdk");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        InitSdk.setApplicationContext(getApplicationContext());

        // Configure the storage location and log level for the Hue SDK
        Persistence.setStorageLocation(getFilesDir().getAbsolutePath(), "HueQuickStart");
        HueLog.setConsoleLogLevel(HueLog.LogLevel.INFO);

        startService(new Intent(this, BackgroundService.class));
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "PHlights Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
