package com.louise.androcamx;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.budiyev.android.codescanner.ScanMode;
import com.google.zxing.Result;

import org.json.JSONException;
import org.json.JSONObject;

public class scanner extends Fragment {
    private CodeScanner mCodeScanner;
    private String uniqueId,user;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final Activity activity = getActivity();
        View root = inflater.inflate(R.layout.activity_scanner, container, false);
        Bundle arguments = getArguments();
        handleBackButton();
        if (arguments != null) {
            uniqueId = arguments.getString("uniqueid");
            user = arguments.getString("user");
        }
        CodeScannerView scannerView = root.findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(activity, scannerView);
        mCodeScanner.setScanMode( ScanMode.CONTINUOUS);
        mCodeScanner.setDecodeCallback(result -> {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(result.getText());
                if (jsonObject.has("app") && jsonObject.has("data") && jsonObject.has("encrypted") && jsonObject.getString("app").equals("ANDROCAMX")) {
                    Bundle bundle = new Bundle();
                    bundle.putString("uniqueid", uniqueId);
                    bundle.putString("user", user);
                    bundle.putSerializable("data", jsonObject.getString("data").split(":"));
                    activity.runOnUiThread(()->{
                    Fragment fragment = new Connection();
                    fragment.setArguments(bundle);
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.replaceFragment(fragment);
                    mainActivity.setTitle("Connection Established");});
                    return;
                } else {
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Wrong QR Code or Not the required one", Toast.LENGTH_SHORT).show());
//                    return;
//                    scannerView.setOnClickListener(view -> mCodeScanner.startPreview());
                }
            } catch (JSONException e) {
//                e.printStackTrace();
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Wrong QR Code or Not the required one", Toast.LENGTH_SHORT).show());
////                    return;
//                scannerView.setOnClickListener(view -> mCodeScanner.startPreview());
//                throw new RuntimeException(e);
            }});

        scannerView.setOnClickListener(view -> mCodeScanner.startPreview());
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }

    @Override
    public void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }
    private void handleBackButton() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                System.out.println("pressedback");
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }
    public String getTitle() {
        return "Code Scanner";
    }
}