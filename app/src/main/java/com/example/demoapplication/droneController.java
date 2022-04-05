package com.example.demoapplication;

import static android.os.SystemClock.sleep;
import static java.lang.Thread.interrupted;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
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
import java.util.List;
import java.util.regex.Matcher;
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
    private ImageView actionTakeOff; // button to get the drone to takeoff
    private ImageView actionLnad;    // button to get the drone to land

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

    }  // end of oncreate

    public void telloConnect(final String strCommand){
        new Thread(new Runnable() {
            public void run() {
                Boolean run = true;
                try {
                    if (strCommand == "disconnect"){
                        run = false;
                    }
                    Log.d("Tello connection :","call3");
                    DatagramSocket udpSocket = new DatagramSocket(null);

                    InetAddress serverAddr = InetAddress.getByName("192.168.10.1");
                    byte[] buf = (strCommand).getBytes("UTF-8");
                    DatagramPacket packet = new DatagramPacket(buf, buf.length,serverAddr, 8889);
                    udpSocket.send(packet);
                    while (run){
                        byte[] message = new byte[1518];
                        DatagramPacket rpacket = new DatagramPacket(message,message.length);
                        Log.i("UDP client: ", "about to wait to receive");
                        udpSocket.setSoTimeout(2000);
                        udpSocket.receive(rpacket);
                        String text = new String(message, 0, rpacket.getLength());
                        Log.d("Received text", text);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (!interrupted()){
                                    sleep(2000);
                                    byte[] buf = new byte[0];
                                    try {
                                        buf = ("battery?").getBytes("UTF-8");
                                        DatagramPacket packet = new DatagramPacket(buf, buf.length,serverAddr, 8889);
                                        udpSocket.send(packet);

                                        DatagramSocket socket = new DatagramSocket(null);
                                        socket.setReuseAddress(true);
                                        socket.setBroadcast(true);
                                        socket.bind(new InetSocketAddress(8890));

                                        byte[] message = new byte[1518];
                                        DatagramPacket rpacket = new DatagramPacket(message,message.length); //, serverAddr, 8890
                                        socket.receive(rpacket);
                                        String text = new String(message, 0, rpacket.getLength());
                                        Matcher DCML = statePattern.matcher(text);
                                        List<String> dec = new ArrayList<String>();
                                        while (DCML.find()) {
                                            dec.add(DCML.group());
                                        }

                                        Log.d("Battery Charge : ",text+"%");
                                        telloBattHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                try{
                                                    jdroneBattery.setText("Battery: "+ dec.get(10)+"%");
                                                    if (Integer.parseInt(dec.get(10)) <= 15){
                                                        jdroneBattery.setBackgroundResource(R.drawable.rounded_corner_red);
                                                    }
                                                    else {
                                                        jdroneBattery.setBackgroundResource(R.drawable.rounded_corner_green);
                                                    }
                                                    if (Integer.parseInt(dec.get(10)) != 0){
                                                        jdroneWIFI.setBackgroundResource(R.drawable.rounded_corner_green);
                                                        jdroneWIFI.setText("WIFI: connected");
                                                    }
                                                    jdroneTOF.setText("TOF: "+dec.get(8)+"cm");
                                                    jdroneBaro.setText("Baro: "+dec.get(11)+"m");
                                                    jdroneHeight.setText("Height: "+dec.get(9));
                                                    jdroneTemperature.setText("Temperature: "+dec.get(7)+"C");
                                                    jHorizontal.setRotation(Integer.parseInt(dec.get(0))*2);
                                                    jdroneSpeed.setText("Speed :"+ Integer.parseInt(dec.get(3)) + Integer.parseInt(dec.get(4)) + Integer.parseInt(dec.get(5))+"cm/s");
                                                    jdroneAccleration.setText("Acceleration: "+Math.round(Math.sqrt(Math.pow(Double.parseDouble(dec.get(13)),2)+Math.pow(Double.parseDouble(dec.get(14)),2)+Math.pow(Double.parseDouble(dec.get(15)),2)))+"g");
                                                    // https://physics.stackexchange.com/questions/41653/how-do-i-get-the-total-acceleration-from-3-axes

                                                    telloBattHandler.removeCallbacks(this);

//                                                                                }
//                                                                            });
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