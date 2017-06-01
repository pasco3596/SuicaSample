package jp.ac.hal.felica;

import android.util.Log;
import android.util.SparseArray;

/**
 * Created by pasuco on 2017/04/27.
 */

public class Suica {
    public int seqNo;
    public int remain;
    public int termId;
    public int procId;
    public int year;
    public int month;
    public int day;
    public String kind;
    public int reasion;
    public int inLine;
    public int inStation;
    public int outLine;
    public int outStation;
    public int inAreaCode;
    public int outAreaCode;


    private void init(byte[] res, int off) {
        this.termId = res[off + 0]; //0: 端末種
        this.procId = res[off + 1]; //1: 処理
        int mixInt = toInt(res, off, 4, 5);

        this.year = (mixInt >> 9) & 0x07f;
        this.month = (mixInt >> 5) & 0x00f;
        this.day = mixInt & 0x01f;

        this.seqNo = toInt(res, off, 12, 13, 14);
        this.remain = toInt(res, off, 11, 10);

        this.reasion = toInt(res,off , 15);

        if (isBuppan(this.procId)) {
            this.kind = "物販";
        } else if (isBus(this.procId)) {
            this.kind = "バス";

//            6-7 : 出線区
//            8-9 : 出駅順
//            Log.e("seqNo", String.valueOf(seqNo));
//            Log.e("出線区", String.format("%02x", new byte[]{res[off + 6], res[off + 7]}));
//            Log.e("出駅順", String.format("%02x", new byte[]{res[off + 8], res[off + 9]}));



        } else {

            this.kind = res[off + 6] < 0x80 ? "JR" : "公営/私鉄";

            //6 : 入線区
            //7 : 入駅順
            //8 : 出線区
            //9 : 出駅順
            this.inLine = Integer.parseInt(String.format("%02x", res[off + 6]), 16);
            this.inStation = Integer.parseInt(String.format("%02x", res[off + 7]), 16);
            this.outLine = Integer.parseInt(String.format("%02x", res[off + 8]), 16);
            this.outStation = Integer.parseInt(String.format("%02x", res[off + 9]), 16);

            // エリアコードが8ビットのうち上位4ビットで判断
            // XXYY    XX が乗車駅、YYが降車駅のエリアコード
            String b = String.format("%08d",Integer.parseInt(Integer.toBinaryString(reasion)));
            String inStr = b.substring(0,2);
            String outStr = b.substring(2,4);

            int in = Integer.parseInt(inStr,2);
            int out = Integer.parseInt(outStr,2);

            this.inAreaCode = in;
            this.outAreaCode = out;
        }

    }

    private int toInt(byte[] res, int off, int... idx) {
        int num = 0;
        for (int i = 0; i < idx.length; i++) {
            num = num << 8;
            num += ((int) res[off + idx[i]]) & 0x0ff;
        }
        return num;
    }

    private boolean isBuppan(int procId) {
        return procId == 70 || procId == 73 || procId == 74
                || procId == 75 || procId == 198 || procId == 203;
    }

    private boolean isBus(int procId) {
        return procId == 13 || procId == 15 || procId == 31 || procId == 35;
    }


    public static Suica parse(byte[] res, int off) {
        Suica self = new Suica();
        self.init(res, off);
        return self;
    }

    @Override
    public String toString() {
        String str = seqNo
                + "," + TERM_MAP.get(termId)
                + "," + PROC_MAP.get(procId)
                + "," + kind
                + "," + year + "/" + month + "/" + day
                + ",残：" + remain + "円\n";
        return str;
    }

    public String getJoko(String in, String out) {
        String str = seqNo
                + ",\n" + TERM_MAP.get(termId)
                + "," + PROC_MAP.get(procId)
                + "," + in
                + "→" + out
                + "," + year + "/" + month + "/" + day
                + ",残：" + remain + "円\n";
        return str;
    }

    public static final SparseArray<String> TERM_MAP = new SparseArray<>();
    public static final SparseArray<String> PROC_MAP = new SparseArray<>();

    static {
        TERM_MAP.put(3, "精算機");
        TERM_MAP.put(4, "携帯型端末");
        TERM_MAP.put(5, "車載端末");
        TERM_MAP.put(7, "券売機");
        TERM_MAP.put(8, "券売機");
        TERM_MAP.put(9, "入金機");
        TERM_MAP.put(18, "券売機");
        TERM_MAP.put(20, "券売機等");
        TERM_MAP.put(21, "券売機等");
        TERM_MAP.put(22, "改札機");
        TERM_MAP.put(23, "簡易改札機");
        TERM_MAP.put(24, "窓口端末");
        TERM_MAP.put(25, "窓口端末");
        TERM_MAP.put(26, "改札端末");
        TERM_MAP.put(27, "携帯電話");
        TERM_MAP.put(28, "乗継精算機");
        TERM_MAP.put(29, "連絡改札機");
        TERM_MAP.put(31, "簡易入金機");
        TERM_MAP.put(70, "VIEW ALTTE");
        TERM_MAP.put(72, "VIEW ALTTE");
        TERM_MAP.put(199, "物販端末");
        TERM_MAP.put(200, "自販機");

        PROC_MAP.put(1, "運賃支払(改札出場)");
        PROC_MAP.put(2, "チャージ");
        PROC_MAP.put(3, "券購(磁気券購入)");
        PROC_MAP.put(4, "精算");
        PROC_MAP.put(5, "精算 (入場精算)");
        PROC_MAP.put(6, "窓出 (改札窓口処理)");
        PROC_MAP.put(7, "新規 (新規発行)");
        PROC_MAP.put(8, "控除 (窓口控除)");
        PROC_MAP.put(13, "バス (PiTaPa系)");
        PROC_MAP.put(15, "バス (IruCa系)");
        PROC_MAP.put(17, "再発 (再発行処理)");
        PROC_MAP.put(19, "支払 (新幹線利用)");
        PROC_MAP.put(20, "入A (入場時オートチャージ)");
        PROC_MAP.put(21, "出A (出場時オートチャージ)");
        PROC_MAP.put(31, "入金 (バスチャージ)");
        PROC_MAP.put(35, "券購 (バス路面電車企画券購入)");
        PROC_MAP.put(70, "物販");
        PROC_MAP.put(72, "特典 (特典チャージ)");
        PROC_MAP.put(73, "入金 (レジ入金)");
        PROC_MAP.put(74, "物販取消");
        PROC_MAP.put(75, "入物 (入場物販)");
        PROC_MAP.put(198, "物現 (現金併用物販)");
        PROC_MAP.put(203, "入物 (入場現金併用物販)");
        PROC_MAP.put(132, "精算 (他社精算)");
        PROC_MAP.put(133, "精算 (他社入場精算)");
    }
}
