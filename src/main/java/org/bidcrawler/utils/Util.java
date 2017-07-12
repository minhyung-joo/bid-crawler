package org.bidcrawler.utils;

/**
 * Created by ravenjoo on 6/24/17.
 */

import java.io.*;
import java.util.Properties;

public class Util {
    public static String[] COLUMNS = { "", "입찰공고번호", "실제개찰일시", "업종제한사항", "기초금액", "예정금액", "투찰금액", "추첨가격1", "추첨가격15", "참가수", "개찰일시(예정)", "진행상황", "공고기관", "수요기관", "입찰방식", "계약방식", "예가방법" };
    public static String[] SITES = { "국방조달청", "LH공사", "도로공사", "한국마사회", "철도시설공단" };

    public static String[] LH_WORKS = { "전체", "시설공사", "용역", "물품", "지급자재" };
    public static String[] LETS_WORKS = { "전체", "시설공사", "기술용역", "물품구매", "일반용역" };
    public static String[] EX_WORKS = { "전체", "공사", "용역", "물품" };

    // DB authentication info.
    public static String DB_ID;
    public static String DB_PW;

    public static String SCHEMA;
    public static String BASE_PATH;

    public static void initialize() {
        Properties properties = new Properties();
        try {
            FileInputStream in = new FileInputStream("properties");
            properties.load(in);

            DB_ID = properties.getProperty("DB_ID");
            DB_PW = properties.getProperty("DB_PW");
            SCHEMA = properties.getProperty("SCHEMA");
            BASE_PATH = properties.getProperty("BASE_PATH");

            in.close();
        } catch (IOException e) {
            e.printStackTrace();

            properties.setProperty("DB_ID", "root");
            properties.setProperty("DB_PW", "");
            properties.setProperty("SCHEMA", "bid_db_2");
            properties.setProperty("BASE_PATH", "C:/Users/owner/Documents/");

            DB_ID = properties.getProperty("DB_ID");
            DB_PW = properties.getProperty("DB_PW");
            SCHEMA = properties.getProperty("SCHEMA");
            BASE_PATH = properties.getProperty("BASE_PATH");

            try {
                FileOutputStream out = new FileOutputStream("properties");
                properties.store(out, "Set default values");
                out.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public static String parseRate(String rate) {
        if (rate.length() < 4) return rate;
        else {
            if (rate.charAt(rate.length() - 1) == '0') {
                rate = rate.substring(0, rate.length() - 1);
            }
            return rate;
        }
    }

    public static void setValues(String id, String pw, String schema, String basePath) {
        DB_ID = id;
        DB_PW = pw;
        SCHEMA = schema;
        BASE_PATH = basePath;

        try {
            Properties properties = new Properties();

            properties.setProperty("DB_ID", id);
            properties.setProperty("DB_PW", pw);
            properties.setProperty("SCHEMA", schema);
            properties.setProperty("BASE_PATH", basePath);

            FileOutputStream out = new FileOutputStream("properties");
            properties.store(out, "Set new values");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * From StackOverFlow
     * http://stackoverflow.com/questions/237159/whats-the-best-way-to-check-to-see-if-a-string-represents-an-integer-in-java
     */
    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}