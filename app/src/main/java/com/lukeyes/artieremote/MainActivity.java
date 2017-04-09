package com.lukeyes.artieremote;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    public Button connectButton;

    public Button disconnectButton;
    Context context;

    ObjectMapper objectMapper;
    final private String MY_ID = "remote";
    private static final String AUTO_ADDRESS = "192.168.1.20";

    WebSocketClient webSocketClient = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        objectMapper = new ObjectMapper();

        setContentView(R.layout.activity_main);
        this.context = this;

        connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConnect();
            }
        });
        connectButton.setEnabled(true);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        disconnectButton = (Button) findViewById(R.id.disconnect_button);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDisconnect();
            }
        });
        disconnectButton.setEnabled(false);

        Resources resources = getResources();
        int button0_0_id = resources.getIdentifier("button0_0", "id", this.getPackageName());
        Button button0_0 = (Button) findViewById(button0_0_id);
        button0_0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonClicked(view);
            }
        });
    }

    public void buttonClicked(View view) {

        Button button = (Button) view;
        String text = String.valueOf(button.getText());
        Message message = new Message();
        message.sender = MY_ID;
        message.recipient = "server";
        message.message = text;
        try {
            String wrappedMessage = objectMapper.writeValueAsString(message);
            if(webSocketClient != null)
                webSocketClient.send(wrappedMessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void onConnect() {
        connectWebSocket(AUTO_ADDRESS);
    }

    public void onDisconnect() {
        Toast.makeText(this, "Disconnect", Toast.LENGTH_SHORT).show();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
            }
        });
    }

    private void connectWebSocket(String address) {
        URI uri;
        try {
            String socketAddress = String.format("ws://%s:8080", address);
            String toastText = String.format("Connecting to %s", socketAddress);
            Toast.makeText(this,toastText,Toast.LENGTH_SHORT).show();
            uri = new URI(socketAddress);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectButton.setEnabled(false);
                        disconnectButton.setEnabled(true);
                        displayString("Opened");
                    }
                });

                Message message = new Message();
                message.sender = MY_ID;
                message.recipient = "server";
                message.message = "Hello from " + Build.MANUFACTURER + " " + Build.MODEL;

                try {
                    String jsonMessage = objectMapper.writeValueAsString(message);
                    this.send(jsonMessage);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(final String message) {
                Log.i("Websocket", message);
                try {
                    final Message parsedMessage = objectMapper.readValue(message, Message.class);
                    if(MY_ID.equals(parsedMessage.recipient)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                displayString(parsedMessage.message);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("Websocket", "Closed " + reason);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectButton.setEnabled(true);
                        disconnectButton.setEnabled(false);
                    }
                });
            }

            @Override
            public void onError(Exception ex) {
                Log.i("Websocket", "Error " + ex.getMessage());
            }
        };

        webSocketClient.connect();
    }

    public void displayString(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
