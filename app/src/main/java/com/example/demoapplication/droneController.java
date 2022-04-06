package com.example.demoapplication;

import static android.os.SystemClock.sleep;
import static java.lang.Thread.interrupted;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
    private ImageView bitImageView; // this is the imageview to which the drone video feed will be displayed

    private int connectionClickCounter = 1; // for counting the number of times the button is clicked
    private boolean connectionFlag = false; // to check and maintain the connection status of the drone. Initially the drone is not conected, so the status is false

    Pattern statePattern = Pattern.compile("-*\\d{0,3}\\.?\\d{0,2}[^\\D\\W\\s]");  // a regex pattern to read the tello state
    private int RC[] = {0,0,0,0};       // initialize an array of variables for remote control
    private Handler telloStateHandler;  // and handler needs to be created to display the tello state values in the UI in realtime
    long startMs;                       // variable to calculate the time difference for video codec
    private MediaCodec m_codec;         // MediaCodec is used to decode the incoming H.264 stream from tello drone
    private boolean detectionFlag = false; // detectionFlag is used to track if the user wants to perform object detection
    private boolean videoStreamFlag = false; // keeps track of the video streaming status from the drone

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
        jdroneWIFI.setBackgroundResource(R.drawable.rounded_corner_red);
        bitImageView = findViewById(R.id.bitView);

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

        videoFeedaction.setOnClickListener(view -> {
            if (connectionFlag) {
                if (videoFeedaction.isChecked()) {
                    videoStreamFlag = true;
                    try {
                        BlockingQueue<Bitmap> frameV = new LinkedBlockingQueue<>(2); // you can increase the blocking queue capacity, it dosen't matter
                        videoServer("streamon", frameV);
                        Runnable DLV = new displayBitmap(frameV);
                        new Thread(DLV).start();
//                        Runnable r = new spikeDetectionThread(frameV);
//                        new Thread(r).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (!videoFeedaction.isChecked()) {
                    telloConnect("streamoff");
                    videoStreamFlag = false;
                }
            } else {
                Toast.makeText(droneController.this, "Drone disconnected", Toast.LENGTH_SHORT);
                videoFeedaction.setChecked(false);
            }
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
                                                    // for calculating acceleration I referred to the above link

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

    public void videoServer(final String strCommand, final BlockingQueue frameV) throws IOException { // add this for surfaceView : , Surface surface
        telloConnect(strCommand);

        BlockingQueue queue = frameV; // create a BlockingQueue since this function creates a thread and outputs a video frame which has to be displayed on the UI thread
                                      // populating a queue and withdrawing from the queue outside the thread seem to be the best way to get the individual frames.
        if (strCommand == "streamon"){
            new Thread(new Runnable() {
                Boolean streamon = true;    // keeps track if the video stream is on or off

                @Override
                public void run() {
                    // SPS and PPS are the golden key (it is like the right combination of keys used to unlock a lock) to decoding the video and displaying the stream
                    byte[] header_sps = {0, 0, 0, 1, 103, 77, 64, 40, (byte) 149, (byte) 160, 60, 5, (byte) 185}; // the correct SPS NAL
                    byte[] header_pps = {0, 0, 0, 1, 104, (byte) 238, 56, (byte) 128};  // the correct PPS NAL

                    MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 960, 720); // H.264 is also know as AVC or Advanced Video Coding
                    format.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps)); // pass the SPS keys
                    format.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps)); // pass the PPS keys
                    format.setInteger(MediaFormat.KEY_WIDTH, 960);              // by default the tello outputs 960 x 720 video
                    format.setInteger(MediaFormat.KEY_HEIGHT, 720);
                    format.setInteger(MediaFormat.KEY_CAPTURE_RATE,30);         // 25-30 fps is good. Feel free to experiment
                    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible); // the output is a YUV420 format which need to be converted later
                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 960 * 720);

                    try {
                        m_codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);  // initialize the decoder with AVC format.
                        m_codec.configure(format, null ,null,0); // pass the format configuration to media codec with surface 'null' if you are processing the video for tasks like object detection, if not set to true
                        startMs = System.currentTimeMillis();   //calculate time to pass to the codec
                        m_codec.start();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ByteArrayOutputStream output = new ByteArrayOutputStream();  // this is were the data from video stream will be stored before passing it to media codec
                    DatagramSocket socketVideo = null;
                    try {
                        socketVideo = new DatagramSocket(null);     // similar to telloState() we create datagram socket with null parameter for address
                        socketVideo.setReuseAddress(true);                  // we will be reusing the address
                        socketVideo.setBroadcast(true);
                        socketVideo.bind(new InetSocketAddress(11111)); // based on tell SDK 1.3, the port for receiving the video frames is 11111


                        byte[] videoBuf = new byte[2048];                   // create an empty byte buffer of size 2018 (you can change the size)
                        DatagramPacket videoPacket = new DatagramPacket(videoBuf, videoBuf.length); // create a datagram packet
//                        int destPos = 0;
//                        byte[] data_new = new byte[60000]; // 1460 + 3      // create another byte buffer of size 600000
                        while (streamon) {                                  // infinite loop to continuously receive
                            socketVideo.receive(videoPacket);               // receive packets from socket
//                            System.arraycopy(videoPacket.getData(), videoPacket.getOffset(), data_new, destPos, videoPacket.getLength());
//                            destPos += videoPacket.getLength();             // get the length of the packet
                            byte[] pacMan = new byte[videoPacket.getLength()]; // create a temporary byte buffer with the received packet size
                            System.arraycopy(videoPacket.getData(), videoPacket.getOffset(), pacMan, 0, videoPacket.getLength());
                            int len = videoPacket.getLength();
                            output.write(pacMan);
                            if (len < 1460) {                               // generally, each frame of video from tello is 1460 bytes in size, with the ending frame that is usually less than <1460 bytes which indicate end of a sequence
//                                destPos=0;
                                byte[] data = output.toByteArray();         // one the stream reaches the end of sequence, the entire byte array containing one complete frame is passed to data and the output variable is reset to receive newer frames
                                output.reset();                             // reset to receive newer frame
                                output.flush();
                                output.close();                             // close
                                int inputIndex = m_codec.dequeueInputBuffer(-1); // dosen't matter of its -1 of 10000
                                if (inputIndex >= 0) {
                                    ByteBuffer buffer = m_codec.getInputBuffer(inputIndex);
                                    if (buffer != null){
                                        buffer.clear(); // exp
                                        buffer.put(data); //  Caused by: java.lang.NullPointerException: Attempt to get length of null array // if nothing else pass: data
                                        // if you change buffer.put also change queueInputBuffer 3rd prameter (lenth of passed byffer array)
                                        long presentationTimeUs = System.currentTimeMillis() - startMs;
                                        m_codec.queueInputBuffer(inputIndex, 0, data.length, presentationTimeUs, 0);  // MediaCodec.BUFFER_FLAG_END_OF_STREAM -> produce green screen // MediaCodec.BUFFER_FLAG_KEY_FRAME and 0 works too // MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                                    }
                                }

                                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                                int outputIndex = m_codec.dequeueOutputBuffer(info, 100); // set it back to 0 if there is error associate with this change in value

                                if (outputIndex >= 0){

                                    if (!detectionFlag){
                                        m_codec.releaseOutputBuffer(outputIndex, false); // true if the surfaceView is available
                                    }

                                    else if (detectionFlag){
                                        try {
                                            Image image = m_codec.getOutputImage(outputIndex); // store the decoded (decoded by Mediacodec) data to Image format
                                            Bitmap BM = imgToBM(image);                        // convert from image format to BitMap format
                                            try {
                                                if (!queue.isEmpty()){
                                                    queue.clear();
                                                }
                                                queue.put(BM);                                 // pass the data to the queue created earlier
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        m_codec.releaseOutputBuffer(outputIndex, false); // true if the surface is available
                                    }
                                }
                            } // end of if to send full frame
                        }
                    } catch (SocketException socketException) {
                        socketException.printStackTrace();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }).start();

        }
        if (strCommand == "streamoff"){
            Log.d("Codec State","stopped and released called...");
            m_codec.stop();         // stop and release the codec
            m_codec.release();
        }

    }

    private Bitmap imgToBM(Image image){        // convert from Image to Bitmap format for neural network processing.
        Image.Plane[] p = image.getPlanes();
        ByteBuffer y = p[0].getBuffer();
        ByteBuffer u = p[1].getBuffer();
        ByteBuffer v = p[2].getBuffer();

        int ySz = y.remaining();
        int uSz = u.remaining();
        int vSz = v.remaining();

        byte[] jm8 = new byte[ySz + uSz + vSz];
        y.get(jm8, 0, ySz);
        v.get(jm8, ySz, vSz);
        u.get(jm8, ySz + vSz, uSz);

        YuvImage yuvImage = new YuvImage(jm8, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0,0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);
        byte[] imgBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imgBytes, 0 , imgBytes.length);
    }

    public class displayBitmap implements Runnable{

        protected BlockingQueue displayQueue;       // create a blocking queue to get the data from queue
        protected Bitmap displayBitmap;             // create a bitmap variable for displaying bitmap

        public displayBitmap(BlockingQueue displayQueue_){
            this.displayQueue = displayQueue_;
        }

        @Override
        public void run(){

            while (true){
                try {
                    displayBitmap = (Bitmap) displayQueue.take();           // take data (video frame) from blocking queue
                    displayQueue.clear();                                   // clear the queue after taking
                    if (displayQueue != null){
                        runOnUiThread(() -> {                               // needs to be on UI thread
                            bitImageView.setImageBitmap(displayBitmap);     // set the bitmap to current frame in the queue
                            bitImageView.invalidate();
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }   // end of displayBitmap
}