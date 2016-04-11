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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.Cursor;
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
    private SQLiteDatabase 		   db;

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
    private String 				   repDisplay;
    private String				   newDate;
    private String 				   formattedDate;
    private String 				   userLifting			= "Darryn";
    private String				   exerciseToDo			= "Deadlift";
    private int					   weightToLift			= 10;
    private int 				   setNumber			= 0;
    
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
        
        repsToDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    insertDbData();
                    readDbData();
                    repCount = 0;
                    setNumber++;
                } catch (Exception e) {
                    Log.i(DCDEBUG, "ERROR WITH ONLICK LISTENER: " + e.getMessage());
                }
            }
        });    
        finishSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                	setNumber = 0;
                	mGray.release();
                    mRgba.release();
                	mOpenCvCameraView.disableView();
                    Intent finalPage = new Intent(FdActivity.this, FinalActivity.class);
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
        openDatabase();
        
        Intent startingPage = new Intent(FdActivity.this, StartingActivity.class);
        startActivity(startingPage);  
        
    }

    @Override
    public void onPause() {
    	dropTable();
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
        db.close();
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    /** Open the database, or create it if it does not exist. */
    public void openDatabase() {
        try {
            String SDcardPath = "data/data/org.opencv.samples.facedetect";
            String DBpath = SDcardPath + "/" + "projectDB.db";
            Log.i(DCDEBUG, "DB Path: " + DBpath);
            db = SQLiteDatabase.openDatabase(DBpath, null, SQLiteDatabase.CREATE_IF_NECESSARY);
            Log.i(DCDEBUG, "DB Opened ");
        } catch (SQLiteException e) {
            Log.i(DCDEBUG, "Error opening DB: " + e.getMessage());
            finish();
        }      
    }

    /** Create the tables if they do not exist in the DB and populate. */
    public void insertDbData() {
        // create the table
        db.beginTransaction();
        try {
            // need to edit XML
            db.execSQL("create table if not exists repTable("
                + "tblID integer PRIMARY KEY autoincrement, "
            	+ "Date text, "
                + "User text, "
            	+ "Exercise text, "
                + "Weight integer, "
            	+ "SetNumber integer, "
                + "Reps integer); ");
            db.setTransactionSuccessful();
            Log.i(DCDEBUG, "Table created successfully");
        } catch (SQLException e1) {
            Log.i(DCDEBUG, "Error creating table: " + e1.getMessage());
            finish();
        }
        finally {
            db.endTransaction();
        }
     
        // populate the table
        db.beginTransaction();
        try {
            // need to edit XML
            db.execSQL("insert into repTable(Date, User, Exercise, Weight, SetNumber, Reps) values "
            		+ "('"+formattedDate+"',"
            		+ "'"+userLifting+"',"
            		+ "'"+exerciseToDo+"',"
            		+ "'"+weightToLift+"',"
            		+ "'"+setNumber+"',"
            		+ "'"+repCount+"');");
            db.setTransactionSuccessful();
            Log.i(DCDEBUG, repCount + " inserted into table successfully");
        } catch (SQLException e2) {
            Log.i(DCDEBUG, "Error inserting into DB: " + e2.getMessage());
            finish();
        }
        finally {
            db.endTransaction();
        }
    }

    /** Query the DB and run the update thread. */
    public void readDbData() {
    	int i=0;
        db.beginTransaction();

        try {
            Cursor curs =  db.rawQuery("SELECT * FROM repTable where tblID = (select max(tblID) from repTable)", null);
            if (curs.moveToFirst()){
            	do {
            		newDate = curs.getString(i+1);
            		setNumber = curs.getInt(i+5);
                    repDisplay = curs.getString(i+6);
                    myHandler.post(updateLastRepQuery);
                    i+=7;
            	} while (curs.moveToNext());
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.i(DCDEBUG, "Error reading from DB: " + e.getMessage());
        }
        finally {
            db.endTransaction();
        }
    }

    /** Drop the repTable from the DB. */
    public void dropTable() {
        // (clean start) action query to drop table
    	db.beginTransaction();
        try {
            db.execSQL("drop table if exists repTable;");
            db.setTransactionSuccessful();
            Log.i(DCDEBUG, "Table dropped successfully");
        } catch (Exception e) {
            Log.i(DCDEBUG, "Table dropped error: " + e.getMessage());
            finish(); 
        }
        finally {
            db.endTransaction();
        }
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
    	lastDbRepEntry.setText("Date: " + newDate + " Set: #" + setNumber + " Reps: " + repDisplay);
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