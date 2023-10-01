package org.bidcrawler;

import org.bidcrawler.utils.Util;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

class DoroParser extends Parser {
    public static final String ANN_LIST = "https://ebid.ex.co.kr/ui/sp/expro/bidnoti/findListBidNoti.do";
    public static final String ANN_DETAIL = "https://ebid.ex.co.kr/ui/sp/expro/bidnoti/findInfoNotiDetail.do";
    public static final String ANN_SHARED = "https://ebid.ex.co.kr/ui/sp/expro/shared/findInfoBidShared.do";
    public static final String RES_LIST = "https://ebid.ex.co.kr/ui/sp/expro/bidresult/findListResult.do";
    public static final String RES_DETAIL = "https://ebid.ex.co.kr/ui/sp/expro/bidresult/findInfoResultDetail.do";
    public static final String PRICE_DETAIL = "https://ebid.ex.co.kr/ui/sp/expro/shared/findBidResultDetail.do";
    public static final String BID_DETAIL = "https://ebid.ex.co.kr/ui/sp/expro/bidresult/findInfoBidVd.do";

    private URL url;
    private HttpsURLConnection con;
    public String cookie;
    public String csrf;
    public String option;

    Connection db_con;
    java.sql.Statement st;
    ResultSet rs;

    String sd;
    String ed;
    String op;
    String wt;
    String it;
    int totalItems;
    int curItem;

    GetFrame frame;
    CheckFrame checkFrame;

    public DoroParser(String sd, String ed, String op, GetFrame frame, CheckFrame checkFrame) throws ClassNotFoundException, SQLException {
        this.sd = sd.replaceAll("-", "");
        this.ed = ed.replaceAll("-", "");
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
                Util.DB_URL,
                Util.DB_ID,
                Util.DB_PW
        );
        st = db_con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        rs = null;
    }
    
    public static void main(String[] args) {
        try {
            DoroParser crawler = new DoroParser("2023-02-05", "2023-05-05", "공사공고", null, null);
            crawler.getCookie();
            crawler.setOption("공사");
            crawler.parseAnnouncementData("공사");
            crawler.parseResultData("공사");
        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getTotal() throws IOException, ClassNotFoundException, SQLException {
        getCookie();
        JSONObject paramMap = new JSONObject();
        paramMap.put("from_date", this.sd);
        paramMap.put("to_date", this.ed);
        paramMap.put("status", "Z");
        paramMap.put("arr_status", new String[] { "MY","QQ","QN","UA","UB","UP","UR","OO" });
        String[] options = new String[]{"CT", "MT", "SV"};
        for (String o : options) {
            paramMap.put("noti_cls", o);
            String path = DoroParser.RES_LIST;
            openHttpConnection(path, "POST");
            JSONArray response = new JSONArray(getResponse(paramMap.toString()));
            totalItems += response.length();
        }

        return totalItems;
    }

    @Override
    public void setDate(String sd, String ed) {
        this.sd = sd.replaceAll("-", "");
        this.ed = ed.replaceAll("-", "");
    }

    public void setOption(String option) {
        this.option = option;
    }

    @Override
    public int getCur() {
        return curItem;
    }

    @Override
    public void manageDifference(String sm, String em) throws SQLException, IOException {

    }

    private void getCookie() throws IOException {
        String path = "https://ebid.ex.co.kr/default.do";
        openHttpConnection(path, "GET");
        String response = getResponse("");
        Document doc = Jsoup.parse(response);
        Elements csrfNodes = doc.getElementsByAttributeValue("name", "_csrf");
        if (!csrfNodes.isEmpty()) {
            csrf = csrfNodes.get(0).attr("content");
        }
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

        if (method.equals("POST")) {
            con.setRequestProperty("X-CSRF-TOKEN", csrf);
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
        //System.out.println("Post parameters : " + param);
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

    private void parseAnnouncementData(String option) throws IOException, SQLException {
        this.option = option;
        String path = DoroParser.ANN_LIST;
        openHttpConnection(path, "POST");
        int page = 1;

        this.wt = option;
        // todo: COPY HEADER AND JSON FIELDS EXACTLY
        JSONObject paramMap = new JSONObject();
        paramMap.put("from_noti_date", this.sd);
        paramMap.put("to_noti_date", this.ed);
        paramMap.put("status", "E");
        paramMap.put("arr_status", new String[] { "EY", "GY", "FY" });
        if (option.equals("공사")) {
            paramMap.put("noti_cls", "CT");
        } else if (option.equals("물품")) {
            paramMap.put("noti_cls", "MT");
        } else if (option.equals("용역")) {
            paramMap.put("noti_cls", "SV");
        }
        paramMap.put("page", "noti");
        int count = 0;
        JSONArray response = new JSONArray(getResponse(paramMap.toString()));
        for (Object obj : response) {
            parseNotiListRow((JSONObject)obj);
            count++;
        }
    }

    private String parseArea(String areaCode) {
        String area = "";

        if (areaCode.equals("03")) {
            area = "강원본부";
        } else if (areaCode.equals("04")) {
            area = "대전충남본부";
        } else if (areaCode.equals("01")) {
            area = "본사";
        } else if (areaCode.equals("02")) {
            area = "수도권본부";
        } else if (areaCode.equals("08")) {
            area = "전북본부";
        } else if (areaCode.equals("07")) {
            area = "부산경남본부";
        } else if (areaCode.equals("09")) {
            area = "충북본부";
        } else if (areaCode.equals("05")) {
            area = "광주전남본부";
        } else if (areaCode.equals("06")) {
            area = "대구경북본부";
        }

        System.out.println(areaCode);
        System.out.println(area);

        return area;
    }

    private String parseResultCode(String resCode) {
        String res = "";

        if (resCode.equals("QQ")) {
            res = "적격심사중";
        } else if (resCode.equals("UB")) {
            res = "낙찰";
        } else if (resCode.equals("MY")) {
            res = "개찰완료";
        } else if (resCode.equals("UA")) {
            res = "재공고";
        } else if (resCode.equals("UP")) {
            res = "유찰";
        }

        return res;
    }

    private String parseDate(String dateStr) {
        StringBuilder sb = new StringBuilder();
        if (dateStr.length() == 8) {
            sb.append(dateStr.substring(0, 4));
            sb.append("-");
            sb.append(dateStr.substring(4, 6));
            sb.append("-");
            sb.append(dateStr.substring(6, 8));
            return sb.toString();
        }

        sb.append(dateStr.substring(0, 4));
        sb.append("-");
        sb.append(dateStr.substring(4, 6));
        sb.append("-");
        sb.append(dateStr.substring(6, 8));
        sb.append(" ");
        sb.append(dateStr.substring(8, 10));
        sb.append(":");
        sb.append(dateStr.substring(10, 12));
        return sb.toString();
    }

    private String parseLicense2(String code) {
        if (code.equals("0036")) {
            return "정보통신공사업";
        } else if (code.equals("0037")) {
            return "전기공사업";
        } else if (code.equals("4992")) {
            return "도장,습식,방수,석공사업";
        } else if (code.equals("0001")) {
            return "토목공사업";
        } else if (code.equals("0039")) {
            return "일반소방시설공사업(전기)";
        } else if (code.equals("0002")) {
            return "건축공사업";
        }

        return "";
    }

    private void parseDetailData(JSONObject detailObj, String where) throws SQLException {
        String annDate = detailObj.getString("noti_date"); // 공고일자
        annDate = parseDate(annDate);
        String hasDup = detailObj.getString("plrl_prc_yn"); // 복수예가적용여부
        hasDup = hasDup.equals("Y") ? "복수" : "단수";
        String hasRebid = detailObj.getString("re_bid_yn"); // 재입찰허용여부
        hasRebid = hasRebid.equals("Y") ? "예" : "아니오";
        String elecBid = detailObj.getString("elec_bid_yn"); // 전자입찰여부
        elecBid = elecBid.equals("Y") ? "예" : "아니오";
        String hasCommon = detailObj.getString("unsc_allow_yn"); // 공동수급가능여부
        hasCommon = hasCommon.equals("Y") ? "예" : "아니오";
        String fieldTour = detailObj.getString("field_desc_yn"); // 현장설명실시여부
        fieldTour = fieldTour.equals("Y") ? "예" : "아니오";
        String mustCommon = detailObj.getString("unsc_dty_yn"); // 공동수급의무여부
        mustCommon = mustCommon.equals("Y") ? "예" : "아니오";
        String openDate = detailObj.getString("open_dt"); // 개찰일시
        openDate = parseDate(openDate);
        long protoPrice = detailObj.getLong("dsgng_amt"); // 설계금액
        long aPrice = 0; // A값
        if (detailObj.has("aval_tot_amt") && !detailObj.isNull("aval_tot_amt")) {
            aPrice = detailObj.getLong("aval_tot_amt");
        }

        long purtCost = 0; // 순공사원가
        if (detailObj.has("purt_const_cst") && !detailObj.isNull("purt_const_cst")) {
            purtCost = detailObj.getLong("purt_const_cst");
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
                "순공사원가=" + purtCost + ", " +
                "A값=" + aPrice + ", ";
        sql += "개찰일시=\"" + openDate + "\" " + where;
        System.out.println(sql);
        st.executeUpdate(sql);
    }

    private void parseNotiListRow(JSONObject row) throws IOException, SQLException {
        boolean enter = true;
        boolean exists = false;
        String where = "";
        String bidno = row.get("noti_no").toString(); // 공고번호
        String area = row.get("area").toString(); // 지역
        area = parseArea(area);
        String compType = row.get("cpt_terms").toString(); // 계약방법
        if (compType.equals("CTH")) {
            compType = "제한경쟁";
        } else if (compType.equals("CTA")) {
            compType = "일반경쟁";
        }
        String prog = row.get("prog_sts").toString(); // 공고상태
        if (prog.equals("EY")) {
            prog = "공고중";
        } else if (prog.equals("FY")) {
            prog = "정정공고중";
        }
        String license1 = row.isNull("bid_shpr1") ? "" : row.getString("bid_shpr1");
        String license2 = row.isNull("bid_shpr2") ? "" : row.getString("bid_shpr2");
        if (option.equals("공사")) {
            if (license1.equals("1000")) {
                license1 = "종합건설업";
            } else if (license1.equals("2000")) {
                license1 = "전문건설업";
            } else if (license1.equals("3000")) {
                license1 = "기타공사업";
            }

            license2 = parseLicense2(license2);
        }
        if (option.equals("물품")) {
            license1 = row.isNull("lmtcpt_apply_bas_cd") ? "" : row.getString("lmtcpt_apply_bas_cd");
        }

        String license = (license1 + " " + license2).trim();

        where = "WHERE 공고번호=\"" + bidno + "\" AND 중복번호=\"1\"";

        if (frame != null) frame.updateInfo(bidno, false);

         String sql = "SELECT EXISTS(SELECT 공고번호 FROM exbidinfo " + where + ")";
         rs = st.executeQuery(sql);
         if (rs.first()) exists = rs.getBoolean(1);
         if (exists) {
           // Check the bid version and update level from the DB.
           sql = "SELECT 공고 FROM exbidinfo " + where;
           rs = st.executeQuery(sql);
           int finished = 0;
           if (rs.first()) {
               finished = rs.getInt(1);
           }
           if (finished > 0) enter = false;
         }
         else {
           sql = "INSERT INTO exbidinfo (공고번호, 분류, 지역, 계약방법, 공고상태, 업종제한사항, 중복번호) VALUES (" +
                   "\""+bidno+"\", \"" + wt + "\", \"" + area + "\", \"" + compType + "\", \"" + prog + "\", \"" + license + "\", \"1\");";
           st.executeUpdate(sql);
         }

        if (enter) {
            parseAnnouncementDetail(row, where);
        }
    }

    private String parseBidTerms(String code) {
        String bidTerms = "";
        if (code.equals("TOT")) {
            bidTerms = "총액입찰";
        } else if (code.equals("DET")) {
            bidTerms = "내역입찰";
        }

        return bidTerms;
    }

    private String parseStlTerms(String code) {
        String stlTerms = "";
        if (code.equals("STR")) {
            stlTerms = "소액전자입찰";
        } else if (code.equals("STE")) {
            stlTerms = "적격심사";
        } else if (code.equals("STK")) {
            stlTerms = "간이형종합심사";
        } else if (code.equals("STH")) {
            stlTerms = "협상";
        } else if (code.equals("STD")) {
            stlTerms = "건설폐기물적격심사";
        } else if (code.equals("STJ")) {
            stlTerms = "기술용역 적격심사";
        }

        return stlTerms;
    }

    private void parseAnnShared(JSONObject annData, String where) throws IOException, SQLException {
        String path = DoroParser.ANN_SHARED;
        openHttpConnection(path, "POST");

        JSONObject paramMap = new JSONObject();
        paramMap.put("noti_no", annData.get("noti_no"));
        paramMap.put("noti_cls", annData.get("noti_cls"));
        paramMap.put("noti_id", annData.get("noti_id"));
        paramMap.put("noti_cont_id", annData.get("noti_cont_id"));
        paramMap.put("bid_no", annData.get("bid_no").toString());
        paramMap.put("bid_rev", annData.get("bid_rev").toString());
        paramMap.put("page", "noti");
        JSONObject response = new JSONObject(getResponse(paramMap.toString()));
        JSONObject info = response.getJSONArray("findListBid").getJSONObject(0);
        String bidTerms = info.getString("bid_terms"); // 입찰
        bidTerms = parseBidTerms(bidTerms);
        String stlTerms = info.getString("stl_terms"); // 심사
        stlTerms = parseStlTerms(stlTerms);

        JSONObject contactInfo = response.getJSONObject("findInfoNotiDetail");
        String bidContact = contactInfo.getString("crhd_reg_nm_no");
        if (bidContact.contains("(")) {
            bidContact = bidContact.split("\\(")[1];
            bidContact = bidContact.substring(0, bidContact.length() - 1);
        } else {
            bidContact = "";
        }

        String contractContact = contactInfo.getString("bdei_reg_nm_no");
        if (contractContact.contains("(")) {
            contractContact = contractContact.split("\\(")[1];
            contractContact = contractContact.substring(0, contractContact.length() - 1);
        } else {
            contractContact = "";
        }

        String sql = "UPDATE exbidinfo SET " +
                "계약방법2=\"" + bidTerms + "\", " +
                "계약방법3=\"" + stlTerms + "\", " +
                "과업관련문의=\"" + bidContact + "\", " +
                "계약관련문의=\"" + contractContact + "\" " + where;
        System.out.println(sql);
        st.executeUpdate(sql);
    }

    private void parseAnnouncementDetail(JSONObject annData, String where) throws IOException, SQLException {
        String path = DoroParser.ANN_DETAIL;
        openHttpConnection(path, "POST");

        JSONObject paramMap = new JSONObject();
        paramMap.put("noti_no", annData.get("noti_no"));
        paramMap.put("noti_cls", annData.get("noti_cls"));
        paramMap.put("noti_id", annData.get("noti_id"));
        paramMap.put("noti_cont_id", annData.get("noti_cont_id"));
        paramMap.put("bid_no", annData.get("bid_no").toString());
        paramMap.put("bid_rev", annData.get("bid_rev").toString());
        paramMap.put("page", "noti");
        JSONObject response = new JSONObject(getResponse(paramMap.toString()));

        parseAnnShared(annData, where);

        JSONObject detailObj = response.getJSONObject("detailData");
        parseDetailData(detailObj, where);

        JSONArray priceArray = response.getJSONArray("estmList");
        for (int i = 0; i < priceArray.length(); i++) {
            JSONObject priceObj = priceArray.getJSONObject(i);
            long price = priceObj.getLong("dec_amt"); // 예가
            String priceSql = "UPDATE exbidinfo SET 복수"+(i+1)+"="+price+" " + where;
            System.out.println(priceSql);
            st.executeUpdate(priceSql);
        }
    }

    private void parseResultData(String option) throws IOException, SQLException {
        String path = DoroParser.RES_LIST;
        openHttpConnection(path, "POST");
        int page = 1;

        this.wt = option;
        JSONObject paramMap = new JSONObject();
        paramMap.put("from_date", this.sd);
        paramMap.put("to_date", this.ed);
        paramMap.put("status", "Z");
        paramMap.put("arr_status", new String[] { "MY","QQ","QN","UA","UB","UP","UR","OO" });
        if (option.equals("공사")) {
            paramMap.put("noti_cls", "CT");
        } else if (option.equals("물품")) {
            paramMap.put("noti_cls", "MT");
        } else if (option.equals("용역")) {
            paramMap.put("noti_cls", "SV");
        }

        JSONArray response = new JSONArray(getResponse(paramMap.toString()));
        totalItems += response.length();
        int count = 0;
        for (Object obj : response) {
            curItem++;
            parseResultRow((JSONObject)obj);
            count++;
        }
    }

    private void parseResultRow(JSONObject row) throws SQLException {
        boolean enter = true;
        boolean exists = false;
        String bidno = row.get("noti_no").toString(); // 공고번호
        String area = row.get("area").toString(); // 지역
        area = parseArea(area);
        String compType = row.get("cpt_terms").toString(); // 계약방법
        if (compType.equals("CTH")) {
            compType = "제한경쟁";
        } else if (compType.equals("CTA")) {
            compType = "일반경쟁";
        }
        String prog = parseResultCode(row.get("prog_sts").toString()); // 결과

        String where = "WHERE 공고번호=\"" + bidno + "\" AND 중복번호=\"1\"";
        String openDate = row.get("open_time").toString(); // 개찰일시
        openDate = parseDate(openDate);

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
                     System.out.println(sql);
                     st.executeUpdate(sql);
                 }
                 enter = false;
             }
           }
           else {
               String sql = "INSERT INTO exbidinfo (공고번호, 분류, 지역, 계약방법, 개찰일시, 결과상태, 중복번호) VALUES (" +
                       "\""+bidno+"\", \"" + wt + "\", \"" + area + "\", \"" + compType + "\", \"" + openDate + "\", \"" + prog + "\", \"1\");";
               System.out.println(sql);
               st.executeUpdate(sql);
           }
        if (enter) {
            try {
                parseResultDetail(row, where);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseResultDetail(JSONObject resData, String where) throws IOException, SQLException {
        String path = DoroParser.RES_DETAIL;
        openHttpConnection(path, "POST");

        JSONObject paramMap = new JSONObject();
        paramMap.put("noti_no", resData.get("noti_no"));
        paramMap.put("noti_cls", resData.get("noti_cls"));
        paramMap.put("noti_id", resData.get("noti_id"));
        paramMap.put("noti_cont_id", resData.get("noti_cont_id"));
        paramMap.put("bid_no", resData.get("bid_no"));
        paramMap.put("bid_rev", resData.get("bid_rev"));
        JSONObject response = new JSONObject(getResponse(paramMap.toString()));

        JSONObject detailObj = response.getJSONObject("detailData");
        parseDetailData(detailObj, where);
        parseAnnShared(resData, where);

        String annDate = detailObj.getString("noti_date"); // 공고일자
        annDate = parseDate(annDate);
        long protoPrice = 0; // 설계금액
        if (detailObj.has("dsgng_amt") && !detailObj.isNull("dsgng_amt")) {
            protoPrice = detailObj.getLong("dsgng_amt");
        }

        parsePriceDetail(detailObj, where);
        //parseBidDetail(detailObj, where);

        String sql = "UPDATE exbidinfo SET " +
                "공고일자=\"" + annDate + "\", ";
        if (protoPrice != 0) {
            sql += "설계금액=" + protoPrice + ", ";
        }

        sql += "완료=1 " + where;
        System.out.println(sql);
        st.executeUpdate(sql);
    }

    private void parseBidDetail(JSONObject resDetail, String where) throws IOException {
        String path = DoroParser.BID_DETAIL;
        openHttpConnection(path, "POST");
        resDetail.remove("bid_prtc_lcs");
        resDetail.remove("lmt_cause_wrt_input");
        resDetail.remove("noti_nm");
        resDetail.remove("cert_dn_val");
        resDetail.remove("noti_cls_nm");
        resDetail.remove("bid_nm");
        try {
            JSONObject response = new JSONObject(getResponse(resDetail.toString()));
            JSONObject bidEntry = response.getJSONArray("findListBidVd").getJSONObject(0);
            long bidPrice = bidEntry.getLong("dec_amt");
            int comp = response.getJSONArray("findListBidVd").length();
            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE exbidinfo SET ");
            sb.append("투찰금액=" + bidPrice + ", ");
            sb.append("참가수=" + comp + " ");
            sb.append(where);
            String sql = sb.toString();
            System.out.println(sql);
            st.executeUpdate(sql);
            //System.out.println(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parsePriceDetail(JSONObject resDetail, String where) throws IOException {
        String path = DoroParser.PRICE_DETAIL;
        openHttpConnection(path, "POST");
        resDetail.remove("bid_prtc_lcs");
        resDetail.remove("lmt_cause_wrt_input");
        resDetail.remove("noti_nm");
        resDetail.remove("cert_dn_val");
        resDetail.remove("noti_cls_nm");
        resDetail.remove("bid_nm");
        try {
            JSONObject response = new JSONObject(getResponse(resDetail.toString()));
            JSONObject bidEntry = response.getJSONArray("resultList").getJSONObject(0);
            long bidPrice = bidEntry.getLong("dec_amt");
            String priceList = response.getJSONObject("resultExe").getString("final_eamt");
            int bidCount = response.getJSONObject("resultExe").getInt("bid_count");
            //System.out.println(priceList);
            String[] priceStrs = priceList.split("<br>");
            String planPrice = priceStrs[0].trim().replaceAll(",", "");
            for (int i = 2; i < priceStrs.length; i++) {
                if (i == 6) {
                    continue;
                }

                String pricePart = priceStrs[i].split("[)]")[1];
                String rePrice = pricePart.trim().replaceAll(",", "");
                String indexPart = priceStrs[i].split("[)]")[0];
                String index = indexPart.split("[(]")[1];
                String sql = "UPDATE exbidinfo SET 복수" + index + "=" + rePrice + " " + where;
                System.out.println(sql);
                st.executeUpdate(sql);
            }

            String sql = "UPDATE exbidinfo SET 예정가격=" + planPrice + ", 참가수=" + bidCount + ", 투찰금액=" + bidPrice + " " + where;
            System.out.println(sql);
            st.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            getCookie();
            parseAnnouncementData("공사");
            parseResultData("공사");
            parseAnnouncementData("용역");
            parseResultData("용역");
            parseAnnouncementData("물품");
            parseResultData("물품");

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
}