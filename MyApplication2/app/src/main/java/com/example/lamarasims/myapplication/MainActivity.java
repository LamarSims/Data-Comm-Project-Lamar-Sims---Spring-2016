package com.example.lamarasims.myapplication;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import android.util.Log;
import android.widget.*;
import android.view.*;

public class MainActivity extends AppCompatActivity {
    //Declare and initialize variables
    private String serverIpAddr = "10.40.13.47";
    private int serverPort = 8081;
    private TextView txtViewConnected;
    private TextView txtViewServerResponse;
    private EditText editTxtToServer;
    private boolean connected = false;
    private Handler handler = new Handler();
    private Socket client;
    private OutputStream outToServer;
    private InputStream inFromServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtViewConnected = (TextView) findViewById(R.id.txtViewConnected);
        txtViewServerResponse = (TextView) findViewById(R.id.txtViewServerResponse);
        editTxtToServer = (EditText) findViewById(R.id.editTxtToServer);
    }

    /* Handles the connection the the server for the client, called when Connect button is pressed */
    public void connectToServer(View view){
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            txtViewConnected.setText("Connecting to " + serverIpAddr + " on port " + serverPort);
                        }
                    });

                    //connect the client to the server using the IP address and port used by the server
                    InetAddress serverAddr = InetAddress.getByName(serverIpAddr);
                    client = new Socket(serverAddr, serverPort);
                    connected = true;

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            txtViewConnected.setText("Just connected to " + client.getRemoteSocketAddress());
                        }
                    });

                    readMessageFromServer(); //method to handle messages from the server to the client
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    /* closes the socket when the Disconnect button is pressed on the device */
    public void closeSocket(View view){
        try{
            //is not already closed then close Client socket
            if (!client.isClosed()) client.close();
            outToServer.close(); //close the outputstream
            inFromServer.close(); //close the inputstream
            handler.post(new Runnable() {
                @Override
                public void run() {
                    txtViewConnected.setText("Disconnected");
                }
            });
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    /* sends messages to the server when the Send button is pressed on the device */
    public void sendMessageToServer(View view){
        new Thread(new Runnable(){
            @Override
            public void run() {
                String message = editTxtToServer.getText().toString(); //get message to be sent
                try {
                    //get the output stream to the server and send the specified message
                    outToServer = client.getOutputStream();
                    DataOutputStream out = new DataOutputStream(outToServer);
                    out.writeUTF(message);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    /* handles the reading of messages received from the server */
    public void readMessageFromServer(){
        new Thread(new Runnable(){
            @Override
            public void run() {
                while (!client.isClosed()){
                    try {
                        //get the input stream from the server and read in the message sent
                        inFromServer = client.getInputStream();
                        DataInputStream in = new DataInputStream(inFromServer);
                        final String serverResponse = in.readUTF(); //get message sent

                        //display message, if no message input stream waits 5 seconds and checks again
                        if (!serverResponse.equals("")){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    txtViewServerResponse.append("-------------\n" +
                                            "Server says: " + serverResponse + "\n" +
                                            "-------------\n");
                                }
                            });
                            Log.d("Main Activity", "I: 4");
                        }else {
                            inFromServer.wait(5000);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
