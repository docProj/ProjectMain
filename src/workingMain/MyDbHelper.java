package org.opencv.samples.facedetect;

import java.util.ArrayList;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MyDbHelper extends SQLiteOpenHelper {
	
	private static final String DEBUGGING             	= "DBdebugging";
	private static final String dbName 					= "projectDB";
	private static final int    dbVersion 				= 2;
	private static final String tableName 				= "repTable";
	// Table Columns
	private static final String tblValID 				= "tblID";
	private static final String tblValDate 				= "Date";
	private static final String tblValUser 				= "User";
	private static final String tblValExercise 			= "Exercise";
	private static final String tblValWeight 			= "Weight";
	private static final String tblValSetNumber			= "SetNumber";
	private static final String tblValReps 				= "Reps";
	
	private static MyDbHelper sInstance;

	public MyDbHelper(Context context) {
		super(context, dbName, null, dbVersion);
	}

	public static synchronized MyDbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MyDbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		String createTable = "CREATE TABLE IF NOT EXISTS " + tableName + 
				"(" +
					tblValID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
					tblValDate + " TEXT," + 
					tblValUser + " TEXT," +
					tblValExercise + " TEXT," +
					tblValWeight + " INTEGER," +
					tblValSetNumber + " INTEGER," +
					tblValReps + " INTEGER" +
				");";	
		db.execSQL(createTable);
		Log.i(DEBUGGING, "TABLE CREATED SUCCESSFULLY");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion != newVersion){
			db.execSQL("DROP TABLE IF EXISTS " + tableName);
			onCreate(db);
			Log.i(DEBUGGING, "TABLE UPGRADED SUCCESSFULLY");
		}
	}
	
	public void dropTable() {
		SQLiteDatabase db = this.getWritableDatabase();
		db.beginTransaction();
		try {
			db.execSQL("DROP TABLE IF EXISTS " + tableName);
			db.setTransactionSuccessful();
		} catch (Exception e) {
			Log.i(DEBUGGING, "ERROR DROPPING TABLE " + e.getMessage());
		} finally {
			db.endTransaction();
		}
	}
	
	public void setInfoToDB(String passedDate, String passedUser, String passedExer,
							int passedWeight, int passedSetNum, int passedRepNum) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues ipValues = new ContentValues();
		ipValues.put(tblValDate, passedDate);
		ipValues.put(tblValUser, passedUser);
		ipValues.put(tblValExercise, passedExer);
		ipValues.put(tblValWeight, passedWeight);
		ipValues.put(tblValSetNumber, passedSetNum);
		ipValues.put(tblValReps, passedRepNum);
		db.insert(tableName, null, ipValues);
		db.close();
		Log.i(DEBUGGING, "JUST PUT VALUES INTO DB " + passedDate + passedUser + 
				passedExer + passedWeight + passedSetNum + passedRepNum);
	}
	
	public ArrayList<String> returnCurrentDayDbData(String currentUserName, String currentDate) {
		ArrayList<String> currentDayInfo = new ArrayList<String>();
		String queryAll = "SELECT User,Exercise,Weight,SetNumber,Reps FROM " + tableName + 
				" WHERE " + tblValUser + "='" + currentUserName + "' AND " + tblValDate + "='" + currentDate + "';";
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor myCursor = db.rawQuery(queryAll, null);
		if(myCursor.moveToFirst()){
			do{
				StringBuilder sb = new StringBuilder();
        		sb.append(myCursor.getString(1)	+ "\t");
        		sb.append(myCursor.getString(2) + "\t");
        		sb.append(myCursor.getString(3) + "\t");
        		sb.append(myCursor.getString(4) + "\t");
        		currentDayInfo.add(sb.toString());
			} while (myCursor.moveToNext());
		}
		return currentDayInfo;	
	}
	
	public ArrayList<String> returnHistoricalDbData(String currentUserName) {
		ArrayList<String> historicalInfo = new ArrayList<String>();
		String queryAll = "SELECT Date,Exercise,Weight,SetNumber,Reps FROM " + tableName + 
				" WHERE " + tblValUser + "='" + currentUserName+ "';";
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor myCursor = db.rawQuery(queryAll, null);
		if(myCursor.moveToFirst()){
			do{
				StringBuilder sb = new StringBuilder();
				sb.append(myCursor.getString(0) + "\t");
        		sb.append(myCursor.getString(1)	+ "\t");
        		sb.append(myCursor.getString(2) + "\t");
        		sb.append(myCursor.getString(3) + "\t");
        		sb.append(myCursor.getString(4) + "\t");
        		historicalInfo.add(sb.toString());
			} while (myCursor.moveToNext());
		}
		return historicalInfo;	
	}

}
