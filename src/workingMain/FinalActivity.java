package org.opencv.samples.facedetect;

import java.util.ArrayList;

import android.app.Activity;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

public class FinalActivity extends Activity {
	private static final String    DCDEBUG             = "darrynFinishingDebug";
	ListView 					   lv;
	ArrayList<String> 			   dbRow;
    private SQLiteDatabase 		   db;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finishpage);
        
        lv = (ListView) findViewById(R.id.listView1);
        lv.setBackgroundColor(Color.LTGRAY);
        finalOpenDatabase();
        dbToListView();
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
            Cursor curs =  db.rawQuery("SELECT * FROM repTable where tblID = (select max(tblID) from repTable)", null);
            if (curs.moveToFirst()){
            	do {

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
}