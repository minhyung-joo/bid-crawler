package org.bidcrawler.utils;

/**
 * Created by ravenjoo on 6/24/17.
 */

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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

    /*
     * From StackOverFlow
     * https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
     */
    public static boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }

        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public static boolean checkDataValidity(ResultSet rs, String site) throws SQLException {
        boolean valid = true;

        String bPrice = "";
        double baseValue = 0;
        if (site.equals("LH공사")) bPrice = rs.getString("기초금액");
        else if (site.equals("국방조달청")) bPrice = rs.getString("기초예비가격");
        else if (site.equals("도로공사") || site.equals("철도시설공단")) bPrice = rs.getString("설계금액");
        else if (site.equals("한국마사회")) bPrice = rs.getString("예비가격기초금액");
        if (bPrice == null) bPrice = "";
        if (!bPrice.equals("") && !(bPrice.equals("0") || bPrice.equals("0.00"))) {
            double amount = Double.parseDouble(bPrice);
            baseValue = amount;
            DecimalFormat formatter = new DecimalFormat("#,###");
            bPrice = formatter.format(amount);
        }
        else valid = false;

        double[] ratios = new double[15];
        for (int i = 1; i <= 15; i++) {
            String dupPrice = rs.getString("복수" + i);
            if (dupPrice == null || dupPrice.equals("")) {
                valid = false;
                break;
            }
            double dupValue = Double.parseDouble(dupPrice);
            if (dupValue == 0) {
                valid = false;
                break;
            }

            double ratio = (dupValue / baseValue - 1) * 100;
            ratios[i-1] = ratio;
        }
        Arrays.sort(ratios);
        double largestRatio = ratios[14];
        double smallestRatio = ratios[0];

        double largestLB = 0;
        double largestUB = 0;
        double smallestLB = 0;
        double smallestUB = 0;
        if (site.equals("LH공사")) {
            largestLB = 1.70;
            largestUB = 2.00;
            smallestLB = -2.00;
            smallestUB = -1.70;

            if (largestRatio >= largestUB || largestRatio <= largestLB) valid = false;
            if (smallestRatio >= smallestUB || smallestRatio <= smallestLB) valid = false;
        }
        else if (site.equals("철도시설공단")) {
            largestLB = 2.00;
            largestUB = 2.70;
            smallestLB = -2.70;
            smallestUB = -2.10;

            if (largestRatio >= largestUB || largestRatio <= largestLB) valid = false;
            if (smallestRatio >= smallestUB || smallestRatio <= smallestLB) valid = false;
        }
        else if (site.equals("도로공사") || site.equals("한국마사회")) {
            largestLB = 2.50;
            largestUB = 3.001;
            smallestLB = -3.00;
            smallestUB = -2.50;

            if (largestRatio >= largestUB || largestRatio <= largestLB) valid = false;
            if (smallestRatio >= smallestUB || smallestRatio <= smallestLB) valid = false;
        }

        Date dateCheck = rs.getDate("개찰일시");
        Calendar passCalendar = Calendar.getInstance();
        passCalendar.set(Calendar.HOUR_OF_DAY, 0);
        passCalendar.set(Calendar.MINUTE, 0);
        passCalendar.set(Calendar.SECOND, 0);
        passCalendar.set(Calendar.MILLISECOND, 0);
        Date passDate = passCalendar.getTime();
        if (dateCheck.after(passDate)) valid = true;

        return valid;
    }

    public static String removeWhitespace(String str)
    {
        if (str == null) {
            return null;
        }

        return str.replaceAll("\\s+", "");
    }

    public static String convertToNumeric(String str) {
        if (str == null) {
            return null;
        }

        return str.replaceAll("[^\\d.-]", "");
    }
}