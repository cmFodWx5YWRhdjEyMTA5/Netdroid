package org.pctechtips.netdroid.dbhelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;

/**
 * Created by jlvaz on 4/12/2017.
 * Database Helper file
 */


public class DatabaseHelper extends SQLiteOpenHelper {

    //The Android's default system path of your application database.
    private static String DB_PATH = "";
    private static String DB_NAME = "ports.db";
    private static int DB_VERSION = 1;
    /*SQL Query to get service running on especific port*/
    private static String SQL_QUERY = "SELECT service FROM ports WHERE port==";
    private SQLiteDatabase mDataBase;
    private final Context context;

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     *
     * @param context
     */
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        DB_PATH = context.getDatabasePath(DB_NAME).getPath();
        this.context = context;
    }

    public void createDataBase() throws IOException {
        if(!checkDataBase()) {
            this.getReadableDatabase();
            copyDataBase();
            this.close();
        }
    }

    private boolean checkDataBase() {
        File DbFile = new File(DB_PATH + DB_NAME);
        return DbFile.exists();
    }

    boolean openDataBase() throws SQLException {
        mDataBase = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.CREATE_IF_NECESSARY);
        return mDataBase != null;
    }

    public synchronized void close(){
        if(mDataBase != null)
            mDataBase.close();
        SQLiteDatabase.releaseMemory();
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    //copy database to app location
    private void copyDataBase() throws IOException {
        InputStream mInput =  context.getAssets().open(DB_NAME);
        String outfileName = DB_PATH;
        OutputStream mOutput = new FileOutputStream(outfileName);
        byte[] buffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(buffer))>0) {
            mOutput.write(buffer, 0, mLength);
        }
        mOutput.flush();
        mInput.close();
        mOutput.close();
    }

    /*
    * SQL query to get servive description
    * of especific port
    * */
    public String getPortService(int port) {
        String service = "";
        SQLiteDatabase db = this.getReadableDatabase();
        String sqlQuery = SQL_QUERY + port + ";";
        Cursor cursor = db.rawQuery(sqlQuery,null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            service = cursor.getString(0);
        }

        // make sure to close the cursor
        cursor.close();
        return (service != "")? service : "Unknown!";
    }

}