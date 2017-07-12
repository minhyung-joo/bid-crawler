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
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bidcrawler.utils.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class NewLHParser extends Parser {

    // For SQL setup.
    Connection db_con;
    java.sql.Statement st;
    ResultSet rs;

    // For HTTP Connection
    URL url;
    HttpURLConnection con;

    final static String BID_ANN = "http://m.ebid.lh.or.kr/ebid.mo.tp.cmd.MobileBidMasterListCmd.dev";
    final static String CONST_NOTI = "http://m.ebid.lh.or.kr/ebid.mo.tp.cmd.MobileBidConstructDetailListCmd.dev";
    final static String SERV_NOTI = "http://m.ebid.lh.or.kr/ebid.mo.tp.cmd.MobileBidsrvcsDetailListCmd.dev";
    final static String MISC_NOTI = "http://m.ebid.lh.or.kr/ebid.mo.tp.cmd.MobileBidctrctgdsDetailListCmd.dev";
    final static String ITEM_NOTI = "http://m.ebid.lh.or.kr/ebid.mo.tp.cmd.MobileBidgdsDetailListCmd.dev";

    final static String BID_RES = "http://m.ebid.lh.or.kr/ebid.mo.ts.cmd.MobileTenderOpenListCmd.dev";
    final static String NORMAL_RES = "http://m.ebid.lh.or.kr/ebid.mo.ts.cmd.MobileTenderOpenDetailCmd.dev";
    final static String NEGO_RES = "http://m.ebid.lh.or.kr/ebid.mo.ts.cmd.MobileAllottblScningPrioritytDetailCmd.dev";
    final static String TECH_RES = "http://m.ebid.lh.or.kr/ebid.mo.ts.cmd.MobileTknlgPriPriorityDetailCmd.dev";

    String sd; // Start date of search. Could be blank.
    String ed; // End date of search. Could be blank.
    String op; // Option for stating which page you want to parse.
    int totalItems;
    int curItem = 0;
    GetFrame frame;

    public NewLHParser(String sd, String ed, String op, GetFrame frame) throws ClassNotFoundException, SQLException {
        this.sd = sd;
        this.ed = ed;
        this.op = op;

        this.frame = frame;

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
        NewLHParser test = new NewLHParser("2017/02/10", "2017/03/20", "", null);

        //System.out.println(test.getTotal());
        test.getNoti();
        test.getRes();
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

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public void getNoti() throws IOException, SQLException {
        String path = NewLHParser.BID_ANN;
        String param = "gubun=Y";
        param += "&s_tndrdocAcptOpenDtm=" + sd;
        param += "&s_tndrdocAcptEndDtm=" + ed;
        openConnection(path, "POST");
        Document doc = Jsoup.parse(getResponse(param, "POST")); // Get the HTML

        Element dataTable = doc.getElementsByClass("search-result").first(); // Get the search result box
        Elements links = dataTable.getElementsByAttribute("onmouseover"); // Filter out the links
        int row = 1;
        Iterator<Element> iter = links.iterator();
        while (iter.hasNext()) {
            if (shutdown) return;

            Element i = iter.next();

            String values = i.attr("onclick").split("[(]")[1];
            values = values.substring(0, values.length() - 2); // Parse the post parameters from the JavaScript call

            String[] keys = values.split(",");
            String bidNum = keys[0].substring(1, keys[0].length() - 1);
            String bidDegree = keys[1].substring(2, keys[1].length() - 1);
            String job = keys[2].substring(2, keys[2].length() - 1);
            String emrgncy = keys[3].substring(2, keys[3].length() - 1);

            if (frame != null) frame.updateInfo(bidNum, false);

            HashMap<String, String> info = new HashMap<String, String>();
            Elements data = i.getElementsByTag("li");
            for (int index = 1; index < data.size(); index++) {
                Element d = data.get(index);
                System.out.println(d.html());
                String[] segments = d.text().split(":");
                String key = segments[0];
                String value = segments[1].trim();
                if (key.equals("ÀÔÂûž¶°šÀÏÀÚ")) {
                    value += ":" + segments[2];
                }
                info.put(key, value);
            }

            boolean exists = false;
            boolean enter = true;
            String where = "WHERE °ø°í¹øÈ£=\"" + bidNum + "\"";
            String sql = "SELECT EXISTS(SELECT °ø°í¹øÈ£ FROM lhbidinfo " + where + ")";
            rs = st.executeQuery(sql);
            if (rs.first()) exists = rs.getBoolean(1);

            if (exists) {
                System.out.println(bidNum + " exists.");
                sql = "SELECT °ø°í, °ø°íÇöÈ² FROM lhbidinfo " + where;
                rs = st.executeQuery(sql);
                int finished = 0;
                String dbProg = "";
                if (rs.first()) {
                    finished = rs.getInt(1);
                    dbProg = rs.getString(2) == null ? "" : rs.getString(2);
                }
                if (finished > 0) {
                    if (dbProg.equals(info.get("ÁøÇà»óÅÂ"))) enter = false;
                    else {
                        sql = "UPDATE lhbidinfo SET °ø°íÇöÈ²=\"" + info.get("ÁøÇà»óÅÂ") + "\" " + where;
                        st.executeUpdate(sql);
                    }
                }
            }
            else {
                sql = "INSERT INTO lhbidinfo (°ø°í¹øÈ£, Ÿ÷¹«, ºÐ·ù, °èŸà¹æ¹ý, ÀÔÂûž¶°šÀÏÀÚ, Áö¿ªº»ºÎ, °ø°íÇöÈ²) VALUES (" +
                        "\"" + bidNum + "\", " +
                        "\"" + info.get("Ÿ÷¹«") + "\", " +
                        "\"" + info.get("ºÐ·ù") + "\", " +
                        "\"" + info.get("°èŸà¹æ¹ý") + "\", " +
                        "\"" + info.get("ÀÔÂûž¶°šÀÏÀÚ") + "\", " +
                        "\"" + info.get("Áö¿ªº»ºÎ") + "\", " +
                        "\"" + info.get("ÁøÇà»óÅÂ") + "\");";
                System.out.println(sql);
                st.executeUpdate(sql);
            }

            if (enter) {
                String itemPath = "";
                if (job.equals("10")) itemPath = NewLHParser.CONST_NOTI;
                else if (job.equals("20")) itemPath = NewLHParser.SERV_NOTI;
                else if (job.equals("30")) itemPath = NewLHParser.ITEM_NOTI;
                else if (job.equals("40")) itemPath = NewLHParser.MISC_NOTI;

                String itemParam = "bidNum=" + bidNum;
                itemParam += "&bidDegree=" + bidDegree;
                itemParam += "&cstrtnJobGbCd=" + job;
                itemParam += "&emrgncyOrder=" + emrgncy;

                openConnection(itemPath, "POST");
                Document itemPage = Jsoup.parse(getResponse(itemParam, "POST"));
                parseItemPage(itemPage, where);
            }

            row++;
            if (row % 10 == 1) {
                // Check if this is the last page
                Element pageDiv = doc.getElementsByClass("paging").first();
                Element pageLastElement = pageDiv.children().last();
                if (pageLastElement.tagName().equals("span")) {
                    break;
                }

                // Get the next page
                String newParam = param + "&targetRow=" + row;

                openConnection(path, "POST");
                doc = Jsoup.parse(getResponse(newParam, "POST"));
                dataTable = doc.getElementsByClass("search-result").first();
                links = dataTable.getElementsByAttribute("onmouseover");
                iter = links.iterator();
            }
        }
    }

    public void parseItemPage(Document itemPage, String where) throws SQLException {
        Elements headers = itemPage.getElementsByTag("th");

        boolean hasBase = false;
        HashMap<String, String> info = new HashMap<String, String>();
        for (Element h : headers) {
            String key = h.text();
            if (h.nextElementSibling() == null) continue;
            String value = h.nextElementSibling().text();
            if (key.equals("¿ë¿ªÀ¯Çü") || key.equals("°ø»çÁŸ·ù")) {
                key = "Ÿ÷ÁŸÀ¯Çü";
            }
            else if (key.equals("°³ÂûÀÏœÃ")) {
                value += ":00";
            }
            else if (key.equals("±âÃÊ±ÝŸ×")) {
                value = value.split(" ")[0];
                value = value.replaceAll(",", "");
                value = value.replaceAll("¿ø", "");
                if (!Util.isInteger(value)) value = "0";
                hasBase = true;
            }
            else if (key.equals("Œ³°è°¡°Ý")) {
                value = value.split(" ")[0];
                value = value.replaceAll(",", "");
                value = value.replaceAll("¿ø", "");
                if (!Util.isInteger(value)) value = "0";
            }
            info.put(key, value);
        }
        String sql = "UPDATE lhbidinfo SET Ÿ÷ÁŸÀ¯Çü=\"" + info.get("Ÿ÷ÁŸÀ¯Çü") + "\", "
                + "ÀÔÂû¹æ¹ý=\"" + info.get("ÀÔÂû¹æ¹ý") + "\", "
                + "ÀÔÂû¹æœÄ=\"" + info.get("ÀÔÂû¹æœÄ") + "\", "
                + "³«ÂûÀÚŒ±Á€¹æ¹ý=\"" + info.get("³«ÂûÀÚŒ±Á€¹æ¹ý") + "\", "
                + "ÀçÀÔÂû=\"ÀçÀÔÂû ŸøÀœ\", "
                + "°³ÂûÀÏœÃ=\"" + info.get("°³ÂûÀÏœÃ") + "\", ";
        if (hasBase) sql += "±âÃÊ±ÝŸ×=" + info.get("±âÃÊ±ÝŸ×") + ", ";
        else sql += "±âÃÊ±ÝŸ×=" + info.get("Œ³°è°¡°Ý") + ", ";
        sql += "°ø°í=1 " + where;

        System.out.println(sql);
        st.executeUpdate(sql);
    }

    public void getRes() throws IOException, SQLException {
        curItem = 0;

        String path = NewLHParser.BID_RES;
        String param = "gubun=Y";
        param += "&s_openDtm1=" + sd;
        param += "&s_openDtm2=" + ed;
        openConnection(path, "POST");
        Document doc = Jsoup.parse(getResponse(param, "POST")); // Get the HTML

        Element dataTable = doc.getElementsByClass("search-result").first(); // Get the search result box
        Elements links = dataTable.getElementsByAttribute("onmouseover"); // Filter out the links
        int row = 1;
        Iterator<Element> iter = links.iterator();
        while (iter.hasNext()) {
            if (shutdown) return;

            Element i = iter.next();

            curItem++;

            String values = i.attr("onclick").split("[(]")[1];
            values = values.substring(0, values.length() - 2); // Parse the post parameters from the JavaScript call

            String[] keys = values.split(",");
            String bidNum = keys[0].substring(1, keys[0].length() - 1);
            String bidDegree = keys[1].substring(2, keys[1].length() - 1);
            String protocol = keys[2].substring(2, keys[2].length() - 1);
            String tndrCtrctMedCd = keys[4].substring(2, keys[4].length() - 1);
            String tenderopenCnt = keys[5].substring(2, keys[5].length() - 1);

            if (frame != null) frame.updateInfo(bidNum, true);

            boolean exists = false;
            boolean enter = true;
            HashMap<String, String> info = new HashMap<String, String>();
            Elements data = i.getElementsByTag("li");
            for (int index = 1; index < data.size(); index++) {
                Element d = data.get(index);
                String[] segments = d.text().split(":");
                String key = segments[0];
                String value = segments[1].trim();
                if (key.equals("°³Âûž¶°šÀÏœÃ")) {
                    value += ":" + segments[2];
                }
                System.out.println(key + " : " + value);
                info.put(key, value);
            }

            String where = "WHERE °ø°í¹øÈ£=\"" + bidNum + "\"";

            rs = st.executeQuery("SELECT EXISTS(SELECT °ø°í¹øÈ£ FROM lhbidinfo " + where + ")");
            if (rs.first()) exists = rs.getBoolean(1);

            if (exists) {
                rs = st.executeQuery("SELECT ¿Ï·á, °³Âû³»¿ª FROM lhbidinfo " + where);
                int finished = 0;
                String dbResult = "";
                if (rs.first()) {
                    finished = rs.getInt(1);
                    dbResult = rs.getString(2) == null ? "" : rs.getString(2);
                }
                if (finished > 0) {
                    System.out.println(bidNum + " exists and " + dbResult);
                    if (dbResult.equals(info.get("°³Âû³»¿ª")))	enter = false;
                    else {
                        String sql = "UPDATE lhbidinfo SET °³Âû³»¿ª=\"" + info.get("°³Âû³»¿ª") + "\" " + where;
                        st.executeUpdate(sql);
                    }
                }
            }
            else {
                String sql = "INSERT INTO lhbidinfo (°ø°í¹øÈ£, Ÿ÷¹«, ºÐ·ù, °³ÂûÀÏœÃ, °³Âû³»¿ª) VALUES (" +
                        "\"" + bidNum + "\", " +
                        "\"" + info.get("Ÿ÷¹«±žºÐ") + "\", " +
                        "\"" + info.get("ºÐ·ù") + "\", " +
                        "\"" + info.get("°³Âûž¶°šÀÏœÃ") + "\", " +
                        "\"" + info.get("°³Âû³»¿ª") + "\");";
                System.out.println(sql);
                st.executeUpdate(sql);
                if (info.get("°³Âû³»¿ª").equals("À¯Âû") || info.get("°³Âû³»¿ª").equals("ºñ°ø°³")) {
                    st.executeUpdate("UPDATE lhbidinfo SET ¿Ï·á=1 " + where);
                    enter = false;
                }
            }

            if (enter && protocol.equals("Y") && Integer.parseInt(tenderopenCnt) > 0){
                String itemPath = "";
                String type = "";
                if (tndrCtrctMedCd.equals("70")){
                    itemPath = NewLHParser.NEGO_RES; //Çù»ó¿¡ÀÇÇÑ°èŸà
                    type = NewLHParser.NEGO_RES;
                }
                else if (tndrCtrctMedCd.equals("90")) {
                    itemPath = NewLHParser.TECH_RES; //±âŒú°¡°ÝºÐž®ÀÔÂû
                    type = NewLHParser.TECH_RES;
                }
                else {
                    itemPath = NewLHParser.NORMAL_RES; //ÀÏ¹Ý
                    type = NewLHParser.NORMAL_RES;
                }

                String itemParam = "bidNum=" + bidNum;
                itemParam += "&bidDegree=" + bidDegree;

                openConnection(itemPath, "POST");
                Document itemPage = Jsoup.parse(getResponse(itemParam, "POST"));
                parseResPage(itemPage, where, type);
            }

            row++;
            if (row % 10 == 1) {
                // Check if this is the last page
                Element pageDiv = doc.getElementsByClass("paging").first();
                Element pageLastElement = pageDiv.children().last();
                if (pageLastElement.tagName().equals("span")) {
                    break;
                }

                // Get the next page
                String newParam = param + "&targetRow=" + row;

                openConnection(path, "POST");
                doc = Jsoup.parse(getResponse(newParam, "POST"));
                dataTable = doc.getElementsByClass("search-result").first();
                links = dataTable.getElementsByAttribute("onmouseover");
                iter = links.iterator();
            }
        }
    }

    public void parseResPage(Document itemPage, String where, String type) throws SQLException {
        Elements infoDivs = itemPage.getElementsByClass("box-table");
        Elements resDiv = itemPage.getElementsByClass("search-result");

        Element infoTable = infoDivs.get(0).getElementsByTag("table").first();

        HashMap<String, String> info = new HashMap<String, String>();
        Elements data = infoTable.getElementsByTag("th");
        for (Element d : data) {
            String key = d.text();
            String value = d.nextElementSibling().text().trim();
            if (key.equals("Œ³°è°¡°Ý") || key.equals("±âÃÊ±ÝŸ×") || key.equals("¿¹Á€°¡°Ý")) {
                value = value.split(" ")[0];
                value = value.replaceAll(",", "");
                value = value.replaceAll("¿ø", "");
                if (!Util.isInteger(value)) value = "0";
            }
            info.put(key, value);
        }

        if (infoDivs.size() > 1) {
            Element priceTable = infoDivs.get(1).getElementsByTag("table").first();

            ArrayList<String> dupPrices = new ArrayList<String>();
            ArrayList<String> dupCounts = new ArrayList<String>();
            ArrayList<String> chosen = new ArrayList<String>();
            Elements priceRows = priceTable.getElementsByTag("tr");
            for (int i = 1; i < priceRows.size(); i++) {
                Elements priceData = priceRows.get(i).getElementsByTag("td");

                String dupPrice = priceData.get(1).text();
                dupPrice = dupPrice.replaceAll(",", "");
                String dupCount = priceData.get(2).text();

                if (!priceData.get(1).attr("style").equals("")) { // Price is highlighted
                    chosen.add(dupPrice);
                }
                dupPrices.add(dupPrice);
                dupCounts.add(dupCount);
            }

            long expPrice = 0L;
            int companies = 0;
            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE lhbidinfo SET ");
            for (int i = 1; i <= dupPrices.size(); i++) {
                sb.append("º¹Œö" + i + "=" + dupPrices.get(i-1) + ", º¹Âü" + i + "=" + dupCounts.get(i-1) + ", ");
                companies += Integer.parseInt(dupCounts.get(i-1));
            }
            for (int i = 1; i <= chosen.size(); i++) {
                sb.append("Œ±ÅÃ°¡°Ý" + i + "=" + chosen.get(i-1) + ", ");
                expPrice += Long.parseLong(chosen.get(i-1));
            }
            if ( (expPrice % 4) > 0 ) {
                expPrice = (expPrice / 4) + 1;
            }
            else expPrice = expPrice / 4;
            companies = companies / 2;
            sb.append("Âü°¡Œö=" + companies + ", ¿¹Á€±ÝŸ×=" + expPrice + " " + where);

            String sql = sb.toString();
            System.out.println(sql);
            st.executeUpdate(sql);
        }

        if (!resDiv.isEmpty()) {
            Elements lists = resDiv.first().getElementsByClass("detail-list");

            for (Element list : lists) {
                Elements listData = list.getElementsByTag("li");
                if (type.equals(NewLHParser.NORMAL_RES)) {
                    if (!listData.get(6).text().contains("³«ÂûÇÏÇÑÀ²¹Ìžž")) {
                        String bidPrice = listData.get(3).text().split(":")[1].trim();
                        bidPrice = bidPrice.replaceAll(",", "");
                        String sql = "UPDATE lhbidinfo SET ÅõÂû±ÝŸ×=" + bidPrice + " " + where;
                        System.out.println(sql);
                        st.executeUpdate(sql);
                        break;
                    }
                }
                else {
                    String bidPrice = listData.get(4).text().split(":")[1].trim();
                    bidPrice = bidPrice.replaceAll(",", "");
                    String sql = "UPDATE lhbidinfo SET ÅõÂû±ÝŸ×=" + bidPrice + " " + where;
                    System.out.println(sql);
                    st.executeUpdate(sql);
                    break;
                }
            }
        }

        String sql = "UPDATE lhbidinfo SET ±âÃÊ±ÝŸ×=" + info.get("±âÃÊ±ÝŸ×") + ", ±âÁž¿¹Á€°¡°Ý=" + info.get("¿¹Á€°¡°Ý") + ", ¿Ï·á=1 " + where;
        System.out.println(sql);
        st.executeUpdate(sql);
    }

    public void run() {
        try {
            setOption("°ø°í");
            if (!shutdown) getNoti();
            setOption("°á°ú");
            if (!shutdown) getRes();
        } catch (IOException | SQLException e) {
            Logger.getGlobal().log(Level.WARNING, e.getMessage());
            e.printStackTrace();
        }
    }

    public int getTotal() throws IOException, ClassNotFoundException, SQLException {
        String path = NewLHParser.BID_RES;
        String param = "gubun=Y";
        param += "&s_openDtm1=" + sd;
        param += "&s_openDtm2=" + ed;
        openConnection(path, "POST");
        Document doc = Jsoup.parse(getResponse(param, "POST"));

        int row = 1;
        Element pageDiv = doc.getElementsByClass("paging").first();
        Elements pages = pageDiv.getElementsByTag("a");
        if (!pages.isEmpty()) {
            int r = Integer.parseInt(pages.last().attr("onclick").replaceAll("[^\\d]", ""));
            while (r > row && !pages.isEmpty()) {
                row = r;
                String newParam = param + "&targetRow=" + row;

                openConnection(path, "POST");
                doc = Jsoup.parse(getResponse(newParam, "POST"));

                pageDiv = doc.getElementsByClass("paging").first();
                pages = pageDiv.getElementsByTag("a");
                r = Integer.parseInt(pages.last().attr("onclick").replaceAll("[^\\d]", ""));
            }
        }

        Element dataTable = doc.getElementsByClass("search-result").first(); // Get the search result box
        Elements links = dataTable.getElementsByAttribute("onmouseover"); // Filter out the links
        if (links.size() > 0) row += (links.size() - 1);
        return row;
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

    }

}