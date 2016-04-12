package org.opencv.samples.facedetect;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class StartingActivity extends Activity implements OnItemSelectedListener {
	private static final String    DCDEBUG             = "darrynStartingDebug";
	private SQLiteDatabase 		   db;
	//private String[] 			   exerciseList		   = {"Deadlift","Bicep Curls"};
	private String[] 			   exerciseList;
	private EditText			   userView;
	private Spinner				   exerciseSpinner;
	private EditText			   weightInput;
	private	Button 				   startOpenCV;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appstart);
        
        userView = (EditText) findViewById(R.id.userView);
        exerciseSpinner = (Spinner) findViewById(R.id.exerciseSpinner);
        weightInput = (EditText) findViewById(R.id.weightInput);
        
        startOpenCV = (Button) findViewById(R.id.startOpenCV);   
        startOpenCV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });  
        
        startingOpenDatabase();
        getSpinnerValues();
        db.close();
        
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, exerciseList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exerciseSpinner.setAdapter(spinnerAdapter); 
        exerciseSpinner.setOnItemSelectedListener(this);
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	db.beginTransaction();
        try {
            db.execSQL("drop table if exists exerciseTable;");
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
    
    /** Open the database, or create it if it does not exist. */
    public void startingOpenDatabase() {
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
    
    public void getSpinnerValues() {
    	int i=0;
        db.beginTransaction();

        try {
            Cursor curs =  db.rawQuery("SELECT Exercise FROM exerciseTable", null);
            exerciseList = new String[curs.getCount()];
            if (curs.moveToFirst()){
            	do {
            		String temp = curs.getString(i);
            		exerciseList[i] = temp;
            		Log.i(DCDEBUG, "Error opening DB: " + exerciseList[i]);
                    i++;
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
    
 // next two methods implement the spinner's listener
    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
    	//exerciseSpinner.setSelection(position);
    	
    }
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    // TODO do nothing – needed by the interface
    }
    
}