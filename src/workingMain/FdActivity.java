package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;


public class FdActivity extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "Project::MainActivity";
    private static final String    DCDEBUG             = "darrynDebug";
    private static final Scalar    DETECT_RECT_COLOR   = new Scalar(255, 0, 0, 255);	// Red
    private static final Scalar    ROI_LINE_COLOR      = new Scalar(0, 0, 255, 255);	// Green

    private MenuItem			   menuItemlineDisplay;
    private MenuItem			   menuItemChangeCamera;
    private MenuItem			   menuItemCamMode1;
    private MenuItem			   menuItemCamMode2;
    private MenuItem			   menuItemCamMode3;
    private TextView 			   numberOfRepsText;
    private TextView 			   lastDbRepEntry;
    private Button 				   repsToDB;
    private Button 				   finishSession;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private DetectionBasedTracker  mNativeDetector;
    private CameraBridgeViewBase   mOpenCvCameraView;
    private boolean				   camChange			=true;
    
    private Mat 				   mFGMask;
    private BackgroundSubtractorMOG2 backsub;
    
    private Point				   p1;
    private Point				   p2;
    private int 				   screenHeight;
    private int 				   screenWidth;
    private int					   lineSet;
    private boolean				   lineShow				= true;
    private int					   operatingMode        = 1;
    
    final Handler 				   myHandler 			= new Handler();
    private int 				   repCount 			= 0;
    private boolean 			   repTestFlag 			= false;
    private String 				   formattedDate;
    private String 				   userLifting;
    private String				   exerciseToDo;
    private int					   weightToLift;
    private int 				   setNumber			= 1;
    
    /** Thread to update the number of reps on screen. */
    final Runnable updateRepCountResult = new Runnable() {
    	public void run() {
    		updateRepCount();
    	}
    };
    final Runnable updateLastRepQuery = new Runnable() {
    	public void run() {
    		updateLastSet();
    	}
    };

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_weightplate6_5);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_weightplate6_5.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);
                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        Intent startingPage = new Intent(FdActivity.this, StartingActivity.class);
        startActivityForResult(startingPage,111); 
        Intent appOpen = new Intent(FdActivity.this, OpeningActivity.class);
        startActivity(appOpen);
        
        setContentView(R.layout.rep_count_view);
        numberOfRepsText =(TextView) findViewById(R.id.numberOfReps);
        lastDbRepEntry =(TextView) findViewById(R.id.lastDbRepEntry);
        repsToDB = (Button) findViewById(R.id.repsToDB);
        finishSession = (Button) findViewById(R.id.finishSession);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        mOpenCvCameraView.setMaxFrameSize(1280, 720);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();
        
        final MyDbHelper myDB = new MyDbHelper(this);
        
        repsToDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    myDB.setInfoToDB(formattedDate, userLifting, exerciseToDo, weightToLift, setNumber, repCount);
                    myHandler.post(updateLastRepQuery);
                } catch (Exception e) {
                    Log.i(DCDEBUG, "ERROR WITH ONLICK LISTENER: " + e.getMessage());
                }
            }
        });    
        finishSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                	setNumber = 1;
                	mGray.release();
                    mRgba.release();
                	mOpenCvCameraView.disableView();
                    Intent finalPage = new Intent(FdActivity.this, FinalActivity.class);
                    finalPage.putExtra("uName", userLifting);
                    finalPage.putExtra("uDate", formattedDate);
                    startActivity(finalPage);                    
                } catch (Exception e) {
                    Log.i(DCDEBUG, "ERROR WITH ONLICK STARTING FINAL ACTIVITY LISTENER: " + e.getMessage());
                }
            }
        });  
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yy", Locale.UK);
        formattedDate = df.format(cal.getTime());   
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	try {
    		if ((requestCode == 111) && (resultCode == Activity.RESULT_OK)) {
    			Bundle userInputReceived = data.getExtras();
    			userLifting = userInputReceived.getString("user");
    			exerciseToDo = userInputReceived.getString("exer");
    			weightToLift = userInputReceived.getInt("weight");
    			if(exerciseToDo.equals("Deadlift"))
    				lineSet = 600;
    			else if(exerciseToDo.equals("Bicep Curls"))
    				lineSet = 700;
    	        getScreenHeightWidth();        
    			Log.i(DCDEBUG, "LineSet to: " + lineSet);
    			Log.i(DCDEBUG, "Returned Values: " + userLifting + ", " + exerciseToDo + ", " + weightToLift);
    		}
    	} catch (Exception e) {
    		Log.i(DCDEBUG, "ERROR WITH RETRIEVING BUNDLE DATA" + e.getMessage());
    	}
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    
    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "called onCreateOptionsMenu");
        menuItemlineDisplay = menu.add("Line Display");
        menuItemChangeCamera = menu.add("Swap Camera");
        menuItemCamMode1 = menu.add("Mode 1 (Default)");
        menuItemCamMode2 = menu.add("Mode 2");
        menuItemCamMode3 = menu.add("Mode 3");
		return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == menuItemlineDisplay)
            lineShow = !lineShow;
        else if(item == menuItemChangeCamera){
        	camChange = !camChange;
        	if(camChange == false){
        		mOpenCvCameraView.disableView();
                mOpenCvCameraView.setCameraIndex(1);
                mOpenCvCameraView.enableView();
        	}
        	else {
        		mOpenCvCameraView.disableView();
                mOpenCvCameraView.setCameraIndex(0);
                mOpenCvCameraView.enableView();
        	}
        }
        else if(item == menuItemCamMode1)
        	operatingMode = 1;
        else if(item == menuItemCamMode2)
        	operatingMode = 2;
        else if(item == menuItemCamMode3)
        	operatingMode = 3;
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        mFGMask = new Mat();
        backsub = Video.createBackgroundSubtractorMOG2();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mFGMask.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if(!camChange){
        	Core.flip(mRgba, mRgba, 1);
        	Core.flip(mGray, mGray, 1);
        }
        if(lineShow)
        	Imgproc.line(mRgba, p1, p2, ROI_LINE_COLOR,8);
        
        switch(operatingMode){
        case 1:
        	MatOfRect detectedObj = new MatOfRect();
            mNativeDetector.detect(mGray, detectedObj);
            
            Rect[] detObjArray = detectedObj.toArray();
            for (int i = 0; i < detObjArray.length; i++){
            	Point centerRec = new Point((detObjArray[i].tl().x+detObjArray[i].br().x)/2,(detObjArray[i].tl().y+detObjArray[i].br().y)/2);
                Imgproc.rectangle(mRgba, detObjArray[i].tl(), detObjArray[i].br(), DETECT_RECT_COLOR, 3); 
                if(centerRec.y < p1.y && repTestFlag == false){
                	repCount++;
                	repTestFlag = true;
                }
                if(centerRec.y > p1.y+detObjArray[i].height/2)
                	repTestFlag = false;
            }
            myHandler.post(updateRepCountResult);
        	break;
        case 2:
        	
        	break;
        case 3:
        	lineShow = false;
        	backsub.apply(mRgba, mFGMask, 0.1);
        	Imgproc.cvtColor(mFGMask, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
        	break;
        default:
        	break;
        }
        return mRgba;
    }
    
    /** Update the on screen number of reps. */
    public void updateRepCount() {
    	numberOfRepsText.setText("Reps = " + String.valueOf(repCount));
    }
    
    public void updateLastSet() {
    	lastDbRepEntry.setText("Set Recorded(#" + setNumber + ") Reps: " + repCount);
        setNumber++;
        repCount = 0;
    }
    
    /** Calculate and store the screen height and width. */
    public void getScreenHeightWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;
		p1 = new Point(0,screenHeight-lineSet);
        p2 = new Point(screenWidth,screenHeight-lineSet);
    }
}