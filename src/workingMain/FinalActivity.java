package org.opencv.samples.facedetect;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FinalActivity extends Activity {
	private static final String    DCDEBUG             = "darrynFinishingDebug";
	private String passedUserName;
	private String passedDate;
	TextView finalDayTextView;
	//TextView finalHistoryTextView;
	Button 				   		   historyButton;
	ListView 					   lvDay;
	//ListView 					   lvHistory;
	ArrayList<String> 			   currentDayInfo;
	ArrayList<String>			   historicalInfo;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	setTheme(android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finishpage);
        finalDayTextView = (TextView) findViewById(R.id.finalDayTextView);
        //finalHistoryTextView = (TextView) findViewById(R.id.finalHistoryTextView);
        historyButton = (Button) findViewById(R.id.historyButton);
        
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
        		//Toast.makeText(getApplicationContext(), "WORK IN PROGRESS!", Toast.LENGTH_LONG).show();
        		Fragment frag = new HistoricalInfoFragment();
        		FragmentTransaction ft = getFragmentManager().beginTransaction();
        		ft.replace(R.id.LinearLayout1, frag);
        		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        		//ft.addToBackStack(null);
        		ft.commit();
            }
        }); 
        
        MyDbHelper myDB = new MyDbHelper(this);
        Intent thisIntent = getIntent();
        Bundle passedData = thisIntent.getExtras();
        if(passedData != null){
        	passedUserName = passedData.getString("uName");
        	passedDate = passedData.getString("uDate");
        	Log.d(DCDEBUG, "User name received: " + passedUserName + " and the date " + passedDate);
        }
        
        finalDayTextView.setText("Summary for " + passedUserName + " for today(" + passedDate + ").");
        lvDay = (ListView) findViewById(R.id.dayList);
        lvDay.setBackgroundColor(Color.LTGRAY);
        currentDayInfo = myDB.returnCurrentDayDbData(passedUserName, passedDate);
        ArrayAdapter<String> todayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, currentDayInfo);
        lvDay.setAdapter(todayAdapter);
        Log.d(DCDEBUG, "Current day data set successfully");
        
//        finalHistoryTextView.setText("Historical Entries for " + passedUserName + ":");
//        lvHistory = (ListView) findViewById(R.id.historyList);
//        lvHistory.setBackgroundColor(Color.LTGRAY);
//        historicalInfo = myDB.returnHistoricalDbData(passedUserName);
//        ArrayAdapter<String> historyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, historicalInfo);
//        lvHistory.setAdapter(historyAdapter);
//        Log.d(DCDEBUG, "Historical data set successfully");
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
}