package com.androidexample.camera;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class VideoCameraActivity extends Activity {
	
	private static final Logger LOG = Logger.getLogger(VideoCameraActivity.class);
	private static final String CAMERA_OPEN_TASK = "CAMERA_OPEN_TASK";
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	private static final String TAG = "VideocameraActivity";
	private static final int RESULT_SETTINGS = 1;
	public static VideoCameraActivity ActivityContext =null; 
	public static TextView output;
	private CameraPreview mPreview;
	private Camera mCamera;
	private MediaRecorder mMediaRecorder;
	private boolean isRecording = false;
	
	HandlerThread mCameraThread;
	Handler mCameraHandler; 
	HandlerThread mOpenSocketThread;
	Handler mOpenSocketHandler; 

	private static String fileName;
	
	private static final HashMap<String, String> videoQuality;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ConfigureLog4J.configure();
        initializeUI();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (mMediaRecorder == null) {
    		initializeUI();
    	}
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	releaseMediaRecorder();
    	releaseCamera();
    }
    
    private void initializeUI() {
        setContentView(R.layout.main);

        // Create an instance of Camera
        LOG.info("onCreate: Getting camera instance");
    	initializeVideoCamera(); 
    	// will start a thread do start camera
        //startCamera();
        //new OpenCameraTask().execute(mCamera);

        try {
            Log.i(TAG, "Sleeping for 1 seconds");
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        LOG.info("-------------------Camera instance mCamera:" + mCamera);
        LOG.info(TAG+ " Creating preview");

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        
        
        LOG.info(TAG+ " Adding listener to button");
        // Add a listener to the Capture button
        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	LOG.info("Capture button clicked isRecording: " + isRecording);
                    if (isRecording) {
                        // stop recording and release camera
                        mMediaRecorder.stop();  // stop the recording
                        releaseMediaRecorder(); // release the MediaRecorder object
                        mCamera.lock();         // take camera access back from MediaRecorder

                        // inform the user that recording has stopped
                        setCaptureButtonText("Start Recording");
                        Toast.makeText(getApplicationContext(), "Recorded: " + fileName, Toast.LENGTH_LONG).show();
                        
                        isRecording = false;
                    } else {
                        // initialize video camera
                    	//initializeVideoCamera(); // will start a thread do start camera
                    	// onPostExecute, will call startRec
                    	// onPostExecut, will call startRec
                    	 startRec();
//                    	if (prepareVideoRecorder()) {
//                    		mMediaRecorder.start();
//                    	}
                    	 isRecording = true;
                    }
                }
            }
        );
       
    }
    
    
    private void initializeVideoCamera() {
    	LOG.debug("initializeVideoCamera mCamera: " + mCamera);

		Log.e("initializeVideoCamera", "openning camera");
        try {
        	if (mCamera == null) {
        		mCamera = Camera.open();
        	}
        } catch (Exception e) {
            Log.e("OpenCameraTask", "failed to open Camera");
            e.printStackTrace();
        }
    }
    
    public void startRec() {
    	LOG.debug("Start rec called mCamera: " + mCamera);
    	if (mCamera == null) {
    		Toast.makeText(getApplicationContext(), "Wait still opening camera", Toast.LENGTH_SHORT).show();
    		return;
    	}

        if (prepareVideoRecorder()) {
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
        	LOG.debug("Rec starting");
        	try {
        		mMediaRecorder.start();
        	} catch (IllegalStateException e) {
        		LOG.error("Failed to start Camera. " + e.getMessage());
        		e.printStackTrace();
        	}

            // inform the user that recording has started
            setCaptureButtonText("Stop Recording");
            isRecording = true;
        } else {
        	LOG.error("Failed to prepare Camera. Releasing it.");
            // prepare didn't work, release the camera
            releaseMediaRecorder();
            // inform user
        }
    }
    /**
     * Open the camera. It is recommended to open it in a separated thread
     * as it can take some time to start.
     * 
     */
    private void startCamera() {
    	LOG.info("startCamera Camera:" + mCamera); 
        if (mCameraThread == null) {
            mCameraThread = new HandlerThread(CAMERA_OPEN_TASK);
            mCameraThread.start();
            mCameraHandler = new Handler(mCameraThread.getLooper());
        }
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCamera = Camera.open();
                    LOG.info("startCamera Camera: " + mCamera);  
                }  catch (Exception e){
                    // Camera is not available (in use or does not exist)
                	LOG.error("Camera could not be opened." + e.getMessage());                	
                }
            }
        });
    }
    
    
    public void setCaptureButtonText(String text) {
    	LOG.info("Setting button text: " + text);
    	 Button captureButton = (Button) findViewById(R.id.button_capture);
    	 captureButton.setText(text);
    }
    
    
    private boolean prepareVideoRecorder(){

    	LOG.info("prepareVideoRecorder Camera:" + mCamera);
        mMediaRecorder = new MediaRecorder();
    	
    	
    	//SensorManager.remapCoordinateSystem()
    	/*
    	 *     public static final int ORIENTATION_UNDEFINED = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;
    public static final int ORIENTATION_SQUARE = 3;
    	 */
    	
	   	 /* Acordingly to 
	   	  * #6 josephba...@gmail.com
			  * I had this problem when triggering a video capturing in my app while the video preview was live.  After much wailing and gnashing of teeth, I figured out that to start video capture on a previewing camera, I had to first completely stop the video preview.  This was accomplished with the following code, which I put of the top of my prepareVideoRecorder() implementation, prior to doing anything with a MediaRecorder:
	   	  */
        try {
   		 Log.d(TAG, "setting camera preview to nulll");
   			mCamera.setPreviewDisplay(null);
   		} catch (java.io.IOException ioe) {
   			Log.d(TAG, "IOException nullifying preview display: " + ioe.getMessage());
   		}
        Log.d(TAG, "stopping camera preview");
   		mCamera.stopPreview();        

        Log.d(TAG, "unlocking camera");
        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        Log.d(TAG, "mediarecorder setting audio/video sources");
        // Step 2: Set sources
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        
//        // Following code does the same as getting a CamcorderProfile (but customizable)
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        // Video Settings 1280x720 
//        mMediaRecorder.setVideoSize(1280, 720);
//        mMediaRecorder.setVideoFrameRate(7);
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
//        //mMediaRecorder.setVideoEncodingBitRate(VIDEO_BITRATE);
//        // Audio Settings
//        mMediaRecorder.setAudioChannels(1); // 1- mono, 2 - stereo
//        mMediaRecorder.setAudioSamplingRate(8000);
//        mMediaRecorder.setAudioEncodingBitRate(8000);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); 
    	
        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        //mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
        Log.d(TAG, "mediarecorder setting camcorder settings");
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_480P));
        //mMediaRecorder.setCaptureRate(0.1); // capture a frame every 10 seconds
        mMediaRecorder.setCaptureRate(1); // capture a frame every 1 seconds

    	LOG.info("prepareVideoRecorder setting output to socket");
        // Step 4: Set output file to the socket
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        mMediaRecorder.setOrientationHint(0);

        Log.d(TAG, "mediarecorder setting preview display");        
        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
        //mMediaRecorder.setPreviewDisplay(null);

        Log.d(TAG, "mediarecorder will be prepared");
        LOG.info("prepareVideoRecorder will prepare mediarecorder");
        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
            LOG.info("prepareVideoRecorder mediaRecorder prepared");
        } catch (IllegalStateException e) {
        	LOG.error("IllegalStateException preparing MediaRecorder: " + e.getMessage());

            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
        	LOG.error("IOException preparing MediaRecorder: " + e.getMessage());
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }
    
    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                  Environment.DIRECTORY_PICTURES), "timelapse");
        

        Log.d(TAG, "mediaStorageDir: " + mediaStorageDir);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        // This method returns the standard, shared and recommended location 
        // for saving pictures and videos. 

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }
        
        fileName = mediaFile.toString();

        Log.d(TAG, "mediaFile: " + mediaFile);
        return mediaFile;
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called -------------------------- ");
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        //releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder(){
    	Log.d(TAG, "releasing media recorder -------------------------- ");
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
    

	/**
	 * @return the mCamera
	 */
	public Camera getmCamera() {
		return mCamera;
	}


	/**
	 * @param mCamera the mCamera to set
	 */
	public void setmCamera(Camera mCamera) {
		this.mCamera = mCamera;
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }
 
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
 
        case R.id.menu_settings:
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, RESULT_SETTINGS);
            break;
        }
        return true;
    }
 
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
 
        switch (requestCode) {
        case RESULT_SETTINGS:
            showUserSettings();
            break;
 
        }
    }
 
    private void showUserSettings() {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
 
        StringBuilder builder = new StringBuilder();
 
        builder.append("\n Video Quality: "
                + videoQuality.get(sharedPrefs.getString("prefVideoQuality", "1004")));
 
        builder.append("\n FPS:"
                + sharedPrefs.getString("prefVideoFPS", "1"));

 
        Toast.makeText(getApplicationContext(), "Settings " + builder.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConfigurationChanged(Configuration myConfig) {
        super.onConfigurationChanged(myConfig);
        Log.d(TAG, "onConfigurationChanged -------------------------- ");
        
        
        
    	
    	Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    	int rotation = display.getRotation();
    	
    	Log.d(TAG, "-------------------- display.getConfiguration : " + rotation);
    	
    	int orientation = getResources().getConfiguration().orientation;
    	
    	Log.d(TAG, "-------------------- getConfiguration.orientation : " + rotation);
        int orient = getResources().getConfiguration().orientation; 
        switch(orient) {
                    case Configuration.ORIENTATION_LANDSCAPE:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
                    case Configuration.ORIENTATION_PORTRAIT:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        break;
                    default:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
    }
    
    static {
    	videoQuality = new HashMap<String, String>();
    	videoQuality.put("1000", "Low (lowest available resolution)");
    	videoQuality.put("1002", "QCIF (176 x 144)");
    	videoQuality.put("1007", "QVGA (320 x 240)");
    	videoQuality.put("1003", "CIF (352 x 288)");
    	videoQuality.put("1004", "480p (720 x 480)");
    	videoQuality.put("1005", "720p (1280 x 720)");
    	videoQuality.put("1006", "1080p (1920 x 1088)");
    	videoQuality.put("1001", "High (highest available resolutions)");
    }

}