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

public class RailnetParser extends Parser {
    final static String NOTI_LIST = "http://ebid.kr.or.kr/eprocm01/IeProcTenderCS?";
    final static String RES_LIST = "http://ebid.kr.or.kr/eprocm01/IeProcConformJudgCS?";

    final static String PRICE_PAGE = "http://ebid.kr.or.kr/eprocm01/IeProcOpenBidCS?cmd=get-f33";

    // For SQL setup.
    Connection db_con;
    java.sql.Statement st;
    ResultSet rs;

    URL url;
    HttpURLConnection con;
    HashMap<String, String> formData;

    String sd;
    String ed;
    String op;
    int totalItems;
    int curItem;

    GetFrame frame;

    public static void main(String args[]) throws ClassNotFoundException, SQLException, IOException {
        RailnetParser rp = new RailnetParser("20160101", "20161231", "공고", null);
        rp.run();
    }

    public RailnetParser(String sd, String ed, String op, GetFrame frame) throws SQLException, ClassNotFoundException {
        this.sd = sd;
        this.ed = ed;
        this.op = op;

        this.frame = frame;

        totalItems = 0;
        curItem = 0;
        formData = new HashMap<String, String>();

        // Set up SQL connection
        db_con = DriverManager.getConnection(
                "jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8",
                Util.DB_ID,
                Util.DB_PW
        );
        st = db_con.createStatement();
        rs = null;
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

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
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
        sd = sd.replaceAll("[^\\d]", "");
        ed = ed.replaceAll("[^\\d]", "");

        if (op.equals("공고")) {
            path = RailnetParser.NOTI_LIST;
            path += "cmd=get-f31&pno=1";
            path += "&fromDate=" + sd;
            path += "&endDate=" + ed;
            path += "&openBidFromDate=&openBidEndDate=&search_state=M&gg_gubun="
                    + "&ic_gonggomyeong=&ic_gubun=&jy_jehan=&todo_gubun=Y&mode=spec";
        }
        else if (op.equals("결과")) {
            path = RailnetParser.RES_LIST;
            path += "cmd=get-f57&pno=1";
            path += "&fromDate=" + sd;
            path += "&endDate=" + ed;
            path += "&str_gg_nyeondo=&gg_gubun=&str_gg_ir_beonho=&str_gg_myeong=&todo_gubun=Y&mode=spec";
        }

        openConnection(path, "GET");
        Document doc = Jsoup.parse(getResponse(null, "GET"));

        Element countDiv = doc.getElementsByClass("bo").first();
        if (op.equals("공고")) {
            String countStr = countDiv.text().split("총건수")[1];
            countStr = countStr.replaceAll("[^\\d]", "");
            totalItems = Integer.parseInt(countStr);
        }
        else if (op.equals("결과")) {
            String countStr = countDiv.nextElementSibling().text().split("총건수")[1];
            countStr = countStr.replaceAll("[^\\d]", "");
            totalItems = Integer.parseInt(countStr);
        }

        Element table = doc.getElementsByTag("table").get(13);
        Elements rows = table.getElementsByTag("tr");
        int index = 1;
        int page = 1;
        curItem = 0;
        for (int i = 0; i < totalItems; i++) {
            curItem++;
            if (shutdown) return;

            Element r = rows.get(index);
            parseListRow(r);

            if (i != 0) {
                if ((i % 10) == 0) {
                    page++;
                    if (op.equals("공고")) {
                        path = RailnetParser.NOTI_LIST;
                        path += "cmd=get-f31&pno=" + page;
                        path += "&fromDate=" + sd;
                        path += "&endDate=" + ed;
                        path += "&openBidFromDate=&openBidEndDate=&search_state=M&gg_gubun="
                                + "&ic_gonggomyeong=&ic_gubun=&jy_jehan=&todo_gubun=Y&mode=spec";
                    }
                    else if (op.equals("결과")) {
                        path = RailnetParser.RES_LIST;
                        path += "cmd=get-f57&pno=" + page;
                        path += "&fromDate=" + sd;
                        path += "&endDate=" + ed;
                        path += "&str_gg_nyeondo=&gg_gubun=&str_gg_ir_beonho=&str_gg_myeong=&todo_gubun=Y&mode=spec";
                    }
                    openConnection(path, "GET");
                    doc = Jsoup.parse(getResponse(null, "GET"));
                    table = doc.getElementsByTag("table").get(13);
                    rows = table.getElementsByTag("tr");
                    index = 1;
                }
                else {
                    index++;
                }
            }
        }
    }

    public void parseListRow(Element row) throws SQLException, IOException {
        boolean enter = true;
        boolean exists = false;
        Elements data = row.getElementsByTag("td");

        if (op.equals("공고")) {
            String bidno = data.get(0).text();
            String reno = bidno.substring(bidno.length() - 2);
            bidno = bidno.substring(0, bidno.length() - 3);
            String expPrice = data.get(2).text();
            expPrice = expPrice.replaceAll("[^\\d]", "");
            if (expPrice.equals("")) expPrice = "0";
            String prog = data.get(5).text();

            if (frame != null) frame.updateInfo(bidno, false);

            String where = "WHERE 공고번호=\"" + bidno + "\" AND 차수=\"" + reno + "\"";

            rs = st.executeQuery("SELECT EXISTS(SELECT 공고번호 FROM railnetbidinfo " + where + ")");
            if (rs.next()) exists = rs.getBoolean(1);

            if (exists) {
                String sql = "SELECT 공고, 공고상태 FROM railnetbidinfo " + where;
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
                        sql = "UPDATE railnetbidinfo SET 공고상태=\"" + prog + "\" " + where;
                        st.executeUpdate(sql);
                    }
                }
                else if (!dbProg.equals(prog)) {
                    sql = "UPDATE railnetbidinfo SET 공고상태=\"" + prog + "\" " + where;
                    st.executeUpdate(sql);
                }
            }
            else {
                // If entry doesn't exists in db, insert new row.
                String sql = "INSERT INTO railnetbidinfo (공고번호, 차수, 설계금액, 공고상태) VALUES (" +
                        "\""+bidno+"\", " +
                        "\""+reno+"\", " +
                        ""+expPrice+", " +
                        "\""+prog+"\");";
                System.out.println(sql);
                st.executeUpdate(sql);
            }
            if (enter) {
                Element link = row.getElementsByTag("a").first();
                String js = link.attr("href");
                js = js.substring(js.indexOf("(")+1, js.indexOf(")"));
                String[] jsParams = js.split(",");

                String url = "http://ebid.kr.or.kr/eprocm01/IeProcTenderCS?popup=pop&cmd=get-f32&gy_er_beonho="+jsParams[0]
                        +"&gg_nyeondo="+jsParams[1].replaceAll("\'", "")
                        +"&gg_gubun="+jsParams[2].replaceAll("\'", "")
                        +"&gg_ir_beonho="+jsParams[3].replaceAll("\'", "")
                        +"&gg_chasu="+jsParams[4].replaceAll("\'", "")
                        +"&ir_beonho="+jsParams[5].replaceAll("\'", "");
                openConnection(url, "GET");
                Document notiPage = Jsoup.parse(getResponse(null, "GET"));
                parseNoti(notiPage);
            }
        }
        else if (op.equals("결과")) {
            String bidno = data.get(0).text();
            String expPrice = data.get(2).text();
            expPrice = expPrice.replaceAll("[^\\d]", "");
            if (expPrice.equals("")) expPrice = "0";
            String openDate = data.get(3).text();
            String result = data.get(4).text();

            if (frame != null) frame.updateInfo(bidno, true);

            String where = "WHERE 공고번호=\"" + bidno + "\"";

            rs = st.executeQuery("SELECT EXISTS(SELECT 공고번호 FROM railnetbidinfo " + where + ")");
            if (rs.next()) exists = rs.getBoolean(1);

            if (exists) {
                String sql = "SELECT 완료, 개찰결과 FROM railnetbidinfo " + where;
                rs = st.executeQuery(sql);

                int finished = 0;
                String dbProg = "";
                if (rs.first()) {
                    finished = rs.getInt(1);
                    dbProg = rs.getString(2) == null ? "" : rs.getString(2);
                }

                if (finished > 0) {
                    if (dbProg.equals(result)) enter = false;
                    else {
                        sql = "UPDATE railnetbidinfo SET 개찰결과=\"" + result + "\" " + where;
                        st.executeUpdate(sql);
                    }
                }
                else if (!dbProg.equals(result)) {
                    sql = "UPDATE railnetbidinfo SET 개찰결과=\"" + result + "\" " + where;
                    st.executeUpdate(sql);
                }
            }
            else {
                // If entry doesn't exists in db, insert new row.
                String sql = "INSERT INTO railnetbidinfo (공고번호, 설계금액, 개찰일시, 개찰결과) VALUES (" +
                        "\""+bidno+"\", " +
                        ""+expPrice+", " +
                        "\""+openDate+"\", " +
                        "\""+result+"\");";
                System.out.println(sql);
                st.executeUpdate(sql);
            }
            if (enter) {
                Element link = row.getElementsByTag("a").first();
                String js = link.attr("href");
                js = js.substring(js.indexOf("(")+1, js.length() - 2);
                System.out.println(js);
                String[] jsParams = js.split(",");

                if (result.equals("개찰")) {
                    String crSangtae = jsParams[5];
                    String gyBangBeob = jsParams[6];
                    String hsTbYeobu = jsParams[7];
                    String icCyYeobu = jsParams[8];
                    String url = "";
                    if (crSangtae.equals("\'13\'") && gyBangBeob.contains("07")) {
                        if (!icCyYeobu.equals("\'N\'")) {
                            if (!hsTbYeobu.equals("\'N\'")) {
                                url = "http://ebid.kr.or.kr/eprocm01/IeProcConformJudgCS?cmd=get-f56&gy_er_beonho="+jsParams[3].replaceAll("\'", "")
                                        +"&gg_nyeondo="+jsParams[0].replaceAll("\'", "")
                                        +"&gg_ir_beonho="+jsParams[1].replaceAll("\'", "")
                                        +"&gg_chasu="+jsParams[2].replaceAll("\'", "")
                                        +"&gg_gubun="+jsParams[4].replaceAll("\'", "")
                                        +"&new_gubun=Y";
                            }
                        }
                        else {
                            String sql = "UPDATE railnetbidinfo SET 계약방법=\"협상에의한계약\", 완료=1 " + where;
                            st.executeUpdate(sql);
                        }
                    }
                    else {
                        if (icCyYeobu.equals("\'N\'")) {
                            url = "http://ebid.kr.or.kr/eprocm01/IeProcOpenBidCS?cmd=get-f34&gy_er_beonho="+jsParams[3].replaceAll("\'", "")
                                    +"&gg_nyeondo="+jsParams[0].replaceAll("\'", "")
                                    +"&gg_ir_beonho="+jsParams[1].replaceAll("\'", "")
                                    +"&gg_chasu="+jsParams[2].replaceAll("\'", "")
                                    +"&gg_gubun="+jsParams[4].replaceAll("\'", "")
                                    +"&new_gubun=Y";
                        }
                        else {
                            url = "http://ebid.kr.or.kr/eprocm01/IeProcConformJudgCS?cmd=get-f56&gy_er_beonho="+jsParams[3].replaceAll("\'", "")
                                    +"&gg_nyeondo="+jsParams[0].replaceAll("\'", "")
                                    +"&gg_ir_beonho="+jsParams[1].replaceAll("\'", "")
                                    +"&gg_chasu="+jsParams[2].replaceAll("\'", "")
                                    +"&gg_gubun="+jsParams[4].replaceAll("\'", "")
                                    +"&new_gubun=Y";
                        }
                    }
                    if (!url.equals("")) {
                        openConnection(url, "GET");
                        Document resPage = Jsoup.parse(getResponse(null, "GET"));
                        parseRes(resPage, result);
                    }
                }
                else {
                    if (jsParams[4].contains("(")) {
                        jsParams[4] = jsParams[4].split("[(]")[0];
                        jsParams[4] = jsParams[4].replaceAll(" ", "");
                    }
                    String url = "http://ebid.kr.or.kr/eprocm01/IeProcConformJudgCS?cmd=get-f53&gy_er_beonho="+jsParams[3]
                            +"&gg_nyeondo="+jsParams[0].replaceAll("\'", "")
                            +"&gg_ir_beonho="+jsParams[1].replaceAll("\'", "")
                            +"&gg_chasu="+jsParams[2].replaceAll("\'", "")
                            +"&sayu="+jsParams[4].replaceAll("\'", "")
                            +"&gg_gubun="+jsParams[5].replaceAll("\'", "");
                    openConnection(url, "GET");
                    Document resPage = Jsoup.parse(getResponse(null, "GET"));
                    parseRes(resPage, result);
                }
            }
        }
    }

    public void parseNoti(Document notiPage) throws SQLException {
        Element infoTable = notiPage.getElementsByTag("table").get(7);
        Elements data = infoTable.getElementsByTag("td");
        String bidno = ""; // 공고번호
        String reno = ""; // 차수
        String priceMethod = ""; // 예가방식
        String criteria = ""; // 심사기준
        String bidRate = ""; // 낙찰하한율
        String deadline = ""; // 입찰서접수마감일시
        String openDate = ""; // 개찰일시

        for (Element d : data) {
            String key = d.text().replaceAll("\u00a0", "");
            if (key.equals("입찰공고번호")) {
                bidno = d.nextElementSibling().text().replaceAll("\u00a0", "");
                reno = bidno.substring(bidno.length() - 2);
                bidno = bidno.substring(0, bidno.length() - 3);
            }
            else if (key.equals("예가방식")) {
                priceMethod = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
            else if (key.equals("심사기준")) {
                criteria = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
            else if (key.equals("낙찰하한율(%)")) {
                bidRate = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
            else if (key.equals("입찰서접수마감일시")) {
                deadline = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
            else if (key.equals("개찰(입찰)일시")) {
                openDate = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
        }

        if (!openDate.equals("")) {
            String where = "WHERE 공고번호=\"" + bidno + "\" AND 차수=\"" + reno + "\"";
            String sql = "UPDATE railnetbidinfo SET 심사기준=\""+criteria+"\", " +
                    "개찰일시=\""+openDate+"\", " +
                    "낙찰하한율=\""+bidRate+"\", " +
                    "입찰서접수마감일시=\""+deadline+"\", " +
                    "예가방식=\""+priceMethod+"\", 공고=1 " + where;
            st.executeUpdate(sql);
        }
    }

    public void parseRes(Document resPage, String result) throws SQLException, IOException {
        Element infoTable = resPage.getElementsByTag("table").get(7);
        Element bidTable = resPage.getElementsByTag("table").get(16);

        Elements data = infoTable.getElementsByTag("td");
        String bidno = ""; // 공고번호
        String reno = ""; // 차수
        String priceMethod = ""; // 예가방식
        String expPrice = ""; // 설계금액
        String planPrice = ""; // 예정가격
        String bidPrice = ""; // 투찰금액
        String compType = ""; // 계약방법
        String bidMethod = ""; // 낙찰자선정방식
        String criteria = ""; // 심사기준
        String bidRate = ""; // 낙찰하한율

        for (Element d : data) {
            String key = d.text().replaceAll("\u00a0", "");
            if (key.equals("입찰공고번호")) {
                bidno = d.nextElementSibling().text().replaceAll("\u00a0", "");
                reno = bidno.substring(bidno.length() - 2);
                bidno = bidno.substring(0, bidno.length() - 3);
            }
            else if (key.equals("예가방식")) {
                priceMethod = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
            else if (key.equals("설계금액")) {
                expPrice = d.nextElementSibling().text().replaceAll("\u00a0", "");
                expPrice = expPrice.replaceAll("[^\\d]", "");
                if (expPrice.equals("")) expPrice = "0";
            }
            else if (key.equals("예정가격")) {
                planPrice = d.nextElementSibling().text().replaceAll("\u00a0", "");
                planPrice = planPrice.replaceAll("[^\\d]", "");
                if (planPrice.equals("")) planPrice = "0";
            }
            else if (key.equals("계약방법")) {
                compType = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
            else if (key.equals("낙찰자선정방식")) {
                bidMethod = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
            else if (key.equals("심사기준")) {
                criteria = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
            else if (key.equals("낙찰하한율(%)")) {
                bidRate = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
        }

        String sql = "SELECT 차수 FROM railnetbidinfo WHERE 공고번호=\"" + bidno + "\"";
        rs = st.executeQuery(sql);
        if (rs.next()) {
            String dbReno = rs.getString("차수");
            if (dbReno == null || dbReno.equals("")) {
                sql = "UPDATE railnetbidinfo SET 차수=\"" + reno + "\" WHERE 공고번호=\"" + bidno + "\"";
                st.executeUpdate(sql);
            }
        }

        if (result.equals("개찰")) {
            Elements linkCheck = resPage.getElementsByAttributeValue("href", "javaScript:getYegaLotRecord();");
            if (linkCheck.size() > 0) {
                Elements inputs = resPage.getElementsByTag("input");
                formData.clear();
                for (Element input : inputs) {
                    formData.put(input.attr("name"), input.val());
                }

                String path = RailnetParser.PRICE_PAGE;
                String param = "";

                Iterator<Entry<String, String>> it = formData.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, String> pair = (Map.Entry<String, String>) it.next();
                    if (pair.getValue() != null) {
                        String val = pair.getValue().toString();
                        param += pair.getKey() + "=" + val + "&";
                    }
                }
                param = param.substring(0, param.length()-1);

                openConnection(path, "POST");
                Document pricePage = Jsoup.parse(getResponse(param, "POST"));
                parsePrice(pricePage);
            }

            Element bidRow = bidTable.getElementsByTag("tr").get(1);
            Element bidCell = bidRow.getElementsByTag("td").get(4);
            bidPrice = bidCell.text().replaceAll("[^\\d]", "");
        }

        if (bidPrice.equals("")) bidPrice = "0";
        if (planPrice.equals("")) planPrice = "0";

        String where = "WHERE 공고번호=\"" + bidno + "\" AND 차수=\"" + reno + "\"";
        sql = "UPDATE railnetbidinfo SET 계약방법=\""+compType+"\", " +
                "설계금액="+expPrice+", " +
                "예정가격="+planPrice+", " +
                "투찰금액="+bidPrice+", " +
                "심사기준=\""+criteria+"\", " +
                "차수=\""+reno+"\", " +
                "낙찰하한율=\""+bidRate+"\", " +
                "낙찰자선정방식=\""+bidMethod+"\", " +
                "예가방식=\""+priceMethod+"\", 완료=1 " + where;
        System.out.println(sql);
        st.executeUpdate(sql);
    }

    public void parsePrice(Document pricePage) throws SQLException {
        Elements tables = pricePage.getElementsByTag("table");

        Element infoTable = tables.get(7);
        Element priceTable = tables.get(16);

        String bidno = "";
        String reno = ""; // 차수
        String demOrg = ""; // 수요기관
        String notiOrg = ""; // 공고기관
        String basePrice = ""; // 기초금액

        Elements data = infoTable.getElementsByTag("td");
        for (Element d : data) {
            String key = d.text().replaceAll("\u00a0", "");
            if (key.equals("입찰공고번호")) {
                bidno = d.nextElementSibling().text().replaceAll("\u00a0", "");
                reno = bidno.substring(bidno.length() - 2);
                bidno = bidno.substring(0, bidno.length() - 3);
            }
            else if (key.equals("수요기관")) {
                demOrg = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
            else if (key.equals("공고기관")) {
                notiOrg = d.nextElementSibling().text().replaceAll("\u00a0", "");
            }
            else if (key.equals("예비가격 기초금액")) {
                basePrice = d.nextElementSibling().text().replaceAll("\u00a0", "");
                basePrice = basePrice.replaceAll("[^\\d]", "");
                if (basePrice.equals("")) basePrice = "0";
            }
        }

        String where = "WHERE 공고번호=\"" + bidno + "\" AND 차수=\"" + reno + "\"";

        Elements priceData = priceTable.getElementsByTag("td");
        for (Element d : priceData) {
            String key = d.text().replaceAll("\u00a0", "");
            if (key.contains("제 ")) {
                key = key.replaceAll("[^\\d]", "");
                String value = d.nextElementSibling().text().replaceAll("\u00a0", "");
                value = value.replaceAll("[^\\d]", "");
                if (value.equals("")) value = "0";

                String sql = "UPDATE railnetbidinfo SET 복수" + key + "=" + value + " " + where;
                st.executeUpdate(sql);
            }
        }

        String sql = "UPDATE railnetbidinfo SET 기초금액=" + basePrice + ", " +
                "수요기관=\"" + demOrg + "\", " +
                "공고기관=\"" + notiOrg + "\" " + where;
        st.executeUpdate(sql);
    }

    public void run() {
        try {
            setOption("공고");
            if (!shutdown) getList();
            setOption("결과");
            if (!shutdown) getList();
            if (frame != null) {
                frame.toggleButton();
            }
        } catch (IOException | SQLException e) {
            Logger.getGlobal().log(Level.WARNING, e.getMessage());
            if (frame != null) {
                frame.toggleButton();
            }

            e.printStackTrace();
        }

    }

    public int getTotal() throws IOException, ClassNotFoundException, SQLException {
        String path = "";
        sd = sd.replaceAll("[^\\d]", "");
        ed = ed.replaceAll("[^\\d]", "");

        path = RailnetParser.RES_LIST;
        path += "cmd=get-f57&pno=1";
        path += "&fromDate=" + sd;
        path += "&endDate=" + ed;
        path += "&str_gg_nyeondo=&gg_gubun=&str_gg_ir_beonho=&str_gg_myeong=&todo_gubun=Y&mode=spec";

        openConnection(path, "GET");
        Document doc = Jsoup.parse(getResponse(null, "GET"));

        Element countDiv = doc.getElementsByClass("bo").first();
        String countStr = countDiv.nextElementSibling().text().split("총건수")[1];
        countStr = countStr.replaceAll("[^\\d]", "");
        totalItems = Integer.parseInt(countStr);

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

    public void manageDifference(String sm, String em) {
    }

}