package com.example.demoapplication;

import static android.os.SystemClock.sleep;
import static java.lang.Thread.interrupted;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.controlwear.virtual.joystick.android.JoystickView;

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
    private ImageView actionTakeOff; // button to get the drone to takeoff
    private ImageView actionLnad;    // button to get the drone to land

    private int connectionClickCounter = 1; // for counting the number of times the button is clicked
    private boolean connectionFlag = false; // to check and maintain the connection status of the drone. Initially the drone is not conected, so the status is false

    Pattern statePattern = Pattern.compile("-*\\d{0,3}\\.?\\d{0,2}[^\\D\\W\\s]");  // a regex pattern to read the tello state
    private int RC[] = {0,0,0,0};       // initialize an array of variables for remote control
    private Handler telloStateHandler;  // and handler needs to be created to display the tello state values in the UI in realtime

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drone_controller);

        telloStateHandler = new Handler();

        jdroneBattery = findViewById(R.id.droneBattery); // reads drone battery level
        jdroneTOF = findViewById(R.id.droneTOF);         // reading Time of Flight
        jdroneBaro = findViewById(R.id.droneBaro);       // reading Barometric pressure
        jdroneTemperature = findViewById(R.id.droneTemperature);       // reading drone temperature
        jdroneHeight = findViewById(R.id.droneHeight);   // reading the current drone height
        jdroneSpeed = findViewById(R.id.droneSpeed);     // reading drone speed
        jdroneAccleration = findViewById(R.id.droneAccleration);    // reading drone accleration
        jdroneWIFI = findViewById(R.id.droneWIFI);       // getting the wifi status
//        jdroneWIFI.setBackgroundResource(R.drawable.rounded_corner_red);

        connection = findViewById(R.id.connectToDrone); // a button to initiate establishing SDK mode with the drone by sending 'command' command
        connection.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if (connectionClickCounter % 2 == 1){   // to enable swith like behavior to connect and disconnect from the drone
                    telloConnect("command");
                    Toast.makeText(droneController.this,"Drone connected",Toast.LENGTH_SHORT).show();
                    connectionFlag = true;              // set the connection status to true
                }
                if (connectionClickCounter % 2 == 0){
                    telloConnect("disconnect");
                    connectionFlag = false;
                    Toast.makeText(droneController.this,"Drone disconnected",Toast.LENGTH_SHORT).show();
                }
                connectionClickCounter++;
            }
        });

        actionTakeOff = findViewById(R.id.takeoff);
        actionTakeOff.setOnClickListener(v -> {
            if (connectionFlag){
                telloConnect("takeoff"); // send takeoff command
            }
        });

        actionLnad = findViewById(R.id.land);
        actionLnad.setOnClickListener(v -> {
            if (connectionFlag){
                telloConnect("land");   // send land command
            }
        });

        JoystickView leftjoystick = (JoystickView) findViewById(R.id.joystickViewLeft); // left joystick where the angle is the movement angle and strength is the extend to which you push the joystick
        leftjoystick.setOnMoveListener((angle, strength) -> {

            if (angle >45 && angle <=135){
                RC[2]= strength;
            }
            if (angle >226 && angle <=315){
                strength *= -1;
                RC[2]= strength;
            }
            if (angle >135 && angle <=225){
                strength *= -1;
                RC[3]= strength;
            }
            if (angle >316 && angle <=359 || angle >0 && angle <=45){
                RC[3]= strength;
            }

            telloConnect("rc "+ RC[0] +" "+ RC[1] +" "+ RC[2] +" "+ RC[3]); // send the command eg,. 'rc 10 00 32 00'
            Arrays.fill(RC, 0); // reset the array with 0 after every virtual joystick move

        });

        JoystickView rightjoystick = (JoystickView) findViewById(R.id.joystickViewRight);
        rightjoystick.setOnMoveListener((angle, strength) -> {
            if (angle >45 && angle <=135){
                RC[1]= strength;
            }
            if (angle >226 && angle <=315){
                strength *= -1;
                RC[1]= strength;
            }
            if (angle >135 && angle <=225){
                strength *= -1;
                RC[0]= strength;
            }
            if (angle >316 && angle <=359 || angle >0 && angle <=45){
                RC[0]= strength;
            }

            telloConnect("rc "+ RC[0] +" "+ RC[1] +" "+ RC[2] +" "+ RC[3]);
            Arrays.fill(RC, 0); // reset the array with 0 after every virtual joystick move
        });

    }  // end of oncreate

    public void telloConnect(final String strCommand){
        new Thread(new Runnable() { // create a new runnable thread to handle tello state
            public void run() {
                Boolean run = true; // always keep running once initiated
                try {
                    if (strCommand == "disconnect"){
                        run = false;
                    }
                    DatagramSocket udpSocket = new DatagramSocket(null); // create a datagram socket with null attribute so that a dynamic port address can be chosen later on

                    InetAddress serverAddr = InetAddress.getByName("192.168.10.1");     // set the tello IP address (refer Tello SDK 1.3)
                    byte[] buf = (strCommand).getBytes("UTF-8");             // command needs to be in UTF-8
                    DatagramPacket packet = new DatagramPacket(buf, buf.length,serverAddr, 8889); // crate new datagram packet
                    udpSocket.send(packet);     // send packets to port 8889
                    while (run){
                        byte[] message = new byte[1518];        // create a new byte message (you can change the size)
                        DatagramPacket rpacket = new DatagramPacket(message,message.length);
                        Log.i("UDP client: ", "about to wait to receive");
                        udpSocket.setSoTimeout(2000);           // set a timeout to close the connection
                        udpSocket.receive(rpacket);             // receive the response packet from tello
                        String text = new String(message, 0, rpacket.getLength()); // convert the message to text
                        Log.d("Received text", text);       // display the text as log in Logcat
                        new Thread(new Runnable() {             // create a new thread to stream tello state
                            @Override
                            public void run() {
                                while (!interrupted()){
                                    sleep(2000);            // I chose 2 seconds as the delay
                                    byte[] buf = new byte[0];
                                    try {
                                        buf = ("battery?").getBytes("UTF-8");
                                        DatagramPacket packet = new DatagramPacket(buf, buf.length,serverAddr, 8889);
                                        udpSocket.send(packet);

                                        DatagramSocket socket = new DatagramSocket(null);   // create a new datagram socket
                                        socket.setReuseAddress(true);                               // set the reuse
                                        socket.setBroadcast(true);
                                        socket.bind(new InetSocketAddress(8890));              // bind to tello state port (refer to SDK 1.3)

                                        byte[] message = new byte[1518];
                                        DatagramPacket rpacket = new DatagramPacket(message,message.length); //, serverAddr, 8890
                                        socket.receive(rpacket);
                                        String text = new String(message, 0, rpacket.getLength());
                                        Matcher DCML = statePattern.matcher(text);                  // use the regex pattern initiated at the beginning of the code to parse the response from tell drone
                                        List<String> dec = new ArrayList<String>();                      // parse the response and store it in an array
                                        while (DCML.find()) {
                                            dec.add(DCML.group());
                                        }

                                        Log.d("Battery Charge : ",text+"%");
                                        telloStateHandler.post(new Runnable() {                     // use the initiated handler to post the tello state output the drone controller UI
                                            @Override
                                            public void run() {
                                                try{
                                                    jdroneBattery.setText("Battery: "+ dec.get(10)+"%");
                                                    if (Integer.parseInt(dec.get(10)) <= 15){
                                                        jdroneBattery.setBackgroundResource(R.drawable.rounded_corner_red); // if battery percentage is below 15 set the background of text to red
                                                    }
                                                    else {
                                                        jdroneBattery.setBackgroundResource(R.drawable.rounded_corner_green); // else display batter percentage with green background
                                                    }
                                                    if (Integer.parseInt(dec.get(10)) != 0){
                                                        jdroneWIFI.setBackgroundResource(R.drawable.rounded_corner_green);     // if wifi is connected and is active then display with green background
                                                        jdroneWIFI.setText("WIFI: connected");
                                                    }
                                                    jdroneTOF.setText("TOF: "+dec.get(8)+"cm");
                                                    jdroneBaro.setText("Baro: "+dec.get(11)+"m");
                                                    jdroneHeight.setText("Height: "+dec.get(9));
                                                    jdroneTemperature.setText("Temperature: "+dec.get(7)+"C");
                                                    jdroneSpeed.setText("Speed :"+ Integer.parseInt(dec.get(3)) + Integer.parseInt(dec.get(4)) + Integer.parseInt(dec.get(5))+"cm/s");
                                                    jdroneAccleration.setText("Acceleration: "+Math.round(Math.sqrt(Math.pow(Double.parseDouble(dec.get(13)),2)+Math.pow(Double.parseDouble(dec.get(14)),2)+Math.pow(Double.parseDouble(dec.get(15)),2)))+"g");
                                                    // https://physics.stackexchange.com/questions/41653/how-do-i-get-the-total-acceleration-from-3-axes
                                                    // for calculating accleration I refered to the above link

                                                    telloStateHandler.removeCallbacks(this);

                                                }catch (Exception e){
                                                    Log.e("Array out of bounds", "error",e);
                                                }
                                            }
                                        });

                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }
                        }).start();

                    }

                } catch (SocketException | UnknownHostException e) {
                    Log.e("Socket Open:", "Error:", e);
                }
                catch (IOException e){
                    Log.e("IOException","error",e);
                }

            }
        }).start();
    }
}