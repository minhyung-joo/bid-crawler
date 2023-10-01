package org.bidcrawler.utils;

/**
 * Created by ravenjoo on 6/24/17.
 */

import javax.sql.rowset.CachedRowSet;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class Util {
    public static String[] COLUMNS = { "", "입찰공고번호", "실제개찰일시", "업종제한사항", "기초금액", "예정금액", "투찰금액", "추첨가격1", "추첨가격15", "참가수", "개찰일시(예정)", "진행상황", "공고기관", "수요기관", "입찰방식", "계약방식", "예가방법" };
    public static String[] DAPA_COLUMNS = { "", "입찰공고번호", "실제개찰일시", "업종제한사항", "기초금액", "예정금액", "투찰금액", "A값", "추첨가격1", "추첨가격15", "참가수", "개찰일시(예정)", "진행상황", "공고기관", "수요기관", "입찰방식", "계약방식", "예가방법", "기초예가적용여부", "사전심사", "낙찰자결정방법", "입찰서제출마감일시", "낙찰하한율", "사정률" };
    public static String[] LH_COLUMNS = { "", "입찰공고번호", "실제개찰일시", "업종제한사항", "기초금액", "예정금액", "투찰금액", "A값", "추첨가격1", "추첨가격15", "참가수", "개찰일시(예정)", "진행상황", "공고기관", "수요기관", "입찰방식", "계약방식", "예가방법", "낙찰자선정방법", "재입찰", "선택가격1", "선택가격2", "선택가격3", "선택가격4", "기존예정가격", "분류", "업무" };
    public static String[] LETS_COLUMNS = { "", "입찰공고번호", "실제개찰일시", "업종제한사항", "기초금액", "예정금액", "투찰금액", "A값", "추첨가격1", "추첨가격15", "참가수", "개찰일시(예정)", "진행상황", "공고기관", "수요기관", "입찰방식", "계약방식", "예가방법", "낙찰자선정방법" };
    public static String[] EX_COLUMNS = { "", "입찰공고번호", "개찰일시", "업종제한사항", "설계금액", "예정가격", "투찰금액", "순공사원가", "A값", "추첨가격1", "추첨가격15", "참가수", "공고일자", "계약방법1", "계약방법2", "계약방법3", "업력", "발주기관" };
    public static String[] RAILNET_COLUMNS = { "", "입찰공고번호", "실제개찰일시", "업종제한사항", "기초금액", "예정금액", "투찰금액", "A값", "추첨가격1", "추첨가격15", "참가수", "개찰일시(예정)", "진행상황", "공고기관", "수요기관", "입찰방식", "계약방식", "예가방법", "심사기준", "낙찰자선정방식", "낙찰하한율" };
    public static String[] SITES = { "국방조달청", "LH공사", "도로공사", "한국마사회", "국가철도공단" };

    public static String[] DAPA_TYPES = { "전체", "경쟁", "협상" };

    public static String[] DAPA_WORKS = { "전체", "시설공사", "용역", "물품" };
    public static String[] LH_WORKS = { "전체", "시설공사", "용역", "물품", "지급자재" };
    public static String[] LETS_WORKS = { "전체", "시설공사", "기술용역", "물품구매", "일반용역" };
    public static String[] EX_WORKS = { "전체", "공사", "용역", "물품" };

    // DB authentication info.
    public static String DB_ID;
    public static String DB_PW;

    public static String SCHEMA;
    public static String BASE_PATH;

    public static String DB_URL;

    public static void initialize() {
        Properties properties = new Properties();
        try {
            FileInputStream in = new FileInputStream("properties");
            properties.load(in);

            DB_ID = properties.getProperty("DB_ID");
            DB_PW = properties.getProperty("DB_PW");
            SCHEMA = properties.getProperty("SCHEMA");
            BASE_PATH = properties.getProperty("BASE_PATH");
            DB_URL = "jdbc:mysql://localhost/" + SCHEMA + "?characterEncoding=utf8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Asia/Seoul";

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
        else if (site.equals("도로공사") || site.equals("국가철도공단")) bPrice = rs.getString("설계금액");
        else if (site.equals("한국마사회")) bPrice = rs.getString("예비가격기초금액");
        if (bPrice == null) bPrice = "";
        if (!bPrice.equals("") && !(bPrice.equals("0") || bPrice.equals("0.00"))) {
            double amount = Double.parseDouble(bPrice);
            baseValue = amount;
        }
        else valid = false;

        double[] ratios = new double[15];
        for (int i = 1; i <= 15; i++) {
            String dupPrice = rs.getString("복수" + i);
            if (dupPrice == null || dupPrice.equals("")) {
                System.out.println("invalid dupprice");
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

            if (largestRatio >= largestUB || largestRatio <= largestLB) {
                System.out.println("invalid ratio " + ratios[14]);
                valid = false;
            }
            if (smallestRatio >= smallestUB || smallestRatio <= smallestLB) {
                System.out.println("invalid ratio " + ratios[0]);
                valid = false;
            }
        }
        else if (site.equals("국가철도공단")) {
            largestLB = 2.00;
            largestUB = 2.70;
            smallestLB = -2.70;
            smallestUB = -2.10;

            if (largestRatio >= largestUB || largestRatio <= largestLB) valid = false;
            if (smallestRatio >= smallestUB || smallestRatio <= smallestLB) valid = false;
        }
        else if (site.equals("한국마사회")) {
            largestLB = 2.50;
            largestUB = 3.001;
            smallestLB = -3.00;
            smallestUB = -2.50;

            if (largestRatio >= largestUB || largestRatio <= largestLB) valid = false;
            if (smallestRatio >= smallestUB || smallestRatio <= smallestLB) valid = false;
        } else if (site.equals("도로공사")) {

        }

        Date dateCheck = rs.getTimestamp("개찰일시");
        Calendar passCalendar = Calendar.getInstance();
        passCalendar.set(Calendar.HOUR_OF_DAY, 0);
        passCalendar.set(Calendar.MINUTE, 0);
        passCalendar.set(Calendar.SECOND, 0);
        passCalendar.set(Calendar.MILLISECOND, 0);
        Date passDate = passCalendar.getTime();
        if (dateCheck == null) {
            // System.out.println(rs.getString("공고번호"));
        }
        if (dateCheck.after(passDate) || dateCheck.equals(passDate)) {
            valid = true;
        }

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


    public static String selectDapa(String org, String sd, String ed, String workType, String lowerBound, String upperBound, String bidType, String today) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM dapabidinfo WHERE ");
        if (!org.equals("")) {
            sqlBuilder.append("발주기관=\"" + org + "\" AND ");
        }
        if (sd != null && ed != null) {
            sqlBuilder.append("개찰일시 >= \"" + sd + "\" AND 개찰일시 <= \"" + ed + "\" AND ");
        }
        if (!workType.equals("전체")) {
            sqlBuilder.append("분류=\"" + workType + "\" AND ");
        }
        if (lowerBound != null && upperBound != null) {
            sqlBuilder.append("하한=\"" + lowerBound + "\" AND 상한=\"" + upperBound + "\" AND ");
        }
        if (!bidType.equals("전체")) {
            sqlBuilder.append("입찰종류=\"" + bidType + "\" AND ");
        }
        sqlBuilder.append("완료 > 0 ");

        // Add unopened notis
        sqlBuilder.append("UNION SELECT * FROM dapabidinfo WHERE ");
        if (!org.equals("")) {
            sqlBuilder.append("발주기관=\"" + org + "\" AND ");
        }
        if (!workType.equals("전체")) {
            sqlBuilder.append("분류=\"" + workType + "\" AND ");
        }
        if (lowerBound != null && upperBound != null) {
            sqlBuilder.append("하한=\"" + lowerBound + "\" AND 상한=\"" + upperBound + "\" AND ");
        }
        if (!bidType.equals("전체")) {
            sqlBuilder.append("입찰종류=\"" + bidType + "\" AND ");
        }
        sqlBuilder.append("개찰일시 >= \"" + today + "\" ORDER BY 실제개찰일시 ASC, 개찰일시 ASC, 공고번호 ASC, 차수 DESC");
        return sqlBuilder.toString();
    }

    public static String selectLh(String org, String sd, String ed, String workType, String lowerBound, String upperBound, String bidType, String today) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM lhbidinfo WHERE ");
        if (!org.equals("")) {
            sqlBuilder.append("지역본부=\"" + org + "\" AND ");
        }
        if (sd != null && ed != null) {
            sqlBuilder.append("개찰일시 >= \"" + sd + "\" AND 개찰일시 <= \"" + ed + "\" AND ");
        }
        if (!workType.equals("전체")) {
            sqlBuilder.append("업무=\"" + workType + "\" AND ");
        }
        sqlBuilder.append("완료 > 0 ");

        // Add unopened notis
        sqlBuilder.append("UNION SELECT * FROM lhbidinfo WHERE ");
        if (!org.equals("")) {
            sqlBuilder.append("지역본부=\"" + org + "\" AND ");
        }
        if (!workType.equals("전체")) {
            sqlBuilder.append("업무=\"" + workType + "\" AND ");
        }
        sqlBuilder.append("개찰일시 >= \"" + today + "\" ORDER BY 개찰일시, 공고번호");
        return sqlBuilder.toString();
    }

    public static String selectEx(String org, String sd, String ed, String workType, String lowerBound, String upperBound, String bidType, String today) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM exbidinfo WHERE ");
        if (!org.equals("")) {
            sqlBuilder.append("지역=\"" + org + "\" AND ");
        }
        if (sd != null && ed != null) {
            sqlBuilder.append("개찰일시 >= \"" + sd + "\" AND 개찰일시 <= \"" + ed + "\" AND ");
        }
        if (!workType.equals("전체")) {
            sqlBuilder.append("분류=\"" + workType + "\" AND ");
        }
        sqlBuilder.append("완료 > 0 AND 예정가격 IS NOT NULL AND 예정가격 > 0 AND 공고일자 >= \"2023-07-24\" ");

        // Add unopened notis
        sqlBuilder.append("UNION SELECT * FROM exbidinfo WHERE ");
        if (!org.equals("")) {
            sqlBuilder.append("지역=\"" + org + "\" AND ");
        }
        if (!workType.equals("전체")) {
            sqlBuilder.append("분류=\"" + workType + "\" AND ");
        }
        sqlBuilder.append("개찰일시 >= \"" + today + "\" AND 완료 IS NULL ORDER BY 개찰일시, 공고번호");
        return sqlBuilder.toString();
    }

    public static String selectLets(String org, String sd, String ed, String workType, String lowerBound, String upperBound, String bidType, String today) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM letsrunbidinfo WHERE ");
        if (!org.equals("")) {
            sqlBuilder.append("사업장=\"" + org + "\" AND ");
        }
        if (sd != null && ed != null) {
            sqlBuilder.append("개찰일시 >= \"" + sd + "\" AND 개찰일시 <= \"" + ed + "\" AND ");
        }
        if (!workType.equals("전체")) {
            sqlBuilder.append("입찰구분=\"" + workType + "\" AND ");
        }
        sqlBuilder.append("완료 > 0 ");

        // Add unopened notis
        sqlBuilder.append("UNION SELECT * FROM letsrunbidinfo WHERE ");
        if (!org.equals("")) {
            sqlBuilder.append("사업장=\"" + org + "\" AND ");
        }
        if (!workType.equals("전체")) {
            sqlBuilder.append("입찰구분=\"" + workType + "\" AND ");
        }
        sqlBuilder.append("개찰일시 >= \"" + today + "\" ORDER BY 개찰일시, 공고번호");
        return sqlBuilder.toString();
    }

    public static String selectRailnet(String org, String sd, String ed, String workType, String lowerBound, String upperBound, String bidType, String today) {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT * FROM railnetbidinfo WHERE ");
        if (sd != null && ed != null) {
            sqlBuilder.append("개찰일시 >= \"" + sd + "\" AND 개찰일시 <= \"" + ed + "\" AND ");
        }
        sqlBuilder.append("완료 > 0 ");

        // Add unopened notis
        sqlBuilder.append("UNION SELECT * FROM railnetbidinfo WHERE ");
        sqlBuilder.append("개찰일시 >= \"" + today + "\" ORDER BY 실제개찰일시, 개찰일시, 공고번호");
        return sqlBuilder.toString();
    }

    public static Object[] getDapaRow(CachedRowSet cachedRowSet, int index) throws SQLException {
        String bidno = cachedRowSet.getString("공고번호");
        String date = cachedRowSet.getString("실제개찰일시");
        if (date.length() == 21) {
            date = date.substring(2, 4) + date.substring(5, 7) + date.substring(8, 10) + " " + date.substring(11, 16);
        }

        String limit = cachedRowSet.getString("면허명칭");

        String bPrice = cachedRowSet.getString("기초예비가격");
        if (bPrice != null && !bPrice.equals("") && !(bPrice.equals("0") || bPrice.equals("0.00"))) {
            double amount = Double.parseDouble(bPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            bPrice = formatter.format(amount);
        }
        else bPrice = "-";

        String ePrice = cachedRowSet.getString("예정가격");
        if (ePrice != null && !ePrice.equals("") && !(ePrice.equals("0") || ePrice.equals("0.00"))) {
            double amount = Double.parseDouble(ePrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            ePrice = formatter.format(amount);
        }
        else ePrice = "-";

        String tPrice = cachedRowSet.getString("투찰금액");
        if (tPrice != null && !tPrice.equals("") && !(tPrice.equals("0") || tPrice.equals("0.00"))) {
            double amount = Double.parseDouble(tPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            tPrice = formatter.format(amount);
        }
        else tPrice = "-";

        String aPrice = cachedRowSet.getString("A값");
        if (aPrice != null && !aPrice.equals("") && !(aPrice.equals("0") || aPrice.equals("0.00"))) {
            double amount = Double.parseDouble(aPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            aPrice = formatter.format(amount);
        }
        else aPrice = "-";

        String dPrice1 = cachedRowSet.getString("복수1");
        if (dPrice1 != null && !dPrice1.equals("") && !(dPrice1.equals("0") || dPrice1.equals("0.00"))) {
            double amount = Double.parseDouble(dPrice1);
            DecimalFormat formatter = new DecimalFormat("#,###");
            dPrice1 = formatter.format(amount);
        }
        else dPrice1 = "-";

        String dPrice2 = cachedRowSet.getString("복수15");
        if (dPrice2 != null && !dPrice2.equals("") && !(dPrice2.equals("0") || dPrice2.equals("0.00"))) {
            double amount = Double.parseDouble(dPrice2);
            DecimalFormat formatter = new DecimalFormat("#,###");
            dPrice2 = formatter.format(amount);
        }
        else dPrice2 = "-";

        String comp = cachedRowSet.getString("참여수");
        if (comp != null && !comp.equals("") && !comp.equals("0")) {
            double amount = Double.parseDouble(comp);
            DecimalFormat formatter = new DecimalFormat("#,###");
            comp = formatter.format(amount);
        }
        else comp = "-";

        String eDate = cachedRowSet.getString("개찰일시");
        if (eDate != null) {
            if (eDate.length() == 21) {
                eDate = eDate.substring(2, 4) + eDate.substring(5, 7) + eDate.substring(8, 10) + " " + eDate.substring(11, 16);
            }
        }

        String prog = cachedRowSet.getString("입찰결과");
        String annOrg = cachedRowSet.getString("발주기관");
        String demOrg = cachedRowSet.getString("발주기관");
        String bidType = cachedRowSet.getString("입찰방법");
        String compType = cachedRowSet.getString("계약방법");
        String priceMethod = "";
        String hasBase = cachedRowSet.getString("기초예가적용여부");
        String prelim = cachedRowSet.getString("사전심사");
        String choiceMethod = cachedRowSet.getString("낙찰자결정방법");
        String dd = cachedRowSet.getString("입찰서제출마감일시");
        if (dd != null) {
            if (dd.length() == 21) {
                dd = dd.substring(2, 4) + dd.substring(5, 7) + dd.substring(8, 10) + " " + dd.substring(11, 16);
            }
        }

        String lbound = cachedRowSet.getString("낙찰하한율");
        lbound = lbound.replaceAll("[^\\d.]", "");
        if (!lbound.equals("")) lbound += "%";
        else lbound = "-";
        String rate = cachedRowSet.getString("사정률");
        if (rate.equals("") || rate.equals("-")) {
            String lowerrate = cachedRowSet.getString("하한");
            if (lowerrate == null || lowerrate.equals("") || lowerrate.equals("-")) rate = "";
            else rate = lowerrate + " ~ " + cachedRowSet.getString("상한");
        }

        if (rate.equals("")) {
            // System.out.println(bidno);
        }

        return new Object[] { index, bidno, date, limit, bPrice, ePrice, tPrice, aPrice, dPrice1, dPrice2,
                comp, eDate, prog, annOrg, demOrg, bidType, compType, priceMethod, hasBase, prelim, choiceMethod, dd, lbound, rate };
    }

    public static Object[] getLhRow(CachedRowSet cachedRowSet, int index) throws SQLException {
        String bidno = cachedRowSet.getString("공고번호");

        String date = cachedRowSet.getString("개찰일시");
        if (date.length() == 21) {
            date = date.substring(2, 4) + date.substring(5, 7) + date.substring(8, 10) + " " + date.substring(11, 16);
        }

        String limit = "-";
        String bPrice = cachedRowSet.getString("기초금액");
        if (bPrice != null && !bPrice.equals("") && !(bPrice.equals("0") || bPrice.equals("0.00"))) {
            double amount = Double.parseDouble(bPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            bPrice = formatter.format(amount);
        }
        else bPrice = "-";

        String ePrice = cachedRowSet.getString("예정금액");
        if (ePrice != null && !ePrice.equals("") && !(ePrice.equals("0") || ePrice.equals("0.00"))) {
            double amount = Double.parseDouble(ePrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            ePrice = formatter.format(amount);
        }
        else ePrice = "-";

        String tPrice = cachedRowSet.getString("투찰금액");
        if (tPrice != null && !tPrice.equals("") && !(tPrice.equals("0") || tPrice.equals("0.00"))) {
            double amount = Double.parseDouble(tPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            tPrice = formatter.format(amount);
        }
        else tPrice = "-";

        String aPrice = cachedRowSet.getString("A값");
        if (aPrice != null && !aPrice.equals("") && !(aPrice.equals("0") || aPrice.equals("0.00"))) {
            double amount = Double.parseDouble(aPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            aPrice = formatter.format(amount);
        }
        else aPrice = "-";

        String dPrice1 = cachedRowSet.getString("복수1");
        if (dPrice1 != null && !dPrice1.equals("") && !(dPrice1.equals("0") || dPrice1.equals("0.00"))) {
            double amount = Double.parseDouble(dPrice1);
            DecimalFormat formatter = new DecimalFormat("#,###");
            dPrice1 = formatter.format(amount);
        }
        else dPrice1 = "-";

        String dPrice2 = cachedRowSet.getString("복수15");
        if (dPrice2 != null && !dPrice2.equals("") && !(dPrice2.equals("0") || dPrice2.equals("0.00"))) {
            double amount = Double.parseDouble(dPrice2);
            DecimalFormat formatter = new DecimalFormat("#,###");
            dPrice2 = formatter.format(amount);
        }
        else dPrice2 = "-";

        String comp = cachedRowSet.getString("참가수");
        if (comp != null && !comp.equals("") && !comp.equals("0")) {
            double amount = Double.parseDouble(comp);
            DecimalFormat formatter = new DecimalFormat("#,###");
            comp = formatter.format(amount);
        }
        else comp = "-";

        String eDate = cachedRowSet.getString("개찰일시");
        if (eDate != null) {
            if (eDate.length() == 21) {
                eDate = eDate.substring(2, 4) + eDate.substring(5, 7) + eDate.substring(8, 10) + " " + eDate.substring(11, 16);
            }
        }

        String prog = cachedRowSet.getString("개찰내역");
        String annOrg = cachedRowSet.getString("지역본부");
        String demOrg = cachedRowSet.getString("지역본부");
        String bidType = cachedRowSet.getString("입찰방식");
        String compType = cachedRowSet.getString("계약방법");
        String priceMethod = "";
        String choiceMethod = cachedRowSet.getString("낙찰자선정방법");
        String rebid = cachedRowSet.getString("재입찰");
        String chosenPrice1 = cachedRowSet.getString("선택가격1");
        if (chosenPrice1 != null && !chosenPrice1.equals("") && !(chosenPrice1.equals("0") || chosenPrice1.equals("0.00"))) {
            double amount = Double.parseDouble(chosenPrice1);
            DecimalFormat formatter = new DecimalFormat("#,###");
            chosenPrice1 = formatter.format(amount);
        }
        else chosenPrice1 = "-";

        String chosenPrice2 = cachedRowSet.getString("선택가격2");
        if (chosenPrice2 != null && !chosenPrice2.equals("") && !(chosenPrice2.equals("0") || chosenPrice2.equals("0.00"))) {
            double amount = Double.parseDouble(chosenPrice2);
            DecimalFormat formatter = new DecimalFormat("#,###");
            chosenPrice2 = formatter.format(amount);
        }
        else chosenPrice2 = "-";

        String chosenPrice3 = cachedRowSet.getString("선택가격3");
        if (chosenPrice3 != null && !chosenPrice3.equals("") && !(chosenPrice3.equals("0") || chosenPrice3.equals("0.00"))) {
            double amount = Double.parseDouble(chosenPrice3);
            DecimalFormat formatter = new DecimalFormat("#,###");
            chosenPrice3 = formatter.format(amount);
        }
        else chosenPrice3 = "-";

        String chosenPrice4 = cachedRowSet.getString("선택가격4");
        if (chosenPrice4 != null && !chosenPrice4.equals("") && !(chosenPrice4.equals("0") || chosenPrice4.equals("0.00"))) {
            double amount = Double.parseDouble(chosenPrice4);
            DecimalFormat formatter = new DecimalFormat("#,###");
            chosenPrice4 = formatter.format(amount);
        }
        else chosenPrice4 = "-";

        String sitePrice = cachedRowSet.getString("기존예정가격");
        if (sitePrice != null && !sitePrice.equals("") && !(sitePrice.equals("0") || sitePrice.equals("0.00"))) {
            double amount = Double.parseDouble(sitePrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            sitePrice = formatter.format(amount);
        }
        else sitePrice = "-";

        String type = cachedRowSet.getString("분류");
        String work = cachedRowSet.getString("업무");

        return new Object[] { index, bidno, date, limit, bPrice, ePrice, tPrice, aPrice, dPrice1, dPrice2,
                comp, eDate, prog, annOrg, demOrg, bidType, compType, priceMethod, choiceMethod, rebid, chosenPrice1, chosenPrice2, chosenPrice3, chosenPrice4, sitePrice, type, work };
    }

    public static Object[] getLetsRow(CachedRowSet cachedRowSet, int index) throws SQLException {
        String bidno = cachedRowSet.getString("공고번호");

        String date = cachedRowSet.getString("개찰일시");
        if (date.length() == 21) {
            date = date.substring(2, 4) + date.substring(5, 7) + date.substring(8, 10) + " " + date.substring(11, 16);
        }

        String limit = "-";
        String bPrice = cachedRowSet.getString("예비가격기초금액");
        if (bPrice != null && !bPrice.equals("") && !(bPrice.equals("0") || bPrice.equals("0.00"))) {
            double amount = Double.parseDouble(bPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            bPrice = formatter.format(amount);
        }
        else bPrice = "-";

        String ePrice = cachedRowSet.getString("예정가격");
        if (ePrice != null && !ePrice.equals("") && !(ePrice.equals("0") || ePrice.equals("0.00"))) {
            double amount = Double.parseDouble(ePrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            ePrice = formatter.format(amount);
        }
        else ePrice = "-";

        String tPrice = cachedRowSet.getString("투찰금액");
        if (tPrice != null && !tPrice.equals("") && !(tPrice.equals("0") || tPrice.equals("0.00"))) {
            double amount = Double.parseDouble(tPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            tPrice = formatter.format(amount);
        }
        else tPrice = "-";

        String aPrice = cachedRowSet.getString("A값");
        if (aPrice != null && !aPrice.equals("") && !(aPrice.equals("0") || aPrice.equals("0.00"))) {
            double amount = Double.parseDouble(aPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            aPrice = formatter.format(amount);
        }
        else aPrice = "-";

        String dPrice1 = cachedRowSet.getString("복수1");
        if (dPrice1 != null && !dPrice1.equals("") && !(dPrice1.equals("0") || dPrice1.equals("0.00"))) {
            double amount = Double.parseDouble(dPrice1);
            DecimalFormat formatter = new DecimalFormat("#,###");
            dPrice1 = formatter.format(amount);
        }
        else dPrice1 = "-";

        String dPrice2 = cachedRowSet.getString("복수15");
        if (dPrice2 != null && !dPrice2.equals("") && !(dPrice2.equals("0") || dPrice2.equals("0.00"))) {
            double amount = Double.parseDouble(dPrice2);
            DecimalFormat formatter = new DecimalFormat("#,###");
            dPrice2 = formatter.format(amount);
        }
        else dPrice2 = "-";

        String comp = cachedRowSet.getString("참여수");
        if (comp != null && !comp.equals("") && !comp.equals("0")) {
            double amount = Double.parseDouble(comp);
            DecimalFormat formatter = new DecimalFormat("#,###");
            comp = formatter.format(amount);
        }
        else comp = "-";

        String eDate = cachedRowSet.getString("개찰일시");
        if (eDate != null) {
            if (eDate.length() == 21) {
                eDate = eDate.substring(2, 4) + eDate.substring(5, 7) + eDate.substring(8, 10) + " " + eDate.substring(11, 16);
            }
        }

        String prog = cachedRowSet.getString("개찰상태");
        String annOrg = cachedRowSet.getString("사업장");
        String demOrg = cachedRowSet.getString("사업장");
        String bidType = cachedRowSet.getString("입찰방식");
        String compType = cachedRowSet.getString("계약방법");
        String priceMethod = cachedRowSet.getString("예정가격방식");
        String choiceMethod = cachedRowSet.getString("낙찰자선정방법");

        return new Object[] { index, bidno, date, limit, bPrice, ePrice, tPrice, aPrice, dPrice1, dPrice2,
                comp, eDate, prog, annOrg, demOrg, bidType, compType, priceMethod, choiceMethod };
    }

    public static Object[] getExRow(CachedRowSet cachedRowSet, int index) throws SQLException {
        String bidno = cachedRowSet.getString("공고번호");
        String date = cachedRowSet.getString("개찰일시");
        if (date.length() == 21) {
            date = date.substring(2, 4) + date.substring(5, 7) + date.substring(8, 10) + " " + date.substring(11, 16);
        }

        String limit = cachedRowSet.getString("업종제한사항");

        String bPrice = cachedRowSet.getString("설계금액");
        if (bPrice != null && !bPrice.equals("") && !(bPrice.equals("0") || bPrice.equals("0.00"))) {
            double amount = Double.parseDouble(bPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            bPrice = formatter.format(amount);
        }
        else bPrice = "-";

        String ePrice = cachedRowSet.getString("예정가격");
        if (ePrice != null && !ePrice.equals("") && !(ePrice.equals("0") || ePrice.equals("0.00"))) {
            double amount = Double.parseDouble(ePrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            ePrice = formatter.format(amount);
        }
        else ePrice = "-";

        String tPrice = cachedRowSet.getString("투찰금액");
        if (tPrice != null && !tPrice.equals("") && !(tPrice.equals("0") || tPrice.equals("0.00"))) {
            double amount = Double.parseDouble(tPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            tPrice = formatter.format(amount);
        }
        else tPrice = "-";

        String purePrice = cachedRowSet.getString("순공사원가");
        if (purePrice != null && !purePrice.equals("") && !(purePrice.equals("0") || purePrice.equals("0.00"))) {
            double amount = Double.parseDouble(purePrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            purePrice = formatter.format(amount);
        }
        else purePrice = "-";

        String aPrice = cachedRowSet.getString("A값");
        if (aPrice != null && !aPrice.equals("") && !(aPrice.equals("0") || aPrice.equals("0.00"))) {
            double amount = Double.parseDouble(aPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            aPrice = formatter.format(amount);
        }
        else aPrice = "-";

        String dPrice1 = cachedRowSet.getString("복수1");
        if (dPrice1 != null && !dPrice1.equals("") && !(dPrice1.equals("0") || dPrice1.equals("0.00"))) {
            double amount = Double.parseDouble(dPrice1);
            DecimalFormat formatter = new DecimalFormat("#,###");
            dPrice1 = formatter.format(amount);
        }
        else dPrice1 = "-";

        String dPrice2 = cachedRowSet.getString("복수15");
        if (dPrice2 != null && !dPrice2.equals("") && !(dPrice2.equals("0") || dPrice2.equals("0.00"))) {
            double amount = Double.parseDouble(dPrice2);
            DecimalFormat formatter = new DecimalFormat("#,###");
            dPrice2 = formatter.format(amount);
        }
        else dPrice2 = "-";

        String comp = cachedRowSet.getString("참가수");
        if (comp != null && !comp.equals("") && !comp.equals("0")) {
            double amount = Double.parseDouble(comp);
            DecimalFormat formatter = new DecimalFormat("#,###");
            comp = formatter.format(amount);
        }
        else comp = "-";

        String eDate = cachedRowSet.getString("공고일자");
        if (eDate != null) {
            if (eDate.length() == 21) {
                eDate = eDate.substring(2, 4) + eDate.substring(5, 7) + eDate.substring(8, 10) + " " + eDate.substring(11, 16);
            }
        }

        String compType = cachedRowSet.getString("계약방법");
        String compType2 = cachedRowSet.getString("계약방법2");
        String compType3 = cachedRowSet.getString("계약방법3");
        String workType = cachedRowSet.getString("분류");
        String area = cachedRowSet.getString("지역");

        return new Object[] { index, bidno, date, limit, bPrice, ePrice, tPrice, purePrice, aPrice, dPrice1, dPrice2,
                comp, eDate, compType, compType2, compType3, workType, area };
    }

    public static Object[] getRailnetRow(CachedRowSet cachedRowSet, int index) throws SQLException {
        String bidno = cachedRowSet.getString("공고번호");
        String date = cachedRowSet.getString("실제개찰일시");
        if (date != null && date.length() == 21) {
            date = date.substring(2, 4) + date.substring(5, 7) + date.substring(8, 10) + " " + date.substring(11, 16);
        } else {
            date = cachedRowSet.getString("개찰일시");
            if (date.length() == 21) {
                date = date.substring(2, 4) + date.substring(5, 7) + date.substring(8, 10) + " " + date.substring(11, 16);
            }
        }

        String limit = "-";
        String bPrice = cachedRowSet.getString("설계금액");
        if (bPrice != null && !bPrice.equals("") && !(bPrice.equals("0") || bPrice.equals("0.00"))) {
            double amount = Double.parseDouble(bPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            bPrice = formatter.format(amount);
        }
        else bPrice = "-";

        String ePrice = cachedRowSet.getString("예정가격");
        if (ePrice != null && !ePrice.equals("") && !(ePrice.equals("0") || ePrice.equals("0.00"))) {
            double amount = Double.parseDouble(ePrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            ePrice = formatter.format(amount);
        }
        else ePrice = "-";

        String tPrice = cachedRowSet.getString("투찰금액");
        if (tPrice != null && !tPrice.equals("") && !(tPrice.equals("0") || tPrice.equals("0.00"))) {
            double amount = Double.parseDouble(tPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            tPrice = formatter.format(amount);
        }
        else tPrice = "-";

        String aPrice = cachedRowSet.getString("A값");
        if (aPrice != null && !aPrice.equals("") && !(aPrice.equals("0") || aPrice.equals("0.00"))) {
            double amount = Double.parseDouble(aPrice);
            DecimalFormat formatter = new DecimalFormat("#,###");
            aPrice = formatter.format(amount);
        }
        else aPrice = "-";

        String dPrice1 = cachedRowSet.getString("복수1");
        if (dPrice1 != null && !dPrice1.equals("") && !(dPrice1.equals("0") || dPrice1.equals("0.00"))) {
            double amount = Double.parseDouble(dPrice1);
            DecimalFormat formatter = new DecimalFormat("#,###");
            dPrice1 = formatter.format(amount);
        }
        else dPrice1 = "-";

        String dPrice2 = cachedRowSet.getString("복수15");
        if (dPrice2 != null && !dPrice2.equals("") && !(dPrice2.equals("0") || dPrice2.equals("0.00"))) {
            double amount = Double.parseDouble(dPrice2);
            DecimalFormat formatter = new DecimalFormat("#,###");
            dPrice2 = formatter.format(amount);
        }
        else dPrice2 = "-";

        String comp = "-";
        String eDate = cachedRowSet.getString("개찰일시");
        if (eDate.length() == 21) {
            eDate = eDate.substring(2, 4) + eDate.substring(5, 7) + eDate.substring(8, 10) + " " + eDate.substring(11, 16);
        }
        String prog = cachedRowSet.getString("개찰결과");
        String annOrg = cachedRowSet.getString("공고기관");
        String demOrg = cachedRowSet.getString("수요기관");
        String bidType = "";
        String compType = cachedRowSet.getString("계약방법");
        String priceMethod = cachedRowSet.getString("예가방식");
        String criteria = cachedRowSet.getString("심사기준");
        String choiceMethod = cachedRowSet.getString("낙찰자선정방식");
        String lBound = cachedRowSet.getString("낙찰하한율");

        return new Object[] { index, bidno, date, limit, bPrice, ePrice, tPrice, aPrice, dPrice1, dPrice2,
                comp, eDate, prog, annOrg, demOrg, bidType, compType, priceMethod, criteria, choiceMethod, lBound };
    }
}