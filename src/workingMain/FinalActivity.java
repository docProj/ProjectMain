package org.opencv.samples.facedetect;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FinalActivity extends Activity {
	private static final String    DCDEBUG             = "darrynFinishingDebug";
	private String passedUserName;
	private String passedDate;
	TextView finalDayTextView;
	TextView finalHistoryTextView;
	ListView 					   lvDay;
	ListView 					   lvHistory;
	ArrayList<String> 			   currentDayInfo;
	ArrayList<String>			   historicalInfo;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finishpage);
        finalDayTextView = (TextView) findViewById(R.id.finalDayTextView);
        finalHistoryTextView = (TextView) findViewById(R.id.finalHistoryTextView);
        
        MyDbHelper myDB = new MyDbHelper(this);
        Intent thisIntent = getIntent();
        Bundle passedData = thisIntent.getExtras();
        if(passedData != null){
        	passedUserName = passedData.getString("uName");
        	passedDate = passedData.getString("uDate");
        }
        
        finalDayTextView.setText("Summary for " + passedUserName + " for today(" + passedDate + ").");
        lvDay = (ListView) findViewById(R.id.dayList);
        lvDay.setBackgroundColor(Color.LTGRAY);
        currentDayInfo = myDB.returnCurrentDayDbData(passedUserName, passedDate);
        ArrayAdapter<String> todayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, currentDayInfo);
        lvDay.setAdapter(todayAdapter);
        
        finalHistoryTextView.setText("Historical Entries for: " + passedUserName);
        lvHistory = (ListView) findViewById(R.id.historyList);
        lvHistory.setBackgroundColor(Color.LTGRAY);
        historicalInfo = myDB.returnHistoricalDbData(passedUserName);
        ArrayAdapter<String> historyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, historicalInfo);
        lvHistory.setAdapter(historyAdapter);

    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
}