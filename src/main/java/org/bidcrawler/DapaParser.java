package org.bidcrawler;

/**
 * Created by ravenjoo on 6/25/17.
 */
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bidcrawler.utils.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class DapaParser extends Parser {
    private final static String BID_ANN_LIST = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=421&cfn=HI_PEB_Announce_Lst";
    private final static String BID_RES_LIST = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=428&cfn=HI_PEB_BidResult_Lst";
    private final static String NEGO_ANN_LIST = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=441&cfn=HI_PEB_OpenNego_Lst";
    private final static String NEGO_RES_LIST = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=443&cfn=HI_PEB_OpenNegoResult_Lst";

    private final static String BID_ANN_INF = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=421&cfn=HI_PEB_Announce_Inf&pointNumb=";
    private final static String BID_RES_INF = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=428&cfn=HI_PEB_BidResult_Inf";
    private final static String NEGO_ANN_INF = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=441&cfn=HI_PEB_OpenNego_Inf";
    private final static String NEGO_RES_INF = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=443&cfn=HI_PEB_OpenNegoResult_Inf";

    private final static String BID_ANN_PRICE = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=421&cfn=HI_PEB_PrePrice_Inf";
    private final static String BID_RES_PRICE = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=428&cfn=HI_PEB_MultiPrePrice_Inf";
    private final static String NEGO_RES_PRICE = "http://www.d2b.go.kr/Internet/jsp/peb/HI_PEB_Main.jsp?md=443&cfn=HI_PEB_OpenNegoMultiPrePrice_Inf1";

    // For SQL setup.
    private Connection db_con;
    java.sql.Statement st;
    ResultSet rs;

    private URL url;
    HttpURLConnection con;
    private HashMap<String, String> formData;
    String sd;
    String ed;
    private String op;
    private int totalItems;
    private int curItem;

    private GetFrame frame;

    public DapaParser(String sd, String ed, String op, GetFrame frame) throws SQLException, ClassNotFoundException {
        sd = sd.replaceAll("-", "%2F");
        ed = ed.replaceAll("-", "%2F");

        this.sd = sd;
        this.ed = ed;
        this.op = op;
        this.frame = frame;

        totalItems = 0;
        curItem = 0;

        formData = new HashMap<>();

        // Set up SQL connection.
        db_con = DriverManager.getConnection(
                "jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8",
                Util.DB_ID,
                Util.DB_PW
        );
        st = db_con.createStatement();
        rs = null;
    }

    public static void main(String args[]) throws IOException, ClassNotFoundException, SQLException {
        DapaParser tester = new DapaParser("2016-08-22", "2016-08-22", "입찰결과", null);

        tester.setOption("건수차이");
        tester.run();
    }

    public void openConnection(String path) throws IOException {
        url = new URL(path);
        con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4");
    }

    public String getResponse(String param) throws IOException {
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(param);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post parameters : " + param);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "EUC-KR"));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public void getList() throws IOException, SQLException {
        String path = "";
        if (op.equals("입찰공고")) path = DapaParser.BID_ANN_LIST;
        else if (op.equals("입찰결과")) path = DapaParser.BID_RES_LIST;
        else if (op.equals("협상공고")) path = DapaParser.NEGO_ANN_LIST;
        else if (op.equals("협상결과")) path = DapaParser.NEGO_RES_LIST;
        else {
            System.out.println("Declare the operation!");
            return;
        }

        openConnection(path);

        String param = "";

        if (op.equals("입찰공고") || op.equals("입찰결과")) {
            param = "pageNo=1&startPageNo=1&pagePerRow=10&txtBidxDateFrom="+sd+"&txtBidxDateTo="+ed;
        }
        else if (op.equals("협상공고") || op.equals("협상결과")) {
            param = "pageNo=1&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+sd+"&txtNegoDateTo="+ed;
        }

        Document doc = Jsoup.parse(getResponse(param));
        System.out.println(doc);
        if (doc.getElementsContainingOwnText("전체 건수 :").size() != 0) {
            totalItems = Integer.parseInt(doc.getElementsContainingOwnText("전체 건수 :").text().split(" ")[3]);
        }
        else totalItems = 0;

        String svrKey = "";
        if (op.equals("협상공고")) {
            svrKey = doc.getElementsByAttributeValue("name", "hidSvrKey").first().val();
        }

        int page = 1;
        int index = 1;

        Element table = doc.getElementsByTag("table").get(0);
        Elements rows = table.getElementsByTag("tr");

        for (int i = 0; i < totalItems; i++) {
            if (shutdown) {
                return;
            }

            if (op.equals("입찰결과") || op.equals("협상결과")) curItem++;
            Element row = rows.get(index);
            if (frame != null) {
                if (op.equals("입찰공고")) frame.updateInfo(row.getElementsByTag("td").get(3).text(), false);
                else if (op.equals("입찰결과")) frame.updateInfo(row.getElementsByTag("td").get(1).text(), true);
                else if (op.equals("협상공고")) frame.updateInfo(row.getElementsByTag("td").get(2).text(), false);
                else if (op.equals("협상결과")) frame.updateInfo(row.getElementsByTag("td").get(1).text(), true);
                frame.repaint();
            }
            boolean enter = parseListRow(row);
            if (enter) {
                // Collect post request parameters.
                Elements inputs = row.getElementsByTag("input");
                formData.clear();
                for (Element input : inputs) {
                    formData.put(input.attr("name"), input.val());
                }
                if (op.equals("협상공고")) {
                    formData.put("hidSvrKey", svrKey);
                }

                getInfo(index - 1, page); // Load the info page and continue parsing.
            }

            if (i % 10 == 9 && i < (totalItems - 1)) {
                index = 1;
                page++;

                // Load next listing page.
                openConnection(path);
                if (op.equals("입찰공고") || op.equals("입찰결과")) {
                    param = "pageNo="+page+"&startPageNo=1&pagePerRow=10&txtBidxDateFrom="+sd+"&txtBidxDateTo="+ed;
                }
                else if (op.equals("협상공고") || op.equals("협상결과")) {
                    param = "pageNo="+page+"&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+sd+"&txtNegoDateTo="+ed;
                }
                doc = Jsoup.parse(getResponse(param));
                table = doc.getElementsByTag("table").get(0);
                rows = table.getElementsByTag("tr");
            }
            else {
                index++;
            }
        }
    }

    public boolean parseListRow(Element row) throws SQLException {
        Elements data = row.getElementsByTag("td");
        boolean enter = true;

        if (op.equals("입찰공고")) {
            String[] bidID = data.get(2).text().replaceAll("\u00a0", "").split(" ");
            String bidType = bidID[1]; // 공고종류
            String bidCode = bidID[2]; // 공고번호
            String facilNum = data.get(3).text(); // 공사번호
            String org = data.get(5).text(); // 발주기관
            String[] deadlines = data.get(6).text().split(" ");
            String paperDeadline = ""; // 입찰서제출 마감일시
            String openDate = ""; // 개찰일자
            if (deadlines.length == 4) {
                paperDeadline = null;
                openDate = deadlines[2] + " " + deadlines[3];
            }
            else if (deadlines.length == 6) {
                paperDeadline = deadlines[2] + " " + deadlines[3];
                openDate = deadlines[4] + " " + deadlines[5];
            }
            else if (deadlines.length == 7) {
                paperDeadline = deadlines[3] + " " + deadlines[4];
                openDate = deadlines[5] + " " + deadlines[6];
            }
            else if (deadlines.length == 8) {
                paperDeadline = deadlines[4] + " " + deadlines[5];
                openDate = deadlines[6] + " " + deadlines[7];
            }
            else if (deadlines.length == 1) {
                paperDeadline = null;
                openDate = deadlines[0] + " 00:00:00";
            }
            String compType = data.get(7).text().split(" ")[0]; // 계약방법
            String priceOpen = data.get(8).text(); // 기초예가공개

            boolean exists = false;

            String[] codeSplit = bidCode.split("-");
            String bidno = codeSplit[0];
            String bidver = codeSplit[1];

            String where = "WHERE 공고번호=\""+bidno+"\" AND " +
                    "차수="+bidver+" AND " +
                    "공사번호=\""+facilNum+"\" AND " +
                    "발주기관=\""+org+"\"";
            String sql = "SELECT EXISTS(SELECT 공사번호 FROM dapabidinfo "+where+");";
            rs = st.executeQuery(sql);
            if (rs.first()) exists = rs.getBoolean(1);

            if (exists) {
                // Check the bid version and update level from the DB.
                sql = "SELECT 공고, 기초예가공개 FROM dapabidinfo "+where;
                rs = st.executeQuery(sql);
                int finished = 0;
                String dbPriceOpen = "";
                if (rs.first()) {
                    finished = rs.getInt(1);
                    dbPriceOpen = rs.getString(2) == null ? "" : rs.getString(2);
                }
                if (finished > 0) {
                    if (dbPriceOpen.equals(priceOpen)) enter = false;
                    else {
                        sql = "UPDATE dapabidinfo SET 기초예가공개=\""+priceOpen+"\" "+where;
                        st.executeUpdate(sql);
                    }
                }
                else if (!dbPriceOpen.equals(priceOpen)) {
                    sql = "UPDATE dapabidinfo SET 기초예가공개=\""+priceOpen+"\" "+where;
                    st.executeUpdate(sql);
                }
            }
            else {
                // If entry doesn't exists in db, insert new row.
                sql = "INSERT INTO dapabidinfo (공고번호, 차수, 공고종류, 공사번호, 발주기관, 개찰일시, 계약방법, 기초예가공개) VALUES (" +
                        "\""+bidno+"\", " +
                        ""+bidver+", " +
                        "\""+bidType+"\", " +
                        "\""+facilNum+"\", " +
                        "\""+org+"\", " +
                        "\""+openDate+"\", " +
                        "\""+compType+"\", " +
                        "\""+priceOpen+"\");";
                st.executeUpdate(sql);
                if (paperDeadline != null) {
                    sql = "UPDATE dapabidinfo SET 입찰서제출마감일시=\""+paperDeadline+"\" " + where;
                    st.executeUpdate(sql);
                }
                if (compType.equals("지명경쟁")) {
                    sql = "UPDATE dapabidinfo SET 공고=1 " + where;
                    st.executeUpdate(sql);
                    enter = false;
                }
            }
        }
        else if (op.equals("입찰결과")) {
            String[] bidID = data.get(0).text().split(" ");
            String bidType = bidID[0]; // 공고종류
            String bidCode = bidID[1]; // 공고번호-차수
            String facilNum = data.get(1).text().replaceAll("\u00a0", ""); // 공사번호
            String org = data.get(3).text(); // 발주기관
            String compType = data.get(4).text(); // 계약방법
            String openDate = data.get(7).text() + ":00"; // 개찰일시
            String result = data.get(8).text(); // 입찰결과

            boolean exists = false;

            String[] codeSplit = bidCode.split("-");
            String bidno = codeSplit[0]; // 공고번호
            String bidver = codeSplit[1]; // 차수

            String where = "WHERE 공고번호=\""+bidno+"\" AND " +
                    "차수="+bidver+" AND " +
                    "공사번호=\""+facilNum+"\" AND " +
                    "발주기관=\""+org+"\"";
            String sql = "SELECT EXISTS(SELECT 공사번호 FROM dapabidinfo "+where+");";
            rs = st.executeQuery(sql);
            if (rs.first()) exists = rs.getBoolean(1);

            if (exists) {
                // Check the bid version and update level from the DB.
                sql = "SELECT 완료, 입찰결과 FROM dapabidinfo "+where;
                rs = st.executeQuery(sql);
                int finished = 0;
                String dbResult = "";
                if (rs.first()) {
                    finished = rs.getInt(1);
                    dbResult = rs.getString(2) == null ? "" : rs.getString(2);
                }
                if (finished > 0) {
                    if (dbResult.equals(result)) enter = false;
                    else {
                        sql = "UPDATE dapabidinfo SET 입찰결과=\""+result+"\" "+where;
                        st.executeUpdate(sql);
                    }
                }
                else if (!dbResult.equals(result)) {
                    sql = "UPDATE dapabidinfo SET 입찰결과=\""+result+"\" "+where;
                    st.executeUpdate(sql);
                }
            }
            else {
                // If entry doesn't exists in db, insert new row.
                sql = "INSERT INTO dapabidinfo (공고번호, 차수, 공고종류, 공사번호, 발주기관, 개찰일시, 계약방법, 입찰결과) VALUES (" +
                        "\""+bidno+"\", " +
                        ""+bidver+", " +
                        "\""+bidType+"\", " +
                        "\""+facilNum+"\", " +
                        "\""+org+"\", " +
                        "\""+openDate+"\", " +
                        "\""+compType+"\", " +
                        "\""+result+"\");";
                st.executeUpdate(sql);
            }
        }
        else if (op.equals("협상공고")) {
            String[] datefrag = data.get(1).text().split(" ");
            String negoDate = ""; // 개찰일시
            if (datefrag.length == 4) negoDate = datefrag[2] + " " + datefrag[3] + ":00";
            else if (datefrag.length == 5) negoDate = datefrag[3] + " " + datefrag[4] + ":00";
            String facilNum = data.get(2).text(); // 공사번호
            String negoVer = data.get(3).text(); // 차수
            String org = data.get(5).text(); // 발주기관
            String prog = data.get(9).text(); // 진행상태

            boolean exists = false;

            String where = "WHERE 공사번호=\""+facilNum+"\" AND " +
                    "차수="+negoVer+" AND " +
                    "발주기관=\""+org+"\"";
            String sql = "SELECT EXISTS(SELECT 공사번호 FROM dapanegoinfo "+where+");";
            rs = st.executeQuery(sql);
            if (rs.first()) exists = rs.getBoolean(1);

            if (exists) {
                // Check the bid version and update level from the DB.
                sql = "SELECT 공고, 진행상태 FROM dapanegoinfo "+where;
                rs = st.executeQuery(sql);
                int finished = 0;
                String dbProg = "";
                if (rs.first()) {
                    finished = rs.getInt(1);
                    dbProg = rs.getString(2) == null ? "" : rs.getString(2);
                }
                if (finished > 0) {
                    if (dbProg.equals(prog)) enter = false;
                    else {
                        sql = "UPDATE dapanegoinfo SET 진행상태=\""+prog+"\" "+where;
                        st.executeUpdate(sql);
                    }
                }
                else if (!dbProg.equals(prog)) {
                    sql = "UPDATE dapanegoinfo SET 진행상태=\""+prog+"\" "+where;
                    st.executeUpdate(sql);
                }
            }
            else {
                // If entry doesn't exists in db, insert new row.
                sql = "INSERT INTO dapanegoinfo (차수, 공사번호, 발주기관, 개찰일시, 진행상태) VALUES (" +
                        ""+negoVer+", " +
                        "\""+facilNum+"\", " +
                        "\""+org+"\", " +
                        "\""+negoDate+"\", " +
                        "\""+prog+"\");";
                st.executeUpdate(sql);
            }
        }
        else if (op.equals("협상결과")) {
            String negoDate = data.get(0).text() + ":00"; // 개찰일시
            String facilNum = data.get(1).text(); // 공사번호
            String negoVer = data.get(2).text(); // 차수
            String org = data.get(4).text(); // 발주기관
            String result = data.get(7).text(); // 협상결과

            boolean exists = false;

            String where = "WHERE 공사번호=\""+facilNum+"\" AND " +
                    "차수="+negoVer+" AND " +
                    "발주기관=\""+org+"\"";
            String sql = "SELECT EXISTS(SELECT 공사번호 FROM dapanegoinfo "+where+");";
            rs = st.executeQuery(sql);
            if (rs.first()) exists = rs.getBoolean(1);

            if (exists) {
                // Check the bid version and update level from the DB.
                sql = "SELECT 완료, 협상결과 FROM dapanegoinfo "+where;
                rs = st.executeQuery(sql);
                int finished = 0;
                String dbResult = "";
                if (rs.first()) {
                    finished = rs.getInt(1);
                    dbResult = rs.getString(2) == null ? "" : rs.getString(2);
                }
                if (finished > 0) {
                    if (dbResult.equals(result)) enter = false;
                    else {
                        sql = "UPDATE dapanegoinfo SET 협상결과=\""+result+"\" "+where;
                        st.executeUpdate(sql);
                    }
                }
                else if (!dbResult.equals(result)) {
                    sql = "UPDATE dapanegoinfo SET 협상결과=\""+result+"\" "+where;
                    st.executeUpdate(sql);
                }
            }
            else {
                // If entry doesn't exists in db, insert new row.
                sql = "INSERT INTO dapanegoinfo (차수, 공사번호, 발주기관, 개찰일시, 협상결과) VALUES (" +
                        ""+negoVer+", " +
                        "\""+facilNum+"\", " +
                        "\""+org+"\", " +
                        "\""+negoDate+"\", " +
                        "\""+result+"\");";
                st.executeUpdate(sql);
            }
        }

        return enter;
    }

    public void getInfo(int hidNumb, int page) throws IOException, SQLException {
        String path = "";

        if (op.equals("입찰공고")) path = DapaParser.BID_ANN_INF;
        else if (op.equals("입찰결과")) path = DapaParser.BID_RES_INF;
        else if (op.equals("협상공고")) path = DapaParser.NEGO_ANN_INF;
        else if (op.equals("협상결과")) path = DapaParser.NEGO_RES_INF;
        else {
            System.out.println("Declare the operation!");
            return;
        }

        // Load info page.
        if (path.equals(DapaParser.BID_ANN_INF)) path += hidNumb;
        openConnection(path);
        String param = "";
        if (op.equals("입찰공고") || op.equals("입찰결과")) {
            param = "hidChkNumb=0&pageNo="+page+"&startPageNo=1&pagePerRow=10&txtBidxDateFrom="+sd+"&txtBidxDateTo="+ed+"&";
        }
        else if (op.equals("협상공고")) {
            param = "hidChkNumb=0&pageNo="+page+"&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+sd+"&txtNegoDateTo="+ed+"&";
        }
        else if (op.equals("협상결과")) {
            param = "hidChkNumb=0&pageNo=1&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+sd+"&txtNegoDateTo="+ed+"&";
        }

        Iterator<Entry<String, String>> it = formData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = (Map.Entry<String, String>) it.next();
            if (pair.getValue() != null) {
                String val = pair.getValue().toString();
                if (val.contains("용역")) val = val.replaceAll("용역", "%BF%EB%BF%AA");
                else if (val.contains("시설")) val = val.replaceAll("시설", "%BD%C3%BC%B3");
                else if (val.contains("감리")) val = val.replaceAll("감리", "%B0%A8%B8%AE");
                else if (val.contains("공원")) val = val.replaceAll("공원", "%B0%F8%BF%F8");
                else if (val.contains("보수")) val = val.replaceAll("보수", "%BA%B8%BC%F6");
                else if (val.contains("책임")) val = val.replaceAll("책임", "%C3%A5%C0%D3");
                else if (val.contains("방포교")) val = val.replaceAll("방포교", "%B9%E6%C6%F7%B1%B3");
                else if (val.contains("표준도설계")) val = val.replaceAll("표준도설계", "%C7%A5%C1%D8%B5%B5%BC%B3%B0%E8");
                else if (val.contains("설계")) val = val.replaceAll("설계", "%BC%B3%B0%E8");
                else if (val.contains("폐기물")) val = val.replaceAll("폐기물", "%C6%F3%B1%E2%B9%B0");
                else if (val.contains("폐기")) val = val.replaceAll("폐기", "%C6%F3%B1%E2");
                else if (val.contains("폐")) val = val.replaceAll("폐", "%C6%F3");
                else if (val.contains("지역정밀")) val = val.replaceAll("지역정밀", "%C1%F6%BF%AA%C1%A4%B9%D0");
                else if (val.contains("건물안전")) val = val.replaceAll("건물안전", "%B0%C7%B9%B0%BE%C8%C0%FC");
                else if (val.contains("정밀안전")) val = val.replaceAll("정밀안전", "%C1%A4%B9%D0%BE%C8%C0%FC");
                else if (val.contains("안전")) val = val.replaceAll("안전", "%BE%C8%C0%FC");
                else if (val.contains("경쟁")) val = val.replaceAll("경쟁", "%B0%E6%C0%EF");
                else if (val.contains("시")) val = val.replaceAll("시", "%BD%C3");
                else if (val.contains("복")) val = val.replaceAll("복", "%BA%B9");
                else if (val.contains("보")) val = val.replaceAll("보", "%BA%B8");
                else if (val.contains("ㅡ")) val = val.replaceAll("ㅡ", "%A4%D1");
                else if (val.contains("육")) val = val.replaceAll("육", "%C0%B0");
                else if (val.contains("으")) val = val.replaceAll("으", "%C0%B8");
                else if (val.contains("공")) val = val.replaceAll("공", "%B0%F8");
                else if (val.contains("기타")) val = val.replaceAll("기타", "%B1%E2%C5%B8");
                param += pair.getKey() + "=" + val + "&";
            }
        }
        param = param.substring(0, param.length()-1);

        // Start parsing.
        Document doc = Jsoup.parse(getResponse(param));
        String where = parseInfo(doc);

        if (where != null) {
            boolean getprice = false;
            if (op.equals("입찰공고")) {
                if (doc.getElementsByAttributeValue("alt", "기초예비가격조회").size() > 0) getprice = true;
            }
            else if (op.equals("입찰결과") || op.equals("협상결과")) {
                if (doc.getElementsByAttributeValue("alt", "복수예가조회").size() > 0) getprice = true;
            }

            if (getprice) {
                Element form = doc.getElementsByAttributeValue("name", "form").get(0);
                Elements formElements = form.children();

                formData.clear();
                for (Element e : formElements) {
                    if (e.tagName().equals("input")) {
                        String key = e.attr("name");
                        String val = e.val();
                        formData.put(key, val);
                    }
                }

                getPrice(where);
            }
            else {
                if (op.equals("입찰공고")) {
                    String sql = "UPDATE dapabidinfo SET 공고=1 "+where;
                    st.executeUpdate(sql);
                }
                else if (op.equals("입찰결과")) {
                    String sql = "UPDATE dapabidinfo SET 완료=1 "+where;
                    st.executeUpdate(sql);
                }
                else if (op.equals("협상공고")) {
                    String sql = "UPDATE dapanegoinfo SET 공고=1 "+where;
                    st.executeUpdate(sql);
                }
                else if (op.equals("협상결과")) {
                    String sql = "UPDATE dapanegoinfo SET 완료=1 "+where;
                    st.executeUpdate(sql);
                }
            }
        }
    }

    public String parseInfo(Document doc) throws SQLException {
        String where = "";

        if (op.equals("입찰공고")) {
            Element infoTable = doc.getElementsByTag("table").first();
            Elements infos = infoTable.getElementsByTag("th");

            String bidno = ""; // 공고번호
            String bidver = ""; // 차수
            String facilNum = ""; // 공사번호
            String org = ""; // 발주기관
            String license = ""; // 면허명칭
            String bidMethod = ""; // 입찰방법
            String hasBase = ""; // 기초예가적용여부
            String prelim = ""; // 사전심사
            String selectMethod = ""; // 낙찰자결정방법

            for (int j = 0; j < infos.size(); j++) {
                String key = infos.get(j).text();
                if (key.equals("공고번호-차수")) {
                    String check = infos.get(j).nextElementSibling().text();
                    if (check.split(" ").length > 1) {
                        String value = infos.get(j).nextElementSibling().text().split(" ")[1];
                        bidno = value.split("-")[0];
                        bidver = value.split("-")[1];
                    }
                    else return null;
                }
                else if (key.equals("공사번호")) {
                    facilNum = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("발주기관")) {
                    org = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("입찰방법")) {
                    bidMethod = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("기초예가적용여부")) {
                    hasBase = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("사전심사")) {
                    prelim = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("낙찰자결정방법")) {
                    selectMethod = infos.get(j).nextElementSibling().text();
                }
            }

            if (doc.getElementsByAttributeValue("summary", "그룹, 면허명칭이 나와있습니다").size() != 0) {
                Element licenseTable = doc.getElementsByAttributeValue("summary", "그룹, 면허명칭이 나와있습니다").get(0);
                Elements licenses = licenseTable.getElementsByTag("tr"); // Rows for the table of licenses

                for (int j = 1; j < licenses.size(); j++) {
                    String li = licenses.get(j).getElementsByTag("td").get(1).text();
                    li = li.replaceAll("\u00a0", "");
                    license += " " + li;
                }
                System.out.println(license.length());
                if (license.length() > 254) license = license.substring(0, 254);
            }

            where = "WHERE 공고번호=\""+bidno+"\" AND " +
                    "차수="+bidver+" AND " +
                    "공사번호=\""+facilNum+"\" AND " +
                    "발주기관=\""+org+"\"";
            String sql = "UPDATE dapabidinfo SET 면허명칭=\""+license+"\", " +
                    "입찰방법=\""+bidMethod+"\", " +
                    "기초예가적용여부=\""+hasBase+"\", " +
                    "사전심사=\""+prelim+"\", " +
                    "낙찰자결정방법=\""+selectMethod+"\" " + where;
            st.executeUpdate(sql);
        }
        else if (op.equals("입찰결과")) {
            Element infoTable = doc.getElementsByTag("table").first();
            Elements infos = infoTable.getElementsByTag("th");

            String bidno = ""; // 공고번호
            String bidver = ""; // 차수
            String facilNum = ""; // 공사번호
            String org = ""; // 발주기관
            String bidMethod = ""; // 입찰방법
            String prelim = ""; // 사전심사
            String selectMethod = ""; // 낙찰자결정방법
            String basePrice = "0"; // 기초예비가격
            String expPrice = "0"; // 예정가격
            String rate = ""; // 사정률
            String bidBound = ""; // 낙찰하한율
            String bidPrice = "0"; // 투찰금액
            String comp = "0"; // 참가수

            for (int j = 0; j < infos.size(); j++) {
                String key = infos.get(j).text();
                if (key.equals("공고번호")) {
                    String check = infos.get(j).nextElementSibling().text();
                    if (check.split(" ").length > 1) {
                        String value = infos.get(j).nextElementSibling().text().split(" ")[1];
                        bidno = value.split("-")[0];
                        bidver = value.split("-")[1];
                    }
                    else return null;
                }
                else if (key.equals("공사번호")) {
                    facilNum = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("발주기관")) {
                    org = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("입찰방법")) {
                    bidMethod = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("사전심사")) {
                    prelim = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("낙찰자결정방법")) {
                    selectMethod = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("기초예비가격")) {
                    basePrice = infos.get(j).nextElementSibling().text();
                    basePrice = basePrice.replaceAll("[^\\d]", "");
                    if (basePrice.equals("")) basePrice = "0";
                }
                else if (key.equals("예정가격")) {
                    expPrice = infos.get(j).nextElementSibling().text();
                    expPrice = expPrice.replaceAll("[^\\d]", "");
                    if (expPrice.equals("")) expPrice = "0";
                }
                else if (key.equals("사정률(%)")) {
                    rate = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("낙찰하한율(%)")) {
                    bidBound = infos.get(j).nextElementSibling().text();
                }
            }

            String summary = "차수, 업체코드 업체명,대표자,투찰금액,낙찰률,결과를 알수 있습니다.";
            if (doc.getElementsByAttributeValue("summary", summary).size() != 0) {
                Element resultTable = doc.getElementsByAttributeValue("summary", summary).get(0);
                if (resultTable.getElementsByTag("tr").size() > 1) {
                    Element top = resultTable.getElementsByTag("tr").get(1);

                    bidPrice = top.getElementsByTag("td").get(4).text();
                    bidPrice = bidPrice.replaceAll("[^\\d]", "");
                    if (bidPrice.equals("")) bidPrice = "0";

                    if (doc.getElementsContainingOwnText("전체 건수 :").size() > 0) {
                        comp = doc.getElementsContainingOwnText("전체 건수 :").text().split(" ")[3];
                        if (comp.equals("")) comp = "0";
                    }
                }
            }

            where = "WHERE 공고번호=\""+bidno+"\" AND " +
                    "차수="+bidver+" AND " +
                    "공사번호=\""+facilNum+"\" AND " +
                    "발주기관=\""+org+"\"";
            String sql = "UPDATE dapabidinfo SET 입찰방법=\""+bidMethod+"\", " +
                    "사전심사=\""+prelim+"\", " +
                    "기초예비가격="+basePrice+", " +
                    "예정가격="+expPrice+", " +
                    "사정률=\""+rate+"\", " +
                    "낙찰하한율=\""+bidBound+"\", " +
                    "투찰금액="+bidPrice+", " +
                    "참여수="+comp+", " +
                    "낙찰자결정방법=\""+selectMethod+"\" " + where;
            st.executeUpdate(sql);
        }
        else if (op.equals("협상공고")) {
            String negno = ""; // 공고번호
            String negoVer = ""; // 차수
            String facilNum = ""; // 공사번호
            String org = ""; // 발주기관
            String compType = ""; // 계약방법
            String selectMethod = ""; // 낙찰자결정방법
            String bidMethod = ""; // 입찰방법
            String hasBase = ""; // 기초예가적용여부
            String paperDeadline = ""; // 견적서제출마감시간
            String negoMethod = ""; // 협상형태
            String basePrice = "0"; // 기초예비가격
            String lowerBound = ""; // 하한
            String upperBound = ""; // 상한
            String bidBound = ""; // 낙찰하한율
            String license = ""; // 면허명칭

            Element infoTable = doc.getElementsByTag("table").first();
            Elements infos = infoTable.getElementsByTag("th"); // Headers for table of details
            for (int j = 0; j < infos.size(); j++) {
                String key = infos.get(j).text();
                if (key.equals("공고번호-차수")) {
                    if (infos.get(j).nextElementSibling().text().length() <= 1) return null;
                    else {
                        String value = infos.get(j).nextElementSibling().text();
                        System.out.println(value);
                        negno = value.split("-")[0];
                        negoVer = value.split("-")[1];
                    }
                }
                else if (key.equals("발주기관")) {
                    org = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("공사번호")) {
                    facilNum = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("계약방법")) {
                    compType = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("낙찰자결정방법")) {
                    selectMethod = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("입찰방법")) {
                    bidMethod = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("기초예가적용여부")) {
                    hasBase = infos.get(j).nextElementSibling().text();
                }
                else if (key.equals("견적서제출마감시간")) {
                    paperDeadline = infos.get(j).nextElementSibling().text() + ":00";
                }
                else if (key.equals("협상형태")) {
                    negoMethod = infos.get(j).nextElementSibling().text();
                }
            }

            Element licenseTable = null;
            Element minPriceTable = null;
            Elements tableNames = doc.getElementsByTag("caption");
            for (int j = 0; j < tableNames.size(); j++) {
                String name = tableNames.get(j).text();
                if (name.equals("그룹")) {
                    licenseTable = tableNames.get(j).parent();
                }
                else if (name.equals("기초예비가격 조회")) {
                    minPriceTable = tableNames.get(j).parent();
                }
            }

            if (minPriceTable != null) {
                Elements minPrices = minPriceTable.getElementsByTag("th");
                for (int j = 0; j < minPrices.size(); j++) {
                    String key = minPrices.get(j).text();
                    if (key.equals("하한(%)")) {
                        lowerBound = minPrices.get(j).nextElementSibling().text();
                    }
                    else if (key.equals("상한(%)")) {
                        upperBound = minPrices.get(j).nextElementSibling().text();
                    }
                    else if (key.equals("낙찰하한율(%)")) {
                        bidBound = minPrices.get(j).nextElementSibling().text();
                    }
                    else if (key.equals("기초예비가격")) {
                        basePrice = minPrices.get(j).nextElementSibling().text();
                        basePrice = basePrice.replaceAll("[^\\d]", "");
                        if (basePrice.equals("")) basePrice = "0";
                    }
                }
            }

            if (licenseTable != null) {
                Elements licenses = licenseTable.getElementsByTag("tr"); // Rows for the table of licenses

                // Compile the licenses into one string.
                for (int j = 1; j < licenses.size(); j++) {
                    String li = licenses.get(j).getElementsByTag("td").get(1).text();
                    li = li.replaceAll("\u00a0", "");
                    license += " " + li;
                }
                if (license.length() > 255) license = license.substring(0, 254);
            }

            where = "WHERE 공사번호=\""+facilNum+"\" AND " +
                    "차수="+negoVer+" AND " +
                    "발주기관=\""+org+"\"";

            String sql = "UPDATE dapanegoinfo SET 계약방법=\""+compType+"\", " +
                    "낙찰자결정방법=\""+selectMethod+"\", " +
                    "입찰방법=\""+bidMethod+"\", " +
                    "기초예가적용여부=\""+hasBase+"\", " +
                    "견적서제출마감일시=\""+paperDeadline+"\", " +
                    "협상형태=\""+negoMethod+"\", " +
                    "상한=\""+upperBound+"\", " +
                    "하한=\""+lowerBound+"\", " +
                    "낙찰하한율=\""+bidBound+"\", " +
                    "기초예비가격="+basePrice+", " +
                    "면허명칭=\""+license+"\", " +
                    "공고=1, " +
                    "공고번호=\""+negno+"\" " + where;
            st.executeUpdate(sql);
        }
        else if (op.equals("협상결과")) {
            String negno = ""; // 공고번호
            String negoVer = ""; // 차수
            String org = ""; // 발주기관
            String facilNum = ""; // 공사번호
            String compType = ""; // 계약방법
            String selectMethod = ""; // 낙찰자결정방법
            String bidMethod = ""; // 입찰방법
            String hasBase = ""; // 기초예가적용여부
            String paperDeadline = ""; // 견적서제출마감시간
            String negoMethod = ""; // 협상형태
            String basePrice = "0"; // 기초예비가격
            String lowerBound = ""; // 하한
            String upperBound = ""; // 상한
            String bidBound = ""; // 낙찰하한율
            String expPrice = "0"; // 예정가격
            String bidPrice = "0"; // 투찰금액
            String comp = "0"; // 참여수

            Element infoTable = doc.getElementsByTag("table").first();
            Elements infos = infoTable.getElementsByTag("th"); // Headers for table of details
            for (int j = 0; j < infos.size(); j++) {
                String key = infos.get(j).text();
                String value = infos.get(j).nextElementSibling().text();
                if (key.equals("공고번호-차수")) {
                    if (value.length() <= 1) return null;
                    else {
                        value = infos.get(j).nextElementSibling().text();
                        negno = value.split("-")[0];
                        negoVer = value.split("-")[1];
                    }
                }
                else if (key.equals("발주기관")) {
                    org = value;
                }
                else if (key.equals("공사번호")) {
                    facilNum = value;
                }
                else if (key.equals("공사번호")) {
                    facilNum = value;
                }
                else if (key.equals("계약방법")) {
                    compType = value;
                }
                else if (key.equals("낙찰자결정방법")) {
                    selectMethod = value;
                }
                else if (key.equals("입찰방법")) {
                    bidMethod = value;
                }
                else if (key.equals("기초예가적용여부")) {
                    hasBase = value;
                }
                else if (key.equals("견적서제출마감일시") && value.length() == 16) {
                    value.replaceAll("/", "-");
                    value += ":00";
                    paperDeadline = value;
                }
                else if (key.equals("협상형태")) {
                    negoMethod = value;
                }
                else if (key.equals("기초예비가격")) {
                    value = value.replaceAll("[^\\d]", "");
                    if (value.equals("")) value = "0";
                    basePrice = value;
                }
                else if (key.equals("예정가격")) {
                    value = value.replaceAll("[^\\d]", "");
                    if (value.equals("")) value = "0";
                    expPrice = value;
                }
                else if (key.equals("하한(%)")) {
                    lowerBound = value;
                }
                else if (key.equals("상한(%)")) {
                    upperBound = value;
                }
                else if (key.equals("낙찰하한율(%)")) {
                    bidBound = value;
                }
            }

            String summary = "공개협상결과에 대해 차수,업체코드,업체명,대표자,견적금액,낙찰률,결과 투찰일시를 알 수 있습니다";
            if (doc.getElementsByAttributeValue("summary", summary).size() != 0) {
                Element resultTable = doc.getElementsByAttributeValue("summary", summary).get(0);
                if (resultTable.getElementsByTag("tr").size() > 1) {
                    Element top = resultTable.getElementsByTag("tr").get(1);
                    if (top.text().length() > 1) {
                        bidPrice = top.getElementsByTag("td").get(4).text();

                        bidPrice = bidPrice.replaceAll("[^\\d]", "");
                        if (bidPrice.equals("")) bidPrice = "0";

                        if (doc.getElementsContainingOwnText("전체 건수 :").size() > 0) {
                            comp = doc.getElementsContainingOwnText("전체 건수 :").text().split(" ")[3];
                            if (comp.equals("")) comp = "0";
                        }
                    }
                }
            }

            where = "WHERE 공사번호=\""+facilNum+"\" AND " +
                    "차수="+negoVer+" AND " +
                    "발주기관=\""+org+"\"";
            String sql = "UPDATE dapanegoinfo SET 계약방법=\""+compType+"\", " +
                    "낙찰자결정방법=\""+selectMethod+"\", " +
                    "입찰방법=\""+bidMethod+"\", " +
                    "기초예가적용여부=\""+hasBase+"\", " +
                    "견적서제출마감일시=\""+paperDeadline+"\", " +
                    "협상형태=\""+negoMethod+"\", " +
                    "상한=\""+upperBound+"\", " +
                    "하한=\""+lowerBound+"\", " +
                    "낙찰하한율=\""+bidBound+"\", " +
                    "기초예비가격="+basePrice+", " +
                    "예정가격="+expPrice+", " +
                    "투찰금액="+bidPrice+", " +
                    "참여수="+comp+", " +
                    "공고번호=\""+negno+"\" " + where;
            System.out.println(sql);
            st.executeUpdate(sql);
        }

        return where;
    }

    public void getPrice(String where) throws IOException, SQLException {
        String path = "";

        if (op.equals("입찰공고")) path = DapaParser.BID_ANN_PRICE;
        else if (op.equals("입찰결과")) path = DapaParser.BID_RES_PRICE;
        else if (op.equals("협상결과")) path = DapaParser.NEGO_RES_PRICE;
        else {
            System.out.println("Declare the operation!");
            return;
        }

        // Load new price page.
        openConnection(path);
        String param = "";
        Iterator<Entry<String, String>> it = formData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> pair = (Map.Entry<String, String>) it.next();
            if (pair.getValue() != null) {
                String val = pair.getValue().toString();
                if (val.contains("용역")) val = val.replaceAll("용역", "%BF%EB%BF%AA");
                else if (val.contains("시설")) val = val.replaceAll("시설", "%BD%C3%BC%B3");
                else if (val.contains("감리")) val = val.replaceAll("감리", "%B0%A8%B8%AE");
                else if (val.contains("공원")) val = val.replaceAll("공원", "%B0%F8%BF%F8");
                else if (val.contains("보수")) val = val.replaceAll("보수", "%BA%B8%BC%F6");
                else if (val.contains("책임")) val = val.replaceAll("책임", "%C3%A5%C0%D3");
                else if (val.contains("방포교")) val = val.replaceAll("방포교", "%B9%E6%C6%F7%B1%B3");
                else if (val.contains("표준도설계")) val = val.replaceAll("표준도설계", "%C7%A5%C1%D8%B5%B5%BC%B3%B0%E8");
                else if (val.contains("설계")) val = val.replaceAll("설계", "%BC%B3%B0%E8");
                else if (val.contains("폐기물")) val = val.replaceAll("폐기물", "%C6%F3%B1%E2%B9%B0");
                else if (val.contains("폐기")) val = val.replaceAll("폐기", "%C6%F3%B1%E2");
                else if (val.contains("폐")) val = val.replaceAll("폐", "%C6%F3");
                else if (val.contains("지역정밀")) val = val.replaceAll("지역정밀", "%C1%F6%BF%AA%C1%A4%B9%D0");
                else if (val.contains("건물안전")) val = val.replaceAll("건물안전", "%B0%C7%B9%B0%BE%C8%C0%FC");
                else if (val.contains("정밀안전")) val = val.replaceAll("정밀안전", "%C1%A4%B9%D0%BE%C8%C0%FC");
                else if (val.contains("안전")) val = val.replaceAll("안전", "%BE%C8%C0%FC");
                else if (val.contains("경쟁")) val = val.replaceAll("경쟁", "%B0%E6%C0%EF");
                else if (val.contains("시")) val = val.replaceAll("시", "%BD%C3");
                else if (val.contains("복")) val = val.replaceAll("복", "%BA%B9");
                else if (val.contains("보")) val = val.replaceAll("보", "%BA%B8");
                else if (val.contains("ㅡ")) val = val.replaceAll("ㅡ", "%A4%D1");
                else if (val.contains("육")) val = val.replaceAll("육", "%C0%B0");
                else if (val.contains("으")) val = val.replaceAll("으", "%C0%B8");
                else if (val.contains("공")) val = val.replaceAll("공", "%B0%F8");
                else if (val.contains("기타")) val = val.replaceAll("기타", "%B1%E2%C5%B8");
                param += pair.getKey() + "=" + val + "&";
            }
        }
        param = param.substring(0, param.length()-1);

        Document doc = Jsoup.parse(getResponse(param));
        parsePricePage(doc, where);
    }

    public void parsePricePage(Document doc, String where) throws SQLException {
        if (op.equals("입찰공고")) {
            String summary = "기초예비가격, 하한, 상한, 낙찰하한율, 국민건강보험료, 국민연금보험료, 평가기준금액, 토목기초금액, 건축기초금액, 노무비기준율, 기타경비기준율, 일반관리비기준비, 이윤기준율, 적용난이도계수가 나와있습니다.";
            if (doc.getElementsByAttributeValue("summary", summary).size() > 0) {
                Element priceTable = doc.getElementsByAttributeValue("summary", summary).get(0);
                Elements hs = priceTable.getElementsByTag("th");

                String lowerBound = ""; // 하한
                String upperBound = ""; // 상한
                String bidBound = ""; // 낙찰하한율
                String rate = ""; // 사정률
                String basePrice = ""; // 기초예비가격

                for (int k = 0; k < hs.size(); k++) {
                    String head = hs.get(k).text();
                    if (head.equals("하한(%)")) {
                        lowerBound = hs.get(k).nextElementSibling().text();
                    }
                    else if (head.equals("상한(%)")) {
                        upperBound = hs.get(k).nextElementSibling().text();
                    }
                    else if (head.equals("낙찰하한율(%)")) {
                        bidBound = hs.get(k).nextElementSibling().text();
                    }
                    else if (head.equals("사정률(%)")) {
                        rate = hs.get(k).nextElementSibling().text();
                    }
                    else if (head.equals("기초예비가격")) {
                        basePrice = hs.get(k).nextElementSibling().text();
                        basePrice = basePrice.replaceAll("[^\\d]", "");
                    }
                }

                String sql = "UPDATE dapabidinfo SET 하한=\""+lowerBound+"\", " +
                        "상한=\""+upperBound+"\", " +
                        "낙찰하한율=\""+bidBound+"\", " +
                        "사정률=\""+rate+"\", " +
                        "기초예비가격="+basePrice+" " + where;
                st.executeUpdate(sql);
            }
            String sql = "UPDATE dapabidinfo SET 공고=1 "+where;
            st.executeUpdate(sql);
        }
        else if (op.equals("입찰결과")) {
            String summary = "선택번호에 대해 금액과 업체명이 나와있습니다.";
            if (doc.getElementsByAttributeValue("summary", summary).size() > 0) {
                Element dupTable = doc.getElementsByAttributeValue("summary", summary).get(0);
                Elements dupRows = dupTable.getElementsByTag("tr");

                for (int x = 1; x <= 5; x++) {
                    Elements r = dupRows.get(x).children();
                    for (int y = 0; y < 9; y += 3) {
                        String dupNo = r.get(y).text();
                        String dupPrice = r.get(y + 1).text();
                        dupPrice = dupPrice.replaceAll(",", "");
                        dupPrice = dupPrice.replaceAll("원", "");
                        String dupCom = r.get(y + 2).text();
                        String s = "UPDATE dapabidinfo SET 복수" + dupNo + "=" + dupPrice + ", 복참" + dupNo + "=" + dupCom + " " + where;
                        st.executeUpdate(s);
                    }
                }
            }
            String sql = "UPDATE dapabidinfo SET 완료=1 " + where;
            st.executeUpdate(sql);
        }
        else if (op.equals("협상결과")) {
            String summary = "복수예비가격에 대해 번호, 금액, 회수를 알 수 있습니다";
            if (doc.getElementsByAttributeValue("summary", summary).size() > 0) {
                Element dupTable = doc.getElementsByAttributeValue("summary", summary).get(0);
                Elements dupRows = dupTable.getElementsByTag("tr");

                for (int x = 1; x <= 5; x++) {
                    Elements r = dupRows.get(x).children();
                    for (int y = 0; y < 9; y += 3) {
                        String dupNo = r.get(y).text();
                        String dupPrice = r.get(y + 1).text();
                        dupPrice = dupPrice.replaceAll("[^\\d]", "");
                        String dupCom = r.get(y + 2).text();
                        String s = "UPDATE dapanegoinfo SET 복수" + dupNo + "=" + dupPrice + ", 복참" + dupNo + "=" + dupCom + " " + where;
                        st.executeUpdate(s);
                    }
                }
            }
            String sql = "UPDATE dapanegoinfo SET 완료=1 " + where;
            st.executeUpdate(sql);
        }
    }

    public void run() {
        curItem = 0;
        try {
            if (op.equals("건수차이")) {
                manageDifference(sd, ed);
            }
            else {
                setOption("입찰공고");
                if (!shutdown) getList();
                setOption("입찰결과");
                if (!shutdown) getList();
                setOption("협상공고");
                if (!shutdown) getList();
                setOption("협상결과");
                if (!shutdown) getList();
            }
        } catch (IOException | SQLException e) {
            Logger.getGlobal().log(Level.WARNING, e.getMessage());
            e.printStackTrace();
        }
    }

    public int getTotal() throws IOException {
        String path = DapaParser.BID_RES_LIST;
        String param = "pageNo=1&startPageNo=1&pagePerRow=10&txtBidxDateFrom="+sd+"&txtBidxDateTo="+ed;

        openConnection(path);
        Document doc = Jsoup.parse(getResponse(param));
        if (doc.getElementsContainingOwnText("전체 건수 :").size() != 0) {
            totalItems = Integer.parseInt(doc.getElementsContainingOwnText("전체 건수 :").text().split(" ")[3]);
        }
        else totalItems = 0;

        path = DapaParser.NEGO_RES_LIST;
        param = "pageNo=1&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+sd+"&txtNegoDateTo="+ed;

        openConnection(path);
        doc = Jsoup.parse(getResponse(param));
        if (doc.getElementsContainingOwnText("전체 건수 :").size() != 0) {
            totalItems += Integer.parseInt(doc.getElementsContainingOwnText("전체 건수 :").text().split(" ")[3]);
        }
        else totalItems += 0;

        return totalItems;
    }

    public void setDate(String sd, String ed) {
        sd = sd.replaceAll("-", "%2F");
        ed = ed.replaceAll("-", "%2F");

        this.sd = sd;
        this.ed = ed;
    }

    public void setOption(String op) {
        this.op = op;
    }

    public int getCur() {
        return curItem;
    }

    public void manageDifference(String sm, String em) throws SQLException, IOException {
        manageDifference(sm, em, "입찰결과");
        manageDifference(sm, em, "협상결과");
    }

    public void manageDifference(String sm, String em, String type) throws SQLException, IOException {
        ArrayList<String> bidNums = new ArrayList<String>();
        ArrayList<String> bidVers = new ArrayList<String>();
        ArrayList<String> facilNums = new ArrayList<String>();
        ArrayList<String> orgs = new ArrayList<String>();

        String path = "";
        String param = "";

        String urlsm = sm.replaceAll("-", "%2F");
        String urlem = em.replaceAll("-", "%2F");

        String sqlsm = sm.replaceAll("%2F", "-");
        String sqlem = em.replaceAll("%2F", "-");

        if (type.equals("입찰결과")) {
            String sql = "SELECT 공고번호, 차수, 공사번호, 발주기관 FROM dapabidinfo WHERE 개찰일시 BETWEEN \"" + sqlsm + " 00:00:00\" AND \"" + sqlem + " 23:59:59\" AND 완료=1;";
            System.out.println(sql);
            rs = st.executeQuery(sql);
            while (rs.next()) {
                bidNums.add(rs.getString("공고번호"));
                bidVers.add(rs.getString("차수"));
                facilNums.add(rs.getString("공사번호"));
                orgs.add(rs.getString("발주기관"));
            }
            path = DapaParser.BID_RES_LIST;
            param = "pageNo=1&startPageNo=1&pagePerRow=10&txtBidxDateFrom="+urlsm+"&txtBidxDateTo="+urlem;
        }
        else if (type.equals("협상결과")) {
            rs = st.executeQuery("SELECT 공사번호, 차수, 발주기관 FROM dapanegoinfo WHERE 개찰일시 BETWEEN \"" + sqlsm + " 00:00:00\" AND \"" + sqlem + " 23:59:59\" AND 완료=1;");
            while (rs.next()) {
                bidVers.add(rs.getString("차수"));
                facilNums.add(rs.getString("공사번호"));
                orgs.add(rs.getString("발주기관"));
            }

            path = DapaParser.NEGO_RES_LIST;
            param = "pageNo=1&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+urlsm+"&txtNegoDateTo="+urlem;
        }

        openConnection(path);
        Document doc = Jsoup.parse(getResponse(param));
        Element table = doc.getElementsByTag("table").get(0);
        Elements rows = table.getElementsByTag("tr");
        if (doc.getElementsContainingOwnText("전체 건수 :").size() != 0) {
            totalItems = Integer.parseInt(doc.getElementsContainingOwnText("전체 건수 :").text().split(" ")[3]);
        }
        else totalItems = 0;

        int page = 1;
        int index = 1;
        for (int i = 0; i < totalItems; i++) {
            Element row = rows.get(index);
            Elements data = row.getElementsByTag("td");

            if (type.equals("입찰결과")) {
                String[] bidID = data.get(0).text().split(" ");
                String bidCode = bidID[1]; // 공고번호-차수
                String facilNum = data.get(1).text().replaceAll("\u00a0", ""); // 공사번호
                String org = data.get(3).text(); // 발주기관
                String[] codeSplit = bidCode.split("-");
                String bidno = codeSplit[0]; // 공고번호
                String bidver = codeSplit[1]; // 차수

                String key = bidno + bidver + facilNum + org;
                String dbKey = bidNums.get(0) + bidVers.get(0) + facilNums.get(0) + orgs.get(0);

                int j = 0;
                while ( (!key.equals(dbKey)) && ( (j + 1) < bidNums.size()) ) {
                    j++;
                    dbKey = bidNums.get(j) + bidVers.get(j) + facilNums.get(j) + orgs.get(j);
                }

                if (j < bidNums.size()) {
                    bidNums.remove(j);
                    bidVers.remove(j);
                    facilNums.remove(j);
                    orgs.remove(j);
                }
            }
            else if (type.equals("협상결과")) {
                String facilNum = data.get(1).text(); // 공사번호
                String negoVer = data.get(2).text(); // 차수
                String org = data.get(4).text(); // 발주기관

                String key = facilNum + negoVer + org;
                String dbKey = facilNums.get(0) + bidVers.get(0) + orgs.get(0);

                int j = 0;
                while ( (!key.equals(dbKey)) && ( (j + 1) < facilNums.size() ) ) {
                    j++;
                    dbKey = facilNums.get(j);
                    dbKey += bidVers.get(j);
                    dbKey += orgs.get(j);
                }

                if (j < facilNums.size()) {
                    bidVers.remove(j);
                    facilNums.remove(j);
                    orgs.remove(j);
                }
            }

            if (i % 10 == 9 && i < (totalItems - 1)) {
                index = 1;
                page++;

                // Load next listing page.
                openConnection(path);
                if (type.equals("입찰결과")) {
                    param = "pageNo="+page+"&startPageNo=1&pagePerRow=10&txtBidxDateFrom="+urlsm+"&txtBidxDateTo="+urlem;
                }
                else if (type.equals("협상결과")) {
                    param = "pageNo="+page+"&startPageNo=1&pagePerRow=10&txtNegoDateFrom="+urlsm+"&txtNegoDateTo="+urlem;
                }
                doc = Jsoup.parse(getResponse(param));
                table = doc.getElementsByTag("table").get(0);
                rows = table.getElementsByTag("tr");
            }
            else {
                index++;
            }
        }

        for (int i = 0; i < facilNums.size(); i++) {
            if (type.equals("입찰결과")) {
                String sql = "DELETE FROM dapabidinfo WHERE 공고번호=\"" + bidNums.get(i) + "\" AND 차수=\"" + bidVers.get(i) + "\" AND "
                        + "공사번호=\"" + facilNums.get(i) + "\" AND 발주기관=\"" + orgs.get(i) + "\"";
                System.out.println(sql);
                st.executeUpdate(sql);
            }
            else {
                String sql = "DELETE FROM dapanegoinfo WHERE 공사번호=\"" + facilNums.get(i) + "\" AND "
                        + "차수=\"" + bidVers.get(i) + "\" AND 발주기관=\"" + orgs.get(i) + "\"";
                System.out.println(sql);
                st.executeUpdate(sql);
            }
        }
    }
}