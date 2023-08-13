package com.louise.androcamx;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class Home extends Fragment {
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;
    private Button scan, qrconnect,manualcon;
    private Thread wifiThread;
    private boolean wifiscancheck = true;
    private Handler handler;
    private Fragment fragment = null;
    private Class<? extends Fragment> fragmentClass = null;
    private ActionBarDrawerToggle drawerToggle;
    private ProgressDialog progressDialog;
    private FirebaseFirestore db;
    private DocumentReference userRef;
    private String uniqueID;
    private static final String TAG = "firebase";
    private static final String USERS_COLLECTION = "AndroCamX";
    private FirebaseAuth firebaseAuth;
    private Map<String, Object> data;
    private FirebaseFirestore firestore;
    private String userId;
    private Activity activity;
    private String broadmsg = "BROADCAST", selfip, shutmsg = "SHUTDOWN";
    private int selfport, broadport = 50000, n = 10, sec = 3;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001, WIFI_PERMISSION_REQUEST_CODE = 1002;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_home, container, false);

        activity = getActivity();
        // Retrieve the arguments passed to the fragment
//        Bundle arguments = getArguments();

        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        selfAddress();
//        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

// Use the uniqueID for further operations

        scan = view.findViewById(R.id.scan);
        qrconnect = view.findViewById(R.id.qrconnect);
        manualcon=view.findViewById(R.id.manual);
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(true);
        scan.setOnClickListener(v -> broadcast());
        qrconnect.setOnClickListener(v -> activity.runOnUiThread(()->{
            Fragment fragment = new scanner();
            MainActivity mainActivity = (MainActivity) activity;
            mainActivity.replaceFragment(fragment);
            mainActivity.setTitle("CodeScanner");}));
        manualcon.setOnClickListener(v -> activity.runOnUiThread(()->{
                Fragment fragment = new Manual();
                MainActivity mainActivity = (MainActivity) activity;
                mainActivity.replaceFragment(fragment);
                mainActivity.setTitle("Manual Connect");}));
        handler = new Handler();
        wifiThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                boolean isWifiEnabled = wifiManager != null && wifiManager.isWifiEnabled();
                boolean isHotspotEnabled = isHotspotEnabled(); // Custom method to check if hotspot is enabled

                if (!isWifiEnabled && !isHotspotEnabled) {
                    if (wifiscancheck) {
                        handler.post(() -> {
                            Toast.makeText(activity, "Wi-Fi or hotspot is not enabled, Wi-Fi is needed for scan", Toast.LENGTH_SHORT).show();
                            scan.setEnabled(false);
                        });
                        wifiscancheck = false;
                    }
                } else {
                    wifiscancheck = true;
                    handler.post(() -> scan.setEnabled(true));
                }
            }
        });
        wifiThread.start();
//        AppCompatActivity activity = (AppCompatActivity) activity;
//        activity.setSupportActionBar(toolbar);
//        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        mDrawer = activity.findViewById(R.id.drawer_layout);
//        drawerToggle = setupDrawerToggle();
//        drawerToggle.setDrawerIndicatorEnabled(true);
//        drawerToggle.syncState();
//        mDrawer.addDrawerListener(drawerToggle);
//        nvDrawer = activity.findViewById(R.id.nvView);
//        setupDrawerContent(nvDrawer);
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE}, WIFI_PERMISSION_REQUEST_CODE);
        }
        return view;
    }

    private boolean isHotspotEnabled() {
        WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            try {
                Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
                method.setAccessible(true);
                return (boolean) method.invoke(wifiManager);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity, "Thanks for granting Camera permission", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                Toast.makeText(activity, "This app requires Camera permission to work,Grant it", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == WIFI_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity, "Thanks for granting Wifi permissions", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "This app also requires Wifi permissions to work,Grant it", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE}, WIFI_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void selfAddress() {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.connect(InetAddress.getByName("8.8.8.8"), 1);
                final String ip = socket.getLocalAddress().getHostAddress();
                activity.runOnUiThread(() -> {
                    selfip = ip;
                    Log.d("Self Address", ip);
                });
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    @SuppressLint("StaticFieldLeak")
    private class BroadcastTask extends AsyncTask<Void, Void, String[]> {
        @Override
        protected void onPreExecute() {
            progressDialog.setTitle("Discover");
            progressDialog.show();
        }

        @Override
        protected String[] doInBackground(Void... params) {
            int attempts = 0;
            String message = "ANDROCAMX:" + selfip + ":" + broadport;
            while (attempts < n) {
                try {
                    DatagramSocket socket = new DatagramSocket(null);
                    socket.setReuseAddress(true);
                    socket.setBroadcast(true);
                    socket.bind(new InetSocketAddress("0.0.0.0", broadport));
                    socket.setSoTimeout(sec * 1000);
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName("255.255.255.255"), broadport);
                    socket.send(packet);
                    Log.d("broadcast", "Broadcasting attempt " + (attempts + 1));
                    attempts++;
                    Log.d("broadcast", "Waiting for response...");
                    long endTime = SystemClock.elapsedRealtime() + sec * 1000;
                    int finalAttempts = attempts;
                    handler.post(() -> progressDialog.setMessage("Trying " + finalAttempts + " attempt....."));
                    while (true) {
                        byte[] buffer = new byte[1024];
                        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(receivedPacket);
                        String data = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                        Log.d("broadcast", data);
                        if (data != null && data.startsWith(broadmsg) && !selfip.equals(receivedPacket.getAddress().getHostAddress())) {
                            Log.d("broadcast", data + " " + receivedPacket.getAddress().getHostAddress());
                            String[] parts = data.split(":");
                            socket.close();
                            return parts;
                        }
                        if (data != null && data.startsWith(shutmsg)) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.e("broadcast error", e.getMessage(), e);
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(String[] parts) {
            progressDialog.dismiss();
            if (parts == null) {
                Toast.makeText(activity, "Failed to discover devices even after " + n + " attempts.", Toast.LENGTH_SHORT).show();
            } else {
                Bundle bundle = new Bundle();
                Log.d("parts",parts.toString());
                bundle.putSerializable("data", parts);
                bundle.putString("user", USERS_COLLECTION);
                bundle.putString("id", uniqueID);
                activity.runOnUiThread(()->{
                    Fragment fragment = new Connection();
                    fragment.setArguments(bundle);
                    MainActivity mainActivity = (MainActivity) activity;
                    mainActivity.replaceFragment(fragment);
                    mainActivity.setTitle("Connection Established");
                });
            }
        }
    }
    public void broadcast() {
        new BroadcastTask().execute();
    }





    public String getTitle(){
        return("AndroCamX");
    }
}
