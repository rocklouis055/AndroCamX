package com.louise.androcamx;

import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Settings extends PreferenceFragmentCompat {
    private static final int MIN_PORT = 49512;
    private static final int MAX_PORT = 65535;
    private static final int[] RESTRICTED_PORTS = {50000}; // Add your restricted port numbers here
    private EditTextPreference tcpport;
    private ListPreference antibandingPref,colorEffectPref,sceneModePref,cameraDevicePref,resolutionPref,whiteBalancePref;
    private int[] antibandingModes;
    private CameraManager cameraManager;
    private UserViewModel userViewModel;
    private DocumentReference userRef;
    private
    Map<String, Object> data = new HashMap<>();
    private EditTextPreference idPref, passPref, backgroundPref, cameraPictureLocationPref, cameraPictureEncoderPref,
            videoRecorderLocationPref, videoRecorderResolutionPref, videoRecorderEncoderPref,modePref;
    private SwitchPreference authenticationPref, encryptedFramesPref, autoExposurePref, hdrPref, rawPref, zoomPref, autoFocusPref,
            autoDimPref, flashPref, autoFlashPref, fullScreenPref, dateTimeOverlayPref;
    private SeekBarPreference timePref, fpsPref, jpegQualityPref;
    private Preference resetSettingsPref, enableAdvancedSettingsPref;

    private static final String TAG = "SettingsFragment";
    public void onPause() {
        saveData();
        super.onPause();
Log.d("leaving","");
    }
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference, rootKey);
        Bundle bundle = getArguments();
//        if (bundle != null) {
//            data = (Map<String, Object>) bundle.getSerializable("data");
//            if (data != null) {
//                // Process the retrieved data
//                Log.d(TAG, String.valueOf(data));
//            }
//        }
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        userRef = userViewModel.getUserRef();
        fetchData();
        tcpport = findPreference("tcp_port");
        authenticationPref = findPreference("authentication");
        idPref = findPreference("id");
        passPref = findPreference("pass");
        encryptedFramesPref = findPreference("encrypted_frames");

        // Camera Settings
        autoExposurePref = findPreference("auto_exposure");
        hdrPref = findPreference("hdr");
        rawPref = findPreference("raw");
        zoomPref = findPreference("zoom");
        autoFocusPref = findPreference("focus_auto_focus");
        whiteBalancePref = findPreference("whitebalance");
        autoDimPref = findPreference("auto_dim");
        timePref = findPreference("time");
        backgroundPref = findPreference("background");
        antibandingPref = findPreference("antibanding");
        colorEffectPref = findPreference("coloreffect");
        sceneModePref = findPreference("scenemode");
        fpsPref = findPreference("fps");
        cameraDevicePref = findPreference("cameradevice");
        resolutionPref = findPreference("resolution");
        flashPref = findPreference("flash");
        autoFlashPref = findPreference("autoflash");
        modePref = findPreference("mode");
        jpegQualityPref = findPreference("jpeg_quality");
        dateTimeOverlayPref = findPreference("date_time_overlay");

        // Camera Picture Settings
        cameraPictureLocationPref = findPreference("camera_picture_location");
        cameraPictureEncoderPref = findPreference("camera_picture_encoder");

        // Video Recorder Settings
        videoRecorderLocationPref = findPreference("video_recorder_location");
        videoRecorderResolutionPref = findPreference("video_recorder_resolution");
        videoRecorderEncoderPref = findPreference("video_recorder_encoder");

        // Display Settings
        fullScreenPref = findPreference("full_screen");

        // Advanced Settings
        resetSettingsPref = findPreference("reset_settings");
        enableAdvancedSettingsPref = findPreference("enable_advanced_settings");
//        authenticationPref.setOnPreferenceChangeListener(preferenceChangeListener);
//        idPref.setOnPreferenceChangeListener(preferenceChangeListener);
//        passPref.setOnPreferenceChangeListener(preferenceChangeListener);
        updatedcamera(0);
        antibandingPref.setOnPreferenceChangeListener((preference, newValue) -> {
            data.put("Antibanding", newValue);
            return true;
        });

        colorEffectPref.setOnPreferenceChangeListener((preference, newValue) -> {
            data.put("ColorEffect", newValue);
            return true;
        });

        sceneModePref.setOnPreferenceChangeListener((preference, newValue) -> {
            data.put("SceneMode", newValue);
            return true;
        });

        cameraDevicePref.setOnPreferenceChangeListener((preference, newValue) -> {
            data.put("CameraDevice", newValue);
            Toast.makeText(getContext(), newValue.toString(), Toast.LENGTH_SHORT).show();
            return true;
        });
        resolutionPref.setOnPreferenceChangeListener((preference, newValue) -> {
            data.put("Resolution", newValue);
            return true;
        });

        whiteBalancePref.setOnPreferenceChangeListener((preference, newValue) -> {
            data.put("WhiteBalance", newValue);
            return true;
        });
        encryptedFramesPref.setOnPreferenceChangeListener((preference, n) ->{
                data.put("Encrypt",n.toString());
            return true;
        });

        autoExposurePref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("AutoExposure",n.toString());
            return true;
        });
        hdrPref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("HDR",n.toString());
            return true;
        });
        rawPref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("RAW",n.toString());
            return true;
        });
        zoomPref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("Zoom",n.toString());
            return true;
        });
        autoFocusPref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("Autofocus",n.toString());
            return true;
        });
        fpsPref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("Fps",n.toString());
            return true;
        });
        autoDimPref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("AutoDim",n.toString());
            return true;
        });
        timePref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("Time",n.toString());
            return true;
        });
//        backgroundPref.setOnPreferenceChangeListener((preference, n) ->{
//            data.put("AutoDim",n.toString());
//            return true;
//        });
        flashPref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("Flash",n.toString());
            return true;
        });
        autoFlashPref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("Autoflash",n.toString());
            return true;
        });
        jpegQualityPref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("JPEG",n.toString());
            return true;
        });
        dateTimeOverlayPref.setOnPreferenceChangeListener((preference, n) ->{
            data.put("Showdatetime",n.toString());
            return true;
        });


//        cameraPictureLocationPref.setOnPreferenceChangeListener(preferenceChangeListener);
//        cameraPictureEncoderPref.setOnPreferenceChangeListener(preferenceChangeListener);
//
//        videoRecorderLocationPref.setOnPreferenceChangeListener(preferenceChangeListener);
//        videoRecorderResolutionPref.setOnPreferenceChangeListener(preferenceChangeListener);
//        videoRecorderEncoderPref.setOnPreferenceChangeListener(preferenceChangeListener);
//
//        fullScreenPref.setOnPreferenceChangeListener(preferenceChangeListener);
//
//        resetSettingsPref.setOnPreferenceClickListener(preferenceClickListener);
//        enableAdvancedSettingsPref.setOnPreferenceClickListener(preferenceClickListener);
        if (tcpport != null) {
            tcpport.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    int val = Integer.parseInt(newValue.toString());
                    if (isRestrictedPort(val)) {
                        Toast.makeText(getContext(), "Selected Restricted Port,setting to default 49512.", Toast.LENGTH_SHORT).show();
                        tcpport.setText(String.valueOf(MIN_PORT));
                        return false;
                    } else if (!(val >= MIN_PORT) || !(val <= MAX_PORT)) {
                        Toast.makeText(getContext(), String.format("Port %s is not supported,setting to %d.", val, val < MIN_PORT ? MIN_PORT : MAX_PORT), Toast.LENGTH_LONG).show();
                        tcpport.setText(String.valueOf(val < MIN_PORT ? MIN_PORT : MAX_PORT));
                        return false;
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), String.format("Port %s is not supported,setting to %d.", newValue.toString(), MAX_PORT), Toast.LENGTH_LONG).show();
                    tcpport.setText(String.valueOf(MAX_PORT));
                    return false;
                }
                data.put("tcpport", tcpport.getText());
                return true;
            });
            tcpport.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (s != null && !s.toString().isEmpty()) {
                            int port;
                            try {
                                port = Integer.parseInt(s.toString());
                            } catch (NumberFormatException e) {
                                return;
                            }
                            if (port < MIN_PORT || port > MAX_PORT) {
                                editText.setError(String.format("Port number should be between %d and %d.", MIN_PORT, MAX_PORT));
                            } else if (isRestrictedPort(port)) {
                                editText.setError("This port number is Restricted.");
                            } else {
                                editText.setError(null);
                            }
                        }
                    }
                });
            });
        }
        cameraManager = (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraProviderFuture.addListener(() -> {
        if (cameraManager != null) {
            try {
                String[] cameraIds = cameraManager.getCameraIdList();
                CharSequence[] entries = new CharSequence[cameraIds.length];
                CharSequence[] entryValues = new CharSequence[cameraIds.length];
                for (int i = 0; i < cameraIds.length; i++) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraIds[i]);
                    Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (lensFacing != null) {
                        String cameraDevice = lensFacing == CameraCharacteristics.LENS_FACING_FRONT ? "Front Camera" : "Back Camera";
                        entries[i] = cameraDevice;
                        entryValues[i] = cameraIds[i];
                    }
                }
                cameraDevicePref.setEntries(entries);
                cameraDevicePref.setEntryValues(entryValues);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
//        cameraDevicePref.setOnPreferenceChangeListener((preference, newValue) -> {
//            int selectedValue = Integer.parseInt(newValue.toString());
////            Toast.makeText(getContext(), selectedValue, Toast.LENGTH_SHORT).show();
//            updatedcamera(selectedValue);
//            return true;
//        });
    }, ContextCompat.getMainExecutor(getContext()));
    }
    private boolean isRestrictedPort(int port) {
        for (int restrictedPort : RESTRICTED_PORTS) {
            if (port == restrictedPort) {return true;}
        }return false;
    }
    private void updatedcamera(int x){
        CameraManager cameraManager = (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraProviderFuture.addListener(() -> {try {
            String cameraId = cameraManager.getCameraIdList()[x];
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            if (antibandingPref != null) {
                antibandingModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);
                if (antibandingModes != null) {
                    String[] antibandingEntries = new String[antibandingModes.length];
                    String[] antibandingEntryValues = new String[antibandingModes.length];

                    for (int i = 0; i < antibandingModes.length; i++) {
                        int mode = antibandingModes[i];
                        antibandingEntries[i] = getAntibandingModeName(mode);
                        antibandingEntryValues[i] = String.valueOf(mode);
                    }

                    antibandingPref.setEntries(antibandingEntries);
                    antibandingPref.setEntryValues(antibandingEntryValues);
                }
            }
            if (colorEffectPref != null) {
                int[] colorEffects = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
                if (colorEffects != null) {
                    String[] colorEffectEntries = new String[colorEffects.length];
                    String[] colorEffectEntryValues = new String[colorEffects.length];

                    for (int i = 0; i < colorEffects.length; i++) {
                        int effect = colorEffects[i];
                        colorEffectEntries[i] = getColorEffectName(effect);
                        colorEffectEntryValues[i] = String.valueOf(effect);
                    }

                    colorEffectPref.setEntries(colorEffectEntries);
                    colorEffectPref.setEntryValues(colorEffectEntryValues);
                }
            }
            if (sceneModePref != null) {
                int[] sceneModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
                if (sceneModes != null) {
                    String[] sceneModeEntries = new String[sceneModes.length];
                    String[] sceneModeEntryValues = new String[sceneModes.length];

                    for (int i = 0; i < sceneModes.length; i++) {
                        int mode = sceneModes[i];
                        sceneModeEntries[i] = getSceneModeName(mode);
                        sceneModeEntryValues[i] = String.valueOf(mode);
                    }

                    sceneModePref.setEntries(sceneModeEntries);
                    sceneModePref.setEntryValues(sceneModeEntryValues);
                }
            }
            // Retrieve and set the resolution preference
            if (resolutionPref != null) {
                StreamConfigurationMap streamConfigMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (streamConfigMap != null) {
                    Size[] sizes = streamConfigMap.getOutputSizes(ImageFormat.JPEG);
                    if (sizes != null) {
                        String[] resolutionEntries = new String[sizes.length];
                        String[] resolutionEntryValues = new String[sizes.length];

                        for (int i = 0; i < sizes.length; i++) {
                            Size size = sizes[i];
                            resolutionEntries[i] = size.getWidth() + "x" + size.getHeight();
                            resolutionEntryValues[i] = size.getWidth() + "x" + size.getHeight();
                        }

                        resolutionPref.setEntries(resolutionEntries);
                        resolutionPref.setEntryValues(resolutionEntryValues);
                    }
                }
            }
            if(whiteBalancePref!=null){
                int[] whiteBalanceModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);

                List<String> whiteBalanceEntries = new ArrayList<>();
                List<String> whiteBalanceEntryValues = new ArrayList<>();

                for (int mode : whiteBalanceModes) {
                    String entry = getWhiteBalanceModeName(mode);
                    String entryValue = String.valueOf(mode);

                    whiteBalanceEntries.add(entry);
                    whiteBalanceEntryValues.add(entryValue);
                }

                String[] entries = whiteBalanceEntries.toArray(new String[0]);
                String[] entryValues = whiteBalanceEntryValues.toArray(new String[0]);
                whiteBalancePref.setEntries(entries);
                whiteBalancePref.setEntryValues(entryValues);

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        }, ContextCompat.getMainExecutor(getContext()));
    }
    private String getAntibandingModeName(int mode) {
        switch (mode) {
            case CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_OFF:
                return "Off";
            case CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_50HZ:
                return "50Hz";
            case CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_60HZ:
                return "60Hz";
            case CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO:
                return "Auto";
            default:
                return "Unknown";
        }
    }
    private String getColorEffectName(int effect) {
        switch (effect) {
            case CameraMetadata.CONTROL_EFFECT_MODE_OFF:
                return "Off";
            case CameraMetadata.CONTROL_EFFECT_MODE_MONO:
                return "Monochrome";
            case CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE:
                return "Negative";
            case CameraMetadata.CONTROL_EFFECT_MODE_SOLARIZE:
                return "Solarize";
            case CameraMetadata.CONTROL_EFFECT_MODE_SEPIA:
                return "Sepia";
            case CameraMetadata.CONTROL_EFFECT_MODE_POSTERIZE:
                return "Posterize";
            case CameraMetadata.CONTROL_EFFECT_MODE_WHITEBOARD:
                return "Whiteboard";
            case CameraMetadata.CONTROL_EFFECT_MODE_BLACKBOARD:
                return "Blackboard";
            case CameraMetadata.CONTROL_EFFECT_MODE_AQUA:
                return "Aqua";
            default:
                return "Unknown";
        }
    }
    private String getWhiteBalanceModeName(int mode) {
        switch (mode) {
            case CameraMetadata.CONTROL_AWB_MODE_OFF:
                return "Off";
            case CameraMetadata.CONTROL_AWB_MODE_AUTO:
                return "Auto";
            case CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT:
                return "Incandescent";
            case CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT:
                return "Fluorescent";
            case CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT:
                return "Warm Fluorescent";
            case CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT:
                return "Daylight";
            case CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT:
                return "Cloudy";
            case CameraMetadata.CONTROL_AWB_MODE_TWILIGHT:
                return "Twilight";
            case CameraMetadata.CONTROL_AWB_MODE_SHADE:
                return "Shade";
            default:
                return "Unknown";
        }
    }

    private String getSceneModeName(int mode) {
        switch (mode) {
            case CameraMetadata.CONTROL_SCENE_MODE_DISABLED:
                return "Disabled";
            case CameraMetadata.CONTROL_SCENE_MODE_ACTION:
                return "Action";
            case CameraMetadata.CONTROL_SCENE_MODE_BARCODE:
                return "Barcode";
            case CameraMetadata.CONTROL_SCENE_MODE_BEACH:
                return "Beach";
            case CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT:
                return "Candlelight";
            case CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS:
                return "Fireworks";
            case CameraMetadata.CONTROL_SCENE_MODE_HDR:
                return "HDR";
            case CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE:
                return "Landscape";
            case CameraMetadata.CONTROL_SCENE_MODE_NIGHT:
                return "Night";
            case CameraMetadata.CONTROL_SCENE_MODE_NIGHT_PORTRAIT:
                return "Night Portrait";
            case CameraMetadata.CONTROL_SCENE_MODE_PARTY:
                return "Party";
            case CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT:
                return "Portrait";
            case CameraMetadata.CONTROL_SCENE_MODE_SNOW:
                return "Snow";
            case CameraMetadata.CONTROL_SCENE_MODE_SPORTS:
                return "Sports";
            case CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO:
                return "Steady Photo";
            case CameraMetadata.CONTROL_SCENE_MODE_SUNSET:
                return "Sunset";
            case CameraMetadata.CONTROL_SCENE_MODE_THEATRE:
                return "Theatre";
            case CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY:
                return "Face Priority";
            default:
                return "Unknown";
        }
    }
    private void saveData() {
        if (userRef!=null) {
            Log.d("savedataload", data.toString());
            userRef.set(data)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Data saved for user"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving data: " + e.getMessage()));
        }
        Log.d("leaving2","");
    }
    private void fetchData() {
        if (userRef!=null) {
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        Log.d(TAG, String.valueOf(document.getData()));
                        Map<String, Object> newData = document.getData();
                        if (newData != null) {
                            data.putAll(newData);
                        }
                        update();
                    }
                } else {
                    Log.e(TAG, "Error fetching data: " + task.getException());
                }
            });
        }
    }
    private void update(){
        encryptedFramesPref.setChecked(Boolean.parseBoolean(String.valueOf(data.getOrDefault("Encrypt", false))));
        autoExposurePref.setChecked(Boolean.parseBoolean(String.valueOf(data.getOrDefault("AutoExposure", false))));
        hdrPref.setChecked(Boolean.parseBoolean(String.valueOf(data.getOrDefault("HDR", false))));
        rawPref.setChecked(Boolean.parseBoolean(String.valueOf(data.getOrDefault("RAW", false))));
        zoomPref.setChecked(Boolean.parseBoolean(String.valueOf(data.getOrDefault("Zoom", false))));
        autoFocusPref.setChecked(Boolean.parseBoolean(String.valueOf(data.getOrDefault("Autofocus", false))));
        fpsPref.setValue(Integer.parseInt(String.valueOf(data.getOrDefault("Fps", "30"))));
        autoDimPref.setChecked(Boolean.parseBoolean(String.valueOf(data.getOrDefault("AutoDim", false))));
        timePref.setValue(Integer.parseInt(String.valueOf(data.getOrDefault("Time", "5"))));
        tcpport.setText(String.valueOf(data.getOrDefault("tcpport", "49512")));
// backgroundPref.setSummary(String.valueOf(data.getOrDefault("Background", "")));
        flashPref.setChecked(Boolean.parseBoolean(String.valueOf(data.getOrDefault("Flash", false))));
        autoFlashPref.setChecked(Boolean.parseBoolean(String.valueOf(data.getOrDefault("Autoflash", false))));
        jpegQualityPref.setValue(Integer.parseInt(String.valueOf(data.getOrDefault("JPEG", "70"))));
        dateTimeOverlayPref.setChecked(Boolean.parseBoolean(String.valueOf(data.getOrDefault("Showdatetime", false))));
        antibandingPref.setValue((String) data.getOrDefault("Antibanding", "0"));
        colorEffectPref.setValue((String) data.getOrDefault("ColorEffect", "0"));
        sceneModePref.setValue((String) data.getOrDefault("SceneMode", "0"));
        cameraDevicePref.setValue((String) data.getOrDefault("CameraDevice", "0"));
        resolutionPref.setValue((String) data.getOrDefault("Resolution", "0"));
        whiteBalancePref.setValue((String) data.getOrDefault("WhiteBalance", "0"));
    }
}