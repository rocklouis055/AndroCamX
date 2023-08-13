package com.louise.androcamx;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Manual extends Fragment {

    private Button connectButton;
    private EditText ipEditText, portEditText;
    private String uniqueId, user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_manual, container, false);

        // Retrieve the arguments passed to the fragment
        Bundle arguments = getArguments();
        if (arguments != null) {
            uniqueId = arguments.getString("uniqueid");
            user = arguments.getString("user");
        }

        // Initialize views
        ipEditText = view.findViewById(R.id.ip);
        portEditText = view.findViewById(R.id.port);
        connectButton = view.findViewById(R.id.connect);

        // Set onClickListener for the Connect button
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ipEditText.getText().toString();
                String port = portEditText.getText().toString();

                // Check if IP and port are provided
                if (ip.isEmpty() || port.isEmpty()) {
                    Toast.makeText(getActivity(), "Please enter IP and port", Toast.LENGTH_SHORT).show();
                } else {
                    // Start the TCP connection task
                    new ConnectTask().execute(ip, port);
                }
            }
        });

        return view;
    }

    // AsyncTask to establish TCP connection and send/receive messages
    private class ConnectTask extends AsyncTask<String, Void, String> {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Show a progress dialog while connecting
            progressDialog = ProgressDialog.show(getActivity(), "Connecting", "Please wait...", true);
        }

        @Override
        protected String doInBackground(String... params) {
            String ip = params[0];
            int port = Integer.parseInt(params[1]);
            String messageToSend = "ANDROCAMX"; // The message to send

            Socket socket = null;
            PrintWriter out = null;
            BufferedReader in = null;
            String receivedMessage = null;

            try {
                // Establish TCP connection
                socket = new Socket(ip, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Send the message to the server
                out.println(messageToSend);

                // Wait to receive the message from the server
                receivedMessage = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Close resources
                try {
                    if (out != null)
                        out.close();
                    if (in != null)
                        in.close();
                    if (socket != null)
                        socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return receivedMessage;
        }

        @Override
        protected void onPostExecute(String receivedMessage) {
            super.onPostExecute(receivedMessage);

            // Dismiss the progress dialog
            progressDialog.dismiss();

            if (receivedMessage != null) {
                // Split the received message using ":"
                String[] splitMessage = receivedMessage.split(":");

                if (splitMessage.length >= 4) {
                    // Create a bundle with the required values
                    Bundle bundle = new Bundle();
                    bundle.putString("uniqueid", uniqueId);
                    bundle.putString("user", user);
                    bundle.putSerializable("data", splitMessage);
                    Fragment fragment = new Connection();
                    fragment.setArguments(bundle);
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.replaceFragment(fragment);
                    mainActivity.setTitle("Connection Established");
                } else {
                    Toast.makeText(getActivity(), "Invalid message format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), "No response from server", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public String getTitle() {
        return "Manual Connect";
    }
}



