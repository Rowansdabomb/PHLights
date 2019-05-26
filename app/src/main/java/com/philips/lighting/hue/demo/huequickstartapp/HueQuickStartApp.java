package com.philips.lighting.hue.demo.huequickstartapp;

import android.app.Application;
import android.content.Intent;

import com.philips.lighting.hue.sdk.wrapper.HueLog;
import com.philips.lighting.hue.sdk.wrapper.Persistence;
import com.philips.lighting.hue.sdk.wrapper.utilities.InitSdk;

public class HueQuickStartApp extends Application {

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
    }
}
