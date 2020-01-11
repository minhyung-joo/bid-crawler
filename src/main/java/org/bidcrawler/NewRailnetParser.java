package org.bidcrawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.bidcrawler.utils.Util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class NewRailnetParser extends Parser
{
    public static final String NOTI_LIST = "http://ebid.kr.or.kr/krn/krnBidList.do";
    public static final String RES_LIST = "http://ebid.kr.or.kr/krn/krnBidOpengList.do";
    public static final String NOTI_PAGE = "http://ebid.kr.or.kr/krn/krnBidDetail.do";
    public static final String RES_PAGE = "http://ebid.kr.or.kr/bid/rpt/bidOpengResult.do";
    public static final String FAIL_RES_PAGE = "http://ebid.kr.or.kr/bid/rpt/bidOpengFailResult.do";
    public static final String PRICE_INFO_PAGE = "http://ebid.kr.or.kr/bid/rpt/bidPdpDrwtRpt.do";
    public static final String PRICE_COMP_PAGE = "http://ebid.kr.or.kr/bid/rpt/bidPdpDrwtStatus.do";
    private Connection db_con;
    private Statement st;
    private ResultSet rs;
    private HttpClient client;
    String cookie = null;
    private GetFrame frame;
    private CheckFrame checkFrame;
    private String startDate;
    private String endDate;
    private String option;
    int totalItems;
    int curItem;

    public NewRailnetParser(String startDate, String endDate, String option, GetFrame frame, CheckFrame checkFrame)
            throws SQLException
    {
        this.startDate = startDate;
        this.endDate = endDate;
        this.option = option;
        this.frame = frame;
        this.checkFrame = checkFrame;
        client = HttpClientBuilder.create().build();
        totalItems = 0;
        curItem = 0;

        db_con = java.sql.DriverManager.getConnection("jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8", Util.DB_ID, Util.DB_PW);
        st = db_con.createStatement();
        rs = null;
    }

    public String sendGetRequest(String path) {
        try {
            HttpGet request = new HttpGet(path);


            request.addHeader("User-Agent", "Mozilla/5.0");

            HttpResponse response = client.execute(request);

            System.out.println("\nSending 'GET' request to URL : " + path);
            System.out.println("Response Code : " + response
                    .getStatusLine().getStatusCode());


            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            return result.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Logger.getGlobal().log(Level.WARNING, sw.toString()); }
        return null;
    }

    public String sendPostRequest(String path, List<NameValuePair> urlParameters)
    {
        try {
            HttpPost post = new HttpPost(path);


            post.setHeader("User-Agent", "Mozilla/5.0");
            post.setEntity(new org.apache.http.client.entity.UrlEncodedFormEntity(urlParameters, "UTF-8"));
            if (cookie != null) {
                post.setHeader("Cookie", cookie);
            }


            BufferedReader rd = new BufferedReader(new InputStreamReader(post.getEntity().getContent(), "UTF-8"));
            StringBuffer result = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            HttpResponse response = client.execute(post);
            System.out.println("\nSending 'POST' request to URL : " + path);
            System.out.println("Post parameters : " + result.toString());
            System.out.println("Response Code : " + response
                    .getStatusLine().getStatusCode());

            if (response.getFirstHeader("Set-Cookie") != null) {
                cookie = response.getFirstHeader("Set-Cookie").getValue();
            }

            rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            result = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            return result.toString();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Logger.getGlobal().log(Level.WARNING, sw.toString()); }
        return null;
    }

    public void parseAnnouncementData() throws SQLException
    {
        String path = "http://ebid.kr.or.kr/krn/krnBidList.do";
        int page = 1;
        List<NameValuePair> urlParameters = new ArrayList();
        urlParameters.add(new BasicNameValuePair("fromDate", startDate));
        urlParameters.add(new BasicNameValuePair("endDate", endDate));
        urlParameters.add(new BasicNameValuePair("pageIndex", page + ""));

        Document doc = Jsoup.parse(sendPostRequest(path, urlParameters));
        Element countDiv = doc.getElementsByClass("grid_count").first();
        Elements spans = countDiv.getElementsByTag("span");
        if (spans.size() == 2) {
            String items = ((Element)spans.get(1)).text().replaceAll("[^\\d.-]", "");
            if (Util.isNumeric(items)) {
                totalItems = Integer.parseInt(items);
                int currentItem = 1;
                Element entryDiv = doc.getElementsByTag("tbody").first();
                Elements itemRows = entryDiv.getElementsByTag("tr");

                while (currentItem <= totalItems) {
                    for (Element itemRow : itemRows) {
                        Element anchor = itemRow.getElementsByTag("a").first();

                        String link = anchor.attr("href");
                        String[] linkData = link.split("'");
                        String gyErBeonho = Util.convertToNumeric(linkData[1]);
                        String ggNyeondo = Util.convertToNumeric(linkData[3]);
                        String ggGubun = Util.convertToNumeric(linkData[5]);
                        String ggIrBeonho = Util.convertToNumeric(linkData[7]);
                        String ggChasu = Util.convertToNumeric(linkData[9]);

                        Elements itemData = itemRow.getElementsByTag("td");
                        String bidNum = Util.removeWhitespace(((Element)itemData.get(2)).text());
                        if (frame != null) {
                            frame.updateInfo(bidNum, false);
                        }

                        boolean enter = true;
                        String where = "WHERE 공고번호=\"" + bidNum + "\" AND 차수=\"" + ggChasu + "\"";
                        String sql = "SELECT 공고 FROM railnetbidinfo " + where;
                        System.out.println(sql);
                        rs = st.executeQuery(sql);
                        if (rs.first()) {
                            String finished = rs.getString(1);
                            if ((finished != null) && (finished.equals("1"))) {
                                enter = false;
                            }
                        } else {
                            sql = "INSERT INTO railnetbidinfo (공고번호, 차수) VALUES (\"" + bidNum + "\", \"" + ggChasu + "\")";
                            System.out.println(sql);
                            st.executeUpdate(sql);
                        }

                        if (enter) {
                            path = "http://ebid.kr.or.kr/krn/krnBidDetail.do";
                            urlParameters.clear();
                            urlParameters.add(new BasicNameValuePair("gyErBeonho", gyErBeonho));
                            urlParameters.add(new BasicNameValuePair("ggNyeondo", ggNyeondo));
                            urlParameters.add(new BasicNameValuePair("ggGubun", ggGubun));
                            urlParameters.add(new BasicNameValuePair("ggIrBeonho", ggIrBeonho));
                            urlParameters.add(new BasicNameValuePair("ggChasu", ggChasu));

                            doc = Jsoup.parse(sendPostRequest(path, urlParameters));
                            parseNotiPage(doc, where);
                        }

                        currentItem++;
                    }

                    if ((currentItem % 10 == 1) && (currentItem <= totalItems)) {
                        page++;
                        path = "http://ebid.kr.or.kr/krn/krnBidList.do";
                        urlParameters.clear();
                        urlParameters.add(new BasicNameValuePair("fromDate", startDate));
                        urlParameters.add(new BasicNameValuePair("endDate", endDate));
                        urlParameters.add(new BasicNameValuePair("pageIndex", page + ""));
                        doc = Jsoup.parse(sendPostRequest(path, urlParameters));
                        entryDiv = doc.getElementsByTag("tbody").first();
                        itemRows = entryDiv.getElementsByTag("tr");
                        String checkText = "자료가 없습니다. 다른 검색조건을 선택해주세요";
                        if (itemRows.get(0).text().equals(checkText)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public void parseNotiPage(Document doc, String where) throws SQLException {
        Elements tables = doc.getElementsByTag("tbody");
        String priceMethod = "";
        String expPrice = "";
        String selectMethod = "";
        String rate = "";
        String paperDeadline = "";
        String openDate = "";
        String basePrice = "";
        String prelim = "";

        for (Element table : tables) {
            Elements headers = table.getElementsByTag("th");
            for (Element header : headers) {
                switch (Util.removeWhitespace(header.text())) {
                    case "예가방식":
                        priceMethod = header.nextElementSibling().text();
                        break;
                    case "심사기준":
                        prelim = header.nextElementSibling().text();
                        break;
                    case "설계금액":
                        expPrice = Util.convertToNumeric(header.nextElementSibling().text());
                        break;
                    case "낙찰자선정방법":
                        selectMethod = header.nextElementSibling().text();
                        break;
                    case "낙찰하한율(%)":
                        rate = Util.removeWhitespace(header.nextElementSibling().text());
                        break;
                    case "입찰서접수마감일시":
                        paperDeadline = header.nextElementSibling().text();
                        break;
                    case "개찰일시":
                        openDate = header.nextElementSibling().text();
                        break;
                    case "개찰(입찰)일시":
                        openDate = header.nextElementSibling().text();
                        break;
                    case "기초금액":
                        basePrice = Util.convertToNumeric(header.nextElementSibling().text());
                }

            }
        }



        if (expPrice.equals("")) {
            expPrice = "0";
        }
        if (basePrice.equals("")) {
            basePrice = "0";
        }
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("UPDATE railnetbidinfo SET ");
        sqlBuilder.append("설계금액=" + expPrice + ", ");
        sqlBuilder.append("기초금액=" + basePrice + ", ");
        if (!paperDeadline.equals("")) {
            sqlBuilder.append("입찰서접수마감일시=\"" + paperDeadline + "\", ");
        }
        sqlBuilder.append("예가방식=\"" + priceMethod + "\", ");
        sqlBuilder.append("공고기관=\"한국철도시설공단\", ");
        sqlBuilder.append("수요기관=\"한국철도시설공단\", ");
        sqlBuilder.append("낙찰자선정방식=\"" + selectMethod + "\", ");
        sqlBuilder.append("심사기준=\"" + prelim + "\", ");
        sqlBuilder.append("낙찰하한율=\"" + rate + "\", ");
        sqlBuilder.append("개찰일시=\"" + openDate + "\", 공고=1 ");
        sqlBuilder.append(where);
        String sql = sqlBuilder.toString();
        System.out.println(sql);
        st.executeUpdate(sql);
    }

    public void parseResultData() throws SQLException {
        String path = "http://ebid.kr.or.kr/krn/krnBidOpengList.do";
        int page = 1;
        List<NameValuePair> urlParameters = new ArrayList();
        urlParameters.add(new BasicNameValuePair("fromDate", startDate));
        urlParameters.add(new BasicNameValuePair("endDate", endDate));
        urlParameters.add(new BasicNameValuePair("pageIndex", page + ""));

        Document doc = Jsoup.parse(sendPostRequest(path, urlParameters));
        Element countDiv = doc.getElementsByClass("grid_count").first();
        Elements spans = countDiv.getElementsByTag("span");
        if (spans.size() == 2) {
            String items = ((Element)spans.get(1)).text().replaceAll("[^\\d.-]", "");
            if (Util.isNumeric(items)) {
                totalItems = Integer.parseInt(items);
                int currentItem = 1;
                Element entryDiv = doc.getElementsByTag("tbody").first();
                Elements itemRows = entryDiv.getElementsByTag("tr");
                while (currentItem <= totalItems) {
                    for (Element itemRow : itemRows) {
                        String link = itemRow.attr("onclick");
                        String[] linkData = link.split("'");
                        String ggNyeondo = linkData[1];
                        String ggGubun = linkData[3];
                        String ggIrBeonho = linkData[5];
                        String ggChasu = linkData[7];
                        String gyErBeonho = linkData[9];

                        Elements itemData = itemRow.getElementsByTag("td");
                        String bidNum = Util.removeWhitespace(((Element)itemData.get(2)).text());
                        String result = Util.removeWhitespace(((Element)itemData.get(6)).text());
                        if (frame != null) {
                            frame.updateInfo(bidNum, true);
                        }
                        if (checkFrame != null) {
                            checkFrame.updateProgress(threadIndex);
                        }

                        boolean enter = true;
                        String where = "WHERE 공고번호=\"" + bidNum + "\" AND 차수=\"" + ggChasu + "\"";
                        String sql = "SELECT 완료, 개찰결과 FROM railnetbidinfo " + where;
                        System.out.println(sql);
                        rs = st.executeQuery(sql);
                        if (rs.first()) {
                            String finished = rs.getString(1);
                            String dbResult = "";
                            if (finished != null) {
                                if (finished.equals("1")) {
                                    if ((dbResult.equals(result)) || (dbResult.equals("낙찰"))) { enter = false;
                                    } else {
                                        sql = "UPDATE railnetbidinfo SET 개찰결과=\"" + result + "\" " + where;
                                        st.executeUpdate(sql);
                                    }
                                }
                                else if (!dbResult.equals(result)) {
                                    sql = "UPDATE railnetbidinfo SET 개찰결과=\"" + result + "\" " + where;
                                    st.executeUpdate(sql);
                                }
                            } else {
                                sql = "UPDATE railnetbidinfo SET 개찰결과=\"" + result + "\" " + where;
                                st.executeUpdate(sql);
                            }
                        } else {
                            sql = "INSERT INTO railnetbidinfo (공고번호, 차수, 개찰결과) VALUES (\"" + bidNum + "\", \"" + ggChasu + "\", \"" + result + "\")";
                            System.out.println(sql);
                            st.executeUpdate(sql);
                        }

                        if (enter) {
                            StringBuilder urlBuilder = new StringBuilder();
                            urlBuilder.append("http://ebid.kr.or.kr/bid/rpt/bidOpengResult.do?");
                            urlBuilder.append("gyErBeonho=" + gyErBeonho);
                            urlBuilder.append("&ggNyeondo=" + ggNyeondo);
                            urlBuilder.append("&ggGubun=" + ggGubun);
                            urlBuilder.append("&ggIrBeonho=" + ggIrBeonho);
                            urlBuilder.append("&ggChasu=" + ggChasu);

                            boolean shouldEnter = true;
                            if (linkData[0].contains("openBidOpengPopUp")) {
                                String crSangtae = linkData[11];
                                String gyBangBeob2 = linkData[13];
                                String hsTbYeobu = linkData[15];
                                String icCyYeobu = linkData[17];
                                if ((crSangtae.equals("13")) && (gyBangBeob2.indexOf("07") > 1)) {
                                    if (icCyYeobu.equals("N")) {
                                        shouldEnter = false;
                                    }
                                    else if (hsTbYeobu.equals("N")) {
                                        shouldEnter = false;
                                    } else {
                                        urlBuilder.append("&mode=B");
                                    }

                                }
                                else if (icCyYeobu.equals("N")) {
                                    urlBuilder.append("&mode=A");
                                } else {
                                    urlBuilder.append("&mode=B");
                                }

                            }
                            else if (linkData[0].contains("getFailOrReBidDesc")) {
                                urlBuilder = new StringBuilder();
                                urlBuilder.append("http://ebid.kr.or.kr/bid/rpt/bidOpengFailResult.do?");
                                urlBuilder.append("gyErBeonho=" + gyErBeonho);
                                urlBuilder.append("&ggNyeondo=" + ggNyeondo);
                                urlBuilder.append("&ggGubun=" + ggGubun);
                                urlBuilder.append("&ggIrBeonho=" + ggIrBeonho);
                                urlBuilder.append("&ggChasu=" + ggChasu);
                            }

                            if (shouldEnter) {
                                path = urlBuilder.toString();
                                doc = Jsoup.parse(sendGetRequest(path));
                                parseResPage(doc, where, path.split("\\?")[1]);
                            }
                        }

                        currentItem++;
                    }

                    if ((currentItem % 10 == 1) && (currentItem <= totalItems)) {
                        page++;
                        path = "http://ebid.kr.or.kr/krn/krnBidOpengList.do";
                        urlParameters.clear();
                        urlParameters.add(new BasicNameValuePair("fromDate", startDate));
                        urlParameters.add(new BasicNameValuePair("endDate", endDate));
                        urlParameters.add(new BasicNameValuePair("pageIndex", page + ""));
                        doc = Jsoup.parse(sendPostRequest(path, urlParameters));
                        entryDiv = doc.getElementsByTag("tbody").first();
                        itemRows = entryDiv.getElementsByTag("tr");
                    }
                }
            }
        }
    }

    public void parseResPage(Document doc, String where, String param) throws SQLException {
        Elements tables = doc.getElementsByTag("table");
        String expPrice = "";
        String priceMethod = "";
        String prelim = "";
        String bidMethod = "";
        String rate = "";
        String openDate = "";
        String basePrice = "";
        String bidPrice = "";
        Elements compRows;
        Elements rowData;
        for (Element table : tables) {
            if (table.getElementsContainingText("사업자등록번호").size() > 0) {
                Element compTable = table.getElementsByTag("tbody").first();
                compRows = compTable.getElementsByTag("tr");
                for (Element row : compRows) {
                    rowData = row.getElementsByTag("td");
                    if (!rowData.isEmpty())
                    {


                        String compResult = Util.removeWhitespace(((Element)rowData.get(6)).text());
                        if ((compResult.equals("낙찰")) || (compResult.equals(""))) {
                            bidPrice = Util.convertToNumeric(((Element)rowData.get(4)).text());
                            break;
                        }
                    }
                }
            } else { Elements tbodys = table.getElementsByTag("tbody");
                for (Element tbody : tbodys) {
                    Elements headers = tbody.getElementsByTag("th");
                    for (Element header : headers) {
                        switch (header.text()) {
                            case "예가방식":
                                priceMethod = Util.removeWhitespace(header.nextElementSibling().text());
                                break;
                            case "설계금액":
                                expPrice = Util.convertToNumeric(header.nextElementSibling().text());
                                break;
                            case "계약방법":
                                bidMethod = Util.removeWhitespace(header.nextElementSibling().text());
                                break;
                            case "심사기준":
                                prelim = header.nextElementSibling().text();
                                break;
                            case "낙찰하한율(%)":
                                rate = Util.removeWhitespace(header.nextElementSibling().text());
                                break;
                            case "개찰일시":
                                openDate = header.nextElementSibling().text();
                                break;
                            case "예정가격":
                                basePrice = Util.convertToNumeric(header.nextElementSibling().text());
                        }

                    }
                }
            }
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("UPDATE railnetbidinfo SET ");
        if (!expPrice.equals("")) {
            sqlBuilder.append("설계금액=" + expPrice + ", ");
        }
        if (!basePrice.equals("")) {
            sqlBuilder.append("예정가격=" + basePrice + ", ");
        }
        if (!bidPrice.equals("")) {
            sqlBuilder.append("투찰금액=" + bidPrice + ", ");
        }
        sqlBuilder.append("예가방식=\"" + priceMethod + "\", ");
        sqlBuilder.append("계약방법=\"" + bidMethod + "\", ");
        sqlBuilder.append("심사기준=\"" + prelim + "\", ");
        sqlBuilder.append("낙찰하한율=\"" + rate + "\", ");
        sqlBuilder.append("개찰일시=\"" + openDate + "\", ");
        sqlBuilder.append("공고기관=\"한국철도시설공단\", ");
        sqlBuilder.append("수요기관=\"한국철도시설공단\", ");
        sqlBuilder.append("완료=1 ");
        sqlBuilder.append(where);

        if (!doc.getElementsContainingOwnText("예비가격 추첨현황").isEmpty()) {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("http://ebid.kr.or.kr/bid/rpt/bidPdpDrwtRpt.do");
            urlBuilder.append("?");
            urlBuilder.append(param);
            String path = urlBuilder.toString();
            Document pricePage = Jsoup.parse(sendGetRequest(path));
            parsePriceInfoPage(pricePage, where);
        }

        if (!doc.getElementsContainingText("예정가격 추첨조서").isEmpty()) {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("http://ebid.kr.or.kr/bid/rpt/bidPdpDrwtStatus.do");
            urlBuilder.append("?");
            urlBuilder.append(param);
            String path = urlBuilder.toString();
            Document compPage = Jsoup.parse(sendGetRequest(path));
            parsePriceCompPage(compPage, where);
        }

        String sql = sqlBuilder.toString();
        System.out.println(sql);
        st.executeUpdate(sql);
    }

    public void parsePriceInfoPage(Document doc, String where) throws SQLException {
        Elements tables = doc.getElementsByTag("table");
        Elements priceRows;
        for (Element table : tables) {
            if (!table.getElementsContainingText("추첨된 예비가격").isEmpty()) {
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("UPDATE railnetbidinfo SET ");
                priceRows = table.getElementsByTag("tr");
                for (Element priceRow : priceRows)
                {
                    if ((priceRow.getElementsContainingText("추첨된 예비가격").isEmpty()) &&
                            (priceRow.getElementsContainingText("추첨되지 않은 예비가격").isEmpty()))
                    {
                        String index = Util.convertToNumeric(priceRow.getElementsByTag("th").first().text());
                        if (!index.equals("")) {
                            String price = Util.convertToNumeric(priceRow.getElementsByTag("td").first().text());
                            sqlBuilder.append("복수" + index + "=" + price + ", ");
                        }
                    }
                }

                sqlBuilder.deleteCharAt(sqlBuilder.length() - 2);
                sqlBuilder.append(where);
                String sql = sqlBuilder.toString();
                System.out.println(sql);
                st.executeUpdate(sql);
            } else {
                Elements headers = table.getElementsByTag("th");
                for (Element header : headers)
                    if (header.text().equals("예비가격 기초금액")) {
                        String basePrice = Util.convertToNumeric(header.nextElementSibling().text());
                        StringBuilder sqlBuilder = new StringBuilder();
                        sqlBuilder.append("UPDATE railnetbidinfo SET ");
                        sqlBuilder.append("기초금액=" + basePrice + " ");
                        sqlBuilder.append(where);
                        String sql = sqlBuilder.toString();
                        System.out.println(sql);
                        st.executeUpdate(sql);
                        break;
                    }
            }
        }
    }

    public void parsePriceCompPage(Document doc, String where) throws SQLException {
        Elements tables = doc.getElementsByTag("table");
        for (Element table : tables) {
            if (!table.getElementsContainingText("예비가격 추첨횟수").isEmpty()) {
                StringBuilder sqlBuilder = new StringBuilder();
                sqlBuilder.append("UPDATE railnetbidinfo SET ");
                Elements compRows = table.getElementsByTag("tr");
                int total = 0;
                for (Element compRow : compRows) {
                    if (compRow.getElementsContainingText("예비가격 추첨횟수").isEmpty()) {
                        Elements indices = compRow.getElementsByTag("th");
                        Elements counts = compRow.getElementsByTag("td");
                        for (int i = 0; i < indices.size(); i++) {
                            String index = Util.convertToNumeric(((Element)indices.get(i)).text());
                            String count = Util.convertToNumeric(((Element)counts.get(i)).text());
                            total += Integer.parseInt(count);
                            sqlBuilder.append("복참" + index + "=" + count + ", ");
                        }
                    }
                }

                total /= 2;
                sqlBuilder.append("참가수=" + total + " ");
                sqlBuilder.append(where);
                String sql = sqlBuilder.toString();
                System.out.println(sql);
                st.executeUpdate(sql);
            }
        }
    }

    public int getTotal() throws java.io.IOException, ClassNotFoundException, SQLException
    {
        String path = "http://ebid.kr.or.kr/krn/krnBidOpengList.do";
        int page = 1;
        List<NameValuePair> urlParameters = new ArrayList();
        urlParameters.add(new BasicNameValuePair("fromDate", startDate));
        urlParameters.add(new BasicNameValuePair("endDate", endDate));
        urlParameters.add(new BasicNameValuePair("pageIndex", page + ""));

        Document doc = Jsoup.parse(sendPostRequest(path, urlParameters));
        Element countDiv = doc.getElementsByClass("grid_count").first();
        Elements spans = countDiv.getElementsByTag("span");
        if (spans.size() == 2) {
            String items = ((Element)spans.get(1)).text().replaceAll("[^\\d.-]", "");
            if (Util.isNumeric(items)) {
                return Integer.parseInt(items);
            }
        }

        return 0;
    }

    public void setDate(String sd, String ed)
    {
        startDate = sd;
        endDate = ed;
    }

    public void setOption(String op)
    {
        option = op;
    }

    public int getCur()
    {
        return 0;
    }

    public void manageDifference(String sm, String em) throws SQLException, java.io.IOException
    {
        try {
            ArrayList<String> bidNums = new ArrayList();
            ArrayList<String> bidVers = new ArrayList();
            String sql = "SELECT 공고번호, 차수 FROM railnetbidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\"";
            rs = st.executeQuery(sql);
            while (rs.next()) {
                bidNums.add(rs.getString("공고번호"));
                bidVers.add(rs.getString("차수"));
            }

            String path = "http://ebid.kr.or.kr/krn/krnBidOpengList.do";
            int page = 1;
            List<NameValuePair> urlParameters = new ArrayList();
            urlParameters.add(new BasicNameValuePair("fromDate", sm));
            urlParameters.add(new BasicNameValuePair("endDate", em));
            urlParameters.add(new BasicNameValuePair("pageIndex", page + ""));

            Document doc = Jsoup.parse(sendPostRequest(path, urlParameters));
            Element countDiv = doc.getElementsByClass("grid_count").first();
            Elements spans = countDiv.getElementsByTag("span");
            if (spans.size() == 2) {
                String items = ((Element)spans.get(1)).text().replaceAll("[^\\d.-]", "");
                if (Util.isNumeric(items)) {
                    totalItems = Integer.parseInt(items);
                    int currentItem = 1;
                    Element entryDiv = doc.getElementsByTag("tbody").first();
                    Elements itemRows = entryDiv.getElementsByTag("tr");
                    while (currentItem <= totalItems) {
                        for (Element itemRow : itemRows) {
                            String link = itemRow.attr("onclick");
                            String[] linkData = link.split("'");
                            String ggChasu = linkData[7];
                            Elements itemData = itemRow.getElementsByTag("td");
                            String bidNum = Util.removeWhitespace(((Element)itemData.get(2)).text());

                            String key = bidNum + ggChasu;
                            for (int i = 0; i < bidNums.size(); i++) {
                                String dbKey = (String)bidNums.get(i) + (String)bidVers.get(i);
                                if (key.equals(dbKey)) {
                                    bidNums.remove(i);
                                    bidVers.remove(i);
                                    break;
                                }
                            }

                            currentItem++;
                        }

                        if ((currentItem % 10 == 1) && (currentItem <= totalItems)) {
                            page++;
                            path = "http://ebid.kr.or.kr/krn/krnBidOpengList.do";
                            urlParameters.clear();
                            urlParameters.add(new BasicNameValuePair("fromDate", sm));
                            urlParameters.add(new BasicNameValuePair("endDate", em));
                            urlParameters.add(new BasicNameValuePair("pageIndex", page + ""));
                            doc = Jsoup.parse(sendPostRequest(path, urlParameters));
                            entryDiv = doc.getElementsByTag("tbody").first();
                            itemRows = entryDiv.getElementsByTag("tr");
                        }
                    }

                    for (int i = 0; i < bidNums.size(); i++) {
                        String bidNum = (String)bidNums.get(i);
                        String bidVer = (String)bidVers.get(i);
                        sql = "DELETE FROM railnetbidinfo WHERE 공고번호=\"" + bidNum + "\" AND 차수=\"" + bidVer + "\"";
                        st.executeUpdate(sql);
                    }
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Logger.getGlobal().log(Level.WARNING, sw.toString());
        }
    }

    public void run()
    {
        try {
            if (option.equals("건수차이")) {
                manageDifference(startDate, endDate);
            } else {
                parseAnnouncementData();
                parseResultData();
            }

            if (frame != null) {
                frame.toggleButton();
            }
            if (checkFrame != null) {
                checkFrame.signalFinish();
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Logger.getGlobal().log(Level.WARNING, sw.toString());
            if (frame != null) {
                frame.toggleButton();
            }
            if (checkFrame != null) {
                checkFrame.signalFinish();
            }
        }
    }
}
