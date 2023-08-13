package com.louise.androcamx;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class Connection extends Fragment {
    private TextView ip,port,name,device;
    private CheckBox check;
    private Button start;
    private Spinner camera,resolution;
    private ConstraintLayout ipportlayout;
    private String selectedCameraOption="0";
    private SeekBar quality;
    private SharedPreferences sharedPreferences;
    private View view;
    private Handler handler;
    private Fragment fragment = null;
    private Class<? extends Fragment> fragmentClass = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_connection, container, false);
        ip=view.findViewById(R.id.ip);
        port=view.findViewById(R.id.port);
        device=view.findViewById(R.id.device);
        name=view.findViewById(R.id.name);
        check=view.findViewById(R.id.check);
        camera=view.findViewById(R.id.camera);
        start=view.findViewById(R.id.start);
        resolution=view.findViewById(R.id.resolution);
        ipportlayout=view.findViewById(R.id.ipportlayout);
        ipportlayout.setVisibility(View.INVISIBLE);
        quality = view.findViewById(R.id.quality);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        quality.setProgress(sharedPreferences.getInt("jpeg_quality",70));
        boolean encryptValue = sharedPreferences.getBoolean("encrypted_frames", false);
        String tcpPortValue = sharedPreferences.getString("tcp_port", "49512");
        int fpsValue = sharedPreferences.getInt("fps", 30);
        Log.d("chkpref",encryptValue+tcpPortValue+fpsValue);
        quality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int finalProgress = seekBar.getProgress();
                Toast.makeText(view.getContext(), "Final Quality: " + finalProgress, Toast.LENGTH_SHORT).show();
            }
        });
        check.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                ipportlayout.setVisibility(View.VISIBLE);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                ipportlayout.setVisibility(View.INVISIBLE);
            }
            check.setChecked(true);
            return false;
        });
//        handler = new Handler();
        CameraManager cameraManager = (CameraManager) view.getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            List<Pair<String, String>> cameraOptions = new ArrayList<>();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null) {
                    String cameraOption;
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        cameraOption = "Front Camera";
                    } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraOption = "Back Camera";
                    } else {
                        cameraOption = "Unknown";
                    }
                    cameraOptions.add(new Pair<>(cameraId, cameraOption));
                }
            }
            ArrayList<String> cameraOptions2 = new ArrayList<>();
            for (Pair<String, String> option : cameraOptions) {
                cameraOptions2.add(option.second);
            }
            ArrayAdapter<String> cameraAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, cameraOptions2);
            camera.setAdapter(cameraAdapter);
            cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            camera.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedCameraOption = (String) parent.getItemAtPosition(position);
                    String selectedCameraId = null;
                    if (selectedCameraOption.equals("Back Camera")) {
                        selectedCameraId ="0";
                    } else if (selectedCameraOption.equals("Front Camera")) {
                        selectedCameraId = "1";
                    }
                    else{
                        selectedCameraId="0";
                    }
                    bindResolutionOptions(cameraManager, selectedCameraId);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
//            Toast.makeText(getContext(),sharedPreferences.getString("cameradevice","0"),Toast.LENGTH_SHORT).show();
            camera.setSelection(Integer.parseInt(sharedPreferences.getString("cameradevice","0")));
            Pair<String, String> initialCameraOption = cameraOptions.get(0);
            String initialCameraId = initialCameraOption.first;
            bindResolutionOptions(cameraManager, initialCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Bundle arguments = getArguments();
        if (arguments != null) {
            String[] data = (String[]) arguments.getSerializable("data");
            for(int i=0;i< data.length;i++){
                System.out.println(data[i]);
            }
            ip.setText(data[1]);
            port.setText(data[2]);
            name.setText(data[3]);
            device.setText(data[4]);
        }
        start.setOnClickListener(view1 -> {
//            DrawerLayout mDrawer = (DrawerLayout) view.findViewById(R.id.drawer_layout);
            Bundle bundle = new Bundle();
            bundle.putInt("port", Integer.parseInt(port.getText().toString()));
            bundle.putString("ip", ip.getText().toString());
            bundle.putString("resolution",resolution.getSelectedItem().toString());
            bundle.putInt("quality",quality.getProgress());
            bundle.putString("camera",selectedCameraOption);
            System.out.println("Cameraoptionsend:"+camera.toString());
//            fragmentClass=Camera.class;
//            try {
//                fragment = fragmentClass.newInstance();
//                fragment.setArguments(bundle);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            FragmentManager fragmentManager = getChildFragmentManager();
//            fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();
//            getActivity().setTitle("Camera");
            Fragment fragment = new Camera();
            fragment.setArguments(bundle);
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.replaceFragment(fragment);
            mainActivity.setTitle("Camera");
        });

        return view;
    }
    private void bindResolutionOptions(CameraManager cameraManager, String cameraId) {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (configMap != null) {
                Size[] sizes = configMap.getOutputSizes(ImageFormat.JPEG);

                // Convert the resolutions to string representations
                List<String> resolutionStrings = new ArrayList<>();
                for (Size size : sizes) {
                    resolutionStrings.add(size.getWidth() + "x" + size.getHeight());
                }
                ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, resolutionStrings);
                resolution.setAdapter(resolutionAdapter);
                resolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                Toast.makeText(getContext(),sharedPreferences.getString("resolution","0") , Toast.LENGTH_SHORT).show();
                resolution.setSelection(resolutionAdapter.getPosition(sharedPreferences.getString("resolution","0")));
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    public String getTitle() {
        return "Connection Established";
    }
}
