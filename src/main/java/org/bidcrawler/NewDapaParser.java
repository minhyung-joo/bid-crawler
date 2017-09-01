package org.bidcrawler;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ravenjoo on 7/22/17.
 */
public class NewDapaParser extends Parser {
    public static final String PROD_ANN_LIST = "http://www.d2b.go.kr/pdb/bid/getGoodsBidAnnounceListNew.json";
    public static final String PROD_ANN_VIEW = "http://www.d2b.go.kr/pdb/bid/bidAnnounceView.do";
    public static final String PROD_NEGO_ANN = "http://www.d2b.go.kr/pdb/openNego/openNegoPlanView.do";
    public static final String PROD_BID_RES = "http://www.d2b.go.kr/pdb/bid/getBidResultList.json";
    public static final String PROD_NEGO_RES = "http://www.d2b.go.kr/pdb/openNego/openNegoResultView.do";
    public static final String PROD_RES_VIEW = "http://www.d2b.go.kr/pdb/bid/bidResultView.do";
    public static final String SERV_BID_ANN = "http://www.d2b.go.kr/psb/bid/getServiceBidAnnounceListNew.json";
    public static final String SERV_BID_RES = "http://www.d2b.go.kr/psb/bid/serviceBidResultList.do?key=608";
    public static final String FACIL_BID_ANN = "http://www.d2b.go.kr/peb/bid/announceList.do?key=541";
    public static final String FACIL_BID_RES = "http://www.d2b.go.kr/peb/bid/bidResultList.do?key=545";

    private class DapaEntry {
        HashMap bidInfo;

        public DapaEntry(JSONObject jsonEntry) {
            bidInfo = new HashMap();
            bidInfo = (HashMap) jsonEntry.toMap();
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

    private GetFrame frame;

    public static void main(String[] args) {
        NewDapaParser parser = new NewDapaParser("20170701", "20170710", "PROD");

        try {
            parser.parseBidData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NewDapaParser(String startDate, String endDate, String option) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.option = option;
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

    public void parseBidData() throws IOException {
        parseAnnouncementData();
        parseResultData();
    }

    private void parseAnnouncementData() throws IOException {
        getAnnouncementList();
    }

    private void parseResultData() {

    }

    private void getAnnouncementList() throws IOException {
        switch (option) {
            case "PROD":
                getProductAnnouncementList();
                break;
            default:
                break;
        }
    }

    private void getProductAnnouncementList() throws IOException {
        String path = NewDapaParser.PROD_ANN_LIST;
        openHttpConnection(path);
        int page = 1;
        String param = "date_divs=1&date_from=" + startDate + "&date_to=" + endDate + "&numb_divs=1&exct_divs=B&currentPageNo=" + page;
        Document doc = Jsoup.parse(getResponse(param));

        System.out.println(doc.body().text());

        JSONObject jsonData = new JSONObject(doc.body().text());
        JSONArray dataArray = jsonData.getJSONArray("list");
        totalItems = jsonData.getInt("totlCnt");
        List bidEntries = getBidEntriesFromJsonArray(dataArray);
        while (!bidEntries.isEmpty()) {
            for (Object entry : bidEntries) {
                JSONObject jsonEntry = (JSONObject) entry;
                DapaEntry annEntry = new DapaEntry(jsonEntry);
                if (annEntry.bidInfo.get("lv2Divs").equals("2")) {
                    parseProdNegoAnnEntry(annEntry);
                } else {
                    parseProdBidAnnEntry(annEntry);
                }
            }

            // Get new page
            page++;
            param = "date_divs=1&date_from=" + startDate + "&date_to=" + endDate + "&numb_divs=1&exct_divs=B&currentPageNo=" + page;
            openHttpConnection(path);
            doc = Jsoup.parse(getResponse(param));
            System.out.println(doc.body().text());
            jsonData = new JSONObject(doc.body().text());
            dataArray = jsonData.getJSONArray("list");
            bidEntries = getBidEntriesFromJsonArray(dataArray);
        }
    }

    private List getBidEntriesFromJsonArray(JSONArray dataArray) {
        List entries = new ArrayList<JSONObject>();

        for (int i = 0; i < dataArray.length(); i++) {
            entries.add(dataArray.getJSONObject(i));
        }

        return entries;
    }

    private void parseProdBidAnnEntry(DapaEntry entry) throws IOException {
        String path = NewDapaParser.PROD_ANN_VIEW;
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
        paramBuilder.append("&pageDivs=G1&bid_divs=bid");
        paramBuilder.append("&searchData={");
        paramBuilder.append("\"date_divs\":\"1\",");
        paramBuilder.append("\"date_from\":\"" + startDate + "\",");
        paramBuilder.append("\"date_to\":\"" + endDate + "\",");
        paramBuilder.append("\"search_divs\":\"\",");
        paramBuilder.append("\"anmt_divs\":\"\",");
        paramBuilder.append("\"dprt_name\":\"\",");
        paramBuilder.append("\"dprt_code\":\"\",");
        paramBuilder.append("\"edix_gtag\":\"\",");
        paramBuilder.append("\"anmt_name\":\"\",");
        paramBuilder.append("\"search_numb\":\"\",");
        paramBuilder.append("\"currentPageNo\":\"1\",");
        paramBuilder.append("\"numb_divs\":\"1\",");
        paramBuilder.append("\"exct_divs\":\"B\"");
        paramBuilder.append("}");

        String param = paramBuilder.toString();
        Document doc = Jsoup.parse(getResponse(param));
        Element infoDiv = doc.getElementsByClass("post").first();
        if (infoDiv != null) {
            System.out.println(infoDiv.html());
            Elements headers = infoDiv.getElementsByTag("th");
            if (headers != null) {
                for (Element header : headers) {
                    //System.out.println(header.text());
                }
            }
        } else {
            System.out.println(doc.html());
        }
    }

    private void parseProdNegoAnnEntry(DapaEntry entry) throws IOException {
        String path = NewDapaParser.PROD_NEGO_ANN;
        openHttpConnection(path);
        StringBuilder paramBuilder = new StringBuilder();
        paramBuilder.append("dmst_itnb=***");
        paramBuilder.append("&dcsn_numb=" + entry.bidInfo.get("dcsnNumb"));
        paramBuilder.append("&negn_pldt=" + entry.bidInfo.get("negnCldt"));
        paramBuilder.append("&dprt_code=" + entry.bidInfo.get("dprtCode"));
        paramBuilder.append("&ordr_year=" + entry.bidInfo.get("rqstYear"));
        paramBuilder.append("&negn_degr=" + entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&anmt_numb=" + entry.bidInfo.get("anmtNumb"));
        paramBuilder.append("&pageDivs=G&list_url=%2Fpdb%2Fbid%2FgoodsBidAnnounceList.do");

        String param = paramBuilder.toString();
        Document doc = Jsoup.parse(getResponse(param));
        Element infoDiv = doc.getElementsByClass("post").first();
        if (infoDiv != null) {
            Element infoTable = infoDiv.getElementsByAttributeValue("summary", "상세테이블").first();
            Elements headers = infoTable.getElementsByTag("th");
            for (Element header : headers) {
                Element data = header.nextElementSibling();
                System.out.println(data.text());
            }
            Element miscTable = infoDiv.getElementsByAttributeValue("summary", "목록테이블").first();
            Elements miscBodies = miscTable.getElementsByTag("tbody");

        } else {
            System.out.println(doc.html());
        }
    }

    private void parseProdBidResEntry() {

    }

    private void getServiceAnnouncementList() {
        String path = NewDapaParser.SERV_BID_ANN;
    }

    private void getFacilityAnnouncementList() {
        String path = NewDapaParser.FACIL_BID_ANN;
    }

    public int getTotal() throws IOException, ClassNotFoundException, SQLException {
        return totalItems;
    }

    public void setDate(String startDate, String endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public int getCur() {
        return curItem;
    }

    public void manageDifference(String sm, String em) throws SQLException, IOException {

    }

    public void run() {

    }
}
