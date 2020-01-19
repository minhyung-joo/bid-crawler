package org.bidcrawler;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.http.Header;
import org.bidcrawler.utils.Util;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ravenjoo on 7/22/17.
 */
public class NewDapaParser extends Parser {
    public static final String PROD_ANN_LIST = "http://www.d2b.go.kr/pdb/bid/getGoodsBidAnnounceListNew.json";
    public static final String BID_ANN_VIEW = "http://www.d2b.go.kr/pdb/bid/bidAnnounceView.do";
    public static final String NEGO_ANN_VIEW = "http://www.d2b.go.kr/pdb/openNego/openNegoPlanView.do";
    public static final String PROD_BID_RES = "http://www.d2b.go.kr/pdb/bid/getBidResultList.json";
    public static final String PROD_NEGO_RES = "http://www.d2b.go.kr/pdb/openNego/openNegoResultView.do";
    public static final String PROD_RES_VIEW = "http://www.d2b.go.kr/pdb/bid/bidResultView.do";
    public static final String PROD_ORG_LIST = "http://www.d2b.go.kr/pdb/bid/getBidRecordList.json";
    public static final String PROD_NEGO_ORG = "http://www.d2b.go.kr/pdb/openNego/getMnufResultList.json";
    public static final String PROD_BID_PRICE = "http://www.d2b.go.kr/pdb/bid/multiReservePriceInfo.do";
    public static final String PROD_NEGO_PRICE = "http://www.d2b.go.kr/pdb/openNego/openNegoMultiPricePop.do";
    public static final String SERV_ANN_LIST = "http://www.d2b.go.kr/psb/bid/getServiceBidAnnounceListNew.json";
    public static final String SERV_BID_RES = "http://www.d2b.go.kr/psb/bid/getBidResultList.json";
    public static final String FACIL_ANN_LIST = "http://www.d2b.go.kr/peb/bid/getAnnounceList.json";
    public static final String FACIL_NEGO_ANN = "http://www.d2b.go.kr/peb/openNego/openNegoPlanView.do";
    public static final String FACIL_BID_ANN = "http://www.d2b.go.kr/peb/bid/announceView.do";
    public static final String FACIL_BID_RES = "http://www.d2b.go.kr/peb/bid/getBidResultList.json";
    public static final String FACIL_RES_VIEW = "http://www.d2b.go.kr/peb/bid/bidResultView.do";
    public static final String FACIL_NEGO_RES = "http://www.d2b.go.kr/peb/openNego/openNegoResultView.do";
    public static final String FACIL_ORG_LIST = "http://www.d2b.go.kr/peb/bid/getBidRecordList.json";
    public static final String FACIL_NEGO_ORG = "http://www.d2b.go.kr/peb/openNego/getMnufResultList.json";
    public static final String FACIL_BID_PRICE = "http://www.d2b.go.kr/peb/bid/multiPriceInfo.do";
    public static final String FACIL_NEGO_PRICE = "http://www.d2b.go.kr/peb/openNego/openNegoMultiPricePop.do";

    private class DapaEntry {
        HashMap<String, String> bidInfo;

        public DapaEntry(JSONObject jsonEntry) {
            bidInfo = new HashMap();
            Iterator<String> jsonKeys = jsonEntry.keys();
            while (jsonKeys.hasNext()) {
                String key = jsonKeys.next();
                String value = jsonEntry.get(key).toString();
                bidInfo.put(key, value);
            }
        }
    }

    // For SQL setup.
    private Connection db_con;
    private java.sql.Statement st;
    private ResultSet rs;

    private URL url;
    private HttpURLConnection con;
    private String startDate;
    private String endDate;
    private String option;
    private int totalItems;
    private int curItem;

    // HttpClient suite
    private HttpClient client;
    String cookie = null;

    private GetFrame frame;
    private CheckFrame checkFrame;

    public static void main(String[] args) {
        try {
            NewDapaParser parser = new NewDapaParser("20180202", "20180212", "PROD", null, null);
            parser.parseBidData();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public NewDapaParser(String startDate, String endDate, String option, GetFrame frame, CheckFrame checkFrame) throws SQLException {
        this.client = HttpClientBuilder.create().build();
        this.startDate = startDate.replaceAll("-", "");
        this.endDate = endDate.replaceAll("-", "");
        this.option = option;
        this.frame = frame;
        this.checkFrame = checkFrame;

        totalItems = 0;
        curItem = 0;

        // Set up SQL connection.
        db_con = DriverManager.getConnection(
                "jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8",
                Util.DB_ID,
                Util.DB_PW
        );
        st = db_con.createStatement();
        rs = null;
    }

    public String sendPostRequest(String path, List<NameValuePair> urlParameters) throws IOException {
        HttpPost post = new HttpPost(path);

        // Set up header
        post.setHeader("User-Agent", "Mozilla/5.0");
        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        if (cookie != null) {
            post.setHeader("Cookie", cookie);
        }

        HttpResponse response = client.execute(post);
        System.out.println("\nSending 'POST' request to URL : " + path);
        System.out.println("Post parameters : " + post.getEntity());
        System.out.println("Response Code : " +
                response.getStatusLine().getStatusCode());

        if (response.getFirstHeader("Set-Cookie") != null) {
            cookie = response.getFirstHeader("Set-Cookie").getValue();
        }
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        return result.toString();
    }

    public void openHttpConnection(String path) throws IOException {
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
        System.out.println("\nSending POST request to URL : " + url);
        System.out.println("Post parameters : " + param);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public void parseBidData() throws IOException, SQLException {
        parseAnnouncementData();
        parseResultData();
    }

    private void parseAnnouncementData() throws IOException, SQLException {
        String path = "";
        switch (option) {
            case "PROD":
                path = NewDapaParser.PROD_ANN_LIST;
                break;
            case "FACIL":
                path = NewDapaParser.FACIL_ANN_LIST;
                break;
            case "SERV":
                path = NewDapaParser.SERV_ANN_LIST;
                break;
            default:
                break;
        }

        openHttpConnection(path);
        int page = 1;
        String param = "date_divs=1&date_from=" + startDate + "&date_to=" + endDate + "&numb_divs=1&exct_divs=B&currentPageNo=" + page;
        Document doc = Jsoup.parse(getResponse(param));

        JSONObject jsonData = new JSONObject(doc.body().text());
        JSONArray dataArray = jsonData.getJSONArray("list");
        List bidEntries = getBidEntriesFromJsonArray(dataArray);
        while (!bidEntries.isEmpty()) {
            for (Object entry : bidEntries) {
                if (shutdown) {
                    return;
                }

                try {
                    JSONObject jsonEntry = (JSONObject) entry;
                    DapaEntry annEntry = new DapaEntry(jsonEntry);
                    if (frame != null) {
                        frame.updateInfo(annEntry.bidInfo.get("anmtNumb"), false);
                    }

                    boolean exists = false;
                    boolean enter = true;

                    String type = getType();
                    String bidNum = annEntry.bidInfo.get("anmtNumb"); // 공고번호
                    String bidVer = annEntry.bidInfo.get("rqstDegr"); // 차수
                    String idenNum = annEntry.bidInfo.get("dcsnNumb"); // 판단번호
                    String itemNum = annEntry.bidInfo.get("dmstItnb"); // 항목번호
                    String bidType = annEntry.bidInfo.get("lv2Divs").equals("2") ? "협상" : "경쟁";
                    String org = annEntry.bidInfo.get("codeVld3"); // 발주기관
                    String annType = annEntry.bidInfo.get("codeVld1"); // 공고종류
                    String compType = annEntry.bidInfo.get("codeVld4"); // 계약방법
                    String openDate = composeDatetime(annEntry.bidInfo.get("bidxDate"), annEntry.bidInfo.get("bidxTime")); // 개찰일시
                    String basicPrice = annEntry.bidInfo.get("bsicExpt"); // 기초예정가격
                    String priceOpen = ""; // 기초예가공개
                    String applyPrice = ""; // 기초예가적용여부
                    if (!Util.isNumeric(basicPrice)) {
                        priceOpen = "적용안함";
                    }
                    else if (Long.parseLong(basicPrice) > 0) {
                        priceOpen = "공개";
                    }
                    else {
                        priceOpen = "공개예정";
                    }

                    if (priceOpen.equals("공개") || priceOpen.equals("공개예정")) {
                        applyPrice = "적용";
                    }

                    if (!Util.isInteger(itemNum)) {
                        itemNum = "***";
                    }

                    String paperDeadline;
                    if (annEntry.bidInfo.get("lv2Divs").equals("2")) {
                        String date = annEntry.bidInfo.get("negnCldt");
                        String time = annEntry.bidInfo.get("negnCltm");
                        paperDeadline = composeDatetime(date, time);
                    } else {
                        String datetime = annEntry.bidInfo.get("bidxEndt");
                        paperDeadline = datetime.substring(0, 4) + "-" + datetime.substring(4, 6) + "-" +
                                datetime.substring(6, 8) + " " + datetime.substring(8, 10) + ":" +
                                datetime.substring(10, 12);
                    }

                    String where = "WHERE 공고번호=\"" + bidNum + "\" AND 차수=" + bidVer + " AND 공사번호=\"" + idenNum + "\"";
                    String sql = "SELECT EXISTS(SELECT 공사번호 FROM dapabidinfo " + where + ")";
                    rs = st.executeQuery(sql);
                    if (rs.first()) exists = rs.getBoolean(1);

                    if (exists) {
                        // Check the bid version and update level from the DB.
                        sql = "SELECT 공고, 기초예가공개 FROM dapabidinfo " + where;
                        rs = st.executeQuery(sql);
                        int finished = 0;
                        String dbPriceOpen = "";
                        if (rs.first()) {
                            finished = rs.getInt(1);
                            dbPriceOpen = rs.getString(2) == null ? "" : rs.getString(2);
                        }

                        if (finished > 0) {
                            enter = false;
                        }

                        if (!dbPriceOpen.equals(priceOpen)) {
                            sql = "UPDATE dapabidinfo SET 기초예가공개=\"" + priceOpen + "\"";
                            if (Util.isNumeric(basicPrice)) {
                                sql += ", 기초예비가격=" + basicPrice;
                            }

                            sql += " " + where;
                            st.executeUpdate(sql);
                        }
                    } else {
                        // If entry doesn't exists in db, insert new row.
                        sql = "INSERT INTO dapabidinfo (분류, 공고번호, 차수, 항목번호, 입찰종류, 공고종류, 공사번호, 발주기관, 개찰일시, 계약방법, ";
                        if (Util.isNumeric(basicPrice)) {
                            sql += "기초예비가격, ";
                        }
                        if (paperDeadline != null) {
                            sql += "입찰서제출마감일시, ";
                        }
                        if (applyPrice.equals("적용")) {
                            sql += "기초예가적용여부, ";
                        }

                        sql += " 기초예가공개) VALUES (" +
                                "\""+type+"\", " +
                                "\""+bidNum+"\", " +
                                ""+bidVer+", " +
                                "\""+itemNum+"\", " +
                                "\""+bidType+"\", " +
                                "\""+annType+"\", " +
                                "\""+idenNum+"\", " +
                                "\""+org+"\", " +
                                "\""+openDate+"\", " +
                                "\""+compType+"\", ";
                        if (Util.isNumeric(basicPrice)) {
                            sql += basicPrice + ", ";
                        }
                        if (paperDeadline != null) {
                            sql += "\"" + paperDeadline + "\", ";
                        }
                        if (applyPrice.equals("적용")) {
                            sql += "\"" + applyPrice + "\", ";
                        }

                        sql += "\""+priceOpen+"\");";
                        System.out.println(sql);
                        st.executeUpdate(sql);
                        if (compType.equals("지명경쟁")) {
                            sql = "UPDATE dapabidinfo SET 공고=1 " + where;
                            st.executeUpdate(sql);
                            enter = false;
                        }
                    }

                    if (enter) {
                        if (annEntry.bidInfo.get("lv2Divs").equals("2")) {
                            parseNegoAnnEntry(annEntry, where);
                        } else {
                            parseBidAnnEntry(annEntry, where);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Get new page
            page++;
            param = "date_divs=1&date_from=" + startDate + "&date_to=" + endDate + "&numb_divs=1&exct_divs=B&currentPageNo=" + page;
            openHttpConnection(path);
            doc = Jsoup.parse(getResponse(param));
            jsonData = new JSONObject(doc.body().text());
            dataArray = jsonData.getJSONArray("list");
            bidEntries = getBidEntriesFromJsonArray(dataArray);
        }
    }

    private String composeDatetime(String date, String time) {
        return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8) + " " + time.substring(0, 2) + ":" + time.substring(2, 4);
    }

    private List getBidEntriesFromJsonArray(JSONArray dataArray) {
        List entries = new ArrayList<JSONObject>();

        for (int i = 0; i < dataArray.length(); i++) {
            entries.add(dataArray.getJSONObject(i));
        }

        return entries;
    }

    private void parseBidAnnEntry(DapaEntry entry, String where) throws IOException, SQLException {
        Document doc = Jsoup.parse(getAnnDetails(entry));

        String bidMethod = ""; // 입찰방법
        String prelim = "비대상"; // 사전심사
        String selectMethod = ""; // 낙찰자결정방법
        String range = ""; // 사정율
        String lowerBound = ""; // 사정율 하한
        String upperBound = ""; // 사정율 상한
        String rate = ""; // 낙찰하한율

        /*
         * Getting the announcement details, including 입찰방법, 낙찰자결정방법, and 사정율
         */
        Elements infoDivs = doc.getElementsByAttributeValue("summary", "상세테이블");
        if (!infoDivs.isEmpty()) {
            for (Element infoDiv : infoDivs) {
                Elements headers = infoDiv.getElementsByTag("th");
                for (Element header : headers) {
                    switch (header.text()) {
                        case "낙찰자결정방법":
                            selectMethod = header.nextElementSibling().text();
                            break;
                        case "입찰방법":
                            bidMethod = header.nextElementSibling().text();
                            break;
                        case "사정율(%)":
                            range = header.nextElementSibling().text();
                            String[] bounds = range.split(" ~ ");
                            lowerBound = bounds[0];
                            upperBound = bounds[1];
                            break;
                        case "하한(%)":
                            lowerBound = header.nextElementSibling().text().replaceAll("[^\\.0123456789]", "");
                            break;
                        case "상한(%)":
                            upperBound = header.nextElementSibling().text().replaceAll("[^\\.0123456789]", "");
                            break;
                        case "낙찰하한율(%)":
                            rate = header.nextElementSibling().text().replaceAll("[^\\.0123456789]", "");
                            break;
                        case "사전심사":
                            prelim = header.nextElementSibling().text();
                            break;
                        case "복수업체연구개발 대상구분":
                            prelim = header.nextElementSibling().text();
                            break;
                        default:
                            break;
                    }
                }
            }
        } else {
            Logger.getGlobal().log(Level.WARNING, "Invalid response from entry " + entry.bidInfo.get("anmtNumb"));
        }

        String license = parseLicenseInfo(doc); // 면허명칭

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("UPDATE dapabidinfo SET ");
        sqlBuilder.append("입찰방법=\"" + bidMethod + "\", ");
        sqlBuilder.append("낙찰자결정방법=\"" + selectMethod + "\", ");
        if (!(lowerBound.equals("") || upperBound.equals(""))) {
            if (range.equals("")) {
                sqlBuilder.append("사정률=\"" + lowerBound + " ~ " + upperBound + "\", ");
            } else {
                sqlBuilder.append("사정률=\"" + range + "\", ");
            }
            sqlBuilder.append("하한=" + lowerBound + ", ");
            sqlBuilder.append("상한=" + upperBound + ", ");
        }
        sqlBuilder.append("사전심사=\"" + prelim + "\", ");
        sqlBuilder.append("면허명칭=\"" + license + "\", ");
        if (!rate.equals("")) {
            sqlBuilder.append("낙찰하한율=" + rate + ", ");
        }
        sqlBuilder.append("공고=1 ");
        sqlBuilder.append(where);
        String sql = sqlBuilder.toString();
        System.out.println(sql);
        st.executeUpdate(sql);
    }

    private String parseLicenseInfo(Document doc) {
        String license = "";
        Element licenseTable = doc.getElementsByAttributeValue("summary", "공종 및 면허제한").first();
        if (licenseTable != null) {
            Element contents = licenseTable.getElementsByTag("tbody").first();
            Elements rows = contents.getElementsByTag("tr");
            for (Element row : rows) {
                license += row.getElementsByTag("td").get(1).text() + " ";
            }
        }

        if (license.length() >= 255) {
            license = license.substring(0, 255);
        }

        return license;
    }

    private String getAnnDetails(DapaEntry entry) throws IOException {
        String path = NewDapaParser.BID_ANN_VIEW;
        if (entry.bidInfo.get("lv1Divs").equals("PEB")) {
            path = NewDapaParser.FACIL_BID_ANN;
        }

        openHttpConnection(path);
        StringBuilder paramBuilder = new StringBuilder();
        paramBuilder.append("dprt_code="+entry.bidInfo.get("dprtCode"));
        paramBuilder.append("&anmt_divs="+entry.bidInfo.get("anmtDivs"));
        paramBuilder.append("&anmt_numb="+entry.bidInfo.get("anmtNumb"));
        paramBuilder.append("&rqst_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&dcsn_numb="+entry.bidInfo.get("dcsnNumb"));
        paramBuilder.append("&rqst_year="+entry.bidInfo.get("rqstYear"));
        paramBuilder.append("&bsic_stat="+entry.bidInfo.get("bsicStat"));
        paramBuilder.append("&anmt_date="+entry.bidInfo.get("anmtDate"));
        paramBuilder.append("&lv2Divs="+entry.bidInfo.get("lv2Divs"));
        paramBuilder.append("&lv1Divs="+entry.bidInfo.get("lv1Divs"));
        paramBuilder.append("&csrt_numb="+entry.bidInfo.get("dcsnNumb"));
        if (option.equals("FACIL")) {
            paramBuilder.append("&pageDivs=E1&bid_divs=bid");
        } else if (option.equals("SERV")) {
            paramBuilder.append("&pageDivs=S1&exct_divs=A");
        } else {
            paramBuilder.append("&pageDivs=G1&bid_divs=bid");
        }

        String param = paramBuilder.toString();
        return getResponse(param);
    }

    private void parseNegoAnnEntry(DapaEntry entry, String where) throws IOException, SQLException {
        Document doc = Jsoup.parse(getNegoAnnDetails(entry));
        String bidMethod = ""; // 입찰방법
        String prelim = "비대상"; // 사전심사
        String selectMethod = ""; // 낙찰자결정방법
        String range = ""; // 사정율
        String lowerBound = ""; // 사정율 하한
        String upperBound = ""; // 사정율 상한
        String rate = ""; // 낙찰하한율
        String negoMethod = ""; // 협상형태

        Element infoDiv = doc.getElementsByClass("post").first();
        if (infoDiv != null) {
            Element infoTable = infoDiv.getElementsByAttributeValue("summary", "상세테이블").first();
            Elements headers = infoTable.getElementsByTag("th");
            for (Element header : headers) {
                switch (header.text()) {
                    case "계약상대자결정방법":
                        selectMethod = header.nextElementSibling().text();
                        break;
                    case "입찰방법":
                        bidMethod = header.nextElementSibling().text();
                        break;
                    case "사정율(%)":
                        range = header.nextElementSibling().text();
                        String[] bounds = range.split(" ~ ");
                        lowerBound = bounds[0];
                        upperBound = bounds[1];
                        break;
                    case "하한(%)":
                        lowerBound = header.nextElementSibling().text().replaceAll(" %", "");
                        break;
                    case "상한(%)":
                        upperBound = header.nextElementSibling().text().replaceAll(" %", "");
                        break;
                    case "낙찰하한율(%)":
                        rate = header.nextElementSibling().text().replaceAll(" %", "");
                        break;
                    case "사전심사":
                    case "복수업체연구개발 대상구분":
                        prelim = header.nextElementSibling().text();
                        break;
                    case "협상형태":
                        negoMethod = header.nextElementSibling().text().trim().replaceAll("\\s+","");
                        break;
                    default:
                        break;
                }
            }
        } else {
            System.out.println(doc.html());
        }

        String license = parseLicenseInfo(doc);

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("UPDATE dapabidinfo SET ");
        sqlBuilder.append("입찰방법=\"" + bidMethod + "\", ");
        sqlBuilder.append("낙찰자결정방법=\"" + selectMethod + "\", ");
        if (!(lowerBound.equals("") || upperBound.equals(""))) {
            if (range.equals("")) {
                sqlBuilder.append("사정률=\"" + lowerBound + " ~ " + upperBound + "\", ");
            } else {
                sqlBuilder.append("사정률=\"" + range + "\", ");
            }
            sqlBuilder.append("하한=" + lowerBound + ", ");
            sqlBuilder.append("상한=" + upperBound + ", ");
        }
        sqlBuilder.append("사전심사=\"" + prelim + "\", ");
        sqlBuilder.append("면허명칭=\"" + license + "\", ");
        if (!rate.equals("")) {
            sqlBuilder.append("낙찰하한율=" + rate + ", ");
        }
        sqlBuilder.append("협상형태=\"" + negoMethod + "\", ");
        sqlBuilder.append("공고=1 ");
        sqlBuilder.append(where);
        String sql = sqlBuilder.toString();
        System.out.println(sql);
        st.executeUpdate(sql);
    }

    private String getNegoAnnDetails(DapaEntry entry) throws IOException {
        String path = NewDapaParser.NEGO_ANN_VIEW;
        if (entry.bidInfo.get("lv1Divs").equals("PEB")) {
            path = NewDapaParser.FACIL_NEGO_ANN;
        }

        openHttpConnection(path);
        StringBuilder paramBuilder = new StringBuilder();
        paramBuilder.append("dmst_itnb=");
        if (entry.bidInfo.containsKey("dmstItnb") && entry.bidInfo.get("dmstItnb") != null) {
            paramBuilder.append(entry.bidInfo.get("dmstItnb"));
        } else {
            paramBuilder.append("***");
        }

        if (option.equals("FACIL")) {
            paramBuilder.append("&csrt_numb=" + entry.bidInfo.get("dcsnNumb"));
            paramBuilder.append("&pageDivs=E&list_url=%2Fpeb%2Fbid%2FannounceList.do");
        } else if (option.equals("SERV")) {
            paramBuilder.append("&dcsn_numb=" + entry.bidInfo.get("dcsnNumb"));
            paramBuilder.append("&csrt_numb=" + entry.bidInfo.get("dcsnNumb"));
            paramBuilder.append("&pageDivs=S&list_url=%2Fpsb%2Fbid%2FserviceBidAnnounceList.do");
        } else {
            paramBuilder.append("&dcsn_numb=" + entry.bidInfo.get("dcsnNumb"));
            paramBuilder.append("&pageDivs=G&list_url=%2Fpdb%2Fbid%2FgoodsBidAnnounceList.do");
        }

        paramBuilder.append("&negn_cldt=" + entry.bidInfo.get("negnCldt"));
        paramBuilder.append("&negn_pldt=" + entry.bidInfo.get("negnCldt"));
        paramBuilder.append("&dprt_code=" + entry.bidInfo.get("dprtCode"));
        paramBuilder.append("&ordr_year=" + entry.bidInfo.get("rqstYear"));
        paramBuilder.append("&negn_degr=" + entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&anmt_numb=" + entry.bidInfo.get("anmtNumb"));
        paramBuilder.append("&lv1Divs=" + entry.bidInfo.get("lv1Divs"));

        String param = paramBuilder.toString();
        return getResponse(param);
    }

    private void parseResultData() throws IOException, SQLException {
        String path = "";
        switch (option) {
            case "PROD":
                path = NewDapaParser.PROD_BID_RES;
                break;
            case "FACIL":
                path = NewDapaParser.FACIL_BID_RES;
                break;
            case "SERV":
                path = NewDapaParser.SERV_BID_RES;
                break;
            default:
                break;
        }

        openHttpConnection(path);
        int page = 1;
        String param = "from_date=" + startDate + "&to_date=" + endDate + "&chkMy=1";
        if (option.equals("PROD")) {
            param += "&exct_divs=B";
        }
        else if (option.equals("SERV")) {
            param += "&exct_divs=A";
        }

        Document doc = Jsoup.parse(getResponse(param));

        JSONObject jsonData = new JSONObject(doc.body().text());
        JSONArray dataArray = jsonData.getJSONArray("list");
        List bidEntries = getBidEntriesFromJsonArray(dataArray);
        while (!bidEntries.isEmpty()) {
            for (Object entry : bidEntries) {
                if (shutdown) {
                    return;
                }

                JSONObject jsonEntry = (JSONObject) entry;
                DapaEntry resEntry = new DapaEntry(jsonEntry);
                curItem++;
                if (frame != null) {
                    frame.updateInfo(resEntry.bidInfo.get("anmtNumb"), true);
                }
                if (checkFrame != null) {
                    checkFrame.updateProgress(threadIndex);
                }

                boolean exists = false;
                boolean enter = true;

                String type = getType();
                String bidNum = resEntry.bidInfo.get("anmtNumb"); // 공고번호
                String bidVer = resEntry.bidInfo.get("rqstDegr"); // 차수
                String idenNum = resEntry.bidInfo.get("dcsnNumb"); // 공사번호
                String itemNum = resEntry.bidInfo.get("dmstItnb"); // 항목번호
                String bidType = resEntry.bidInfo.get("lvDivs2").equals("NEGO") ? "협상" : "경쟁"; // 입찰종류
                String annType = resEntry.bidInfo.get("anmtDivsNm"); // 공고종류
                String org = resEntry.bidInfo.get("dprtCodeNm"); // 발주기관
                String result = resEntry.bidInfo.get("resultNm"); // 입찰결과
                String bidMethod = resEntry.bidInfo.get("bidxMthdNm"); // 입찰방법
                String compType = resEntry.bidInfo.get("contMthdNm"); // 계약방법
                String selectMethod = resEntry.bidInfo.get("bidnMthdNm"); // 낙찰자결정방법
                String openDate = resEntry.bidInfo.get("bidxDatm"); // 개찰일시
                openDate = openDate.substring(0, 4) + "-" + openDate.substring(4, 6) + "-" + openDate.substring(6, 8)
                        + " " + openDate.substring(8, 10) + ":" + openDate.substring(10, 12);

                if (!Util.isInteger(itemNum)) {
                    itemNum = "***";
                }

                String where = "WHERE 공고번호=\""+bidNum+"\" AND " +
                        "차수="+bidVer+" AND " +
                        "공사번호=\""+idenNum+"\" AND " +
                        "항목번호=\""+itemNum+"\"";
                String sql = "SELECT EXISTS(SELECT 공고번호 FROM dapabidinfo "+where+");";
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
                } else {
                    // If entry doesn't exists in db, insert new row.
                    sql = "INSERT INTO dapabidinfo (분류, 공고번호, 차수, 항목번호, 공사번호, 입찰종류, 공고종류, 발주기관, 개찰일시, 입찰방법, 계약방법, 입찰결과";
                    if (selectMethod.length() > 1) {
                        sql += ", 낙찰자결정방법";
                    }
                    sql += ") VALUES (" +
                            "\""+type+"\", " +
                            "\""+bidNum+"\", " +
                            ""+bidVer+", " +
                            "\""+itemNum+"\", " +
                            "\""+idenNum+"\", " +
                            "\""+bidType+"\", " +
                            "\""+annType+"\", " +
                            "\""+org+"\", " +
                            "\""+openDate+"\", " +
                            "\""+bidMethod+"\", " +
                            "\""+compType+"\", " +
                            "\""+result+"\"";
                    if (selectMethod.length() > 1) {
                        sql += ", \"" + selectMethod + "\"";
                    }
                    sql += ");";
                    st.executeUpdate(sql);

                    // Check if there is announcement data, and copy the relevant data if there is.
                    boolean annExists = false;
                    String annWhere = "WHERE 공고번호=\""+bidNum+"\" AND " +
                            "차수="+bidVer+" AND " +
                            "공사번호=\""+idenNum+"\"";
                    sql = "SELECT EXISTS(SELECT 공고번호 FROM dapabidinfo "+annWhere+");";
                    rs = st.executeQuery(sql);
                    if (rs.first()) annExists = rs.getBoolean(1);

                    if (annExists) {
                        sql = "SELECT 면허명칭, 사전심사, 입찰서제출마감일시, 공고 FROM dapabidinfo " + annWhere;
                        rs = st.executeQuery(sql);
                        int annFinished = 0;
                        while (rs.next()) {
                            annFinished = rs.getInt(4);
                            if (annFinished == 1) {
                                break;
                            }
                        }

                        if (annFinished == 1) {
                            String license = rs.getString(1);
                            String prelim = rs.getString(2);
                            String paperDeadline = rs.getString(3);

                            sql = "UPDATE dapabidinfo SET ";
                            if (license != null) {
                                sql += "면허명칭=\"" + license + "\", ";
                            }
                            if (prelim != null) {
                                sql += "사전심사=\"" + prelim + "\", ";
                            }
                            if (paperDeadline != null) {
                                sql += "입찰서제출마감일시=\"" + paperDeadline + "\", ";
                            }
                            sql += "공고=1 ";
                            sql += where;
                            st.executeUpdate(sql);
                        }
                    }
                }

                if (enter) {
                    if (resEntry.bidInfo.get("lvDivs2").equals("BID")) {
                        parseBidResEntry(resEntry, where);
                    } else {
                        parseNegoResEntry(resEntry, where);
                    }
                }
            }

            // Get new page
            page++;
            param = "from_date=" + startDate + "&to_date=" + endDate + "&chkMy=1&currentPageNo=" + page;
            if (option.equals("PROD")) {
                param += "&exct_divs=B";
            }
            else if (option.equals("SERV")) {
                param += "&exct_divs=A";
            }

            openHttpConnection(path);
            doc = Jsoup.parse(getResponse(param));
            System.out.println(doc.body().text());
            jsonData = new JSONObject(doc.body().text());
            dataArray = jsonData.getJSONArray("list");
            bidEntries = getBidEntriesFromJsonArray(dataArray);
        }
    }

    private void parseBidResEntry(DapaEntry entry, String where) throws IOException, SQLException {
        Document doc = Jsoup.parse(getResDetails(entry));

        String basePrice = ""; // 기초예비가격
        String expPrice = ""; // 예정가격
        String range = ""; // 사정률
        String rate = ""; // 낙찰하한율
        String lowerBound = ""; // 하한
        String upperBound = ""; // 상한
        String prelim = "비대상"; // 사전심사

        /*
         * Getting the result details, including 공고번호, 개찰일시, and 입찰결과
         */
        Elements infoDivs = doc.getElementsByAttributeValue("summary", "상세테이블");
        if (!infoDivs.isEmpty()) {
            for (Element infoDiv : infoDivs) {
                Elements headers = infoDiv.getElementsByTag("th");
                for (Element header : headers) {
                    if (header.text().equals("예정가격")) {
                        expPrice = header.nextElementSibling().text();
                        expPrice = expPrice.replaceAll("[^\\.0123456789]","");
                    }
                    if (header.text().equals("사정률(%)")) {
                        range = header.nextElementSibling().text();
                        range = range.trim().replace("\\s+", "");
                        if (range.contains("~") && range.length() >= 3) {
                            String[] bounds = range.split("~");
                            lowerBound = bounds[0];
                            upperBound = bounds[1];
                        } else {
                            range = "";
                        }
                    }
                    if (header.text().equals("낙찰하한율(%)")) {
                        rate = header.nextElementSibling().text();
                        rate = rate.replaceAll("[^\\.0123456789]","");
                    }
                    if (header.text().equals("기초예비가격")) {
                        basePrice = header.nextElementSibling().text();
                        basePrice = basePrice.replaceAll("[^\\.0123456789]","");
                    }
                    if (header.text().equals("사전심사")) {
                        prelim = header.nextElementSibling().text();
                        prelim = prelim.trim().replaceAll("\\s+", "");
                    }
                }
            }
        } else {
            Logger.getGlobal().log(Level.WARNING, "Invalid response from entry " + entry.bidInfo.get("anmtNumb"));
        }

        String companies = null;
        String bidPrice = null;
        if (!entry.bidInfo.get("resultNm").equals("유찰")) {
            HashMap org = getBidOrgDetails(entry);
            if (org != null) {
                companies = org.get("totlCnt").toString();
                bidPrice = org.get("tbidAmnt").toString();
                if (bidPrice.equals("0")) {
                    bidPrice = org.get("tbidUtpr").toString();
                }
            }
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("UPDATE dapabidinfo SET ");
        if (!basePrice.equals("") && Util.isNumeric(basePrice)) {
            sqlBuilder.append("기초예비가격=" + basePrice + ", ");
            sqlBuilder.append("기초예가적용여부=\"적용\", ");
            sqlBuilder.append("기초예가공개=\"공개\", ");
        }
        if (!expPrice.equals("") && Util.isNumeric(expPrice)) {
            sqlBuilder.append("예정가격=" + expPrice + ", ");
        }
        if (!range.equals("")) {
            sqlBuilder.append("사정률=\"" + range + "\", ");
            sqlBuilder.append("하한=" + lowerBound + ", ");
            sqlBuilder.append("상한=" + upperBound + ", ");
        }
        if (Util.isNumeric(rate)) {
            sqlBuilder.append("낙찰하한율=" + rate + ", ");
        }
        if (bidPrice != null && Util.isNumeric(bidPrice)) {
            sqlBuilder.append("투찰금액=" + bidPrice + ", ");
        }
        if (companies != null && Util.isNumeric(companies)) {
            sqlBuilder.append("참여수=" + companies + ", ");
        }
        if (entry.bidInfo.get("resultNm").equals("유찰")) {
            sqlBuilder.append("완료=1, ");
        }
        sqlBuilder.append("사전심사=\"" + prelim + "\", ");
        sqlBuilder.append("결과=1 ");
        sqlBuilder.append(where);
        String sql = sqlBuilder.toString();
        System.out.println(sql);
        st.executeUpdate(sql);

        if (doc.getElementsContainingOwnText("복수예가조회").size() > 0) {
            parseBidDupPrices(entry, where);
        }
    }

    private String getResDetails(DapaEntry entry) throws IOException {
        String path = NewDapaParser.PROD_RES_VIEW;
        if (entry.bidInfo.get("lvDivs1").equals("PEB")) {
            path = NewDapaParser.FACIL_RES_VIEW;
        }

        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("ordr_year", entry.bidInfo.get("ordrYear")));
        urlParameters.add(new BasicNameValuePair("dprt_code", entry.bidInfo.get("dprtCode")));
        urlParameters.add(new BasicNameValuePair("dcsn_numb", entry.bidInfo.get("dcsnNumb")));
        urlParameters.add(new BasicNameValuePair("csrt_numb", entry.bidInfo.get("dcsnNumb")));
        urlParameters.add(new BasicNameValuePair("bidx_date", entry.bidInfo.get("bidxDate")));
        urlParameters.add(new BasicNameValuePair("benf_pldt", entry.bidInfo.get("bidxDate")));
        urlParameters.add(new BasicNameValuePair("anmt_numb", entry.bidInfo.get("anmtNumb")));
        urlParameters.add(new BasicNameValuePair("rqst_degr", entry.bidInfo.get("rqstDegr")));
        urlParameters.add(new BasicNameValuePair("anmt_degr", entry.bidInfo.get("rqstDegr")));
        urlParameters.add(new BasicNameValuePair("anmt_divs", entry.bidInfo.get("anmtDivs")));
        urlParameters.add(new BasicNameValuePair("bidx_stat", entry.bidInfo.get("bidxStat")));
        urlParameters.add(new BasicNameValuePair("bidx_note", entry.bidInfo.get("resultNm")));
        urlParameters.add(new BasicNameValuePair("lvDivs1", entry.bidInfo.get("lvDivs1")));
        urlParameters.add(new BasicNameValuePair("lvDivs2", entry.bidInfo.get("lvDivs2")));
        if (entry.bidInfo.containsKey("dmstItnb") && entry.bidInfo.get("dmstItnb") != null) {
            urlParameters.add(new BasicNameValuePair("dmst_itnb", entry.bidInfo.get("dmstItnb")));
        } else {
            urlParameters.add(new BasicNameValuePair("dmst_itnb", "***"));
        }

        if (option.equals("PROD")) {
            urlParameters.add(new BasicNameValuePair("pageDivs", "G6"));
        } else if (option.equals("SERV")) {
            urlParameters.add(new BasicNameValuePair("pageDivs", "S"));
        } else {
            urlParameters.add(new BasicNameValuePair("pageDivs", "E"));
        }

        return sendPostRequest(path, urlParameters);
    }

    private HashMap getBidOrgDetails(DapaEntry entry) throws IOException {
        String path = NewDapaParser.PROD_ORG_LIST;
        if (entry.bidInfo.get("lvDivs1").equals("PEB")) {
            path = NewDapaParser.FACIL_ORG_LIST;
        }

        openHttpConnection(path);
        StringBuilder paramBuilder = new StringBuilder();
        paramBuilder.append("ordr_year="+entry.bidInfo.get("ordrYear"));
        paramBuilder.append("&dprt_code="+entry.bidInfo.get("dprtCode"));
        paramBuilder.append("&dcsn_numb="+entry.bidInfo.get("dcsnNumb"));
        paramBuilder.append("&csrt_numb="+entry.bidInfo.get("dcsnNumb"));
        paramBuilder.append("&bidx_date="+entry.bidInfo.get("bidxDate"));
        paramBuilder.append("&benf_pldt="+entry.bidInfo.get("bidxDate"));
        paramBuilder.append("&anmt_numb="+entry.bidInfo.get("anmtNumb"));
        paramBuilder.append("&rqst_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&anmt_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&benf_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&anmt_divs="+entry.bidInfo.get("anmtDivs"));
        paramBuilder.append("&lvDivs1="+entry.bidInfo.get("lvDivs1"));
        paramBuilder.append("&lvDivs2="+entry.bidInfo.get("lvDivs2"));
        paramBuilder.append("&dmst_itnb=");
        if (entry.bidInfo.containsKey("dmstItnb") && entry.bidInfo.get("dmstItnb") != null) {
            paramBuilder.append(entry.bidInfo.get("dmstItnb"));
        } else {
            paramBuilder.append("***");
        }

        if (option.equals("PROD")) {
            paramBuilder.append("&pageDivs=G");
        } else if (option.equals("SERV")) {
            paramBuilder.append("&pageDivs=S");
        } else {
            paramBuilder.append("&pageDivs=E");
        }

        String param = paramBuilder.toString();
        Document doc = Jsoup.parse(getResponse(param));
        JSONObject jsonData = new JSONObject(doc.body().text());
        JSONArray orgArray = jsonData.getJSONArray("list");

        for (int i = 0; i < orgArray.length(); i++) {
            HashMap orgEntry = (HashMap) orgArray.getJSONObject(i).toMap();
            String result = (String) orgEntry.get("bidxNote");
            if (result != null && (result.equals("낙찰") || result.equals("1순위") || result.equals("동가낙찰"))) {
                return orgEntry;
            }
        }

        if (orgArray.length() > 0) {
            return (HashMap) orgArray.getJSONObject(0).toMap();
        }

        return null;
    }

    private void parseBidDupPrices(DapaEntry entry, String where) throws IOException, SQLException {
        String path = NewDapaParser.PROD_BID_PRICE;
        if (entry.bidInfo.get("lvDivs1").equals("PEB")) {
            path = NewDapaParser.FACIL_BID_PRICE;
        }

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("ordr_year", entry.bidInfo.get("ordrYear")));
        urlParameters.add(new BasicNameValuePair("dprt_code", entry.bidInfo.get("dprtCode")));
        urlParameters.add(new BasicNameValuePair("dcsn_numb", entry.bidInfo.get("dcsnNumb")));
        urlParameters.add(new BasicNameValuePair("csrt_numb", entry.bidInfo.get("dcsnNumb")));
        urlParameters.add(new BasicNameValuePair("bidx_date", entry.bidInfo.get("bidxDate")));
        urlParameters.add(new BasicNameValuePair("benf_pldt", entry.bidInfo.get("bidxDate")));
        urlParameters.add(new BasicNameValuePair("anmt_numb", entry.bidInfo.get("anmtNumb")));
        urlParameters.add(new BasicNameValuePair("rqst_degr", entry.bidInfo.get("rqstDegr")));
        urlParameters.add(new BasicNameValuePair("anmt_degr", entry.bidInfo.get("rqstDegr")));
        urlParameters.add(new BasicNameValuePair("benf_degr", entry.bidInfo.get("rqstDegr")));
        urlParameters.add(new BasicNameValuePair("anmt_divs", entry.bidInfo.get("anmtDivs")));
        urlParameters.add(new BasicNameValuePair("bidx_stat", entry.bidInfo.get("bidxStat")));
        urlParameters.add(new BasicNameValuePair("bidx_note", entry.bidInfo.get("resultNm")));
        urlParameters.add(new BasicNameValuePair("lvDivs1", entry.bidInfo.get("lvDivs1")));
        urlParameters.add(new BasicNameValuePair("lvDivs2", entry.bidInfo.get("lvDivs2")));
        urlParameters.add(new BasicNameValuePair("bidx_qlfy", "Y"));
        if (entry.bidInfo.containsKey("dmstItnb") && entry.bidInfo.get("dmstItnb") != null) {
            urlParameters.add(new BasicNameValuePair("dmst_itnb", entry.bidInfo.get("dmstItnb")));
        } else {
            urlParameters.add(new BasicNameValuePair("dmst_itnb", "***"));
        }

        if (option.equals("PROD")) {
            urlParameters.add(new BasicNameValuePair("pageDivs", "G6"));
        } else if (option.equals("SERV")) {
            urlParameters.add(new BasicNameValuePair("pageDivs", "S"));
        } else {
            urlParameters.add(new BasicNameValuePair("pageDivs", "E"));
        }

        Document doc = Jsoup.parse(sendPostRequest(path, urlParameters));
        Elements priceTables = doc.getElementsByAttributeValue("summary", "목록테이블"); // Fetch the price tables
        if (priceTables.size() == 2) {
            parsePriceTable(priceTables.first().getElementsByTag("tbody").first(), where);
        } else {
            Logger.getGlobal().log(Level.WARNING, "Invalid price result from " + entry.bidInfo.get("anmtNumb"));
        }
    }

    private void parseNegoResEntry(DapaEntry entry, String where) throws IOException, SQLException {
        Document doc = Jsoup.parse(getNegoResDetails(entry));

        String basePrice = ""; // 기초예비가격
        String expPrice = ""; // 예정가격
        String rate = ""; // 낙찰하한율
        String lowerBound = ""; // 하한
        String upperBound = ""; // 상한
        String result = entry.bidInfo.get("resultNm"); // 협상결과
        String selectMethod = ""; // 낙찰자결정방법
        String bidMethod = ""; // 입찰방법

        /*
         * Getting the result details, including 공고번호, 개찰일시, and 입찰결과
         */
        Elements infoDivs = doc.getElementsByAttributeValue("summary", "상세테이블");
        if (!infoDivs.isEmpty()) {
            for (Element infoDiv : infoDivs) {
                Elements headers = infoDiv.getElementsByTag("th");
                for (Element header : headers) {
                    if (header.text().equals("예정가격")) {
                        expPrice = header.nextElementSibling().text();
                        expPrice = expPrice.replaceAll("[^\\.0123456789]","");
                    }
                    if (header.text().equals("상한(%)")) {
                        upperBound = header.nextElementSibling().text();
                        upperBound = upperBound.replaceAll("[^\\.0123456789]","");
                    }
                    if (header.text().equals("하한(%)")) {
                        lowerBound = header.nextElementSibling().text();
                        lowerBound = lowerBound.replaceAll("[^\\.0123456789]","");
                    }
                    if (header.text().equals("낙찰하한율(%)")) {
                        rate = header.nextElementSibling().text();
                        rate = rate.replaceAll("[^\\.0123456789]","");
                    }
                    if (header.text().equals("기초예비가격")) {
                        basePrice = header.nextElementSibling().text();
                        basePrice = basePrice.replaceAll("[^\\.0123456789]","");
                    }
                    if (header.text().equals("낙찰자결정방법")) {
                        selectMethod = header.nextElementSibling().text();
                        selectMethod = selectMethod.trim().replaceAll("\\s+", "");
                    }
                    if (header.text().equals("입찰방법")) {
                        bidMethod = header.nextElementSibling().text();
                        bidMethod = bidMethod.trim().replaceAll("\\s+", "");
                    }
                }
            }
        } else {
            Logger.getGlobal().log(Level.WARNING, "Invalid response from entry " + entry.bidInfo.get("anmtNumb"));
        }

        String companies = null;
        String bidPrice = null;
        if (!entry.bidInfo.get("resultNm").equals("유찰")) {
            HashMap org = getNegoOrgDetails(entry);
            if (org != null) {
                companies = org.get("totlCnt").toString();
                if (entry.bidInfo.get("lvDivs1").equals("PEB")) {
                    bidPrice = org.get("tnegnAmnt").toString();
                } else {
                    if (org.get("vnegnAmnt") != null) {
                        bidPrice = org.get("vnegnAmnt").toString();
                    } else if (org.get("vnegnUtpr") != null) {
                        bidPrice = org.get("vnegnUtpr").toString();
                    }
                }
            }
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("UPDATE dapabidinfo SET ");
        if (!basePrice.equals("") && Util.isNumeric(basePrice)) {
            sqlBuilder.append("기초예비가격=" + basePrice + ", ");
            sqlBuilder.append("기초예가적용여부=\"적용\", ");
        }
        if (!expPrice.equals("") && Util.isNumeric(expPrice)) {
            sqlBuilder.append("예정가격=" + expPrice + ", ");
        }
        if (!(upperBound.equals("") || lowerBound.equals(""))) {
            sqlBuilder.append("하한=" + lowerBound + ", ");
            sqlBuilder.append("상한=" + upperBound + ", ");
            sqlBuilder.append("사정률=\"" + lowerBound + "~" + upperBound + "\", ");
        }
        if (Util.isNumeric(rate)) {
            sqlBuilder.append("낙찰하한율=" + rate + ", ");
        }
        if (bidPrice != null && Util.isNumeric(bidPrice)) {
            sqlBuilder.append("투찰금액=" + bidPrice + ", ");
        }
        if (companies != null && Util.isNumeric(companies)) {
            sqlBuilder.append("참여수=" + companies + ", ");
        }
        if (entry.bidInfo.get("resultNm").equals("유찰")) {
            sqlBuilder.append("완료=1, ");
        }
        sqlBuilder.append("낙찰자결정방법=\"" + selectMethod + "\", ");
        sqlBuilder.append("입찰방법=\"" + bidMethod + "\", ");
        sqlBuilder.append("입찰결과=\"" + result + "\", ");
        sqlBuilder.append("결과=1 ");
        sqlBuilder.append(where);
        String sql = sqlBuilder.toString();
        System.out.println(sql);
        st.executeUpdate(sql);

        if (doc.getElementsContainingOwnText("복수예가조회").size() > 0) {
            parseNegoDupPrices(entry, where);
        }
    }

    private String getNegoResDetails(DapaEntry entry) throws IOException {
        String path = NewDapaParser.PROD_NEGO_RES;
        if (entry.bidInfo.get("lvDivs1").equals("PEB")) {
            path = NewDapaParser.FACIL_NEGO_RES;
        }

        openHttpConnection(path);
        StringBuilder paramBuilder = new StringBuilder();
        paramBuilder.append("ordr_year="+entry.bidInfo.get("ordrYear"));
        paramBuilder.append("&dprt_code="+entry.bidInfo.get("dprtCode"));
        paramBuilder.append("&dcsn_numb="+entry.bidInfo.get("dcsnNumb"));
        paramBuilder.append("&csrt_numb="+entry.bidInfo.get("dcsnNumb"));
        paramBuilder.append("&bidx_date="+entry.bidInfo.get("bidxDate"));
        paramBuilder.append("&negn_pldt="+entry.bidInfo.get("bidxDate"));
        paramBuilder.append("&negn_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&anmt_numb="+entry.bidInfo.get("anmtNumb"));
        paramBuilder.append("&rqst_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&anmt_divs="+entry.bidInfo.get("anmtDivs"));
        paramBuilder.append("&bidx_stat="+entry.bidInfo.get("bidxStat"));
        paramBuilder.append("&lvDivs1="+entry.bidInfo.get("lvDivs1"));
        paramBuilder.append("&lvDivs2="+entry.bidInfo.get("lvDivs2"));
        paramBuilder.append("&dmst_itnb=");
        if (entry.bidInfo.containsKey("dmstItnb") && entry.bidInfo.get("dmstItnb") != null) {
            paramBuilder.append(entry.bidInfo.get("dmstItnb"));
        } else {
            paramBuilder.append("***");
        }

        if (option.equals("PROD")) {
            paramBuilder.append("&pageDivs=G");
        } else if (option.equals("SERV")) {
            paramBuilder.append("&pageDivs=S");
        } else {
            paramBuilder.append("&pageDivs=E");
        }

        String param = paramBuilder.toString();
        return getResponse(param);
    }

    private HashMap getNegoOrgDetails(DapaEntry entry) throws IOException {
        String path = NewDapaParser.PROD_NEGO_ORG;
        if (entry.bidInfo.get("lvDivs1").equals("PEB")) {
            path = NewDapaParser.FACIL_NEGO_ORG;
        }

        openHttpConnection(path);
        StringBuilder paramBuilder = new StringBuilder();
        paramBuilder.append("ordr_year="+entry.bidInfo.get("ordrYear"));
        paramBuilder.append("&dprt_code="+entry.bidInfo.get("dprtCode"));
        paramBuilder.append("&dcsn_numb="+entry.bidInfo.get("dcsnNumb"));
        paramBuilder.append("&csrt_numb="+entry.bidInfo.get("dcsnNumb"));
        paramBuilder.append("&bidx_date="+entry.bidInfo.get("bidxDate"));
        paramBuilder.append("&negn_pldt="+entry.bidInfo.get("bidxDate"));
        paramBuilder.append("&benf_pldt="+entry.bidInfo.get("bidxDate"));
        paramBuilder.append("&anmt_numb="+entry.bidInfo.get("anmtNumb"));
        paramBuilder.append("&rqst_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&negn_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&anmt_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&benf_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&anmt_divs="+entry.bidInfo.get("anmtDivs"));
        paramBuilder.append("&lvDivs1="+entry.bidInfo.get("lvDivs1"));
        paramBuilder.append("&lvDivs2="+entry.bidInfo.get("lvDivs2"));
        paramBuilder.append("&dmst_itnb=");
        if (entry.bidInfo.containsKey("dmstItnb") && entry.bidInfo.get("dmstItnb") != null) {
            paramBuilder.append(entry.bidInfo.get("dmstItnb"));
        } else {
            paramBuilder.append("***");
        }

        if (option.equals("PROD")) {
            paramBuilder.append("&pageDivs=G");
        } else if (option.equals("SERV")) {
            paramBuilder.append("&pageDivs=S");
        } else {
            paramBuilder.append("&pageDivs=E");
        }

        String param = paramBuilder.toString();
        Document doc = Jsoup.parse(getResponse(param));
        JSONObject jsonData = new JSONObject(doc.body().text());
        JSONArray orgArray = jsonData.getJSONArray("resultList");

        for (int i = 0; i < orgArray.length(); i++) {
            HashMap orgEntry = (HashMap) orgArray.getJSONObject(i).toMap();
            String result = (String) orgEntry.get("negnNote");
            if (result.equals("낙찰")) {
                return orgEntry;
            }
        }

        if (orgArray.length() > 0) {
            return (HashMap) orgArray.getJSONObject(0).toMap();
        }

        return null;
    }

    private void parseNegoDupPrices(DapaEntry entry, String where) throws IOException, SQLException {
        String path = NewDapaParser.PROD_NEGO_PRICE;
        if (entry.bidInfo.get("lvDivs1").equals("PEB")) {
            path = NewDapaParser.FACIL_NEGO_PRICE;
        }

        openHttpConnection(path);
        StringBuilder paramBuilder = new StringBuilder();
        paramBuilder.append("ordr_year="+entry.bidInfo.get("ordrYear"));
        paramBuilder.append("&dprt_code="+entry.bidInfo.get("dprtCode"));
        paramBuilder.append("&dcsn_numb="+entry.bidInfo.get("dcsnNumb"));
        paramBuilder.append("&csrt_numb="+entry.bidInfo.get("dcsnNumb"));
        paramBuilder.append("&bidx_date="+entry.bidInfo.get("bidxDate"));
        paramBuilder.append("&negn_pldt="+entry.bidInfo.get("bidxDate"));
        paramBuilder.append("&benf_pldt="+entry.bidInfo.get("bidxDate"));
        paramBuilder.append("&anmt_numb="+entry.bidInfo.get("anmtNumb"));
        paramBuilder.append("&rqst_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&negn_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&anmt_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&benf_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&anmt_divs="+entry.bidInfo.get("anmtDivs"));
        paramBuilder.append("&lvDivs1="+entry.bidInfo.get("lvDivs1"));
        paramBuilder.append("&lvDivs2="+entry.bidInfo.get("lvDivs2"));
        paramBuilder.append("&dmst_itnb=");
        if (entry.bidInfo.containsKey("dmstItnb") && entry.bidInfo.get("dmstItnb") != null) {
            paramBuilder.append(entry.bidInfo.get("dmstItnb"));
        } else {
            paramBuilder.append("***");
        }

        if (option.equals("PROD")) {
            paramBuilder.append("&pageDivs=G");
        } else if (option.equals("SERV")) {
            paramBuilder.append("&pageDivs=S");
        } else {
            paramBuilder.append("&pageDivs=E");
        }

        String param = paramBuilder.toString();
        Document doc = Jsoup.parse(getResponse(param));
        Elements priceTables = doc.getElementsByAttributeValue("summary", "목록테이블"); // Fetch the price tables
        if (priceTables.size() == 2) {
            parsePriceTable(priceTables.first().getElementsByTag("tbody").first(), where);
        } else {
            Logger.getGlobal().log(Level.WARNING, "Invalid price result from " + entry.bidInfo.get("anmtNumb"));
        }
    }

    private void parsePriceTable(Element priceTable, String where) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("UPDATE dapabidinfo SET ");
        Elements indexCells = priceTable.getElementsByTag("th");
        for (Element indexCell : indexCells) {
            String index = indexCell.text();
            Element priceCell = indexCell.nextElementSibling();
            String price = priceCell.text().replaceAll("[^\\.0123456789]", "");
            Element freqCell = priceCell.nextElementSibling();
            String freq = freqCell.text().replaceAll("[^\\.0123456789]", "");
            sqlBuilder.append("복수" + index + "=" + price + ", 복참" + index + "=" + freq + ", ");
        }
        sqlBuilder.append("완료=1 ");
        sqlBuilder.append(where);
        String sql = sqlBuilder.toString();
        System.out.println(sql);
        st.executeUpdate(sql);
    }

    private String getType() {
        switch (option) {
            case "PROD":
                return "물품";
            case "SERV":
                return "용역";
            case "FACIL":
                return "시설공사";
            default:
                return "";
        }
    }

    public int getTotal() throws IOException, ClassNotFoundException, SQLException {
        String path = NewDapaParser.FACIL_BID_RES;
        String param = "from_date=" + startDate + "&to_date=" + endDate + "&chkMy=1";
        openHttpConnection(path);
        Document doc = Jsoup.parse(getResponse(param));
        JSONObject jsonData = new JSONObject(doc.body().text());
        totalItems = jsonData.getInt("totlCnt");
        path = NewDapaParser.PROD_BID_RES;
        param = "from_date=" + startDate + "&to_date=" + endDate + "&chkMy=1&exct_divs=B";
        openHttpConnection(path);
        doc = Jsoup.parse(getResponse(param));
        jsonData = new JSONObject(doc.body().text());
        totalItems += jsonData.getInt("totlCnt");
        path = NewDapaParser.SERV_BID_RES;
        param = "from_date=" + startDate + "&to_date=" + endDate + "&chkMy=1&exct_divs=A";
        openHttpConnection(path);
        doc = Jsoup.parse(getResponse(param));
        jsonData = new JSONObject(doc.body().text());
        totalItems += jsonData.getInt("totlCnt");

        return totalItems;
    }

    public void setDate(String startDate, String endDate) {
        this.startDate = startDate.replaceAll("-", "");
        this.endDate = endDate.replaceAll("-", "");
    }

    public void setOption(String option) {
        this.option = option;
    }

    public int getCur() {
        return curItem;
    }

    public void manageDifference(String sm, String em) throws SQLException, IOException {
        ArrayList<String> bidNums = new ArrayList<String>();
        ArrayList<String> bidVers = new ArrayList<String>();
        ArrayList<String> idenNums = new ArrayList<String>();
        ArrayList<String> itemNums = new ArrayList<String>();

        String sql = "SELECT 공고번호, 차수, 공사번호, 항목번호 FROM dapabidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 결과=1;";
        System.out.println(sql);
        rs = st.executeQuery(sql);
        while (rs.next()) {
            bidNums.add(rs.getString("공고번호"));
            bidVers.add(rs.getString("차수"));
            idenNums.add(rs.getString("공사번호"));
            itemNums.add(rs.getString("발주기관"));
        }

        String[] types = { "PROD", "SERV", "FACIL" };
        String sd = sm.replaceAll("-", "");
        String ed = em.replaceAll("-", "");
        for (String type : types) {
            String path = "";
            switch (type) {
                case "PROD":
                    path = NewDapaParser.PROD_BID_RES;
                    break;
                case "FACIL":
                    path = NewDapaParser.FACIL_BID_RES;
                    break;
                case "SERV":
                    path = NewDapaParser.SERV_BID_RES;
                    break;
                default:
                    break;
            }

            openHttpConnection(path);
            int page = 1;
            String param = "from_date=" + sd + "&to_date=" + ed + "&chkMy=1";
            if (type.equals("PROD")) {
                param += "&exct_divs=B";
            }
            else if (type.equals("SERV")) {
                param += "&exct_divs=A";
            }

            Document doc = Jsoup.parse(getResponse(param));

            System.out.println(doc.html());

            JSONObject jsonData = new JSONObject(doc.body().text());
            JSONArray dataArray = jsonData.getJSONArray("list");
            List bidEntries = getBidEntriesFromJsonArray(dataArray);
            while (!bidEntries.isEmpty()) {
                for (Object entry : bidEntries) {
                    if (shutdown) {
                        return;
                    }

                    JSONObject jsonEntry = (JSONObject) entry;
                    DapaEntry resEntry = new DapaEntry(jsonEntry);

                    String bidNum = resEntry.bidInfo.get("anmtNumb"); // 공고번호
                    String bidVer = resEntry.bidInfo.get("rqstDegr"); // 차수
                    String idenNum = resEntry.bidInfo.get("dcsnNumb"); // 공사번호
                    String itemNum = resEntry.bidInfo.get("dmstItnb"); // 항목번호
                    if (!Util.isInteger(itemNum)) {
                        itemNum = "***";
                    }

                    String key = bidNum + bidVer + idenNum + itemNum;
                    int i = 0;
                    for ( ; i < bidNums.size(); i++) {
                        String dbKey = bidNums.get(i) + bidVers.get(i) + idenNums.get(i) + itemNums.get(i);
                        if (key.equals(dbKey)) {
                            break;
                        }
                    }

                    bidNums.remove(i);
                    bidVers.remove(i);
                    idenNums.remove(i);
                    itemNums.remove(i);
                }

                // Get new page
                page++;
                param = "from_date=" + sd + "&to_date=" + ed + "&chkMy=1&currentPageNo=" + page;
                if (type.equals("PROD")) {
                    param += "&exct_divs=B";
                }
                else if (type.equals("SERV")) {
                    param += "&exct_divs=A";
                }

                openHttpConnection(path);
                doc = Jsoup.parse(getResponse(param));
                System.out.println(doc.body().text());
                jsonData = new JSONObject(doc.body().text());
                dataArray = jsonData.getJSONArray("list");
                bidEntries = getBidEntriesFromJsonArray(dataArray);
            }
        }

        for (int i = 0; i < bidNums.size(); i++) {
            sql = "DELETE FROM dapabidinfo WHERE 공고번호=\"" + bidNums.get(i) + "\" AND 차수=\"" + bidVers.get(i) + "\" AND "
                    + "공사번호=\"" + idenNums.get(i) + "\" AND 항목번호=\"" + itemNums.get(i) + "\"";
            System.out.println(sql);
            st.executeUpdate(sql);
        }
    }

    public void run() {
        curItem = 0;
        try {
//            if (option.equals("건수차이")) {
//                manageDifference(sd, ed);
//            }

            setOption("PROD");
            if (!shutdown) parseBidData();
            setOption("SERV");
            if (!shutdown) parseBidData();
            setOption("FACIL");
            if (!shutdown) parseBidData();

            if (frame != null) {
                frame.toggleButton();
            }
            if (checkFrame != null) {
                checkFrame.signalFinish();
            }
        } catch (Exception e) {
            Logger.getGlobal().log(Level.WARNING, e.getMessage());
            e.printStackTrace();
            if (frame != null) {
                frame.toggleButton();
            }
            if (checkFrame != null) {
                checkFrame.signalFinish();
            }
        }
    }
}
