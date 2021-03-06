package com.philips.lighting.hue.demo.huequickstartapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import com.marcinmoskala.arcseekbar.ArcSeekBar;
import com.marcinmoskala.arcseekbar.ProgressListener;
import com.philips.lighting.hue.demo.huequickstartapp.NotificationBroadcastReceiver;

import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnection;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedEvent;
import com.philips.lighting.hue.sdk.wrapper.connection.ConnectionEvent;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryImpl;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeBuilder;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeState;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.SupportedFeature;
import com.philips.lighting.hue.sdk.wrapper.domain.clip.ClipResponse;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightStateImpl;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.Group;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupClass;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupLightLocation;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupStream;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.GroupType;
import com.philips.lighting.hue.sdk.wrapper.domain.resource.ProxyMode;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Color;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Entertainment;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Callback;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Message;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Observer;
import com.philips.lighting.hue.sdk.wrapper.entertainment.StartCallback;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Location;
import com.philips.lighting.hue.sdk.wrapper.entertainment.effect.ExplosionEffect;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridge;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String TAG = "HueQuickStartApp";

    private int brightness = 75;

    private Bridge bridge;
    private Entertainment entertainment;
    private boolean lightsAreOn = false;
    private boolean backgroundServiceRunning = false;

    private BridgeDiscovery bridgeDiscovery;

    private List<BridgeDiscoveryResult> bridgeDiscoveryResults;

    // UI elements
    private TextView statusTextView;
    private ListView bridgeDiscoveryListView;
    private TextView bridgeIpTextView;
    private View pushlinkImage;
    private Button bridgeDiscoveryButton;
    private Button toggleLightButton;
    private Button toggleBackgroundService;
    private TextView defaultBrightnessSeekBarValue;
    private ArcSeekBar defaultBrightnessSeekBar;

    private NotificationManager NotificationManager;
    private BroadcastReceiver NotificationReceiver = null;
    private BroadcastReceiver broadcastReceiver = null;

    enum UIState {
        Idle,
        BridgeDiscoveryRunning,
        BridgeDiscoveryResults,
        Connecting,
        Pushlinking,
        Connected,
        EntertainmentReady
    }

    private UIState currentState = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = getApplicationContext();

        bridge = null;
        entertainment = null;

        // Setup the UI
        statusTextView = (TextView)findViewById(R.id.status_text);
        bridgeDiscoveryListView = (ListView)findViewById(R.id.bridge_discovery_result_list);
        bridgeDiscoveryListView.setOnItemClickListener(this);
        bridgeIpTextView = (TextView)findViewById(R.id.bridge_ip_text);
        pushlinkImage = findViewById(R.id.pushlink_image);
        bridgeDiscoveryButton = (Button)findViewById(R.id.bridge_discovery_button);
        bridgeDiscoveryButton.setOnClickListener(this);
        toggleLightButton = (Button)findViewById(R.id.toggle_light_button);
        toggleLightButton.setOnClickListener(this);
        toggleBackgroundService = (Button)findViewById(R.id.toggle_background_service);
        toggleBackgroundService.setOnClickListener(this);

        defaultBrightnessSeekBar = (ArcSeekBar)findViewById(R.id.default_brightness_seekbar);

        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        brightness = sharedPreferences.getInt(getString(R.string.default_brightness), 75);
        defaultBrightnessSeekBar.setOnClickListener(this);
        defaultBrightnessSeekBarValue = (TextView)findViewById(R.id.default_brightness_seekbar_value);
        ProgressListener progressListener = new ProgressListener() {
            @Override
            public void invoke(int progress) {
                defaultBrightnessSeekBarValue.setText("" + progress);
                brightness = (255 * progress) / 100;
            }
        };
        progressListener.invoke(brightness);

        ProgressListener onStopListener = new ProgressListener() {
            @Override
            public void invoke(int progress) {
//              save default brightness to local storage
                SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(getString(R.string.default_brightness), brightness);
                editor.commit();
                turnOnLights();
            }
        };
        defaultBrightnessSeekBar.setOnProgressChangedListener(progressListener);
        defaultBrightnessSeekBar.setOnStopTrackingTouch(onStopListener);
        defaultBrightnessSeekBar.setProgressBackgroundColor(R.color.colorPrimary);

        // Create the BroadCastReceiver
        NotificationReceiver = new NotificationBroadcastReceiver(this);

        // Register the filter
        IntentFilter NotificationFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        NotificationFilter.addAction("android.intent.action." + context.getString(R.string.TURN_ON_LIGHTS));
        NotificationFilter.addAction("android.intent.action." + context.getString(R.string.TURN_OFF_LIGHTS));
        this.registerReceiver(NotificationReceiver, NotificationFilter);

        IntentFilter receiver = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver.addAction(context.getString(R.string.TURN_ON_LIGHTS));
        receiver.addAction(context.getString(R.string.TURN_OFF_LIGHTS));

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if( (currentState == UIState.Connected || currentState == UIState.EntertainmentReady )&& intent.getAction() == "TURN_OFF_LIGHTS") {
                    turnOffLights();
                }
                if( (currentState == UIState.Connected || currentState == UIState.EntertainmentReady )&&  intent.getAction() == "TURN_ON_LIGHTS") {
                    turnOnLights();
                }
            }
        };
        this.registerReceiver(broadcastReceiver, receiver);

        // Connect to a bridge or start the bridge discovery
        String bridgeIp = getLastUsedBridgeIp();
        if (bridgeIp == null) {
            startBridgeDiscovery();
        } else {
            connectToBridge(bridgeIp);
        }

        Intent service = new Intent(context, BackgroundService.class);
        context.startService(service);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(NotificationReceiver);
        unregisterReceiver(broadcastReceiver);
    }

    public void startService(View v) {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        serviceIntent.putExtra("fixthis", "random");

        startService(serviceIntent);
    }

    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        stopService(serviceIntent);
    }

    /**
     * Use the KnownBridges API to retrieve the last connected bridge
     * @return Ip address of the last connected bridge, or null
     */
    private String getLastUsedBridgeIp() {
        List<KnownBridge> bridges = KnownBridges.getAll();

        if (bridges.isEmpty()) {
            return null;
        }

        return Collections.max(bridges, new Comparator<KnownBridge>() {
            @Override
            public int compare(KnownBridge a, KnownBridge b) {
                return a.getLastConnected().compareTo(b.getLastConnected());
            }
        }).getIpAddress();
    }

    /**
     * Start the bridge discovery search
     * Read the documentation on meethue for an explanation of the bridge discovery options
     */
    private void startBridgeDiscovery() {
        disconnectFromBridge();

        bridgeDiscovery = new BridgeDiscoveryImpl();
        // ALL Include [UPNP, IPSCAN, NUPNP, MDNS] but in some nets UPNP, NUPNP and MDNS is not working properly
        bridgeDiscovery.search(BridgeDiscovery.Option.ALL, bridgeDiscoveryCallback);
        
        updateUI(UIState.BridgeDiscoveryRunning, "Scanning the network for hue bridges...");
    }

    /**
     * Stops the bridge discovery if it is still running
     */
    private void stopBridgeDiscovery() {
        if (bridgeDiscovery != null) {
            bridgeDiscovery.stop();
            bridgeDiscovery = null;
        }
    }

    /**
     * The callback that receives the results of the bridge discovery
     */
    private BridgeDiscovery.Callback bridgeDiscoveryCallback = new BridgeDiscovery.Callback() {
        @Override
        public void onFinished(final List<BridgeDiscoveryResult> results, final BridgeDiscovery.ReturnCode returnCode) {
            // Set to null to prevent stopBridgeDiscovery from stopping it
            bridgeDiscovery = null;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (returnCode == BridgeDiscovery.ReturnCode.SUCCESS) {
                        bridgeDiscoveryListView.setAdapter(new BridgeDiscoveryResultAdapter(getApplicationContext(), results));
                        bridgeDiscoveryResults = results;

                        updateUI(UIState.BridgeDiscoveryResults, "Found " + results.size() + " bridge(s) in the network.");
                    } else if (returnCode == BridgeDiscovery.ReturnCode.STOPPED) {
                        Log.i(TAG, "Bridge discovery stopped.");
                    } else {
                        updateUI(UIState.Idle, "Error doing bridge discovery: " + returnCode);
                    }
                }
            });
        }
    };

    /**
     * Use the BridgeBuilder to create a bridge instance and connect to it
     */
    private void connectToBridge(String bridgeIp) {
        stopBridgeDiscovery();
        disconnectFromBridge();

        bridge = new BridgeBuilder("app name", "device name")
                .setIpAddress(bridgeIp)
                .setConnectionType(BridgeConnectionType.LOCAL)
                .setBridgeConnectionCallback(bridgeConnectionCallback)
                .addBridgeStateUpdatedCallback(bridgeStateUpdatedCallback)
                .build();

        bridge.connect();

        bridgeIpTextView.setText("Bridge IP: " + bridgeIp);
        updateUI(UIState.Connecting, "Connecting to bridge...");
    }

    /**
     * Disconnect a bridge
     * The hue SDK supports multiple bridge connections at the same time,
     * but for the purposes of this demo we only connect to one bridge at a time.
     */
    private void disconnectFromBridge() {
        if (bridge != null) {
            bridge.disconnect();
            bridge = null;
        }
    }

    /**
     * The callback that receives bridge connection events
     */
    private BridgeConnectionCallback bridgeConnectionCallback = new BridgeConnectionCallback() {
        @Override
        public void onConnectionEvent(BridgeConnection bridgeConnection, ConnectionEvent connectionEvent) {
            Log.i(TAG, "Connection event: " + connectionEvent);

            switch (connectionEvent) {
                case LINK_BUTTON_NOT_PRESSED:
                    updateUI(UIState.Pushlinking, "Press the link button to authenticate.");
                    break;

                case COULD_NOT_CONNECT:
                    updateUI(UIState.Connecting, "Could not connect.");
                    break;

                case CONNECTION_LOST:
                    updateUI(UIState.Connecting, "Connection lost. Attempting to reconnect.");
                    break;

                case CONNECTION_RESTORED:
                    updateUI(UIState.Connected, "Connection restored.");
                    break;

                case DISCONNECTED:
                    // User-initiated disconnection.
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onConnectionError(BridgeConnection bridgeConnection, List<HueError> list) {
            for (HueError error : list) {
                Log.e(TAG, "Connection error: " + error.toString());
            }
        }
    };

    /**
     * The callback the receives bridge state update events
     */
    private BridgeStateUpdatedCallback bridgeStateUpdatedCallback = new BridgeStateUpdatedCallback() {
        @Override
        public void onBridgeStateUpdated(Bridge bridge, BridgeStateUpdatedEvent bridgeStateUpdatedEvent) {
            Log.i(TAG, "Bridge state updated event: " + bridgeStateUpdatedEvent);

            switch (bridgeStateUpdatedEvent) {
                case INITIALIZED:
                    // The bridge state was fully initialized for the first time.
                    // It is now safe to perform operations on the bridge state.
                    updateUI(UIState.Connected, "Connected!");
                    setupEntertainmentGroup();
                    break;

                case LIGHTS_AND_GROUPS:
                    // At least one light was updated.
                    break;

                default:
                    break;
            }
        }
    };

    public void turnOnLights() {
        BridgeState bridgeState = bridge.getBridgeState();
        List<LightPoint> lights = bridgeState.getLights();
        lightsAreOn = true;

        for (final LightPoint light : lights) {
            final LightState lightState = new LightStateImpl();

            lightState.setOn(true);
            SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
            brightness = sharedPreferences.getInt(getString(R.string.default_brightness), 75);
            lightState.setBrightness(brightness);

            light.updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
                @Override
                public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> list, List<HueError> errorList) {
                    if (returnCode == ReturnCode.SUCCESS) {
                        Log.i(TAG, "Turn on light " + light.getIdentifier() + " to brightness " + lightState.getBrightness());
                    } else {
                        Log.e(TAG, "Error turning on light " + light.getIdentifier());
                        for (HueError error : errorList) {
                            Log.e(TAG, error.toString());
                        }
                    }
                }
            });
        }
    }

    public void turnOffLights() {
        BridgeState bridgeState = bridge.getBridgeState();
        List<LightPoint> lights = bridgeState.getLights();
        lightsAreOn = false;

        for (final LightPoint light : lights) {
            final LightState lightState = new LightStateImpl();

            if(light.getLightState().isOn()) {
                lightState.setOn(false);
            }

            light.updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
                @Override
                public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> list, List<HueError> errorList) {
                    if (returnCode == ReturnCode.SUCCESS) {
                        Log.i(TAG, "Turn off light " + light.getIdentifier());
                    } else {
                        Log.e(TAG, "Error turning off light " + light.getIdentifier());
                        for (HueError error : errorList) {
                            Log.e(TAG, error.toString());
                        }
                    }
                }
            });
        }
    }

    /**
     * Setup the group used for entertainment
     */
    private void setupEntertainmentGroup() {
        // look for an existing entertainment group

        List<Group> groups = bridge.getBridgeState().getGroups();
        for (Group group : groups) {
            if (group.getGroupType() == GroupType.ENTERTAINMENT) {
                createEntertainmentObject(group.getIdentifier());
                return;
            }
        }

        // Could not find an existing group, create a new one with all color lights
        List<LightPoint> validLights = getValidLights();

        if (validLights.isEmpty()) {
            Log.e(TAG, "No color lights found for entertainment");
            return;
        }

        createEntertainmentGroup(validLights);
    }

    /**
     * Create an entertainment group
     * @param validLights List of supported lights
     */
    private void createEntertainmentGroup(List<LightPoint> validLights) {
        ArrayList<String> lightIds = new ArrayList<String>();
        ArrayList<GroupLightLocation> lightLocations = new ArrayList<GroupLightLocation>();
        Random rand = new Random();

        for (LightPoint light : validLights) {
            lightIds.add(light.getIdentifier());

            GroupLightLocation location = new GroupLightLocation();
            location.setLightIdentifier(light.getIdentifier());
            location.setX(rand.nextInt(11) / 10.0 - 0.5);
            location.setY(rand.nextInt(11) / 10.0 - 0.5);
            location.setZ(rand.nextInt(11) / 10.0 - 0.5);

            lightLocations.add(location);
        }

        Group group = new Group();
        group.setName("NewEntertainmentGroup");
        group.setGroupType(GroupType.ENTERTAINMENT);
        group.setGroupClass(GroupClass.TV);

        group.setLightIds(lightIds);
        group.setLightLocations(lightLocations);

        GroupStream stream = new GroupStream();
        stream.setProxyMode(ProxyMode.AUTO);
        group.setStream(stream);

        bridge.createResource(group, new BridgeResponseCallback() {
            @Override
            public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> responses, List<HueError> errors) {
                if (returnCode == ReturnCode.SUCCESS) {
                    createEntertainmentObject(responses.get(0).getStringValue());
                } else {
                    Log.e(TAG, "Could not create entertainment group.");
                }
            }
        });
    }

    /**
     * Create an entertainment object and register an observer to receive messages
     * @param groupId The entertainment group to be used
     */
    private void createEntertainmentObject(String groupId) {
        int defaultPort = 2100;

        entertainment = new Entertainment(bridge, defaultPort, groupId);

        entertainment.registerObserver(new Observer() {
            @Override
            public void onMessage(Message message) {
                Log.i(TAG, "Entertainment message: " + message.getType() + " " + message.getUserMessage());
            }
        }, Message.Type.RENDER);

        updateUI(UIState.EntertainmentReady, "Connected, entertainment ready.");
    }

    /**
     * Get a list of all lights that support entertainment
     * @return Valid lights
     */
    private List<LightPoint> getValidLights() {
        ArrayList<LightPoint> validLights = new ArrayList<LightPoint>();
        for (final LightPoint light : bridge.getBridgeState().getLights()) {
            if (light.getInfo().getSupportedFeatures().contains(SupportedFeature.STREAM_PROXYING)) {
                validLights.add(light);
            }
        }
        return validLights;
    }

    // UI methods
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String bridgeIp = bridgeDiscoveryResults.get(i).getIP();

        connectToBridge(bridgeIp);
    }

    @Override
    public void onClick(View view) {
        if (view == toggleLightButton) {
            if(lightsAreOn) {
                toggleLightButton.setText(R.string.on_button);
                turnOffLights();
            } else {
                toggleLightButton.setText(R.string.off_button);
                turnOnLights();
            }
        }

        if (view == toggleBackgroundService) {
            if(backgroundServiceRunning) {
                backgroundServiceRunning = false;
                toggleBackgroundService.setText(R.string.disable_notification);
                startService((Button)findViewById(R.id.toggle_background_service));
            } else {
                backgroundServiceRunning = true;
                toggleBackgroundService.setText(R.string.enable_notification);
                stopService((Button)findViewById(R.id.toggle_background_service));
            }
        }

        if (view == bridgeDiscoveryButton) {
            startBridgeDiscovery();
        }
    }

    private void updateUI(final UIState state, final String status) {
        this.currentState = state;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Status: " + status);
                statusTextView.setText(status);

                bridgeDiscoveryListView.setVisibility(View.GONE);
                bridgeIpTextView.setVisibility(View.GONE);
                pushlinkImage.setVisibility(View.GONE);
                bridgeDiscoveryButton.setVisibility(View.GONE);
                toggleLightButton.setVisibility(View.GONE);

                switch (state) {
                    case Idle:
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        break;
                    case BridgeDiscoveryRunning:
                        bridgeDiscoveryListView.setVisibility(View.VISIBLE);
                        break;
                    case BridgeDiscoveryResults:
                        bridgeDiscoveryListView.setVisibility(View.VISIBLE);
                        break;
                    case Connecting:
                        bridgeIpTextView.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        break;
                    case Pushlinking:
                        bridgeIpTextView.setVisibility(View.VISIBLE);
                        pushlinkImage.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        break;
                    case Connected:
                        bridgeIpTextView.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        toggleLightButton.setVisibility(View.VISIBLE);
                        break;
                    case EntertainmentReady:
                        bridgeIpTextView.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        toggleLightButton.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });
    }
}
