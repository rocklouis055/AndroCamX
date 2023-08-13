package com.louise.androcamx;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class Help extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_help, container, false);
//
//        TextView button = view.findViewById(R.id.bt1);
//        button.setOnClickListener(v -> {
//            // Call your custom function here
//            performCustomAction();
//        });

        return view;
    }

    private void performCustomAction() {
        // Implement your custom logic here
        // This function will be called when the button is clicked
    }
    public String getTitle() {
        return "Help";
    }
}
