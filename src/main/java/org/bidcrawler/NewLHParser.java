package org.bidcrawler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bidcrawler.utils.Util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



public class NewLHParser
        extends Parser
{
    Connection db_con;
    Statement st;
    ResultSet rs;
    URL url;
    HttpURLConnection con;
    static final String BID_ANN = "https://m.ebid.lh.or.kr:8443/ebid.mo.tp.cmd.MobileBidMasterListCmd.dev";
    static final String CONST_NOTI = "https://m.ebid.lh.or.kr:8443/ebid.mo.tp.cmd.MobileBidConstructDetailListCmd.dev";
    static final String SERV_NOTI = "https://m.ebid.lh.or.kr:8443/ebid.mo.tp.cmd.MobileBidsrvcsDetailListCmd.dev";
    static final String MISC_NOTI = "https://m.ebid.lh.or.kr:8443/ebid.mo.tp.cmd.MobileBidctrctgdsDetailListCmd.dev";
    static final String ITEM_NOTI = "https://m.ebid.lh.or.kr:8443/ebid.mo.tp.cmd.MobileBidgdsDetailListCmd.dev";
    static final String BID_RES = "https://m.ebid.lh.or.kr:8443/ebid.mo.ts.cmd.MobileTenderOpenListCmd.dev";
    static final String NORMAL_RES = "https://m.ebid.lh.or.kr:8443/ebid.mo.ts.cmd.MobileTenderOpenDetailCmd.dev";
    static final String NEGO_RES = "https://m.ebid.lh.or.kr:8443/ebid.mo.ts.cmd.MobileAllottblScningPrioritytDetailCmd.dev";
    static final String TECH_RES = "https://m.ebid.lh.or.kr:8443/ebid.mo.ts.cmd.MobileTknlgPriPriorityDetailCmd.dev";
    String sd;
    String ed;
    String op;
    int totalItems;
    int curItem = 0;
    GetFrame frame;
    private CheckFrame checkFrame;

    public NewLHParser(String sd, String ed, String op, GetFrame frame, CheckFrame checkFrame) throws ClassNotFoundException, SQLException {
        this.sd = sd;
        this.ed = ed;
        this.op = op;

        this.frame = frame;
        this.checkFrame = checkFrame;

        db_con = DriverManager.getConnection("jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8", Util.DB_ID, Util.DB_PW);
        st = db_con.createStatement();
        rs = null;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
        NewLHParser test = new NewLHParser("2017/02/10", "2017/03/20", "", null, null);


        test.getNoti();
        test.getRes();
    }

    public void openConnection(String path, String method) throws IOException {
        url = new URL(path);
        con = ((HttpURLConnection)url.openConnection());

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

        StringBuffer response = new StringBuffer();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public void getNoti() throws IOException, SQLException {
        String path = BID_ANN;
        String param = "gubun=Y";
        param = param + "&s_tndrdocAcptOpenDtm=" + sd;
        param = param + "&s_tndrdocAcptEndDtm=" + ed;
        openConnection(path, "POST");
        Document doc = Jsoup.parse(getResponse(param, "POST"));

        Element dataTable = doc.getElementsByClass("search-result").first();
        Elements links = dataTable.getElementsByAttribute("onmouseover");
        int row = 1;
        Iterator<Element> iter = links.iterator();
        while (iter.hasNext()) {
            if (shutdown) { return;
            }
            Element i = (Element)iter.next();

            String values = i.attr("onclick").split("[(]")[1];
            values = values.substring(0, values.length() - 2);

            String[] keys = values.split(",");
            String bidNum = keys[0].substring(1, keys[0].length() - 1);
            String bidDegree = keys[1].substring(2, keys[1].length() - 1);
            String job = keys[2].substring(2, keys[2].length() - 1);
            String emrgncy = keys[3].substring(2, keys[3].length() - 1);

            if (frame != null) { frame.updateInfo(bidNum, false);
            }
            HashMap<String, String> info = new HashMap();
            Elements data = i.getElementsByTag("li");
            for (int index = 1; index < data.size(); index++) {
                Element d = (Element)data.get(index);
                System.out.println(d.html());
                String[] segments = d.text().split(":");
                String key = segments[0];
                String value = segments[1].trim();
                if (key.equals("입찰마감일자")) {
                    value = value + ":" + segments[2];
                }
                info.put(key, value);
            }

            boolean exists = false;
            boolean enter = true;
            String where = "WHERE 공고번호=\"" + bidNum + "\"";
            String sql = "SELECT EXISTS(SELECT 공고번호 FROM lhbidinfo " + where + ")";
            rs = st.executeQuery(sql);
            if (rs.first()) { exists = rs.getBoolean(1);
            }
            if (exists) {
                System.out.println(bidNum + " exists.");
                sql = "SELECT 공고, 공고현황 FROM lhbidinfo " + where;
                rs = st.executeQuery(sql);
                int finished = 0;
                String dbProg = "";
                if (rs.first()) {
                    finished = rs.getInt(1);
                    dbProg = rs.getString(2) == null ? "" : rs.getString(2);
                }

                if (finished > 0) {
                    if (dbProg.equals(info.get("진행상태"))) { enter = false;
                    } else {
                        sql = "UPDATE lhbidinfo SET 공고현황=\"" + (String)info.get("진행상태") + "\" " + where;
                        st.executeUpdate(sql);

                    }
                }
            }
            else
            {
                sql = "INSERT INTO lhbidinfo (공고번호, 업무, 분류, 계약방법, 입찰마감일자, 지역본부, 공고현황) VALUES (\"" + bidNum + "\", \"" + (String)info.get("업무") + "\", \"" + (String)info.get("분류") + "\", \"" + (String)info.get("계약방법") + "\", \"" + (String)info.get("입찰마감일자") + "\", \"" + (String)info.get("지역본부") + "\", \"" + (String)info.get("진행상태") + "\");";
                System.out.println(sql);
                st.executeUpdate(sql);
            }

            if (enter) {
                String itemPath = "";
                if (job.equals("10")) { itemPath = CONST_NOTI;
                } else if (job.equals("20")) { itemPath = SERV_NOTI;
                } else if (job.equals("30")) { itemPath = ITEM_NOTI;
                } else if (job.equals("40")) { itemPath = MISC_NOTI;
                }
                String itemParam = "bidNum=" + bidNum;
                itemParam = itemParam + "&bidDegree=" + bidDegree;
                itemParam = itemParam + "&cstrtnJobGbCd=" + job;
                itemParam = itemParam + "&emrgncyOrder=" + emrgncy;

                openConnection(itemPath, "POST");
                Document itemPage = Jsoup.parse(getResponse(itemParam, "POST"));
                parseItemPage(itemPage, where);
            }

            row++;
            if (row % 10 == 1)
            {
                Element pageDiv = doc.getElementsByClass("paging").first();
                Element pageLastElement = pageDiv.children().last();
                if (pageLastElement.tagName().equals("span")) {
                    break;
                }


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
        Elements licenses = itemPage.getElementsContainingOwnText("요구면허");
        if (!licenses.isEmpty()) {
            System.out.println(licenses.get(0).html());
        }

        Elements headers = itemPage.getElementsByTag("th");

        boolean hasBase = false;
        HashMap<String, String> info = new HashMap();
        for (Element h : headers) {
            String key = h.text();
            if (h.nextElementSibling() != null) {
                String value = h.nextElementSibling().text();
                if ((key.equals("용역유형")) || (key.equals("공사종류"))) {
                    key = "업종유형";
                }
                else if (key.equals("개찰일시")) {
                    value = value + ":00";
                }
                else if (key.equals("기초금액") || key.equals("설계가격") || key.equals("가격점수제외금액(A)")) {
                    value = value.split(" ")[0];
                    value = value.replaceAll(",", "");
                    value = value.replaceAll("원", "");
                    if (!Util.isInteger(value)) value = "0";
                    if (key.equals("기초금액")) {
                        hasBase = true;
                    }
                }
                info.put(key, value);
            }
        }

        String sql = "UPDATE lhbidinfo SET 업종유형=\"" + info.get("업종유형") + "\", 입찰방법=\"" + info.get("입찰방법") + "\", 입찰방식=\"" + info.get("입찰방식") + "\", 낙찰자선정방법=\"" + info.get("낙찰자선정방법") + "\", 재입찰=\"재입찰 없음\", 개찰일시=\"" + info.get("개찰일시") + "\", ";
        if (hasBase) {
            sql += "기초금액=" + info.get("기초금액") + ", ";
        }
        else {
            sql += "기초금액=" + info.get("설계가격") + ", ";
        }

        if (info.containsKey("가격점수제외금액(A)")) {
            sql += "A값=" + info.get("가격점수제외금액(A)") + ", ";
        }

        sql = sql + "공고=1 " + where;

        System.out.println(sql);
        st.executeUpdate(sql);
    }

    public void getRes() throws IOException, SQLException {
        curItem = 0;

        String path = BID_RES;
        String param = "gubun=Y";
        param = param + "&s_openDtm1=" + sd;
        param = param + "&s_openDtm2=" + ed;
        openConnection(path, "POST");
        Document doc = Jsoup.parse(getResponse(param, "POST"));

        Element dataTable = doc.getElementsByClass("search-result").first();
        Elements links = dataTable.getElementsByAttribute("onmouseover");
        int row = 1;
        Iterator<Element> iter = links.iterator();
        while (iter.hasNext()) {
            if (shutdown) { return;
            }
            Element i = (Element)iter.next();

            curItem += 1;

            String values = i.attr("onclick").split("[(]")[1];
            values = values.substring(0, values.length() - 2);

            String[] keys = values.split(",");
            String bidNum = keys[0].substring(1, keys[0].length() - 1);
            String bidDegree = keys[1].substring(2, keys[1].length() - 1);
            String protocol = keys[2].substring(2, keys[2].length() - 1);
            String tndrCtrctMedCd = keys[4].substring(2, keys[4].length() - 1);
            String tenderopenCnt = keys[5].substring(2, keys[5].length() - 1);

            if (frame != null) frame.updateInfo(bidNum, true);
            if (checkFrame != null) {
                checkFrame.updateProgress(threadIndex);
            }

            boolean exists = false;
            boolean enter = true;
            HashMap<String, String> info = new HashMap();
            Elements data = i.getElementsByTag("li");
            for (int index = 1; index < data.size(); index++) {
                Element d = (Element)data.get(index);
                String[] segments = d.text().split(":");
                String key = segments[0];
                String value = segments[1].trim();
                if (key.equals("개찰마감일시")) {
                    value = value + ":" + segments[2];
                }
                System.out.println(key + " : " + value);
                info.put(key, value);
            }

            String where = "WHERE 공고번호=\"" + bidNum + "\"";

            rs = st.executeQuery("SELECT EXISTS(SELECT 공고번호 FROM lhbidinfo " + where + ")");
            if (rs.first()) { exists = rs.getBoolean(1);
            }
            if (exists) {
                rs = st.executeQuery("SELECT 완료, 개찰내역 FROM lhbidinfo " + where);
                int finished = 0;
                String dbResult = "";
                if (rs.first()) {
                    finished = rs.getInt(1);
                    dbResult = rs.getString(2) == null ? "" : rs.getString(2);
                    System.out.println(finished);
                }
                if (finished > 0) {
                    System.out.println(bidNum + " exists and " + dbResult);
                    if (dbResult.equals(info.get("개찰내역"))) {
                        enter = false;
                    }
                    else {
                        String sql = "UPDATE lhbidinfo SET 개찰내역=\"" + (String)info.get("개찰내역") + "\" ";
                        if ((((String)info.get("개찰내역")).equals("유찰")) || (((String)info.get("개찰내역")).equals("비공개"))) {
                            sql = sql + ", 완료=1 ";
                        }

                        sql = sql + where;
                        st.executeUpdate(sql);
                    }
                }
                else if (!dbResult.equals("")) {
                    st.executeUpdate("UPDATE lhbidinfo SET 완료=1 " + where);
                } else {
                    String sql = "UPDATE lhbidinfo SET 개찰내역=\"" + (String)info.get("개찰내역") + "\" ";
                    if ((((String)info.get("개찰내역")).equals("유찰")) || (((String)info.get("개찰내역")).equals("비공개"))) {
                        sql = sql + ", 완료=1 ";
                    }

                    sql = sql + where;
                    st.executeUpdate(sql);

                }



            }
            else
            {

                String sql = "INSERT INTO lhbidinfo (공고번호, 업무, 분류, 개찰일시, 개찰내역) VALUES (\"" + bidNum + "\", \"" + (String)info.get("업무구분") + "\", \"" + (String)info.get("분류") + "\", \"" + (String)info.get("개찰마감일시") + "\", \"" + (String)info.get("개찰내역") + "\");";
                System.out.println(sql);
                st.executeUpdate(sql);
                if ((((String)info.get("개찰내역")).equals("유찰")) || (((String)info.get("개찰내역")).equals("비공개"))) {
                    st.executeUpdate("UPDATE lhbidinfo SET 완료=1 " + where);
                    enter = false;
                }
            }

            if ((enter) && (protocol.equals("Y")) && (Integer.parseInt(tenderopenCnt) > 0)) {
                String itemPath = "";
                String type = "";
                if (tndrCtrctMedCd.equals("70")) {
                    itemPath = "https://m.ebid.lh.or.kr:8443/ebid.mo.ts.cmd.MobileAllottblScningPrioritytDetailCmd.dev";
                    type = "https://m.ebid.lh.or.kr:8443/ebid.mo.ts.cmd.MobileAllottblScningPrioritytDetailCmd.dev";
                }
                else if (tndrCtrctMedCd.equals("90")) {
                    itemPath = "https://m.ebid.lh.or.kr:8443/ebid.mo.ts.cmd.MobileTknlgPriPriorityDetailCmd.dev";
                    type = "https://m.ebid.lh.or.kr:8443/ebid.mo.ts.cmd.MobileTknlgPriPriorityDetailCmd.dev";
                }
                else {
                    itemPath = "https://m.ebid.lh.or.kr:8443/ebid.mo.ts.cmd.MobileTenderOpenDetailCmd.dev";
                    type = "https://m.ebid.lh.or.kr:8443/ebid.mo.ts.cmd.MobileTenderOpenDetailCmd.dev";
                }

                String itemParam = "bidNum=" + bidNum;
                itemParam = itemParam + "&bidDegree=" + bidDegree;

                openConnection(itemPath, "POST");
                Document itemPage = Jsoup.parse(getResponse(itemParam, "POST"));
                parseResPage(itemPage, where, type);
            }

            row++;
            if (row % 10 == 1)
            {
                Element pageDiv = doc.getElementsByClass("paging").first();
                Element pageLastElement = pageDiv.children().last();
                if (pageLastElement.tagName().equals("span")) {
                    break;
                }


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

        Element infoTable = ((Element)infoDivs.get(0)).getElementsByTag("table").first();

        HashMap<String, String> info = new HashMap();
        Elements data = infoTable.getElementsByTag("th");
        for (Element d : data) {
            String key = d.text();
            String value = d.nextElementSibling().text().trim();
            if (
                    key.equals("설계가격") ||
                    key.equals("기초금액") ||
                    key.equals("예정가격") ||
                    key.equals("공사예산금액") ||
                    key.equals("가격점수제외금액(A)")
            ) {
                value = value.split(" ")[0];
                value = value.replaceAll(",", "");
                value = value.replaceAll("원", "");
                if (!Util.isInteger(value)) {
                    value = "0";
                }

                if (key.equals("공사예산금액")) {
                    key = "예정가격";
                }
            }
            info.put(key, value);
        }
        ArrayList<String> dupPrices;
        if (infoDivs.size() > 1) {
            Element priceTable = ((Element)infoDivs.get(1)).getElementsByTag("table").first();

            dupPrices = new ArrayList();
            ArrayList<String> dupCounts = new ArrayList();
            ArrayList<String> chosen = new ArrayList();
            Elements priceRows = priceTable.getElementsByTag("tr");
            for (int i = 1; i < priceRows.size(); i++) {
                Elements priceData = ((Element)priceRows.get(i)).getElementsByTag("td");

                String dupPrice = ((Element)priceData.get(1)).text();
                dupPrice = dupPrice.replaceAll(",", "");
                String dupCount = ((Element)priceData.get(2)).text();

                if (!((Element)priceData.get(1)).attr("style").equals("")) {
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
                sb.append("복수" + i + "=" + (String)dupPrices.get(i - 1) + ", 복참" + i + "=" + (String)dupCounts.get(i - 1) + ", ");
                companies += Integer.parseInt((String)dupCounts.get(i - 1));
            }
            for (int i = 1; (i <= chosen.size()) && (i <= 4); i++) {
                sb.append("선택가격" + i + "=" + (String)chosen.get(i - 1) + ", ");
                expPrice += Long.parseLong((String)chosen.get(i - 1));
            }
            if (expPrice % 4L > 0L) {
                expPrice = expPrice / 4L + 1L;
            } else
                expPrice /= 4L;
            companies /= 2;
            sb.append("참가수=" + companies + ", 예정금액=" + expPrice + " " + where);

            String sql = sb.toString();
            System.out.println(sql);
            st.executeUpdate(sql);
        }

        if (!resDiv.isEmpty()) {
            Elements lists = resDiv.first().getElementsByClass("detail-list");

            for (Element list : lists) {
                Elements listData = list.getElementsByTag("li");
                if (listData.size() < 7) {
                    break;
                }

                if (type.equals("https://m.ebid.lh.or.kr:8443/ebid.mo.ts.cmd.MobileTenderOpenDetailCmd.dev")) {
                    if (!((Element)listData.get(6)).text().contains("낙찰하한율미만")) {
                        String bidPrice = ((Element)listData.get(3)).text().split(":")[1].trim();
                        bidPrice = bidPrice.replaceAll(",", "");
                        String sql = "UPDATE lhbidinfo SET 투찰금액=" + bidPrice + " " + where;
                        System.out.println(sql);
                        st.executeUpdate(sql);
                        break;
                    }
                }
                else {
                    String bidPrice = ((Element)listData.get(4)).text().split(":")[1].trim();
                    bidPrice = bidPrice.replaceAll(",", "");
                    String sql = "UPDATE lhbidinfo SET 투찰금액=" + bidPrice + " " + where;
                    System.out.println(sql);
                    st.executeUpdate(sql);
                    break;
                }
            }
        }

        String sql = "UPDATE lhbidinfo SET ";
        if (info.containsKey("기초금액")) sql = sql + "기초금액=" + (String)info.get("기초금액") + ", ";
        if (info.containsKey("예정가격")) sql = sql + "기존예정가격=" + (String)info.get("예정가격") + ", ";
        if (info.containsKey("가격점수제외금액(A)")) sql = sql + "A값=" + (String)info.get("가격점수제외금액(A)") + ", ";
        sql = sql + "완료=1 " + where;
        System.out.println(sql);
        st.executeUpdate(sql);
    }

    public void run() {
        try {
            if (op.equals("건수차이")) {
                manageDifference(sd, ed);
            } else {
                setOption("공고");
                if (!shutdown) getNoti();
                setOption("결과");
                if (!shutdown) { getRes();
                }
            }
            if (frame != null && !shutdown) {
                frame.toggleButton();
            }
            if (checkFrame != null && !shutdown) {
                checkFrame.signalFinish();
            }
        } catch (IOException|SQLException e) {
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

    public int getTotal() throws IOException, ClassNotFoundException, SQLException {
        String path = BID_RES;
        String param = "gubun=Y";
        param = param + "&s_openDtm1=" + sd;
        param = param + "&s_openDtm2=" + ed;
        openConnection(path, "POST");
        Document doc = Jsoup.parse(getResponse(param, "POST"));

        int row = 1;
        Element pageDiv = doc.getElementsByClass("paging").first();
        if (pageDiv == null) {
            return 0;
        }

        Elements pages = pageDiv.getElementsByTag("a");
        if (!pages.isEmpty()) {
            int r = Integer.parseInt(pages.last().attr("onclick").replaceAll("[^\\d]", ""));
            while ((r > row) && (!pages.isEmpty())) {
                row = r;
                String newParam = param + "&targetRow=" + row;

                openConnection(path, "POST");
                doc = Jsoup.parse(getResponse(newParam, "POST"));

                pageDiv = doc.getElementsByClass("paging").first();
                pages = pageDiv.getElementsByTag("a");
                r = Integer.parseInt(pages.last().attr("onclick").replaceAll("[^\\d]", ""));
            }
        }

        Element dataTable = doc.getElementsByClass("search-result").first();
        Elements links = dataTable.getElementsByAttribute("onmouseover");
        if (links.size() > 0) row += links.size() - 1;
        return row;
    }

    public void setDate(String sd, String ed) {
        this.sd = sd.replaceAll("-", "/");
        this.ed = ed.replaceAll("-", "/");
    }

    public void setOption(String op) {
        this.op = op;
    }

    public int getCur() {
        return curItem;
    }

    public void manageDifference(String sm, String em) throws SQLException, IOException {
        try {
            ArrayList<String> bidNums = new ArrayList();
            String sql = "SELECT 공고번호 FROM lhbidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\"";
            rs = st.executeQuery(sql);
            while (rs.next()) {
                bidNums.add(rs.getString("공고번호"));
            }

            String path = BID_RES;
            String param = "gubun=Y";
            param = param + "&s_openDtm1=" + sd;
            param = param + "&s_openDtm2=" + ed;
            openConnection(path, "POST");
            Document doc = Jsoup.parse(getResponse(param, "POST"));

            Element dataTable = doc.getElementsByClass("search-result").first();
            Elements links = dataTable.getElementsByAttribute("onmouseover");
            int row = 1;
            Iterator<Element> iter = links.iterator();
            while (iter.hasNext()) {
                if (shutdown) { return;
                }
                Element i = (Element)iter.next();

                curItem += 1;

                String values = i.attr("onclick").split("[(]")[1];
                values = values.substring(0, values.length() - 2);

                String[] keys = values.split(",");
                String bidNum = keys[0].substring(1, keys[0].length() - 1);
                for (int k = 0; k < bidNums.size(); k++) {
                    if (((String)bidNums.get(k)).equals(bidNum)) {
                        bidNums.remove(k);
                        break;
                    }
                }

                row++;
                if (row % 10 == 1)
                {
                    Element pageDiv = doc.getElementsByClass("paging").first();
                    Element pageLastElement = pageDiv.children().last();
                    if (pageLastElement.tagName().equals("span")) {
                        break;
                    }


                    String newParam = param + "&targetRow=" + row;

                    openConnection(path, "POST");
                    doc = Jsoup.parse(getResponse(newParam, "POST"));
                    dataTable = doc.getElementsByClass("search-result").first();
                    links = dataTable.getElementsByAttribute("onmouseover");
                    iter = links.iterator();
                }
            }

            for (int i = 0; i < bidNums.size(); i++) {
                String bidNum = (String)bidNums.get(i);
                sql = "DELETE FROM lhbidinfo WHERE 공고번호=\"" + bidNum + "\"";
                st.executeUpdate(sql);
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Logger.getGlobal().log(Level.WARNING, sw.toString());
        }
    }
}
