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
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;


public class FdActivity extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "Project::MainActivity";
    private static final String    DCDEBUG             = "darrynDebug";
    private static final Scalar    DETECT_RECT_COLOR   = new Scalar(255, 0, 0, 255);	// Red
    private static final Scalar    LINE_COLOR     	   = new Scalar(0, 0, 255, 255);	// Green

    private TextView 			   numberOfRepsText;
    private TextView 			   lastDbRepEntry;
    private Button 				   repsToDB;
    private Button 				   finishSession;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private DetectionBasedTracker  mNativeDetector;
    private CameraBridgeViewBase   mOpenCvCameraView;

    private Point				   p1;
    private Point				   p2;
    private int					   lineSet				= 600;
    private int 				   screenHeight;
    private int 				   screenWidth;
    
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
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_weightplate2);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_weightplate5.xml");
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
                    repCount = 0;
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
        getScreenHeightWidth();        
        Intent startingPage = new Intent(FdActivity.this, StartingActivity.class);
        startActivityForResult(startingPage,111); 
        
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
    			Log.i(DCDEBUG, "Returned Values: " + userLifting + ", " + exerciseToDo + ", " + weightToLift);
    		}
    	} catch (Exception e) {
    		Log.i(DCDEBUG, "ERROR WITH RETRIEVING BUNDLE DATA" + e.getMessage());
    	}
    }

    @Override
    public void onPause() {
    	//dropTable();
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

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        MatOfRect detectedObj = new MatOfRect();
        if (mNativeDetector != null)
                mNativeDetector.detect(mGray, detectedObj);
        else 
            Log.e(TAG, "Detection method is not selected!");
        
    	Imgproc.line(mRgba, p1, p2, LINE_COLOR,8);
        
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
        return mRgba;
    }
    
    /** Update the on screen number of reps. */
    public void updateRepCount() {
    	numberOfRepsText.setText("Reps = " + String.valueOf(repCount));
    }
    
    public void updateLastSet() {
    	lastDbRepEntry.setText("Set Recorded(#" + setNumber + ") Reps: " + repCount);
        setNumber++;
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