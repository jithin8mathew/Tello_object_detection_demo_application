package com.example.demoapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.regex.Pattern;

public class droneController extends AppCompatActivity {

    private TextView jdroneWIFI;
    private TextView jdroneTOF;
    private TextView jdroneBaro;
    private TextView jdroneTemperature;
    private TextView jdroneHeight;
    private TextView jdroneBattery;
    private TextView jdroneSpeed;
    private TextView jdroneAccleration;

    private Switch videoFeedaction;
    private FloatingActionButton DronewheatSpikeDetection;
    private SeekBar setDroneSpeedBar;

    private FloatingActionButton connection;
    private int connectionClickCounter = 1; // for counting the number of times the button is clicked
    private boolean connectionFlag = false; // to check and maintain the connection status of the drone. Initially the drone is not conected, so the status is false

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drone_controller);

        jdroneBattery = findViewById(R.id.droneBattery); // reads drone battery level
        jdroneTOF = findViewById(R.id.droneTOF);         // reading Time of Flight
        jdroneBaro = findViewById(R.id.droneBaro);       // reading Barometric pressure
        jdroneTemperature = findViewById(R.id.droneTemperature);       // reading drone temperature
        jdroneHeight = findViewById(R.id.droneHeight);   // reading the current drone height
        jdroneSpeed = findViewById(R.id.droneSpeed);     // reading drone speed
        jdroneAccleration = findViewById(R.id.droneAccleration);    // reading drone accleration
        jdroneWIFI = findViewById(R.id.droneWIFI);       // getting the wifi status
//        jdroneWIFI.setBackgroundResource(R.drawable.rounded_corner_red);



        Pattern statePattern = Pattern.compile("-*\\d{0,3}\\.?\\d{0,2}[^\\D\\W\\s]");  // a regex pattern to read the tello state

        connection = findViewById(R.id.connectToDrone); // a button to initiate establishing SDK mode with the drone by sending 'command' command
        connection.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if (connectionClickCounter % 2 == 1){   // to enable swith like behavior to connect and disconnect from the drone
                    telloConnect("command");
                    Toast.makeText(droneControl.this,"Drone connected",Toast.LENGTH_SHORT).show();
                    connectionFlag = true;              // set the connection status to true
                }
                if (connectionClickCounter % 2 == 0){
                    telloConnect("disconnect");
                    connectionFlag = false;
                    Toast.makeText(droneControl.this,"Drone disconnected",Toast.LENGTH_SHORT).show();
                }
                connectionClickCounter++;
            }
        });


    }
}