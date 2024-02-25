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
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bidcrawler.utils.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.apache.http.client.utils.URIBuilder;

public class LetsParser extends Parser {

    // For SQL setup.
    Connection db_con;
    java.sql.Statement st;
    ResultSet rs;

    final static String ANN_LIST = "https://ebid.kra.co.kr/bid/notice/all/list.do";
    final static String RES_LIST = "https://ebid.kra.co.kr/res/all/list.do";
    final static String ANN_INFO = "https://ebid.kra.co.kr/bid/notice/all/view.do";
    final static String RES_INFO = "https://ebid.kra.co.kr/res/all/view.do";

    URL url;
    HttpURLConnection con;
    HashMap<String, String> formData;
    String sd;
    String ed;
    String op;
    int totalItems;
    int curItem;

    GetFrame frame;
    CheckFrame checkFrame;

    public LetsParser(String sd, String ed, String op, GetFrame frame, CheckFrame checkFrame) throws SQLException, ClassNotFoundException {
        this.sd = sd;
        this.ed = ed;
        this.op = op;
        this.frame = frame;
        this.checkFrame = checkFrame;

        totalItems = 0;
        curItem = 0;

        formData = new HashMap<>();
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
        LetsParser tester = new LetsParser("2016-07-01", "2016-08-16", "공고", null, null);

        tester.getList();
    }

    public void run() {
        try {
            if (op.equals("건수차이")) {
                manageDifference(sd, ed);
            } else {
                curItem = 0;
                setOption("공고");
                if (!shutdown) getList();
                setOption("결과");
                if (!shutdown) getList();
            }

            if (frame != null && !shutdown) {
                frame.toggleButton();
            }
            if (checkFrame != null && !shutdown) {
                checkFrame.signalFinish();
            }
        } catch (IOException | SQLException e) {
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

    public void openConnection(String path, String method) throws IOException {
        url = new URL(path);
        con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod(method);
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4");
    }

    public String getResponse(String param, String method) throws IOException {
        if (method.equals("POST")) {
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(param);
            wr.flush();
            wr.close();
        }

        int responseCode = con.getResponseCode();
        System.out.println("\nSending " + method + " request to URL : " + url);
        System.out.println("Post parameters : " + param);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), Charset.forName("euc-kr")));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public Document getListPage(int page) throws IOException {
        if (op.equals("공고")) {
            return getAnnListPage(page);
        } else if (op.equals("결과")) {
            return getResListPage(page);
        } else {
            System.out.println("Please declare which list to fetch");
            return null;
        }
    }

    private Document getAnnListPage(int page) throws IOException {
        String path = LetsParser.ANN_LIST;
        openConnection(path, "POST");
        URIBuilder builder = new URIBuilder();
        builder.addParameter("pageIndex", page + "");
        builder.addParameter("openDateFrom", sd);
        builder.addParameter("openDateTo", ed);
        String param = builder.toString();
        return Jsoup.parse(getResponse(param, "POST"));
    }

    private Document getResListPage(int page) throws IOException {
        String path = LetsParser.RES_LIST;
        URIBuilder builder = new URIBuilder();
        builder.addParameter("page", page + "");
        builder.addParameter("is_from_main", "true");
        builder.addParameter("open_date_from", sd);
        builder.addParameter("open_date_to", ed);
        String param = builder.toString();
        openConnection(path + param, "GET");
        return Jsoup.parse(getResponse(null, "GET"));
    }

    public void getList() throws IOException, SQLException {
        if (db_con == null) {
            // Set up SQL connection.
            db_con = DriverManager.getConnection(
                    Util.DB_URL,
                    Util.DB_ID,
                    Util.DB_PW
            );
            st = db_con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = null;
        }

        int page = 1;
        Document doc = getListPage(page);
        Elements rows = doc.getElementsByTag("table").get(1).getElementsByTag("tr");
        int items;

        do {
            if (rows.get(1).text().length() > 24) {
                for (int i = 1; i < rows.size(); i++) {
                    if (shutdown) {
                        return;
                    }

                    Element row = rows.get(i);
                    boolean enter = parseListRow(row);

                    if (enter) {
                        String bn = row.getElementsByTag("td").get(1).text();
                        String br = row.getElementsByTag("td").get(6).text();
                        getInfo(bn, br);
                    }
                }
                page++;
                boolean nextPage = false;
                Element pagingdiv = doc.getElementsByAttributeValue("class", "bid_paging").first();
                Elements pages = pagingdiv.getElementsByTag("a");
                String pagestr = page + "";

                for (int i = 0; i < pages.size(); i++) {
                    String text = pages.get(i).text();
                    System.out.println(text);
                    if (text.equals(pagestr) || text.equals("다음 목록으로 이동")) {
                        nextPage = true;
                        break;
                    }
                }
                if (nextPage) {
                    doc = getListPage(page);
                    rows = doc.getElementsByTag("table").get(1).getElementsByTag("tr");
                    items = rows.size();
                }
                else {
                    items = 0;
                }
            }
            else {
                items = 0;
            }
        } while(items > 0);
    }

    public HashMap<String, String> parseAnnListRow(Element row) {
        HashMap<String, String> listItems = new HashMap<>();
        Elements data = row.getElementsByTag("td");
        listItems.put("bidno", data.get(1).text());
        listItems.put("place", data.get(2).text());
        listItems.put("compType", data.get(4).text());
        listItems.put("deadline", data.get(6).text() + " 00:00:00");
        listItems.put("bidMethod", data.get(7).text());
        listItems.put("prog", data.get(8).text());
        return listItems;
    }

    public HashMap<String, String> parseResListRow(Element row) {
        HashMap<String, String> listItems = new HashMap<>();
        Elements data = row.getElementsByTag("td");
        listItems.put("bidno", data.get(1).text());
        listItems.put("workType", data.get(3).text());
        listItems.put("compType", data.get(4).text());
        listItems.put("openDate", data.get(5).text());
        listItems.put("result", data.get(6).text());
        return listItems;
    }

    public boolean parseListRow(Element row) throws SQLException {
        boolean enter = true;
        if (op.equals("공고")) {
            HashMap<String, String> listItems = parseAnnListRow(row);
            String bidno = listItems.get("bidno"); // 공고번호
            String place = listItems.get("place"); // 사업장
            String compType = listItems.get("compType"); // 계약방식
            String deadline = listItems.get("deadline"); // 입찰마감
            String bidMethod = listItems.get("bidMethod"); // 입찰방식
            String prog = listItems.get("prog"); // 상태

            if (frame != null) frame.updateInfo(bidno, false);

            boolean exists = false;

            String where = "WHERE 공고번호=\""+bidno+"\"";

            String sql = "SELECT EXISTS(SELECT 공고번호 FROM letsrunbidinfo "+where+")";
            rs = st.executeQuery(sql);
            if (rs.first()) exists = rs.getBoolean(1);

            if (exists) {
                sql = "SELECT 공고, 공고상태 FROM letsrunbidinfo " + where;
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
                        sql = "UPDATE letsrunbidinfo SET 공고상태=\""+prog+"\" "+where;
                        st.executeUpdate(sql);
                    }
                }
                else if (!dbProg.equals(prog)) {
                    sql = "UPDATE letsrunbidinfo SET 공고상태=\""+prog+"\" "+where;
                    st.executeUpdate(sql);
                }
            }
            else {
                sql = "INSERT INTO letsrunbidinfo (공고번호, 사업장, 계약방법, 입찰마감, 입찰방식, 공고상태) VALUES (" +
                        "\""+bidno+"\", " +
                        "\""+place+"\", " +
                        "\""+compType+"\", " +
                        "\""+deadline+"\", " +
                        "\""+bidMethod+"\", " +
                        "\""+prog+"\");";
                st.executeUpdate(sql);
            }
        }
        else if (op.equals("결과")) {
            HashMap<String, String> listItems = parseResListRow(row);
            String bidno = listItems.get("bidno"); // 공고번호
            String workType = listItems.get("workType"); // 입찰구분
            String compType = listItems.get("compType"); // 계약방법
            String openDate = listItems.get("openDate"); // 개찰일시
            String result = listItems.get("result"); // 개찰상태

            System.out.println(bidno);

            if (frame != null) frame.updateInfo(bidno, true);
            if (checkFrame != null) {
                checkFrame.updateProgress(threadIndex);
            }

            boolean exists = false;

            String where = "WHERE 공고번호=\""+bidno+"\"";

            String sql = "SELECT EXISTS(SELECT 공고번호 FROM letsrunbidinfo "+where+")";
            rs = st.executeQuery(sql);
            if (rs.first()) exists = rs.getBoolean(1);

            if (exists) {
                sql = "SELECT 완료, 개찰상태 FROM letsrunbidinfo " + where;
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
                        sql = "UPDATE letsrunbidinfo SET 개찰상태=\""+result+"\" "+where;
                        st.executeUpdate(sql);
                    }
                }
                else if (!dbResult.equals(result)) {
                    sql = "UPDATE letsrunbidinfo SET 개찰상태=\""+result+"\" "+where;
                    st.executeUpdate(sql);
                }
            }
            else {
                sql = "INSERT INTO letsrunbidinfo (공고번호, 입찰구분, 계약방법, 개찰일시, 개찰상태) VALUES (" +
                        "\""+bidno+"\", " +
                        "\""+workType+"\", " +
                        "\""+compType+"\", " +
                        "\""+openDate+"\", " +
                        "\""+result+"\");";
                st.executeUpdate(sql);
            }
        }

        return enter;
    }

    public Document getResultInfoPage(String bidno) throws IOException {
        String path = LetsParser.RES_INFO;

        openConnection(path, "POST");

        String param = "b_code="+bidno.substring(3)+"&b_type=1&is_from_main=true&page=1&open_date_from="+sd+"&open_date_to="+ed;
        Document doc = Jsoup.parse(getResponse(param, "POST"));
        return doc;
    }

    public Document getAnnInfoPage(String bidno) throws IOException {
        String path = LetsParser.ANN_INFO;
        path += "?bCode="+bidno.substring(3)+"&b_code="+bidno.substring(3);

        openConnection(path, "GET");
        Document doc = Jsoup.parse(getResponse(null, "GET"));
        return doc;
    }

    public void getInfo(String bidno, String result) throws IOException, SQLException {
        if (op.equals("결과")) {
            Document doc = getResultInfoPage(bidno);
            parseInfo(doc, bidno);

            if (!result.equals("유찰") && !result.equals("입찰취소")) {
                parseResult(bidno);
            }

            parseAnn(bidno);
            String sql = "UPDATE letsrunbidinfo SET 완료=1 WHERE 공고번호=\"" + bidno + "\";";
            st.executeUpdate(sql);
        }
        else if (op.equals("공고")) {
            Document doc = getAnnInfoPage(bidno);
            Elements captions = doc.getElementsByTag("caption");

            String selectMethod = ""; // 낙찰자결정방법
            String workType = ""; // 입찰구분
            String place = ""; // 사업장
            String bidMethod = ""; // 입찰방식
            String deadline = ""; // 입찰마감
            String openDate = ""; // 개찰일시
            String priceMethod = ""; // 예정가격방식
            String expPrice = "0"; // 예정금액
            String basePrice = "0"; // 예비가격기초금액

            for (int j = 0; j < captions.size(); j++) {
                if (captions.get(j).text().equals("입찰공고 계약전체 표")) {
                    Element infoTable = captions.get(j).parent();
                    Elements infos = infoTable.getElementsByTag("th"); // Headers for table of details
                    for (int k = 0; k < infos.size(); k++) {
                        String key = infos.get(k).text();
                        if (key.equals("사업장")) {
                            place = infos.get(k).nextElementSibling().text();
                        }
                        else if (key.equals("전자입찰여부")) {
                            bidMethod = infos.get(k).nextElementSibling().text();
                        }
                        else if (key.equals("입찰구분")) {
                            workType = infos.get(k).nextElementSibling().text();
                        }
                        else if (key.equals("낙찰자결정방법")) {
                            selectMethod = infos.get(k).nextElementSibling().text();
                        }
                    }
                }
                else if (captions.get(j).text().equals("입찰진행순서표")) {
                    Element timeTable = captions.get(j).parent();
                    Elements infos = timeTable.getElementsByTag("th"); // Headers for table of details
                    for (int k = 0; k < infos.size(); k++) {
                        String key = infos.get(k).text();
                        if (key.equals("입찰마감")) {
                            deadline = infos.get(k).nextElementSibling().text();
                        }
                        else if (key.equals("개찰일시")) {
                            openDate = infos.get(k).nextElementSibling().text();
                        }
                    }
                }
                else if (captions.get(j).text().equals("예정가격정보표")) {
                    Element priceTable = captions.get(j).parent();
                    Elements infos = priceTable.getElementsByTag("th"); // Headers for table of details
                    for (int k = 0; k < infos.size(); k++) {
                        String key = infos.get(k).text();
                        if (key.equals("예정가격방식")) {
                            priceMethod = infos.get(k).nextElementSibling().text();
                        }
                        else if (key.equals("예정금액")) {
                            expPrice = infos.get(k).nextElementSibling().text();
                            expPrice = expPrice.replaceAll("[^\\d]", "");
                            if (expPrice.equals("")) expPrice = "0";
                        }
                        else if (key.equals("예정금액")) {
                            basePrice = infos.get(k).nextElementSibling().text();
                            basePrice = basePrice.replaceAll("[^\\d]", "");
                            if (basePrice.equals("")) basePrice = "0";
                        }
                    }
                }
            }

            String sql = "UPDATE letsrunbidinfo SET " +
                    "사업장=\"" + place + "\", " +
                    "입찰구분=\"" + workType + "\", " +
                    "입찰방식=\"" + bidMethod + "\", " +
                    "낙찰자선정방법=\"" + selectMethod + "\", " +
                    "예정가격방식=\"" + priceMethod + "\", " +
                    "예정가격=" + expPrice + ", " +
                    "예비가격기초금액=" + basePrice + ", " +
                    "입찰마감=\"" + deadline + "\", " +
                    "공고=1, " +
                    "개찰일시=\"" + openDate + "\" WHERE 공고번호=\"" + bidno + "\";";
            st.executeUpdate(sql);
        }
    }

    public void parseInfo(Document doc, String bidno) throws SQLException {
        String where = "WHERE 공고번호=\"" + bidno + "\"";

        String selectMethod = ""; // 낙찰자선정방법
        String expPrice = "0"; // 예정가격
        String basePrice = "0"; // 예비가격기초금액
        String rate = ""; // 낙찰하한율
        String boundPrice = "0"; // 낙찰하한금액

        Element infoTable = null;
        Element expPriceTable = null;
        Element dupPriceTable = null;
        Element minPriceTable = null;
        Elements tableNames = doc.getElementsByTag("caption");
        for (int j = 0; j < tableNames.size(); j++) {
            String name = tableNames.get(j).text();
            if (name.equals("입찰개요")) {
                infoTable = tableNames.get(j).parent();
            }
            else if (name.equals("예정가격정보")) {
                expPriceTable = tableNames.get(j).parent();
            }
            else if (name.equals("복수 예비가격별 선택상황") || name.equals("복수 예비가격별 선택상황 표")) {
                dupPriceTable = tableNames.get(j).parent();
            }
            else if (name.equals("제한적 최저가") || name.equals("적격심사")) {
                minPriceTable = tableNames.get(j).parent();
            }
        }

        if (infoTable != null) {
            Elements infos = infoTable.getElementsByTag("th"); // Headers for table of details
            for (int j = 0; j < infos.size(); j++) {
                String key = infos.get(j).text();
                if (key.equals("낙찰자선정방법")) {
                    selectMethod = infos.get(j).nextElementSibling().text();
                }
            }
        }

        if (expPriceTable != null) {
            Elements expPrices = expPriceTable.getElementsByTag("th");
            for (int j = 0; j < expPrices.size(); j++) {
                String key = expPrices.get(j).text();
                if (key.equals("예정가격")) {
                    String value = expPrices.get(j).nextElementSibling().text();
                    expPrice = value.replaceAll("[^\\d]", "");
                    if (expPrice.equals("")) expPrice = "0";
                }
                else if (key.equals("예비가격기초금액")) {
                    String value = expPrices.get(j).nextElementSibling().text();
                    basePrice = value.replaceAll("[^\\d]", "");
                    if (basePrice.equals("")) basePrice = "0";
                }
            }
        }

        if (minPriceTable != null) {
            Elements minPrices = minPriceTable.getElementsByTag("th");
            for (int j = 0; j < minPrices.size(); j++) {
                String key = minPrices.get(j).text();
                String value = minPrices.get(j).nextElementSibling().text();
                if (key.equals("낙찰하한율")) {
                    rate = value;
                }
                else if (key.equals("낙찰하한금액")) {
                    boundPrice = value.replaceAll("[^\\d]", "");
                    if (boundPrice.equals("")) boundPrice = "0";
                }
            }
        }

        if (dupPriceTable != null) {
            Elements dupPriceRows = dupPriceTable.getElementsByTag("tr");
            int companies = 0;
            for (int x = 1; x <= 5; x++) {
                Elements r = dupPriceRows.get(x).children();
                for (int y = 0; y < 9; y += 3) {
                    String dupNo = r.get(y).text();
                    String dupPrice = r.get(y + 1).text();
                    dupPrice = dupPrice.replaceAll("[^\\d]", "");
                    String dupCom = r.get(y + 2).text().trim();
                    String s = "UPDATE letsrunbidinfo SET 복수" + dupNo + "=" + dupPrice + ", 복참" + dupNo + "=" + dupCom + " " + where;
                    st.executeUpdate(s);
                    companies += Integer.parseInt(dupCom);
                }
                System.out.println("dup prices fetched");
            }
            st.executeUpdate("UPDATE letsrunbidinfo SET 참여수=" + companies + " " + where);
        }

        String sql = "UPDATE letsrunbidinfo SET " +
                "낙찰자선정방법=\"" + selectMethod + "\", " +
                "예정가격=" + expPrice + ", " +
                "예비가격기초금액=" + basePrice + ", " +
                "낙찰하한금액=" + boundPrice + ", " +
                "낙찰하한율=\"" + rate + "\" " + where;
        st.executeUpdate(sql);
    }

    public void parseResult(String bidno) throws IOException, SQLException {
        String path = "https://ebid.kra.co.kr/res/result/bd_list_result_company_1.do?page=1&b_code="+bidno.substring(3)+"&select_method=11";

        openConnection(path, "GET");

        Document doc = Jsoup.parse(getResponse(null, "GET"));
        Element resultTable = null;
        Elements captions = doc.getElementsByTag("caption");
        for (int j = 0; j < captions.size(); j++) {
            String name = captions.get(j).text();
            if (name.equals("업체현황") || name.equals("입찰업체 현황")) {
                resultTable = captions.get(j).parent();
                if (resultTable.getElementsByTag("tr").get(1).text().length() > 18) {
                    Element top = resultTable.getElementsByTag("tr").get(1);
                    String topPrice = top.getElementsByTag("td").get(4).text();
                    topPrice = topPrice.replaceAll(",", "");
                    System.out.println("투찰금액 : " + topPrice);
                    st.executeUpdate("UPDATE letsrunbidinfo SET 투찰금액=" + topPrice + " WHERE 공고번호 =\""+bidno+"\"");
                }
            }
        }
    }

    public void parseAnn(String bidno) throws IOException, SQLException {
        String path = "https://ebid.kra.co.kr/bid/popup/bd_view_notice.do?b_code="+bidno.substring(3);

        openConnection(path, "GET");

        Document doc = Jsoup.parse(getResponse(null, "GET"));
        Elements captions = doc.getElementsByTag("caption");

        String place = ""; // 사업장
        String bidMethod = ""; // 입찰방식
        String deadline = ""; // 입찰마감
        String openDate = ""; // 개찰일시
        String priceMethod = ""; // 예정가격방식

        for (int j = 0; j < captions.size(); j++) {
            if (captions.get(j).text().equals("입찰공고 계약전체 표")) {
                Element infoTable = captions.get(j).parent();
                Elements infos = infoTable.getElementsByTag("th"); // Headers for table of details
                for (int k = 0; k < infos.size(); k++) {
                    String key = infos.get(k).text();
                    if (key.equals("사업장")) {
                        place = infos.get(k).nextElementSibling().text();
                    }
                    else if (key.equals("전자입찰여부")) {
                        bidMethod = infos.get(k).nextElementSibling().text();
                    }
                }
            }
            else if (captions.get(j).text().equals("입찰진행순서표")) {
                Element timeTable = captions.get(j).parent();
                Elements infos = timeTable.getElementsByTag("th"); // Headers for table of details
                for (int k = 0; k < infos.size(); k++) {
                    String key = infos.get(k).text();
                    if (key.equals("입찰마감")) {
                        deadline = infos.get(k).nextElementSibling().text();
                    }
                    else if (key.equals("개찰일시")) {
                        openDate = infos.get(k).nextElementSibling().text();
                    }
                }
            }
            else if (captions.get(j).text().equals("예정가격정보표")) {
                Element priceTable = captions.get(j).parent();
                Elements infos = priceTable.getElementsByTag("th"); // Headers for table of details
                for (int k = 0; k < infos.size(); k++) {
                    String key = infos.get(k).text();
                    if (key.equals("예정가격방식")) {
                        priceMethod = infos.get(k).nextElementSibling().text();
                    }
                }
            }
        }

        String sql = "UPDATE letsrunbidinfo SET " +
                "사업장=\"" + place + "\", " +
                "입찰방식=\"" + bidMethod + "\", " +
                "예정가격방식=\"" + priceMethod + "\", " +
                "입찰마감=\"" + deadline + "\", " +
                "개찰일시=\"" + openDate + "\" WHERE 공고번호=\"" + bidno + "\";";
        st.executeUpdate(sql);
    }

    public int getTotal() throws IOException {
        String path = LetsParser.RES_LIST;
        String param = "?is_from_main=true&page=1&open_date_from="+sd+"&open_date_to="+ed;

        openConnection(path + param, "GET");
        Document doc = Jsoup.parse(getResponse(null, "GET"));
        Element pagingdiv = doc.getElementsByAttributeValue("class", "bid_paging").first();
        Element lastButton = pagingdiv.getElementsByAttributeValue("class", "btns last_page").first();
        if (lastButton != null) {
            String lastpath = "https://ebid.kra.co.kr/res/all/";
            lastpath += lastButton.attr("href");
            openConnection(lastpath, "GET");

            Document lastPage = Jsoup.parse(getResponse(null, "GET"));
            Elements rows = lastPage.getElementsByTag("table").get(1).getElementsByTag("tr");
            String lastindex = rows.get(rows.size() - 1).getElementsByTag("td").first().text();
            totalItems = Integer.parseInt(lastindex);
        }
        else {
            Elements rows = doc.getElementsByTag("table").get(1).getElementsByTag("tr");
            String lastindex = rows.get(rows.size() - 1).getElementsByTag("td").first().text();
            if (lastindex.contains("없습니다")) totalItems = 0;
            else totalItems = Integer.parseInt(lastindex);
        }

        return totalItems;
    }

    public void setDate(String sd, String ed) {
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
        if (db_con == null) {
            // Set up SQL connection.
            db_con = DriverManager.getConnection(
                    "jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8",
                    Util.DB_ID,
                    Util.DB_PW
            );
            st = db_con.createStatement();
            rs = null;
        }

        ArrayList<String> bidNums = new ArrayList<String>();

        String sql = "SELECT 공고번호 FROM letsrunbidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 완료=1;";
        System.out.println(sql);
        rs = st.executeQuery(sql);
        while (rs.next()) {
            System.out.println("rs");
            bidNums.add(rs.getString("공고번호"));
        }

        int page = 1;
        setOption("결과");
        Document doc = getListPage(page);
        Elements rows = doc.getElementsByTag("table").get(1).getElementsByTag("tr");
        int items = 0;

        do {
            if (rows.get(1).text().length() > 24) {
                for (int i = 1; i < rows.size(); i++) {
                    if (shutdown) {
                        return;
                    }

                    Element row = rows.get(i);
                    Elements data = row.getElementsByTag("td");
                    String bidNum = data.get(1).text();
                    System.out.println(bidNum);
                    if (bidNums.indexOf(bidNum) != -1) {
                        bidNums.remove(bidNum);
                    }
                }
                page++;
                boolean nextPage = false;
                Element pagingdiv = doc.getElementsByAttributeValue("class", "bid_paging").first();
                Elements pages = pagingdiv.getElementsByTag("a");
                String pagestr = page + "";

                for (int i = 0; i < pages.size(); i++) {
                    String text = pages.get(i).text();
                    System.out.println(text);
                    if (text.equals(pagestr) || text.equals("다음 목록으로 이동")) {
                        nextPage = true;
                        break;
                    }
                }
                if (nextPage) {
                    doc = getListPage(page);
                    rows = doc.getElementsByTag("table").get(1).getElementsByTag("tr");
                    items = rows.size();
                }
                else {
                    items = 0;
                }
            }
        } while (items > 0);

        for (int i = 0; i < bidNums.size(); i++) {
            String bidNum = bidNums.get(i);
            sql = "DELETE FROM letsrunbidinfo WHERE 공고번호=\"" + bidNum + "\";";
            System.out.println(sql);
            st.executeUpdate(sql);
        }
    }
}