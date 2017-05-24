package jp.ac.hal.felica;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


/**
 * Created by pasuco on 2017/05/12.
 */

public class DAO {
    private SQLiteDatabase database;

    public DAO(SQLiteDatabase database) {
        this.database = database;
    }

    public String getStation(int areaCode, int lineCode, int stationCode) {

        String station = "不明";
        try {
            Cursor c = database.rawQuery("select * from stationcode where areacode = ? and linecode = ? and stationcode = ?",
                    new String[]{String.valueOf(areaCode), String.valueOf(lineCode), String.valueOf(stationCode)});
            if (c.moveToFirst()) {
                station = c.getString(c.getColumnIndex("StationName"));
            }
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return station;
    }

    public String select() {
        String str = "不明";
        try {
            Cursor c = database.rawQuery("select * from stationcode where areacode = 0 and linecode = 1 and stationcode = 1", null);
            c.moveToFirst();
            str = c.getString(c.getColumnIndex("StationName"));
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    public String getBusStation() {
        return "";
    }

}
