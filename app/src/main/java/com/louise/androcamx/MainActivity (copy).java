//package com.louise.androcamx;
//import android.Manifest;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.ActionBarDrawerToggle;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.Toolbar;
//import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.core.view.GravityCompat;
//import androidx.drawerlayout.widget.DrawerLayout;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.lifecycle.ViewModel;
//import androidx.lifecycle.ViewModelProvider;
//
//import android.annotation.SuppressLint;
//import android.app.ProgressDialog;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.ActivityInfo;
//import android.content.pm.PackageManager;
//import android.content.res.Configuration;
//import android.net.ConnectivityManager;
//import android.net.wifi.WifiManager;
//import android.os.AsyncTask;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Parcel;
//import android.os.Parcelable;
//import android.os.SystemClock;
//import android.util.DisplayMetrics;
//import android.util.Log;
//import android.view.Display;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.Window;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.Toast;
//import com.google.android.material.navigation.NavigationView;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.firestore.DocumentReference;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.FirebaseFirestoreSettings;
//
//import java.io.IOException;
//import java.io.Serializable;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//import java.net.InetSocketAddress;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//public class MainActivity extends AppCompatActivity {
//    private DrawerLayout mDrawer;
//    private Toolbar toolbar;
//    private NavigationView nvDrawer;
//    private Button scan, qrconnect,manualcon;
//    private Thread wifiThread;
//    private boolean wifiscancheck = true;
//    private Handler handler;
//    private ConstraintLayout mclayout;
//    private Fragment fragment = null;
//    private Class<? extends Fragment> fragmentClass = null;
//    private ActionBarDrawerToggle drawerToggle;
//    private ProgressDialog progressDialog;
//    private FirebaseFirestore db;
//    private DocumentReference userRef;
//    private String uniqueID;
//    private static final String TAG = "firebase";
//    private static final String USERS_COLLECTION = "AndroCamX";
//    private FirebaseAuth firebaseAuth;
//    private Map<String, Object> data;
//    private FirebaseFirestore firestore;
//    private String userId;
//    private String broadmsg = "BROADCAST", selfip, shutmsg = "SHUTDOWN";
//    private int selfport, broadport = 50000, n = 10, sec = 3;
//    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001, WIFI_PERMISSION_REQUEST_CODE = 1002;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        setContentView(R.layout.activity_main);
//        Window window = getWindow();
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        selfAddress();
////        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
//        uniqueID = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);//UUID.randomUUID().toString();
//        Log.d("uid", uniqueID);
//// Use the uniqueID for further operations
//        FirebaseApp.initializeApp(this);
//        firebaseAuth = FirebaseAuth.getInstance();
//        firestore = FirebaseFirestore.getInstance();
//
//        // Enable offline caching
//        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
//                .setPersistenceEnabled(true)
//                .build();
//        firestore.setFirestoreSettings(settings);
//        signInAnonymously();
//        scan = findViewById(R.id.scan);
//        qrconnect = findViewById(R.id.qrconnect);
//        mclayout = findViewById(R.id.mclayout);
//        manualcon=findViewById(R.id.manual);
//        progressDialog = new ProgressDialog(this);
//        progressDialog.setCancelable(true);
//        scan.setOnClickListener(view -> broadcast());
//        qrconnect.setOnClickListener(v -> {
//            fragmentClass = scanner.class;
//            mclayout.setVisibility(View.GONE);
//            try {
//                fragment = fragmentClass.newInstance();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            FragmentManager fragmentManager = getSupportFragmentManager();
//            fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
//            setTitle("CodeScanner");
//            mDrawer.closeDrawer(GravityCompat.START);
//        });
//        manualcon.setOnClickListener(v ->{
//            fragmentClass = Manual.class;
//            mclayout.setVisibility(View.GONE);
//            try {
//                fragment = fragmentClass.newInstance();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            FragmentManager fragmentManager = getSupportFragmentManager();
//            fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
//            setTitle("Manual Connect");
//            mDrawer.closeDrawer(GravityCompat.START);
//        });
//        handler = new Handler();
//        wifiThread = new Thread(() -> {
//            while (!Thread.currentThread().isInterrupted()) {
//                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//                boolean isWifiEnabled = wifiManager != null && wifiManager.isWifiEnabled();
//                boolean isHotspotEnabled = isHotspotEnabled(); // Custom method to check if hotspot is enabled
//
//                if (!isWifiEnabled && !isHotspotEnabled) {
//                    if (wifiscancheck) {
//                        handler.post(() -> {
//                            Toast.makeText(MainActivity.this, "Wi-Fi or hotspot is not enabled, Wi-Fi is needed for scan", Toast.LENGTH_SHORT).show();
//                            scan.setEnabled(false);
//                        });
//                        wifiscancheck = false;
//                    }
//                } else {
//                    wifiscancheck = true;
//                    handler.post(() -> scan.setEnabled(true));
//                }
//            }
//        });
//        wifiThread.start();
//        toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        mDrawer = findViewById(R.id.drawer_layout);
//        drawerToggle = setupDrawerToggle();
//        drawerToggle.setDrawerIndicatorEnabled(true);
//        drawerToggle.syncState();
//        mDrawer.addDrawerListener(drawerToggle);
//        nvDrawer = findViewById(R.id.nvView);
//        setupDrawerContent(nvDrawer);
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
//        }
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
//                || ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE}, WIFI_PERMISSION_REQUEST_CODE);
//        }
//    }
//
//    private boolean isHotspotEnabled() {
//        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        if (wifiManager != null) {
//            try {
//                Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
//                method.setAccessible(true);
//                return (boolean) method.invoke(wifiManager);
//            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
//                e.printStackTrace();
//            }
//        }
//        return false;
//    }
//
//    @SuppressLint("MissingSuperCall")
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, "Thanks for granting Camera permission", Toast.LENGTH_SHORT).show();
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
//            } else {
//                Toast.makeText(this, "This app requires Camera permission to work,Grant it", Toast.LENGTH_SHORT).show();
//            }
//        } else if (requestCode == WIFI_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
//                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, "Thanks for granting Wifi permissions", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(this, "This app also requires Wifi permissions to work,Grant it", Toast.LENGTH_SHORT).show();
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE}, WIFI_PERMISSION_REQUEST_CODE);
//            }
//        }
//    }
//
//    private void selfAddress() {
//        new Thread(() -> {
//            try {
//                DatagramSocket socket = new DatagramSocket();
//                socket.connect(InetAddress.getByName("8.8.8.8"), 1);
//                final String ip = socket.getLocalAddress().getHostAddress();
//                MainActivity.this.runOnUiThread(() -> {
//                    selfip = ip;
//                    Log.d("Self Address", ip);
//                });
//                socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }
//
//    @SuppressLint("StaticFieldLeak")
//    private class BroadcastTask extends AsyncTask<Void, Void, String[]> {
//        @Override
//        protected void onPreExecute() {
//            progressDialog.setTitle("Discover");
//            progressDialog.show();
//        }
//
//        @Override
//        protected String[] doInBackground(Void... params) {
//            int attempts = 0;
//            String message = "ANDROCAMX:" + selfip + ":" + broadport;
//            while (attempts < n) {
//                try {
//                    DatagramSocket socket = new DatagramSocket(null);
//                    socket.setReuseAddress(true);
//                    socket.setBroadcast(true);
//                    socket.bind(new InetSocketAddress("0.0.0.0", broadport));
//                    socket.setSoTimeout(sec * 1000);
//                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName("255.255.255.255"), broadport);
//                    socket.send(packet);
//                    Log.d("broadcast", "Broadcasting attempt " + (attempts + 1));
//                    attempts++;
//                    Log.d("broadcast", "Waiting for response...");
//                    long endTime = SystemClock.elapsedRealtime() + sec * 1000;
//                    int finalAttempts = attempts;
//                    handler.post(() -> progressDialog.setMessage("Trying " + finalAttempts + " attempt....."));
//                    while (true) {
//                        byte[] buffer = new byte[1024];
//                        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
//                        socket.receive(receivedPacket);
//                        String data = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
//                        Log.d("broadcast", data);
//                        if (data != null && data.startsWith(broadmsg) && !selfip.equals(receivedPacket.getAddress().getHostAddress())) {
//                            Log.d("broadcast", data + " " + receivedPacket.getAddress().getHostAddress());
//                            String[] parts = data.split(":");
//                            socket.close();
//                            return parts;
//                        }
//                        if (data != null && data.startsWith(shutmsg)) {
//                            break;
//                        }
//                    }
//                } catch (Exception e) {
//                    Log.e("broadcast error", e.getMessage(), e);
//                }
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(String[] parts) {
//            progressDialog.dismiss();
//            if (parts == null) {
//                Toast.makeText(MainActivity.this, "Failed to discover devices even after " + n + " attempts.", Toast.LENGTH_SHORT).show();
//            } else {
//                fragmentClass = Connection.class;
//                mclayout.setVisibility(View.GONE);
//                Bundle bundle = new Bundle();
//                Log.d("parts",parts.toString());
//                bundle.putSerializable("data", parts);
//                bundle.putString("user", USERS_COLLECTION);
//                bundle.putString("id", uniqueID);
////                bundle.putSerializable("data", new HashMap<>(data));
//                try {
//                    fragment = fragmentClass.newInstance();
//                    fragment.setArguments(bundle);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                FragmentManager fragmentManager = getSupportFragmentManager();
//                fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
//                setTitle("Connection Established");
//                mDrawer.closeDrawer(GravityCompat.START);
//            }
//        }
//    }
//
//    public void broadcast() {
//        new BroadcastTask().execute();
//    }
//
//    private void setupDrawerContent(NavigationView navigationView) {
//        navigationView.setNavigationItemSelectedListener(
//                menuItem -> {
//                    selectDrawerItem(menuItem);
//                    return true;
//                });
//    }
//
//    private ActionBarDrawerToggle setupDrawerToggle() {
//        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                mDrawer.openDrawer(GravityCompat.START);
//                return true;
//        }
//        if (drawerToggle.onOptionsItemSelected(item)) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    public void selectDrawerItem(MenuItem menuItem) {
//        switch (menuItem.getItemId()) {
//            case R.id.home:
//                Intent intent = new Intent(this, MainActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(intent);
//                break;
//            case R.id.settings:
//                fragmentClass = Settings.class;
//                break;
//            case R.id.help:
//                fragmentClass = Help.class;
//                break;
//            case R.id.contact:
//                fragmentClass = Contact.class;
//                break;
//            case R.id.about:
//                fragmentClass = About.class;
//                break;
//        }
//        mclayout.setVisibility(View.GONE);
//        try {
//            fragment = fragmentClass.newInstance();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
//        setTitle(menuItem.getTitle());
//        mDrawer.closeDrawer(GravityCompat.START);
//    }
//
//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);
//        // Sync the toggle state after onRestoreInstanceState has occurred.
//        drawerToggle.syncState();
//    }
//
//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        // Pass any configuration change to the drawer toggles
//        drawerToggle.onConfigurationChanged(newConfig);
//    }
//
//    public void replaceFragment(Fragment fragment) {
//        getSupportFragmentManager().beginTransaction().replace(R.id.flContent, fragment).commit();
//    }
//
//    private void signInAnonymously() {
//        System.out.println("starting sign in");
//        firebaseAuth.signInAnonymously()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        userId = firebaseAuth.getCurrentUser().getUid();
//                        Log.d(TAG, "Anonymous sign-in successful. User ID: " + userId);
//
//                        fetchData(userId);
//
//                        // Fetch data
//                    } else {
//                        Log.e(TAG, "Anonymous sign-in failed. Error: " + task.getException());
//                    }
//                });
//    }
//
//    private void saveData(String userId, String uniqueId) {
//        data = new HashMap<>();
//        data.put("uniqueId", uniqueId);
//        data.put("deviceName", Build.MANUFACTURER + " " + Build.MODEL);
//        data.put("deviceModel", Build.MODEL);
//        data.put("deviceResolution", getDeviceResolution());
////        userRef = firestore.collection(USERS_COLLECTION).document(userId);
//        Log.d("chk", userRef.getPath());
//        userRef.set(data)
//                .addOnSuccessListener(aVoid -> Log.d(TAG, "Data saved for user: " + uniqueId))
//                .addOnFailureListener(e -> Log.e(TAG, "Error saving data: " + e.getMessage()));
//    }
//
//    private String getDeviceResolution() {
//        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//        Display display = windowManager.getDefaultDisplay();
//        DisplayMetrics metrics = new DisplayMetrics();
//        display.getRealMetrics(metrics);
//        int width = metrics.widthPixels;
//        int height = metrics.heightPixels;
//        return width + "x" + height;
//    }
//
//    private void fetchData(String userId) {
//        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
//        userRef = firestore.collection(USERS_COLLECTION).document(uniqueID);
//        if (userRef!=null) {
//            userViewModel.setUserRef(userRef);
//            userRef.get().addOnCompleteListener(task -> {
//                if (task.isSuccessful()) {
//                    DocumentSnapshot document = task.getResult();
//                    if (document != null && document.exists()) {
//                        Log.d(TAG, "Fetched data: " + document.getData());
//                        if (!document.getData().containsKey("uniqueId"))
//                            saveData(userId, uniqueID);
//                    } else {
//                        Log.e(TAG, "No such document");
//                        saveData(userId, uniqueID);
//                    }
//                } else {
//                    Log.e(TAG, "Error fetching data: " + task.getException());
//                }
//            });
//        }
//    }
//}
