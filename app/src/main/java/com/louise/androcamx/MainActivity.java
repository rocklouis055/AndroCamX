package com.louise.androcamx;
import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout mDrawer;
    private Toolbar toolbar;
    private NavigationView nvDrawer;
    private Fragment fragment = null;
    private Class<? extends Fragment> fragmentClass = null;
    private ActionBarDrawerToggle drawerToggle;
    private ProgressDialog progressDialog;
    private DocumentReference userRef;
    private String uniqueID;
    private static final String TAG = "firebase";
    private static final String USERS_COLLECTION = "AndroCamX";
    private FirebaseAuth firebaseAuth;
    private Map<String, Object> data;
    private FirebaseFirestore firestore;
    private String userId;
    private Stack<String> fragmentTitleStack;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001, WIFI_PERMISSION_REQUEST_CODE = 1002,PERMISSION_REQUEST_CODE=1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDrawer = findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();
        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerToggle.syncState();
        mDrawer.addDrawerListener(drawerToggle);
        nvDrawer = findViewById(R.id.nvView);
        setupDrawerContent(nvDrawer);
        FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        uniqueID = android.provider.Settings.Secure.getString(this.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);//UUID.randomUUID().toString();
        Log.d("uid", uniqueID);
        // Enable offline caching
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);
        signInAnonymously();
        handleBackButton();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE}, WIFI_PERMISSION_REQUEST_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it from the user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
        fragmentClass = Home.class;
        try {
            fragment = fragmentClass.newInstance();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        fragmentTitleStack = new Stack<>();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
        setTitle("AndroCamX");
        mDrawer.closeDrawer(GravityCompat.START);
    }


    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            // Handle camera permission request result
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Thanks for granting Camera permission", Toast.LENGTH_SHORT).show();
                requestManageAllFilesPermission();
                return;
            } else {
                Toast.makeText(this, "This app requires Camera permission to work, please grant it", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == WIFI_PERMISSION_REQUEST_CODE) {
            // Handle WiFi permission request result
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Thanks for granting WiFi permissions", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "This app also requires WiFi permissions to work, please grant them", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            // Handle file storage permission request result
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, proceed with file storage operations
                requestManageAllFilesPermission();
            } else {
                // Permission is denied, handle accordingly (e.g., show a message or disable file storage features)
//                Toast.makeText(this, "File storage permission denied, some features may not work", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void requestManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Toast.makeText(this, "File storage permission with All files access permission is also required", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent,  PERMISSION_REQUEST_CODE);
        } else {
            // Permission is already granted or not required
            // Perform your desired operations here
        }
    }
    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    selectDrawerItem(menuItem);
                    return true;
                });
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open, R.string.drawer_close);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void selectDrawerItem(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.home:
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case R.id.settings:
                fragmentClass = Settings.class;
                break;
            case R.id.help:
                fragmentClass = Help.class;
                break;
            case R.id.contact:
                fragmentClass = Contact.class;
                break;
            case R.id.about:
                fragmentClass = About.class;
                break;
        }
        try {
            fragment = fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).addToBackStack(null).commit();
        setTitle(menuItem.getTitle());

        mDrawer.closeDrawer(GravityCompat.START);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }

    public void replaceFragment(Fragment fragment) {
        fragmentTitleStack.push(getTitle().toString());

        // Update the title based on the new Fragment
//        updateTitle(fragment);
        getSupportFragmentManager().beginTransaction().replace(R.id.flContent, fragment).addToBackStack(null).commit();
    }
    private void handleBackButton() {
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            // Enable or disable the back button in the action bar based on the back stack count
//            int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
////            getSupportActionBar().setDisplayHomeAsUpEnabled(backStackEntryCount > 0);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle the back button press in the action bar
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Check if there are Fragments in the back stack
        if (fragmentManager.getBackStackEntryCount() > 0) {
            // Pop the top Fragment from the back stack to go back to the previous Fragment
            fragmentManager.popBackStackImmediate();
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.flContent);
            // Update the title based on the current Fragment
            updateTitle(currentFragment);
        } else {
            // If there are no Fragments in the back stack, let the system handle the back button press
            super.onBackPressed();
        }
    }
    private void updateTitle(Fragment fragment) {
        if (fragment instanceof scanner) {
            setTitle("Code Scanner");
        } else if (fragment instanceof Settings) {
            setTitle("Settings");
        } else if (fragment instanceof Manual) {
            setTitle("Manual Connect");
        }else if (fragment instanceof Home) {
            setTitle("AndroCamX");
        }else if (fragment instanceof About) {
            setTitle("About");
        }else if (fragment instanceof Camera) {
            setTitle("Camera");
        }else if (fragment instanceof Contact) {
            setTitle("Contact");
        }else if (fragment instanceof Help) {
            setTitle("Help");
        }
        // Add more conditions for other Fragments
    }
    private void fetchData(String userId) {
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userRef = firestore.collection(USERS_COLLECTION).document(uniqueID);
        if (userRef!=null) {
            userViewModel.setUserRef(userRef);
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        Log.d(TAG, "Fetched data: " + document.getData());
                        if (!document.getData().containsKey("uniqueId"))
                            saveData(userId, uniqueID);
                    } else {
                        Log.e(TAG, "No such document");
                        saveData(userId, uniqueID);
                    }
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                    long numlocal=sharedPreferences.getLong("imagenumber", 0);
                    if (!document.getData().containsKey("ImageNumber")){
                        userRef.update("ImageNumber", String.valueOf(numlocal));
                    }
                    else{
                        long numserv= Long.parseLong(document.getData().getOrDefault("ImageNumber", "0").toString());
                        if (numserv>numlocal) sharedPreferences.edit().putLong("imagenumber", numserv).apply();
                        else userRef.update("ImageNumber", String.valueOf(numlocal));
                    }
                } else {
                    Log.e(TAG, "Error fetching data: " + task.getException());
                }
            });
        }
    }
    private void saveData(String userId, String uniqueId) {
        data = new HashMap<>();
        data.put("uniqueId", uniqueId);
        data.put("deviceName", Build.MANUFACTURER + " " + Build.MODEL);
        data.put("deviceModel", Build.MODEL);
        data.put("deviceResolution", getDeviceResolution());
//        userRef = firestore.collection(USERS_COLLECTION).document(userId);
        Log.d("chk", userRef.getPath());
        userRef.set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Data saved for user: " + uniqueId))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving data: " + e.getMessage()));
    }
    private void signInAnonymously() {
        System.out.println("starting sign in");
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userId = firebaseAuth.getCurrentUser().getUid();
                        Log.d(TAG, "Anonymous sign-in successful. User ID: " + userId);

                        fetchData(userId);

                        // Fetch data
                    } else {
                        Log.e(TAG, "Anonymous sign-in failed. Error: " + task.getException());
                    }
                });
    }
    private String getDeviceResolution() {
        WindowManager windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        return width + "x" + height;
    }

}
