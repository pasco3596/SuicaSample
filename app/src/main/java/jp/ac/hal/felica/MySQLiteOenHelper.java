package jp.ac.hal.felica;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by pasuco on 2017/05/11.
 */

public class MySQLiteOenHelper extends SQLiteOpenHelper {
    private static final String DB_FILE_NAME = "station.db";
    private static final String DB_NAME = "station";
    private static final int DATABASE_VERSION = 1;
    private final Context mContext;
    private final File dbPath;


    public MySQLiteOenHelper(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        mContext = context;
        dbPath = mContext.getDatabasePath(DB_NAME);


    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase database = super.getWritableDatabase();
        try {
            database = copyDatabase(database);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return database;
    }


    @Override
    public void onCreate(SQLiteDatabase database) {

    }


    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        try {
            copyDatabase(database);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SQLiteDatabase copyDatabase(SQLiteDatabase database) throws IOException {
        database.close();
        try {
            InputStream inputStream = mContext.getAssets().open(DB_FILE_NAME);
            OutputStream outputStream = new FileOutputStream(this.dbPath);
            byte[] buffer = new byte[1024 * 4];
            int n = 0;
            while (-1 != (n = inputStream.read(buffer))) {
                outputStream.write(buffer, 0, n);
            }

            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return super.getWritableDatabase();
    }
}
