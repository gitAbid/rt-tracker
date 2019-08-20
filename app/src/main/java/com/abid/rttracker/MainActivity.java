package com.abid.rttracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.abid.rttracker.services.LocationTrackerService;
import com.abid.rttracker.utils.NetworkUtil;
import com.airbnb.lottie.LottieAnimationView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    TextView mStartStop;
    LottieAnimationView lottieAnimationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStartStop = findViewById(R.id.btnStartStop);
        lottieAnimationView = findViewById(R.id.lavAnimation);
        mStartStop.setOnClickListener(this);

    }


    @Override
    protected void onResume() {
        super.onResume();
        checkLocationService();
        updateButtonStatus();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean checkLocationService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            } else {
                return true;
            }
        }
        return false;
    }

    private void updateButtonStatus() {
        if (isMyServiceRunning()) {
            lottieAnimationView.playAnimation();
            mStartStop.setText(getString(R.string.stop));
            mStartStop.setBackground(getDrawable(R.drawable.button_background_secondary));
        } else {
            lottieAnimationView.setProgress(.5f);
            lottieAnimationView.cancelAnimation();
            mStartStop.setText(getString(R.string.start));
            mStartStop.setBackground(getDrawable(R.drawable.button_background_primary));
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        checkLocationService();
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationTrackerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (!isMyServiceRunning()) {
            if (checkLocationService() && enableGPS() && isNetworkAvailable()) {
                Intent intent = new Intent(MainActivity.this, LocationTrackerService.class);
                startService(intent);
            }
        } else {
            Intent intent = new Intent(MainActivity.this, LocationTrackerService.class);
            stopService(intent);
        }
        updateButtonStatus();
    }

    private boolean enableGPS() {
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            new AlertDialog.Builder(this)
                    .setMessage(R.string.gps_network_not_enabled)
                    .setPositiveButton(R.string.open_location_settings, (paramDialogInterface, paramInt) -> {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
        return gps_enabled;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.offline_title)
                    .setMessage(R.string.offline)
                    .setPositiveButton(R.string.ok, (paramDialogInterface, paramInt) -> {
                    })
                    .show();
        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
