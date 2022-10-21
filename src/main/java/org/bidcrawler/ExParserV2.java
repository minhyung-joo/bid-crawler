package org.bidcrawler;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExParserV2 extends Parser {
    public static final String ANN_LIST = "https://ebid.ex.co.kr/ui/sp/expro/bidnoti/findListBidNoti.do";

    private URL url;
    private HttpsURLConnection con;
    private String cookie;

    private String startDate;
    private String endDate;
    private String option;
    private int totalItems;
    private int curItem;
    private GetFrame frame;
    private CheckFrame checkFrame;

    public ExParserV2(String startDate, String endDate, String option, GetFrame frame, CheckFrame checkFrame)
            throws SQLException
    {
        this.startDate = startDate.replaceAll("-", "");
        this.endDate = endDate.replaceAll("-", "");
        this.option = option;
    }

    public void openHttpConnection(String path, String method) throws IOException {
        url = new URL(path);
        con = (HttpsURLConnection) url.openConnection();

        con.setRequestMethod(method);
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.6,en;q=0.4");
        con.setRequestProperty("Origin", " https://ebid.ex.co.kr");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        if (cookie != null) {
            con.setRequestProperty("Cookie", cookie);
        }
    }

    public String getResponse(String param) throws IOException {
        if (param != null && !param.equals("")) {
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(param);
            wr.flush();
            wr.close();
        }

        int responseCode = con.getResponseCode();
        System.out.println("\nSending POST request to URL : " + url);
        System.out.println("Post parameters : " + param);
        System.out.println("Response Code : " + responseCode);
        if (cookie == null) {
            String visitorId = null;
            String jsession = null;
            for (Map.Entry<String, List<String>> entry : con.getHeaderFields().entrySet()) {
                String key = entry.getKey();
                List<String> value = entry.getValue();
                if (key != null && key.equals("Set-Cookie")) {
                    for (int i = 0; i < value.size(); i++) {
                        String cookiePart = value.get(i);
                        if (cookiePart.contains("__smVisitorID")) {
                            visitorId = cookiePart.split(";")[0];
                        } else if (cookiePart.contains("JSESSIONID")) {
                            jsession = cookiePart.split(";")[0];
                        }
                    }
                }
            }

            if (visitorId != null && jsession != null) {
                cookie = visitorId + "; " + jsession;
            }
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    @Override
    public int getTotal() throws IOException, ClassNotFoundException, SQLException {
        return 0;
    }

    @Override
    public void setDate(String sd, String ed) {

    }

    @Override
    public void setOption(String op) {

    }

    @Override
    public int getCur() {
        return 0;
    }

    @Override
    public void manageDifference(String sm, String em) throws SQLException, IOException {

    }

    @Override
    public void run() {
        curItem = 0;
        try {
            if (option.equals("건수차이")) {
                String sd = this.startDate.substring(0, 4) + "-" + this.startDate.substring(4, 6) + "-" + this.startDate.substring(6, 8);
                String ed = this.endDate.substring(0, 4) + "-" + this.endDate.substring(4, 6) + "-" + this.endDate.substring(6, 8);
                manageDifference(sd, ed);
            } else {
                getCookie();
                if (!shutdown) parseBidData();
            }

            if (frame != null && !shutdown) {
                frame.toggleButton();
            }
            if (checkFrame != null && !shutdown) {
                checkFrame.signalFinish();
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            Logger.getGlobal().log(Level.WARNING, sStackTrace);
            e.printStackTrace();
            if (frame != null && !shutdown) {
                frame.toggleButton();
            }
            if (checkFrame != null && !shutdown) {
                checkFrame.signalFinish();
            }
        }
    }

    private void getCookie() throws IOException {
        String path = "https://ebid.ex.co.kr/default.do";
        openHttpConnection(path, "GET");
        getResponse("");
    }

    public void parseBidData() throws IOException, SQLException {
        String[] options = {"FACIL", "PROD", "SERV"};
        for (int i = 0; i < options.length; i++) {
            parseAnnouncementData(options[i]);
            parseResultData(options[i]);
        }
    }

    private void parseAnnouncementData(String option) throws IOException {
        String path = ExParserV2.ANN_LIST;
        openHttpConnection(path, "POST");
        int page = 1;

        //todo: COPY HEADER AND JSON FIELDS EXACTLY
        JSONObject paramMap = new JSONObject();
        paramMap.put("from_noti_date", startDate);
        paramMap.put("to_noti_date", endDate);
        paramMap.put("status", "E");
        paramMap.put("arr_status", new String[] {"EY", "GY", "FY"});
        if (option.equals("FACIL")) {
            paramMap.put("noti_cls", "CT");
        } else if (option.equals("PROD")) {
            paramMap.put("noti_cls", "MT");
        } else if (option.equals("SERV")) {
            paramMap.put("noti_cls", "SV");
        }
        paramMap.put("page", "noti");
        JSONArray response = new JSONArray(getResponse(paramMap.toString()));
        System.out.println(response.get(0));
    }

    private void parseResultData(String option) {
    }
}
