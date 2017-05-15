package com.example.lenovo.pmcrowdssensev1;

// This class handles all the database activities
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.content.Context;
import android.content.ContentValues;
import android.util.Log;

public class MyDBHandler extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "messageDB.db";
    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_REALTIME = "_realtime";
    public static final String COLUMN_PMV = "_PMV";
    public static final String COLUMN_COORDINATES = "_coordinates";
    public static final String COLUMN_DEVICEID = "_deviceID";



    //We need to pass database information along to superclass
    public MyDBHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_MESSAGES + "(" +
                MyDBHandler.COLUMN_REALTIME + " TEXT, " +
                MyDBHandler.COLUMN_PMV + " TEXT, " +
                MyDBHandler.COLUMN_COORDINATES + " TEXT, " +
                MyDBHandler.COLUMN_DEVICEID + " TEXT " +
                ");";
        db.execSQL(query);
    }
    //Lesson 51
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    //Add a new row to the database
    public void addMessage(Messages message){
        ContentValues values = new ContentValues();
        values.put(MyDBHandler.COLUMN_REALTIME, message.get_realtime());
        values.put(MyDBHandler.COLUMN_PMV, message.get_PMV());
        values.put(MyDBHandler.COLUMN_COORDINATES, message.get_coordinates());
        values.put(MyDBHandler.COLUMN_DEVICEID, message.get_deviceID());
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_MESSAGES, null, values);
        //     db.close();
    }

    //Delete a message from the database
    public void deleteMessage(){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_MESSAGES);
        //   db.execSQL("UPDATE SQLITE_SEQUENCE SET _id=0 WHERE NAME=" + TABLE_MESSAGES);

    }

    public long countMessage(){
        SQLiteDatabase db = getWritableDatabase();
        long numRows = DatabaseUtils.queryNumEntries(db, TABLE_MESSAGES);
        return numRows;

    }
    // this is goint in record_TextView in the Main activity.
    public String databaseToString(){
        String dbString = "";
        SQLiteDatabase db = getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_MESSAGES + " WHERE 1";// why not leave out the WHERE  clause?

        //Cursor points to a location in your results
        Cursor recordSet = db.rawQuery(query, null);
        //Move to the first row in your results
        recordSet.moveToFirst();

        //Position after the last row means the end of the results
        while (!recordSet.isAfterLast()) {
            // null could happen if we used our empty constructor
            if (recordSet.getString(recordSet.getColumnIndex("_realtime")) != null) {
                dbString += recordSet.getString(recordSet.getColumnIndex("_realtime"));
                dbString += "\n";
            }
            recordSet.moveToNext();
        }
        //    db.close();
        return dbString;
    }

    public String getTableAsString() {
        SQLiteDatabase db = getWritableDatabase();
        String tableString = "";
        Cursor allRows  = db.rawQuery("SELECT * FROM " + TABLE_MESSAGES + " WHERE 1", null);
        allRows.moveToLast();
        if (allRows.moveToLast() ){
            String[] columnNames = allRows.getColumnNames();
            do {
                for (String name: columnNames) {
                    tableString += String.format("%s: %s\n", name,
                            allRows.getString(allRows.getColumnIndex(name)));
                }
                tableString += "\n";

            } while (allRows.moveToNext());
        }

        return tableString;
    }

}
