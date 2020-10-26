package org.bidcrawler;

/**
 * Created by ravenjoo on 6/25/17.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bidcrawler.utils.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ExParser extends Parser {
    final static String BASE_PATH = "http://ebid.ex.co.kr/ebid/jsps/ebid/";
    final static String ANN_LIST = "/bidNoti/bidNotiCompanyList.jsp?";
    final static String RES_LIST = "/bidResult/bidResultList.jsp?";
    final static String ANN_INF = "/bidNoti/";
    final static String RES_INF = "/bidResult/";

    final static String CONST_PRICE = "http://ebid.ex.co.kr/ebid/jsps/ebid/const/bidResult/bidResultNego.jsp?";
    final static String SERV_PRICE = "http://ebid.ex.co.kr/ebid/jsps/ebid/serv/bidResult/bidResultNego.jsp?";
    final static String BUY_PRICE = "http://ebid.ex.co.kr/ebid/jsps/ebid/buy/bidResult/bidResultNego.jsp?";

    // For SQL setup.
    Connection db_con;
    java.sql.Statement st;
    ResultSet rs;

    URL url;
    HttpURLConnection con;
    String sd;
    String ed;
    String op;
    String wt;
    String it;
    int totalItems;
    int curItem;

    GetFrame frame;
    CheckFrame checkFrame;

    // HttpClient suite
    private HttpClient client;

    public ExParser(String sd, String ed, String op, GetFrame frame, CheckFrame checkFrame) throws ClassNotFoundException, SQLException {
        sd = sd.replaceAll("-", "");
        ed = ed.replaceAll("-", "");

        this.sd = sd;
        this.ed = ed;
        if (op.length() == 4) {
            this.op = op;
            this.wt = op.substring(0, 2);
            this.it = op.substring(2, 4);
        }
        totalItems = 0;
        curItem = 0;

        this.frame = frame;
        this.checkFrame = checkFrame;

        // Set up SQL connection.
        db_con = DriverManager.getConnection(
                "jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8",
                Util.DB_ID,
                Util.DB_PW
        );
        st = db_con.createStatement();
        rs = null;

        this.client = HttpClientBuilder.create().build();
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
        ExParser tester = new ExParser("2017-01-01", "2017-02-11", "공사결과", null, null);

        tester.run();
    }

    private String sendGetRequest(String path) throws IOException {
        HttpGet request = new HttpGet(path);

        // add request header
        request.addHeader("User-Agent", "Mozilla/5.0");

        HttpResponse response = client.execute(request);

        System.out.println("\nSending 'GET' request to URL : " + path);
        System.out.println("Response Code : " +
                response.getStatusLine().getStatusCode());

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent(), "euc-kr"));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        return result.toString();
    }

    public String getPage() throws IOException {
        String path = ExParser.BASE_PATH;
        if (wt.equals("공사")) path += "const";
        else if (wt.equals("용역")) path += "serv";
        else if (wt.equals("물품")) path += "buy";

        if (it.equals("공고")) path += ExParser.ANN_LIST;
        else if (it.equals("결과")) path += ExParser.RES_LIST;

        if (wt.equals("물품")) {
            path += "startDate=" + sd;
            path += "&endDate=" + ed;
        }
        else {
            path += "s_noti_date=" + sd;
            path += "&e_noti_date=" + ed;
        }

        return path;
    }

    public void getList() throws IOException, SQLException {
        String path = getPage();

        Document doc = Jsoup.parse(sendGetRequest(path));
        totalItems = Integer.parseInt(doc.getElementsByClass("totalCount_001").first().text().split("건")[0].replaceAll("[^\\d]", ""));
        Element listing = doc.getElementsByTag("table").get(0);
        Elements rows = listing.getElementsByTag("tr");

        int page = 1;
        int index = 1;
        int startnum = 1;
        int endnum = 10;
        for (int i = 0; i < totalItems; i++) {
            if (shutdown) {
                return;
            }

            if (it.equals("결과")) curItem++;
            Element row = rows.get(index);
            parseListRow(row);

            if (i % 10 == 9 && i < (totalItems - 1)) {
                index = 1;
                page++;
                startnum += 10;
                endnum += 10;
                String nextpage = path + "&page=" + page;
                nextpage += "&startnum=" + startnum;
                nextpage += "&endnum=" + endnum;

                doc = Jsoup.parse(sendGetRequest(nextpage));
                listing = doc.getElementsByTag("table").get(0);
                rows = listing.getElementsByTag("tr");
            }
            else {
                index++;
            }
        }
    }

    public void parseListRow(Element row) throws SQLException, IOException {
        boolean enter = true;
        boolean exists = false;
        String where = "";
        Elements data = row.getElementsByTag("td");
        if (it.equals("공고")) {
            String bidno = data.get(1).text(); // 공고번호
            String area = data.get(2).text(); // 지역
            String compType = data.get(6).text(); // 계약방법
            String prog = data.get(8).text(); // 공고상태
            where = "WHERE 공고번호=\"" + bidno + "\" AND 중복번호=\"1\"";

            if (frame != null) frame.updateInfo(bidno, false);

            String sql = "SELECT EXISTS(SELECT 공고번호 FROM exbidinfo " + where + ")";
            rs = st.executeQuery(sql);
            if (rs.first()) exists = rs.getBoolean(1);

            if (exists) {
                if (!prog.equals("정정공고중")) {
                    // Check the bid version and update level from the DB.
                    sql = "SELECT 공고 FROM exbidinfo " + where;
                    rs = st.executeQuery(sql);
                    int finished = 0;
                    if (rs.first()) {
                        finished = rs.getInt(1);
                    }
                    if (finished > 0) enter = false;
                }
            }
            else {
                sql = "INSERT INTO exbidinfo (공고번호, 분류, 지역, 계약방법, 공고상태, 중복번호) VALUES (" +
                        "\""+bidno+"\", \"" + wt + "\", \"" + area + "\", \"" + compType + "\", \"" + prog + "\", \"1\");";
                st.executeUpdate(sql);
            }
        }
        else if (it.equals("결과")) {
            String bidno = data.get(1).text(); // 공고번호
            String area = data.get(2).text(); // 지역
            String compType = data.get(5).text(); // 계약방법
            String openDate = data.get(6).text(); // 개찰일시
            String prog = data.get(7).text(); // 결과
            where = "WHERE 공고번호=\"" + bidno + "\" AND 중복번호=\"1\"";

            if (frame != null) frame.updateInfo(bidno, true);
            if (checkFrame != null) {
                checkFrame.updateProgress(threadIndex);
            }

            // Check if the 공고번호 already exists in the DB.
            rs = st.executeQuery("SELECT EXISTS(SELECT 공고번호 FROM exbidinfo " + where + ")");
            if (rs.first()) exists = rs.getBoolean(1);

            if (exists) {
                System.out.println(bidno + " exists.");
                // Check the bid version and update level from the DB.
                rs = st.executeQuery("SELECT 완료, 결과상태, 개찰일시 FROM exbidinfo " + where);
                int finished = 0;
                String dbResult = "";
                String dbDate = "";
                if (rs.first()) {
                    finished = rs.getInt(1);
                    dbResult = rs.getString(2) == null ? "" : rs.getString(2);
                    dbDate = rs.getString(3);
                }
                if (finished > 0) {
                    if (!dbResult.equals(prog) || !openDate.equals(dbDate.substring(0, 10))) {
                        dbDate = openDate;
                        String sql = "UPDATE exbidinfo SET 결과상태=\"" + prog + "\", 개찰일시=\"" + dbDate + "\" " + where;
                        System.out.println(dbDate);
                        st.executeUpdate(sql);
                    }
                    enter = false;
                }
            }
            else {
                String sql = "INSERT INTO exbidinfo (공고번호, 분류, 지역, 계약방법, 개찰일시, 결과상태, 중복번호) VALUES (" +
                        "\""+bidno+"\", \"" + wt + "\", \"" + area + "\", \"" + compType + "\", \"" + openDate + "\", \"" + prog + "\", \"1\");";
                st.executeUpdate(sql);
            }
        }

        if (enter) {
            Element link = row.getElementsByTag("td").get(3).getElementsByTag("a").first();
            String itempath = ExParser.BASE_PATH;
            if (wt.equals("공사")) itempath += "const";
            else if (wt.equals("용역")) itempath += "serv";
            else if (wt.equals("물품")) itempath += "buy";

            if (it.equals("공고")) {
                itempath += ExParser.ANN_INF;
                itempath += link.attr("href");
            }
            else if (it.equals("결과")) {
                itempath += ExParser.RES_INF;
                itempath += link.attr("href").substring(2);
            }

            Document itemdoc = Jsoup.parse(sendGetRequest(itempath));
            parseInfo(itemdoc, itempath, where, 1);
        }
    }

    public void parseInfo(Document doc, String itempath, String where, int dup) throws SQLException, IOException {
        if (it.equals("공고")) {
            String annDate = ""; // 공고일자
            String hasDup = ""; // 복수예가적용여부
            String hasRebid = ""; // 재입찰허용여부
            String elecBid = ""; // 전자입찰여부
            String hasCommon = ""; // 공동수급가능여부
            String fieldTour = ""; // 현장설명실시여부
            String mustCommon = ""; // 공동수급의무여부
            String openDate = ""; // 개찰일시
            String protoPrice = ""; // 설계금액
            String aPrice = "0"; // A값

            String workNum = null;
            String contNum = null;
            Elements workNumElements = doc.getElementsContainingOwnText("과업관련문의");
            if (workNumElements.size() > 0) {
                String numText = workNumElements.get(0).nextElementSibling().text();
                workNum = numText.substring(numText.indexOf('(') + 1, numText.indexOf(')'));
            }

            Elements contNumElements = doc.getElementsContainingOwnText("계약관련문의");
            if (contNumElements.size() > 0) {
                String numText = contNumElements.get(0).nextElementSibling().text();
                contNum = numText.substring(numText.indexOf('(') + 1, numText.indexOf(')'));
            }

            Elements tables = doc.getElementsByTag("caption");
            for (int j = 0; j < tables.size(); j++) {
                String caption = tables.get(j).text();
                String priceCaption = "복수예비가격리스트로 15개의 가격리스트로 구성되어 있습니다.";
                String dupCaption = "해당 표는 공사 발주 내역에 대한 문서명, 파일 정보 표입니다.";

                if (caption.equals("복수예비가격") || caption.equals(priceCaption)) {
                    Element dupTable = tables.get(j).parent();
                    Elements dupData = dupTable.getElementsByTag("td");

                    for (int k = 0; k < dupData.size(); k++) {
                        String dupPrice = dupData.get(k).text();
                        dupPrice = dupPrice.replaceAll(",", "");
                        st.executeUpdate("UPDATE exbidinfo SET 복수"+(k+1)+"="+dupPrice+" " + where);
                    }
                }
                else if (caption.equals("발주 내역") || caption.equals(dupCaption)) {
                    Element detailTable = tables.get(j).parent();
                    Elements rows = detailTable.getElementsByTag("tr");

                    if (rows.size() > 2) {
                        int k = dup + 1;
                        if (rows.size() > k) {
                            boolean exists = false;
                            boolean enter = true;

                            String sql = "SELECT 공고번호, 지역, 계약방법, 공고상태 FROM exbidinfo " + where;
                            rs = st.executeQuery(sql);
                            String bidno = "";
                            String area = "";
                            String compType = "";
                            String prog = "";

                            if (rs.next()) {
                                bidno = rs.getString("공고번호");
                                area = rs.getString("지역");
                                compType = rs.getString("계약방법");
                                prog = rs.getString("공고상태");
                            }

                            String dupwhere = "WHERE 공고번호=\"" + bidno + "\" AND 중복번호=\"" + k + "\"";
                            sql = "SELECT EXISTS(SELECT 공고번호 FROM exbidinfo " + dupwhere + ")";
                            rs = st.executeQuery(sql);
                            if (rs.first()) exists = rs.getBoolean(1);

                            if (exists) {
                                // Check the bid version and update level from the DB.
                                sql = "SELECT 공고 FROM exbidinfo " + dupwhere;
                                rs = st.executeQuery(sql);
                                int finished = 0;
                                if (rs.first()) {
                                    finished = rs.getInt(1);
                                }
                                if (finished > 0) enter = false;
                            }
                            else {
                                sql = "INSERT INTO exbidinfo (공고번호, 분류, 지역, 계약방법, 공고상태, 중복번호) VALUES (" +
                                        "\""+bidno+"\", \"" + wt + "\", \"" + area + "\", \"" + compType + "\", \"" + prog + "\", \"" + k + "\");";
                                st.executeUpdate(sql);
                            }

                            if (enter) {
                                String newpath = itempath.split("\\?")[0];
                                newpath += "?notino=" + bidno.replaceAll("-", "");
                                newpath += "&bidno=" + k;
                                newpath += "&bidseq=1";
                                Document itemdoc = Jsoup.parse(sendGetRequest(newpath));
                                parseInfo(itemdoc, newpath, dupwhere, k);
                            }
                        }
                    }
                }
            }

            Elements headers = doc.getElementsByTag("th");
            for (Element h : headers) {
                String key = h.text().replaceAll(" ", "");
                if (key.equals("공고일자")) {
                    annDate = h.nextElementSibling().text();
                    annDate += " 00:00:00";
                }
                else if (key.equals("복수예가적용여부")) {
                    hasDup = h.nextElementSibling().text();
                }
                else if (key.equals("재입찰허용여부")) {
                    hasRebid = h.nextElementSibling().text();
                }
                else if (key.equals("전자입찰여부")) {
                    elecBid = h.nextElementSibling().text();
                }
                else if (key.equals("공동수급가능여부")) {
                    hasCommon = h.nextElementSibling().text();
                }
                else if (key.equals("현장설명실시여부")) {
                    fieldTour = h.nextElementSibling().text();
                }
                else if (key.equals("공동수급의무여부")) {
                    mustCommon = h.nextElementSibling().text();
                }
                else if (key.equals("설계금액")) {
                    protoPrice = h.nextElementSibling().text();
                    protoPrice = protoPrice.replaceAll("[^\\d.]", "");
                    if (protoPrice.equals("")) protoPrice = "0";
                }
                else if (key.equals("개찰일시")) {
                    openDate = h.nextElementSibling().text() + ":00";
                }
                else if (key.equals("A값")) {
                    aPrice = h.nextElementSibling().text();
                    aPrice = aPrice.replaceAll("[^\\d.]", "");
                    if (aPrice.equals("")) aPrice = "0";
                }
            }

            String sql = "UPDATE exbidinfo SET 공고=1, " +
                    "공고일자=\"" + annDate + "\", " +
                    "복수예가여부=\"" + hasDup + "\", " +
                    "재입찰허용여부=\"" + hasRebid + "\", " +
                    "전자입찰여부=\"" + elecBid + "\", " +
                    "공동수급가능여부=\"" + hasCommon + "\", " +
                    "현장설명실시여부=\"" + fieldTour + "\", " +
                    "공동수급의무여부=\"" + mustCommon + "\", " +
                    "설계금액=" + protoPrice + ", " +
                    "A값=" + aPrice + ", ";
            if (workNum != null) {
                sql += "과업관련문의=\"" + workNum + "\", ";
            }
            if (contNum != null) {
                sql += "계약관련문의=\"" + contNum + "\", ";
            }

            sql += "개찰일시=\"" + openDate + "\" " + where;
            System.out.println(sql);
            st.executeUpdate(sql);
        }
        else if (it.equals("결과")) {
            Elements headers = doc.getElementsByTag("th");
            String annDate = ""; // 공고일자
            String expPrice = "0"; // 예정가격
            String protoPrice = "0"; // 설계금액
            String bidPrice = "0"; // 투찰금액
            String comp = "0"; // 참여수

            Elements captions = doc.getElementsByTag("caption");
            for (Element c : captions) {
                String dupCaption = "해당 표는 공사 발주 내역에 대한 문서명, 파일 정보 표입니다.";

                if (c.text().equals("입찰업체")) {
                    Element resTable = c.parent();
                    Elements resRows = resTable.getElementsByTag("tr");
                    if (resRows.size() > 1) {
                        if (resRows.get(1).text().length() > 13) {
                            String price = resRows.get(1).getElementsByTag("td").get(3).text();
                            bidPrice = price.replaceAll("[^\\d.]", "");
                            System.out.println(bidPrice);
                        }
                    }
                }

                else if (c.text().equals("발주 내역") || c.text().equals(dupCaption)) {
                    Element detailTable = c.parent();
                    Elements rows = detailTable.getElementsByTag("tr");

                    if (rows.size() > 2) {
                        int k = dup + 1;
                        if (rows.size() > k) {
                            boolean exists = false;
                            boolean enter = true;
                            String sql = "SELECT 공고번호, 지역, 계약방법, 개찰일시, 결과상태 FROM exbidinfo " + where;
                            rs = st.executeQuery(sql);
                            String bidno = "";
                            String area = "";
                            String compType = "";
                            String openDate = "";
                            String prog = "";

                            if (rs.next()) {
                                bidno = rs.getString("공고번호");
                                area = rs.getString("지역");
                                compType = rs.getString("계약방법");
                                openDate = rs.getString("개찰일시");
                                prog = rs.getString("결과상태");
                            }

                            String dupwhere = "WHERE 공고번호=\"" + bidno + "\" AND 중복번호=\"" + k + "\"";
                            // Check if the 공고번호 already exists in the DB.
                            rs = st.executeQuery("SELECT EXISTS(SELECT 공고번호 FROM exbidinfo " + dupwhere + ")");
                            if (rs.first()) exists = rs.getBoolean(1);

                            if (exists) {
                                System.out.println(bidno + " exists.");
                                // Check the bid version and update level from the DB.
                                rs = st.executeQuery("SELECT 완료, 결과상태 FROM exbidinfo " + dupwhere);
                                int finished = 0;
                                String dbResult = "";
                                if (rs.first()) {
                                    finished = rs.getInt(1);
                                    dbResult = rs.getString(2) == null ? "" : rs.getString(2);
                                }
                                if (finished > 0) {
                                    if (!dbResult.equals(prog)) {
                                        sql = "UPDATE exbidinfo SET 결과상태=\"" + prog + "\" " + where;
                                        st.executeUpdate(sql);
                                    }
                                    enter = false;
                                }
                            }
                            else {
                                sql = "INSERT INTO exbidinfo (공고번호, 분류, 지역, 계약방법, 개찰일시, 결과상태, 중복번호) VALUES (" +
                                        "\""+bidno+"\", \"" + wt + "\", \"" + area + "\", \"" + compType + "\", \"" + openDate + "\", \"" + prog + "\", \"" + k + "\");";
                                st.executeUpdate(sql);
                            }

                            if (enter) {
                                String newpath = itempath.split("\\?")[0];
                                newpath += "?notino=" + bidno.replaceAll("-", "");
                                newpath += "&bidno=" + k;
                                newpath += "&bidseq=1";
                                Document itemdoc = Jsoup.parse(sendGetRequest(newpath));
                                parseInfo(itemdoc, newpath, dupwhere, k);
                            }
                        }
                    }
                }
            }

            for (Element h : headers) {
                String key = h.text();

                if (key.equals("공고일자")) {
                    annDate = h.nextElementSibling().text() + " 00:00:00";
                }
                else if (key.equals("예정가격")) {
                    expPrice = h.nextElementSibling().text();
                    expPrice = expPrice.replaceAll("[^\\d.]", "");
                    if (expPrice.equals("")) expPrice = "0";
                }
                else if (key.equals("설계가격") || key.equals("설계금액")) {
                    protoPrice = h.nextElementSibling().text();
                    protoPrice = protoPrice.replaceAll("[^\\d.]", "");
                    if (protoPrice.equals("")) protoPrice = "0";
                }
            }

            Element buttonDiv = null;
            if (wt.equals("공사") || wt.equals("물품")) buttonDiv = doc.getElementsByAttributeValue("class", "center_btn_area").first();
            else if (wt.equals("용역")) buttonDiv = doc.getElementsByAttributeValue("class", "btn_area").first();

            if (buttonDiv == null) {
                System.out.println(doc.html());
            }

            if (
                buttonDiv.getElementsContainingText("입찰실시결과").size() > 0 ||
                buttonDiv.getElementsContainingOwnText("입찰결과").size() > 0
            ) {
                String pricepath = "";
                if (wt.equals("공사")) pricepath = ExParser.CONST_PRICE;
                else if (wt.equals("용역")) pricepath = ExParser.SERV_PRICE;
                else if (wt.equals("물품")) pricepath = ExParser.BUY_PRICE;

                pricepath += itempath.split("\\?")[1];
                Document pricePage = Jsoup.parse(sendGetRequest(pricepath));
                Element dateHeader = pricePage.getElementsContainingOwnText("입찰일시").first();
                if (dateHeader == null) {
                    dateHeader = pricePage.getElementsContainingOwnText("개찰일시").first();
                }

                if (dateHeader != null) {
                    String openDate = dateHeader.nextElementSibling().text().trim();
                    String sql = "UPDATE exbidinfo SET 개찰일시=\"" + openDate + "\" " + where;
                    System.out.println(sql);
                    st.executeUpdate(sql);

                    Element priceTable = pricePage.getElementsByAttributeValue("class", "print_table").first();
                    if (priceTable != null) {
                        Element priceData = priceTable.getElementsByTag("td").get(3);
                        for (String s : priceData.text().split(" ")) {
                            if (s.contains(")")) {
                                String ind = s.split("\\)")[0];
                                String price = s.split("\\)")[1];

                                ind = ind.replaceAll("[^\\d]", "");
                                price = price.replaceAll("[^\\d.]", "");
                                sql = "UPDATE exbidinfo SET 복수"+ind+"="+price+" " + where;
                                st.executeUpdate(sql);
                            }
                        }
                    }
                }
            }

            if (buttonDiv.getElementsContainingText("투찰업체현황").size() > 0) {
                itempath = itempath.replace("bidResultDetail", "bidResult");
                itempath = itempath.replace("notino", "p_notino");
                itempath = itempath.replace("bidno", "p_bidno");
                itempath = itempath.replace("bidseq", "p_bidseq");
                itempath = itempath.replace("state", "p_state");

                Document compPage = Jsoup.parse(sendGetRequest(itempath));
                comp = compPage.getElementsByClass("totalCount_001").first().text();
                comp = comp.replaceAll("[^\\d]", "");
                if (comp.equals("")) comp = "0";
            }

            String sql = "UPDATE exbidinfo SET 완료=1, " +
                    "공고일자=\"" + annDate + "\", ";

            if (!expPrice.equals("0")) {
                sql += "예정가격=" + expPrice + ", ";
            }
            if (!protoPrice.equals("0")) {
                sql += "설계금액=" + protoPrice + ", ";
            }
            if (!bidPrice.equals("0")) {
                sql += "투찰금액=" + bidPrice + ", ";
            }

            sql += "참가수=" + comp + " " + where;
            st.executeUpdate(sql);
        }
    }

    public void run() {
        try {
            if (op.equals("건수차이")) {
                if (!shutdown) manageDifference(sd, ed);
            }
            else {
                curItem = 0;
                setOption("공사공고");
                if (!shutdown) getList();
                setOption("공사결과");
                if (!shutdown) getList();
                setOption("용역공고");
                if (!shutdown) getList();
                setOption("용역결과");
                if (!shutdown) getList();
                setOption("물품공고");
                if (!shutdown) getList();
                setOption("물품결과");
                if (!shutdown) getList();
            }

            if (frame != null && !shutdown) {
                frame.toggleButton();
            }
            if (checkFrame != null && !shutdown) {
                checkFrame.signalFinish();
            }
        } catch (Exception e) {
            Logger.getGlobal().log(Level.WARNING, e.getMessage());
            if (frame != null && !shutdown) {
                frame.toggleButton();
            }
            if (checkFrame != null && !shutdown) {
                checkFrame.signalFinish();
            }

            e.printStackTrace();
        }
    }

    public int getTotal() throws IOException {
        totalItems = 0;
        String[] bidTypes = { "공사결과", "용역결과", "물품결과" };

        for (String bidtype : bidTypes) {
            setOption(bidtype);
            String path = getPage();
            Document doc = Jsoup.parse(sendGetRequest(path));
            totalItems += Integer.parseInt(doc.getElementsByClass("totalCount_001").first().text().split("건")[0].replaceAll("[^\\d]", ""));
        }

        return totalItems;
    }

    public void setDate(String sd, String ed) {
        sd = sd.replaceAll("-", "");
        ed = ed.replaceAll("-", "");

        this.sd = sd;
        this.ed = ed;
    }

    public void setOption(String op) {
        this.op = op;
        this.wt = op.substring(0, 2);
        this.it = op.substring(2, 4);
    }

    public int getCur() {
        return curItem;
    }

    public void manageDifference(String sm, String em) throws SQLException, IOException {
        manageDifference(sm, em, "공사결과");
        manageDifference(sm, em, "용역결과");
        manageDifference(sm, em, "물품결과");
    }

    public void manageDifference(String sm, String em, String type) throws SQLException, IOException {
        setOption(type);

        String sqlsm = sm.substring(0, 4) + "-" + sm.substring(4, 6) + "-" + sm.substring(6);
        String sqlem = em.substring(0, 4) + "-" + em.substring(4, 6) + "-" + em.substring(6);
        String sql = "SELECT DISTINCT 공고번호 FROM exbidinfo WHERE 개찰일시 BETWEEN \"" + sqlsm + " 00:00:00\" AND \"" + sqlem + " 23:59:59\" AND 분류=\"" + wt + "\" AND 중복번호=1 AND 완료=1;";
        System.out.println(sql);
        rs = st.executeQuery(sql);

        ArrayList<String> bidNums = new ArrayList<String>();

        while (rs.next()) {
            bidNums.add(rs.getString("공고번호"));
        }

        String path = getPage();

        Document doc = Jsoup.parse(sendGetRequest(path));
        totalItems = Integer.parseInt(doc.getElementsByClass("totalCount_001").first().text().split("건")[0].replaceAll("[^\\d]", ""));
        System.out.println("전체 건 : " + totalItems);
        Element listing = doc.getElementsByTag("table").get(0);
        Elements rows = listing.getElementsByTag("tr");

        int page = 1;
        int index = 1;
        int startnum = 1;
        int endnum = 10;
        for (int i = 0; i < totalItems; i++) {
            if (shutdown) {
                return;
            }

            Element row = rows.get(index);
            Elements data = row.getElementsByTag("td");
            String bidno = data.get(1).text(); // 공고번호

            for (int j = 0; j < bidNums.size(); j++) {
                if (bidno.equals(bidNums.get(j))) {
                    bidNums.remove(j);
                    break;
                }
            }

            if (i % 10 == 9 && i < (totalItems - 1)) {
                index = 1;
                page++;
                startnum += 10;
                endnum += 10;
                String nextpage = path + "&page=" + page;
                nextpage += "&startnum=" + startnum;
                nextpage += "&endnum=" + endnum;

                doc = Jsoup.parse(sendGetRequest(nextpage));
                listing = doc.getElementsByTag("table").get(0);
                rows = listing.getElementsByTag("tr");
            }
            else {
                index++;
            }
        }

        for (int i = 0; i < bidNums.size(); i++) {
            sql = "DELETE FROM exbidinfo WHERE 공고번호=\"" + bidNums.get(i) + "\"";
            st.executeUpdate(sql);
            System.out.println(sql);
        }
    }
}

