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
    public static final String BID_ANN_VIEW = "http://www.d2b.go.kr/pdb/bid/bidAnnounceView.do";
    public static final String NEGO_ANN_VIEW = "http://www.d2b.go.kr/pdb/openNego/openNegoPlanView.do";
    public static final String PROD_BID_RES = "http://www.d2b.go.kr/pdb/bid/getBidResultList.json";
    public static final String PROD_NEGO_RES = "http://www.d2b.go.kr/pdb/openNego/openNegoResultView.do";
    public static final String PROD_RES_VIEW = "http://www.d2b.go.kr/pdb/bid/bidResultView.do";
    public static final String PROD_ORG_LIST = "http://www.d2b.go.kr/pdb/bid/getBidRecordList.json";
    public static final String PROD_NEGO_ORG = "http://www.d2b.go.kr/pdb/openNego/getMnufResultList.json";
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
        NewDapaParser parser = new NewDapaParser("20180101", "20180120", "FACIL");
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
        parseResultData();
        parseAnnouncementData();
    }

    private void parseAnnouncementData() throws IOException {
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

        System.out.println(doc.html());

        JSONObject jsonData = new JSONObject(doc.body().text());
        JSONArray dataArray = jsonData.getJSONArray("list");
        if (option.equals("SERV")) {
            totalItems = jsonData.getInt("totalCount");
        } else {
            totalItems = jsonData.getInt("totlCnt");
        }
        List bidEntries = getBidEntriesFromJsonArray(dataArray);
        while (!bidEntries.isEmpty()) {
            for (Object entry : bidEntries) {
                JSONObject jsonEntry = (JSONObject) entry;
                DapaEntry annEntry = new DapaEntry(jsonEntry);
                if (annEntry.bidInfo.get("lv2Divs").equals("2")) {
                    parseNegoAnnEntry(annEntry);
                } else {
                    parseBidAnnEntry(annEntry);
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

    private void parseBidAnnEntry(DapaEntry entry) throws IOException {
        Document doc = Jsoup.parse(getAnnDetails(entry));

        /*
         * Getting the announcement details, including 공고번호, 개찰일시, and 기초예가
         */
        Elements infoDivs = doc.getElementsByAttributeValue("summary", "상세테이블");
        if (!infoDivs.isEmpty()) {
            for (Element infoDiv : infoDivs) {
                Elements headers = infoDiv.getElementsByTag("th");
                for (Element header : headers) {
                    System.out.println(header.nextElementSibling().text());
                }
            }
        } else {
            System.out.println(doc.html());
        }

        String person = parseContactInfo(doc);
        System.out.println(person);
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

    private void parseNegoAnnEntry(DapaEntry entry) throws IOException {
        Document doc = Jsoup.parse(getNegoAnnDetails(entry));
        Element infoDiv = doc.getElementsByClass("post").first();
        if (infoDiv != null) {
            Element infoTable = infoDiv.getElementsByAttributeValue("summary", "상세테이블").first();
            Elements headers = infoTable.getElementsByTag("th");
            for (Element header : headers) {
                Element data = header.nextElementSibling();
                System.out.println(data.text());
            }
        } else {
            System.out.println(doc.html());
        }

        String person = parseContactInfo(doc);
        System.out.println(person);
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

    private String parseContactInfo(Document doc) {
        Element label = null;
        Elements candidates = doc.getElementsContainingOwnText("문의처");
        for (Element candidate : candidates) {
            if (candidate.text().equals("문의처")) {
                label = candidate;
            }
        }

        if (label != null) {
            Element contactDiv = label.nextElementSibling();
            Elements contactData = contactDiv.getElementsByTag("td");
            return contactData.get(1).text();
        }

        return "";
    }

    private void parseResultData() throws IOException {
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
        Document doc = Jsoup.parse(getResponse(param));

        System.out.println(doc.html());

        JSONObject jsonData = new JSONObject(doc.body().text());
        JSONArray dataArray = jsonData.getJSONArray("list");
        totalItems = jsonData.getInt("totlCnt");
        List bidEntries = getBidEntriesFromJsonArray(dataArray);
        while (!bidEntries.isEmpty()) {
            for (Object entry : bidEntries) {
                JSONObject jsonEntry = (JSONObject) entry;
                DapaEntry annEntry = new DapaEntry(jsonEntry);
                if (annEntry.bidInfo.get("lvDivs2").equals("BID")) {
                    parseBidResEntry(annEntry);
                } else {
                    parseNegoResEntry(annEntry);
                }
            }

            // Get new page
            page++;
            param = "from_date=" + startDate + "&to_date=" + endDate + "&chkMy=1&currentPageNo=" + page;
            openHttpConnection(path);
            doc = Jsoup.parse(getResponse(param));
            System.out.println(doc.body().text());
            jsonData = new JSONObject(doc.body().text());
            dataArray = jsonData.getJSONArray("list");
            bidEntries = getBidEntriesFromJsonArray(dataArray);
        }
    }

    private void parseBidResEntry(DapaEntry entry) throws IOException {
        Document doc = Jsoup.parse(getResDetails(entry));

        /*
         * Getting the result details, including 공고번호, 개찰일시, and 입찰결과
         */
        Elements infoDivs = doc.getElementsByAttributeValue("summary", "상세테이블");
        if (!infoDivs.isEmpty()) {
            for (Element infoDiv : infoDivs) {
                Elements headers = infoDiv.getElementsByTag("th");
                for (Element header : headers) {
                    System.out.println(header.nextElementSibling().text());
                }
            }
        } else {
            System.out.println(doc.html());
        }

        if (!entry.bidInfo.get("resultNm").equals("유찰")) {
            HashMap org = getBidOrgDetails(entry);
            if (org != null) {
                System.out.println(org.get("mfkrName"));
            }
        }
    }

    private String getResDetails(DapaEntry entry) throws IOException {
        String path = NewDapaParser.PROD_RES_VIEW;
        if (entry.bidInfo.get("lvDivs1").equals("PEB")) {
            path = NewDapaParser.FACIL_RES_VIEW;
        }

        openHttpConnection(path);
        StringBuilder paramBuilder = new StringBuilder();
        paramBuilder.append("ordr_year="+entry.bidInfo.get("ordrYear"));
        paramBuilder.append("&dprt_code="+entry.bidInfo.get("dprtCode"));
        paramBuilder.append("&dcsn_numb="+entry.bidInfo.get("dcsnNumb"));
        paramBuilder.append("&bidx_date="+entry.bidInfo.get("bidxDate"));
        paramBuilder.append("&anmt_numb="+entry.bidInfo.get("anmtNumb"));
        paramBuilder.append("&rqst_degr="+entry.bidInfo.get("rqstDegr"));
        paramBuilder.append("&anmt_degr="+entry.bidInfo.get("rqstDegr"));
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
            paramBuilder.append("&pageDivs=G6");
        } else if (option.equals("SERV")) {
            paramBuilder.append("&pageDivs=S");
        } else {
            paramBuilder.append("&pageDivs=E");
        }

        String param = paramBuilder.toString();
        return getResponse(param);
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
            if (result.equals("낙찰")) {
                return orgEntry;
            }
        }

        return null;
    }

    private void parseNegoResEntry(DapaEntry entry) throws IOException {
        Document doc = Jsoup.parse(getNegoResDetails(entry));

        /*
         * Getting the result details, including 공고번호, 개찰일시, and 입찰결과
         */
        Elements infoDivs = doc.getElementsByAttributeValue("summary", "상세테이블");
        if (!infoDivs.isEmpty()) {
            for (Element infoDiv : infoDivs) {
                Elements headers = infoDiv.getElementsByTag("th");
                for (Element header : headers) {
                    System.out.println(header.nextElementSibling().text());
                }
            }
        } else {
            System.out.println(doc.html());
        }

        if (!entry.bidInfo.get("resultNm").equals("유찰")) {
            HashMap org = getNegoOrgDetails(entry);
            if (org != null) {
                System.out.println(org.get("mfkrName"));
            }
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

        return null;
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
