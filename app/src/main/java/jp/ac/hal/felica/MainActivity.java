package jp.ac.hal.felica;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    List<Suica> list;
    TextView tv;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFilters;
    private String[][] techLists;
    private NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.textview);

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter intentFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        try {
            intentFilter.addDataType("text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }

        intentFilters = new IntentFilter[]{intentFilter};
        techLists = new String[][]{
                new String[]{NfcF.class.getName()}
        };

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        MySQLiteOenHelper openHelper = new MySQLiteOenHelper(this);
        SQLiteDatabase database = openHelper.getWritableDatabase();

        database.close();
        Button bt = (Button) findViewById(R.id.button);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MySQLiteOenHelper openHelper = new MySQLiteOenHelper(MainActivity.this);
                SQLiteDatabase database = openHelper.getWritableDatabase();
                DAO dao = new DAO(database);
                Log.e("select", dao.select());

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return;
        }
        list = new ArrayList<>();
        byte[] felicabyte = new byte[]{0};

        felicabyte = tag.getId();

        NfcF nfc = NfcF.get(tag);

        try {
            nfc.connect();
            // カードによって取得できる件数がまちまちなので10件とする
            byte[] req = read(felicabyte, 10);
            byte[] res = nfc.transceive(req);

            nfc.close();
            // res[0] = データ長
            // res[1] = 0x07
            // res[2〜9] = カードID
            // res[10,11] = エラーコード。0=正常。
            if (res[10] != 0x00) throw new RuntimeException("Felica error.");
            // res[12] = 応答ブロック数
            // res[13+n*16] = 履歴データ。16byte/ブロックの繰り返し。
            int size = res[12];
            for (int i = 0; i < size; i++) {
                Suica suica = Suica.parse(res, 13 + i * 16);
                list.add(suica);
            }

            //新着順なので時系列順に
            Collections.reverse(list);

            MySQLiteOenHelper openHelper = new MySQLiteOenHelper(this);
            SQLiteDatabase database = openHelper.getWritableDatabase();
            DAO dao = new DAO(database);

            String str = "";
            for (int i = 0; i < list.size(); i++) {
                Suica s = list.get(i);

                ///チャージの履歴のとるからわかんにくい
                if (s.kind.equals("JR") || s.kind.equals("公営/私鉄")) {
                    int inAreaCode = s.inAreaCode;
                    int outAreaCode = s.outAreaCode;
                    int inLineCode = s.inLine;
                    int inStationcede = s.inStation;
                    int outLineCode = s.outLine;
                    int outStationCode = s.outStation;

                    String inStation = dao.getStation(inAreaCode, inLineCode, inStationcede);
                    String outStation = dao.getStation(outAreaCode, outLineCode, outStationCode);
                    str += s.getJoko(inStation, outStation);
                }
            }
            database.close();
            tv.setText(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    private byte[] read(byte[] idm, int size) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(100);
        bout.write(0);           // データ長バイトのダミー
        bout.write(0x06);        // Felicaコマンド「Read Without Encryption」
        bout.write(idm);         // カードID 8byte
        bout.write(1);           // サービスコードリストの長さ(以下２バイトがこの数分繰り返す)
        bout.write(0x0f);        // 履歴のサービスコード下位バイト
        bout.write(0x09);        // 履歴のサービスコード上位バイト
        bout.write(size);        // ブロック数

        for (int i = 0; i < size; i++) {
            bout.write(0x80);
            bout.write(i);
        }

        byte[] msg = bout.toByteArray();
        msg[0] = (byte) msg.length;

        return msg;
    }
}
