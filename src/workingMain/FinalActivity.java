package org.opencv.samples.facedetect;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FinalActivity extends Activity {
	private static final String    DCDEBUG             = "darrynFinishingDebug";
	private String passedUserName;
	private String passedDate;
	TextView finalTextView;
	ListView 					   lv;
	ArrayList<String> 			   currentDayInfo	   = new ArrayList<String>();
    private SQLiteDatabase 		   db;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finishpage);
        finalTextView = (TextView) findViewById(R.id.finalTextView);
        
        Intent thisIntent = getIntent();
        Bundle passedData = thisIntent.getExtras();
        if(passedData != null){
        	passedUserName = passedData.getString("uName");
        	passedDate = passedData.getString("uDate");
        }
        
        finalTextView.setText("Summary for " + passedUserName + " for today(" + passedDate + ").");
        lv = (ListView) findViewById(R.id.listView1);
        lv.setBackgroundColor(Color.LTGRAY);
        ArrayAdapter<String> todayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, currentDayInfo);
        lv.setAdapter(todayAdapter);
        currentDayInfo.add("Yeah");
        currentDayInfo.add("This");
        currentDayInfo.add("My");
        currentDayInfo.add("Projecto");
        //finalOpenDatabase();
        //dbToListView();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        //db.close();
    }
    
    public void finalOpenDatabase() {
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
    
    public void dbToListView(){
    	int i=0;
        db.beginTransaction();
        try {
            Cursor finCurs1 =  db.rawQuery("SELECT Exercise, Weight, SetNumber, Reps FROM repTable WHERE User = '"+passedUserName+"' AND Date = '"+passedDate+"'", null);
            if(finCurs1.moveToFirst()){
        	Log.i(DCDEBUG, "I GET HERE 3");
	        	do {
	        		Log.i(DCDEBUG, "I GET HERE 4");
	        		StringBuilder sb = new StringBuilder();
	        		sb.append(finCurs1.getString(i)	  + "\t");
	        		sb.append(finCurs1.getString(i+1) + "\t");
	        		sb.append(finCurs1.getString(i+2) + "\t");
	        		sb.append(finCurs1.getString(i+3) + "\t");
	                Log.i(DCDEBUG, "StringBuilder: " + sb.toString());
	        		currentDayInfo.add(sb.toString());
	                i+=4;
	        	} while (finCurs1.moveToNext());
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.i(DCDEBUG, "Error reading from DB: " + e.getMessage());
        }
        finally {
            db.endTransaction();
        }
    }
}