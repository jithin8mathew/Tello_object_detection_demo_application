# Tello drone-based object detection app for Android

### This repository will walk you through android app development process for object detection using Tello Drone.

<p align="center">

<a href="https://github.com/jithin8mathew/tailwindcss-v2-dark-mode-template">
   <img align="center" style="margin:0.5rem" src="https://img.shields.io/github/search/jitin8mathew/Tello_object_detection_demo_application/goto?style=for-the-badge"/>
  </a>

  <a href="https://github.com/jithin8mathew/tailwindcss-v2-dark-mode-template">
     <img align="center" style="margin:0.5rem" src="https://img.shields.io/codeclimate/issues/jithin8mathew/Tello_object_detection_demo_application?style=for-the-badge"/>
    </a>

  <a href="https://github.com/jithin8mathew/tailwindcss-v2-dark-mode-template">
         <img align="center" style="margin:0.5rem" src="https://img.shields.io/github/downloads/jithin8mathew/Tello_object_detection_demo_application/total?style=for-the-badge"/>
  </a>

   <a href="https://github.com/jithin8mathew/tailwindcss-v2-dark-mode-template">
           <img align="center" style="margin:0.5rem" src="https://img.shields.io/github/issues/jithin8mathew/Tello_object_detection_demo_application?style=for-the-badge"/>
   </a>

   <a href="https://github.com/jithin8mathew/tailwindcss-v2-dark-mode-template">
              <img align="center" style="margin:0.5rem" src="https://img.shields.io/github/license/jithin8mathew/Tello_object_detection_demo_application?style=for-the-badge"/>
      </a>


</p>

[![App banner](./app/src/main/assets/droneControl_initial.png)](https://github.com/jithin8mathew)

### This project will demonstrate how to:
- Create a UI for controlling Tello Drone
- Handle drone using virtual controls
- Process H.264 (AVC) encoded video from Tello and display it on app
- Perform near real-time object detection from Tello video frame

### Things to fix:
- 0.    Video feed keeps getting stuck during flight (Temporary fix: turn the video switch at the top left of the screen on and off again). A permanent fix is needed.
- 1.	I could not figure out a way to toggle between and SurfaceView and BitMap displaying for simply viewing video frames vs performing object detection
- 2.	Some of the calculations like drone acceleration etc. needs more attention and correction.
- 3.	Improve the efficiency of the code and simplify it.
- 4.	Closing datagram sockets while not in use or going back to the main page is not working.
- 5.    Capture image and record video buttons does not work at this point, but I will be adding these features soon.

[![App demo](./app/src/main/assets/demoApplication.gif)](https://github.com/jithin8mathew)

### How to install this application?

Open android studio> open demoApplication project > Build > run app (preferably connecting your phone)

### Things to keep in mind:

- While testing on Pixel 4 (Android 12) I faced issues associated with UDP connection. After connecting to Tello's wifi, under internet> wifi> TelloNetworkName> Privacy, there are two options. 1) Use randomized MAC (default) 2) Use device MAC. After connecting to Tello netwok, swith between these options before using the app.
- The above step will disconnect from the wifi and reconnect again, only then will this app work on Pixel 4 (Android 12), this could be only a problem on my device Pixel 4 in general (I don't know it yet)
- There could be several bugs, things could be done in an easier and efficient way, possibly. Please report bugs and issues.
- If you face any issues during development or installation, please raise an issue in my GitHub page for this project

 More importantly, I am excited to see cool application of this project. And finally, if you liked this project and find it interesting, please star the project so that it can reach more people. Thank you and hope you have some fun with this!.

This project is explained in detail at https://medium.com/@jithinjm1995/building-an-android-application-to-control-tello-drone-flight-and-perform-real-time-object-ab953f6c5f5b

# LICENSE

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.