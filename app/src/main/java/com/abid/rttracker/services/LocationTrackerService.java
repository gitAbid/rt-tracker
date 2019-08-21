package com.abid.rttracker.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.abid.rttracker.R;
import com.abid.rttracker.model.Data;
import com.google.android.gms.location.LocationRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.patloew.rxlocation.RxLocation;

import io.reactivex.disposables.Disposable;

public class LocationTrackerService extends Service {

    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    private static final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";
    private static final String CHANNEL_RT_TRACKER = "rt_service_tracker";
    private static Disposable disposable;
    String unique_id;
    private DatabaseReference myRef;
    private FirebaseDatabase database;
    private RxLocation rxLocation;
    private LocationRequest locationRequest;
    private CharSequence textTitle = "Location";
    private CharSequence textContent = "Location Tracker";

    public LocationTrackerService() {
    }

    private static void error(Throwable throwable) {
        //handle error
    }

    private void accept(Address address) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("devices");

        unique_id = Settings.Secure.getString(getApplicationContext()
                .getContentResolver(), Settings.Secure.ANDROID_ID);

        rxLocation = new RxLocation(this);
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(100);
        Log.d(TAG_FOREGROUND_SERVICE, "My foreground service onCreate().");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            startForegroundService();
            Toast.makeText(getApplicationContext(), "Foreground service is started.", Toast.LENGTH_LONG).show();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /* Used to build and start foreground service. */
    @SuppressLint("MissingPermission")
    private void startForegroundService() {
        Log.d(TAG_FOREGROUND_SERVICE, "Start foreground service.");
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_RT_TRACKER)
                .setSmallIcon(R.drawable.googleg_standard_color_18)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .build();

        disposable = rxLocation.location().updates(locationRequest)
                .flatMap(location -> rxLocation.geocoding().fromLocation(location).toObservable())
                .subscribe(address -> {
                    Log.e("Location", address.getAdminArea() + " " + address.getLatitude());
                    myRef.child(unique_id).setValue(new Data("Abid",
                                    unique_id,
                                    address.getLatitude(),
                                    address.getLongitude(),
                                    System.currentTimeMillis()));
                }, LocationTrackerService::error);
        // Start foreground service.
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG_FOREGROUND_SERVICE, "Stop foreground service.");
        stopForeground(true);
        if (disposable != null) {
            disposable.dispose();
        }
        // Stop the foreground service.
        stopSelf();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_RT_TRACKER, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
