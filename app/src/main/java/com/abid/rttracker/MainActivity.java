package com.abid.rttracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.abid.rttracker.services.LocationTrackerService;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    TextView mStartStop;
    ImageView ivUserButton;
    RelativeLayout rl_credentials_container;
    Button mSave, mCancel;
    LottieAnimationView lottieAnimationView;
    Animation slide_up, slide_down;
    boolean isVisible = false;
    TextInputEditText mUserName, mUserId;
    SharedPreferences.Editor editor;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getSharedPreferences(Constants.DB, Context.MODE_PRIVATE);

        setContentView(R.layout.activity_main);
        mStartStop = findViewById(R.id.btnStartStop);
        lottieAnimationView = findViewById(R.id.lavAnimation);
        rl_credentials_container = findViewById(R.id.rl_credentials_container);
        ivUserButton = findViewById(R.id.ivUserButton);
        mSave = findViewById(R.id.btnSave);
        mCancel = findViewById(R.id.btnCancel);

        mUserName = findViewById(R.id.etName);
        mUserId = findViewById(R.id.etUserId);

        mStartStop.setOnClickListener(this);
        ivUserButton.setOnClickListener(this);
        mCancel.setOnClickListener(this);
        mSave.setOnClickListener(this);
        ivUserButton.setOnClickListener(this);

        slide_down = AnimationUtils.loadAnimation(this, R.anim.anim_down);
        slide_up = AnimationUtils.loadAnimation(this, R.anim.anim_up);

        mUserId.setText(sharedPref.getString(Constants.USER_ID, ""));
        mUserName.setText(sharedPref.getString(Constants.USER_NAME, ""));
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
            editor = sharedPref.edit();
            editor.putBoolean(Constants.ONLINE_STATUS, true);
            editor.commit();
            lottieAnimationView.playAnimation();
            mStartStop.setText(getString(R.string.stop));
            mStartStop.setBackground(getDrawable(R.drawable.button_background_secondary));
        } else {
            editor = sharedPref.edit();
            editor.putBoolean(Constants.ONLINE_STATUS, false);
            editor.commit();
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
        if (view == mStartStop) {
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
        } else if (view == ivUserButton) {
            if (!isVisible) {
                slideDownAnimation();
            }
        } else if (view == mSave) {
            String userName, userId;
            userName = mUserName.getText().toString();
            userId = mUserId.getText().toString();
            editor = sharedPref.edit();
            editor.putString(Constants.USER_NAME, userName);
            editor.putString(Constants.USER_ID, userId);
            editor.commit();

            slideUpAnimation();
        } else if (view == mCancel) {
            slideUpAnimation();
        }
    }

    private void slideUpAnimation() {
        rl_credentials_container.setAnimation(null);
        rl_credentials_container.setAnimation(slide_up);
        slide_up.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                rl_credentials_container.setVisibility(View.GONE);
                isVisible = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rl_credentials_container.startAnimation(slide_up);
    }

    private void slideDownAnimation() {
        rl_credentials_container.setAnimation(slide_down);
        rl_credentials_container.setVisibility(View.VISIBLE);
        slide_down.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isVisible = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        rl_credentials_container.startAnimation(slide_down);
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
