package org.bidcrawler;

/**
 * Created by ravenjoo on 6/24/17.
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.ss.util.CellAddress;
import org.bidcrawler.utils.*;

import javax.sql.rowset.CachedRowSet;

public class ExcelWriter {
    Connection con;
    java.sql.Statement st;
    ResultSet rs;
    String sql;
    CachedRowSet cachedRowSet;

    String name;
    String defaultPath;
    String basePath;
    String filePath;
    String site;
    Workbook workbook;
    Sheet sheet;
    HSSFCellStyle money;
    SimpleDateFormat sdf;
    String today;

    String sd;
    String ed;
    String org;
    String workType;
    String lowerBound;
    String upperBound;
    String bidType;
    int rowIndex = 1;

    public ExcelWriter(String site, String sql, CachedRowSet cachedRowSet) {
        this.sql = sql;
        this.cachedRowSet = cachedRowSet;
        this.site = site;
        defaultPath = "F:/";
        basePath = Util.BASE_PATH;
        workbook = new HSSFWorkbook();
        sheet = workbook.createSheet("입찰정보");
        money = (HSSFCellStyle) workbook.createCellStyle();
        HSSFDataFormat moneyFormat = (HSSFDataFormat) workbook.createDataFormat();
        money.setDataFormat(moneyFormat.getFormat(BuiltinFormats.getBuiltinFormat(3)));
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat todayFormat = new SimpleDateFormat("yyyy-MM-dd");
        today = todayFormat.format(new Date());
    }

    public void setOptions(String sd, String ed, String org, String workType, String lowerBound, String upperBound, String bidType) {
        this.sd = sd;
        this.ed = ed;
        this.org = org;
        this.workType = workType;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.bidType = bidType;
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
        ExcelWriter tester = new ExcelWriter("국방조달청", null, null);

        tester.lhBidToExcel();
    }

    public void toExcel() throws ClassNotFoundException, SQLException, IOException {
        if (site.equals("LH공사")) {
            lhBidToExcel();
        }
        else if (site.equals("국방조달청")) {
            dapaBidToExcel();
        }
        else if (site.equals("한국마사회")) {
            letsrunBidToExcel();
        }
        else if (site.equals("도로공사")) {
            exBidToExcel();
        }
        else if (site.equals("국가철도공단")) {
            railnetBidToExcel();
        }
    }

    public void connectDB() throws ClassNotFoundException, SQLException {
        con = DriverManager.getConnection(
                "jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC",
                Util.DB_ID,
                Util.DB_PW
        );
        st = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        System.out.println("DB Connected");
    }

    public void adjustColumns() {
        for (int i = 0; i < 59; i++) {
            if (i == 3) sheet.setColumnWidth(i, 1000);
            else {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > 3600) sheet.setColumnWidth(i, 3600);
            }
        }

        for (int i = 10; i < 39; i++) {
            if (i != 23) sheet.setColumnHidden(i, true);
        }
    }

    public void labelColumns() {
        int cellIndex = 0;

        Row columnNames = sheet.createRow(0);
        columnNames.createCell(cellIndex++).setCellValue("순번");

        if (site.equals("도로공사") || site.equals("국가철도공단")) columnNames.createCell(cellIndex++).setCellValue("입찰공고번호");
        else if (site.equals("국방조달청")) columnNames.createCell(cellIndex++).setCellValue("공고번호-차수");
        else if (site.equals("LH공사") || site.equals("한국마사회")) columnNames.createCell(cellIndex++).setCellValue("공고번호");
        else if (site.equals("철도공사")) columnNames.createCell(cellIndex++).setCellValue("공고번호(차수)");

        if (site.equals("국방조달청")) {
            columnNames.createCell(cellIndex++).setCellValue("실제개찰일시");
        } else {
            columnNames.createCell(cellIndex++).setCellValue("개찰일시");
        }

        if (site.equals("도로공사") || site.equals("국가철도공단")) columnNames.createCell(cellIndex++).setCellValue("업종제한사항");
        else if (site.equals("LH공사")) columnNames.createCell(cellIndex++).setCellValue("요구면허");
        else if (site.equals("국방조달청")) columnNames.createCell(cellIndex++).setCellValue("면허명칭[코드]");
        else if (site.equals("한국마사회")) columnNames.createCell(cellIndex++).setCellValue("입찰구분");
        else if (site.equals("철도공사")) columnNames.createCell(cellIndex++).setCellValue("공고구분");

        if (site.equals("도로공사") || site.equals("국가철도공단")) columnNames.createCell(cellIndex++).setCellValue("설계금액");
        else columnNames.createCell(cellIndex++).setCellValue("기초금액");
        columnNames.createCell(cellIndex++).setCellValue("예정가격");
        columnNames.createCell(cellIndex++).setCellValue("투찰금액");
        columnNames.createCell(cellIndex++).setCellValue("A값");
        if (site.equals("도로공사")) columnNames.createCell(cellIndex++).setCellValue("순공사원가");
        columnNames.createCell(cellIndex++).setCellValue("1");
        columnNames.createCell(cellIndex++).setCellValue("2");
        columnNames.createCell(cellIndex++).setCellValue("3");
        columnNames.createCell(cellIndex++).setCellValue("4");
        columnNames.createCell(cellIndex++).setCellValue("5");
        columnNames.createCell(cellIndex++).setCellValue("6");
        columnNames.createCell(cellIndex++).setCellValue("7");
        columnNames.createCell(cellIndex++).setCellValue("8");
        columnNames.createCell(cellIndex++).setCellValue("9");
        columnNames.createCell(cellIndex++).setCellValue("10");
        columnNames.createCell(cellIndex++).setCellValue("11");
        columnNames.createCell(cellIndex++).setCellValue("12");
        columnNames.createCell(cellIndex++).setCellValue("13");
        columnNames.createCell(cellIndex++).setCellValue("14");
        columnNames.createCell(cellIndex++).setCellValue("15");
        columnNames.createCell(cellIndex++).setCellValue("1");
        columnNames.createCell(cellIndex++).setCellValue("2");
        columnNames.createCell(cellIndex++).setCellValue("3");
        columnNames.createCell(cellIndex++).setCellValue("4");
        columnNames.createCell(cellIndex++).setCellValue("5");
        columnNames.createCell(cellIndex++).setCellValue("6");
        columnNames.createCell(cellIndex++).setCellValue("7");
        columnNames.createCell(cellIndex++).setCellValue("8");
        columnNames.createCell(cellIndex++).setCellValue("9");
        columnNames.createCell(cellIndex++).setCellValue("10");
        columnNames.createCell(cellIndex++).setCellValue("11");
        columnNames.createCell(cellIndex++).setCellValue("12");
        columnNames.createCell(cellIndex++).setCellValue("13");
        columnNames.createCell(cellIndex++).setCellValue("14");
        columnNames.createCell(cellIndex++).setCellValue("15");
        columnNames.createCell(cellIndex++).setCellValue("참가수");
        if (site.equals("도로공사")) {
            columnNames.createCell(cellIndex++).setCellValue("공고일자");
            columnNames.createCell(cellIndex++).setCellValue("복수예가여부");
            columnNames.createCell(cellIndex++).setCellValue("재입찰허용여부");
            columnNames.createCell(cellIndex++).setCellValue("전자입찰여부");
            columnNames.createCell(cellIndex++).setCellValue("공동수급 가능여부");
            columnNames.createCell(cellIndex++).setCellValue("현장설명실시여부");
            columnNames.createCell(cellIndex++).setCellValue("공동수급 의무여부");
            columnNames.createCell(cellIndex++).setCellValue("과업관련문의");
            columnNames.createCell(cellIndex++).setCellValue("계약관련문의");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("계약방법1");
            columnNames.createCell(cellIndex++).setCellValue("계약방법2");
            columnNames.createCell(cellIndex++).setCellValue("계약방법3");
            columnNames.createCell(cellIndex++).setCellValue("발주기관");
            columnNames.createCell(cellIndex++).setCellValue("업력");
        }
        else if (site.equals("LH공사")) {
            columnNames.createCell(cellIndex++).setCellValue("입찰마감일자");
            columnNames.createCell(cellIndex++).setCellValue("업무");
            columnNames.createCell(cellIndex++).setCellValue("계약방법");
            columnNames.createCell(cellIndex++).setCellValue("입찰방법");
            columnNames.createCell(cellIndex++).setCellValue("입찰방식");
            columnNames.createCell(cellIndex++).setCellValue("낙찰자선정방법");
            columnNames.createCell(cellIndex++).setCellValue("재입찰");
            columnNames.createCell(cellIndex++).setCellValue("선택가격1");
            columnNames.createCell(cellIndex++).setCellValue("선택가격2");
            columnNames.createCell(cellIndex++).setCellValue("선택가격3");
            columnNames.createCell(cellIndex++).setCellValue("선택가격4");
            columnNames.createCell(cellIndex++).setCellValue("사이트예정금액");
            columnNames.createCell(cellIndex++).setCellValue("개찰내역");
            columnNames.createCell(cellIndex++).setCellValue("분류");
            columnNames.createCell(cellIndex++).setCellValue("지역본부");
            columnNames.createCell(cellIndex++).setCellValue("업종유형");
        }
        else if (site.equals("한국마사회")) {
            columnNames.createCell(cellIndex++).setCellValue("입찰마감");
            columnNames.createCell(cellIndex++).setCellValue("공고상태");
            columnNames.createCell(cellIndex++).setCellValue("개찰상태");
            columnNames.createCell(cellIndex++).setCellValue("낙찰하한금액");
            columnNames.createCell(cellIndex++).setCellValue("낙찰하한율");
            columnNames.createCell(cellIndex++).setCellValue("계약방법");
            columnNames.createCell(cellIndex++).setCellValue("예정가격방식");
            columnNames.createCell(cellIndex++).setCellValue("낙찰자결정방법");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("입찰방식");
        }
        else if (site.equals("국방조달청")) {
                columnNames.createCell(cellIndex++).setCellValue("개찰일시");
                columnNames.createCell(cellIndex++).setCellValue("입찰서제출마감일시");
                columnNames.createCell(cellIndex++).setCellValue("입찰구분공고구분");
                columnNames.createCell(cellIndex++).setCellValue("발주기관");
                columnNames.createCell(cellIndex++).setCellValue("계약방법");
                columnNames.createCell(cellIndex++).setCellValue("입찰방법");
                columnNames.createCell(cellIndex++).setCellValue("기초예가적용여부");
                columnNames.createCell(cellIndex++).setCellValue("사전심사");
                columnNames.createCell(cellIndex++).setCellValue("낙찰자결정방법");
                columnNames.createCell(cellIndex++).setCellValue("입찰서제출마감일시");
                columnNames.createCell(cellIndex++).setCellValue("낙찰하한율");
                columnNames.createCell(cellIndex++).setCellValue("");
                columnNames.createCell(cellIndex++).setCellValue("");
                columnNames.createCell(cellIndex++).setCellValue("");
                columnNames.createCell(cellIndex++).setCellValue("");
                columnNames.createCell(cellIndex++).setCellValue("사정률");
        }
        else if (site.equals("철도공사")) {
            columnNames.createCell(cellIndex++).setCellValue("투찰종료일시");
            columnNames.createCell(cellIndex++).setCellValue("입찰방식");
            columnNames.createCell(cellIndex++).setCellValue("공고분류");
            columnNames.createCell(cellIndex++).setCellValue("발주부서");
            columnNames.createCell(cellIndex++).setCellValue("계약방법");
            columnNames.createCell(cellIndex++).setCellValue("입찰방식");
            columnNames.createCell(cellIndex++).setCellValue("재입찰 허용여부");
            columnNames.createCell(cellIndex++).setCellValue("예가방식");
            columnNames.createCell(cellIndex++).setCellValue("총예가갯수");
            columnNames.createCell(cellIndex++).setCellValue("선택 예비가격");
            columnNames.createCell(cellIndex++).setCellValue("선택 예비가격");
            columnNames.createCell(cellIndex++).setCellValue("선택 예비가격");
            columnNames.createCell(cellIndex++).setCellValue("선택 예비가격");
            columnNames.createCell(cellIndex++).setCellValue("낙찰하한율(%)");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("사정률");
        }
        else if (site.equals("국가철도공단")) {
            columnNames.createCell(cellIndex++).setCellValue("개찰일시");
            columnNames.createCell(cellIndex++).setCellValue("발주기관");
            columnNames.createCell(cellIndex++).setCellValue("수요기관");
            columnNames.createCell(cellIndex++).setCellValue("낙찰자 선정방법");
            columnNames.createCell(cellIndex++).setCellValue("심사기준");
            columnNames.createCell(cellIndex++).setCellValue("계약방법");
            columnNames.createCell(cellIndex++).setCellValue("낙찰자선정방식");
            columnNames.createCell(cellIndex++).setCellValue("낙찰하한율(%)");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("");
            columnNames.createCell(cellIndex++).setCellValue("예가방식");
        }
    }

    public void toFile() throws IOException {
        name = "";
        switch (site) {
            case "국방조달청":
                nameDapaFile();
                break;
            case "LH공사":
                nameLhFile();
                break;
            case "도로공사":
                nameExFile();
                break;
            case "한국마사회":
                nameLetsrunFile();
                break;
            case "국가철도공단":
                nameRailnetFile();
                break;
        }

        int fi = 2;
        File f = new File(defaultPath);
        FileOutputStream fos = null;
        if (f.exists()) {
            defaultPath = "F:/" + name + ".xls";
            f = new File(defaultPath);
            while (f.exists() && !f.isDirectory()) {
                defaultPath = "F:/" + name + "-" + fi + ".xls";
                f = new File(defaultPath);
                fi++;
            }
            fos = new FileOutputStream(defaultPath);
        }
        else {
            filePath = basePath + name + ".xls";
            f = new File(filePath);
            while (f.exists() && !f.isDirectory()) {
                filePath = basePath + name + "-" + fi + ".xls";
                f = new File(filePath);
                fi++;
            }
            fos = new FileOutputStream(filePath);
        }
        workbook.getSheetAt(0).setActiveCell(new CellRangeAddress(new CellAddress("B1"), new CellAddress("BC" + (rowIndex - 1))));
        workbook.write(fos);
        fos.close();
    }

    private void nameRailnetFile() {
        name += site;
        if (workType == null) {
            name += "(전체)";
        }
        else {
            name += "(" + workType + ")";
        }

        if (lowerBound != null && upperBound != null) {
            name += String.format("-%.2f~%.2f", Double.parseDouble(upperBound), Double.parseDouble(lowerBound));
        }
    }

    private void nameExFile() {
        name = "한국도로공사";
        if (org != null && !org.equals("")) {
            name += " " + org;
        }

        if (workType == null) {
            name += "(전체)";
        }
        else {
            if (workType.equals("시설공사")) { name += "(공사)"; }
            else if (workType.equals("기술용역")) { name += "(기술)"; }
            else if (workType.equals("물품구매")) { name += "(물품)"; }
            else if (workType.equals("일반용역")) { name += "(일반)"; }
            else { name += "(" + workType + ")"; }
        }

        if (lowerBound != null && upperBound != null) {
            name += String.format("-%.2f~%.2f", Double.parseDouble(upperBound), Double.parseDouble(lowerBound));
        }
    }

    private void nameLhFile() {
        name = "한국토지주택공사";
        if (org != null && !org.equals("")) {
            name += " " + org;
        }

        if (workType == null) {
            name += "(전체)";
        }
        else {
            if (workType.equals("시설공사")) { name += "(공사)"; }
            else if (workType.equals("기술용역")) { name += "(기술)"; }
            else if (workType.equals("물품구매")) { name += "(물품)"; }
            else if (workType.equals("일반용역")) { name += "(일반)"; }
            else { name += "(" + workType + ")"; }
        }

        if (lowerBound != null && upperBound != null) {
            name += String.format("-%.2f~%.2f", Double.parseDouble(upperBound), Double.parseDouble(lowerBound));
        }
    }

    private void nameLetsrunFile() {
        name = "한국마사회";
        if (org != null && !org.equals("")) {
            name += " " + org;
        }

        if (workType == null) {
            name += "(전체)";
        }
        else {
            if (workType.equals("시설공사")) { name += "(공사)"; }
            else if (workType.equals("기술용역")) { name += "(기술)"; }
            else if (workType.equals("물품구매")) { name += "(물품)"; }
            else if (workType.equals("일반용역")) { name += "(일반)"; }
            else { name += "(" + workType + ")"; }
        }

        if (lowerBound != null && upperBound != null) {
            name += String.format("-%.2f~%.2f", Double.parseDouble(upperBound), Double.parseDouble(lowerBound));
        }
    }

    private void nameDapaFile() {
        name = "국방조달청";
        if (org != null && !org.equals("")) {
            // Replace first word with org
            name = org;
        }

        if (bidType == null) {
            name += " (전체) ";
        } else {
            name += " (" + bidType + ") ";
        }

        if (workType == null) {
            name += "(전체)";
        }
        else {
            if (workType.equals("시설공사")) { name += "(공사)"; }
            else if (workType.equals("기술용역")) { name += "(기술)"; }
            else if (workType.equals("물품구매")) { name += "(물품)"; }
            else if (workType.equals("일반용역")) { name += "(일반)"; }
            else { name += "(" + workType + ")"; }
        }

        if (lowerBound != null && upperBound != null) {
            name += String.format("-%.2f~%.2f", Double.parseDouble(upperBound), Double.parseDouble(lowerBound));
        }
    }

    public void naraBidToExcel() throws ClassNotFoundException, SQLException, IOException {
        connectDB();

        labelColumns();

        adjustColumns();
    }

    public void railnetBidToExcel() throws ClassNotFoundException, SQLException, IOException {
        connectDB();

        labelColumns();
        if (cachedRowSet == null) {
            rs = st.executeQuery(sql);
        } else {
            rs = cachedRowSet.getOriginal();
        }

        int cellIndex = 0;
        int index = 1;
        while(rs.next()) {
            if (!Util.checkDataValidity(rs, site)) {
                continue;
            }

            Row row = sheet.createRow(rowIndex++);
            cellIndex = 0;
            row.createCell(cellIndex++).setCellValue(index);
            row.createCell(cellIndex++).setCellValue(rs.getString("공고번호"));
            if (rs.getString("실제개찰일시") != null) {
                String dd = rs.getString("실제개찰일시");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
                row.createCell(cellIndex++).setCellValue(dd);
            }
            else {
                String dd = rs.getString("개찰일시");
                dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
                row.createCell(cellIndex++).setCellValue(dd);
            }
            row.createCell(cellIndex++).setCellValue("");
            HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
            basePriceCell.setCellStyle(money);
            basePriceCell.setCellValue(rs.getLong("설계금액"));
            HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
            expectedPriceCell.setCellStyle(money);
            expectedPriceCell.setCellValue(rs.getLong("예정가격"));
            HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
            bidPriceCell.setCellStyle(money);
            bidPriceCell.setCellValue(rs.getLong("투찰금액"));
            HSSFCell aPriceCell = (HSSFCell) row.createCell(cellIndex++);
            aPriceCell.setCellStyle(money);
            aPriceCell.setCellValue(rs.getLong("A값"));
            HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell1.setCellStyle(money);
            dupPriceCell1.setCellValue(rs.getLong("복수1"));
            HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell2.setCellStyle(money);
            dupPriceCell2.setCellValue(rs.getLong("복수2"));
            HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell3.setCellStyle(money);
            dupPriceCell3.setCellValue(rs.getLong("복수3"));
            HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell4.setCellStyle(money);
            dupPriceCell4.setCellValue(rs.getLong("복수4"));
            HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell5.setCellStyle(money);
            dupPriceCell5.setCellValue(rs.getLong("복수5"));
            HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell6.setCellStyle(money);
            dupPriceCell6.setCellValue(rs.getLong("복수6"));
            HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell7.setCellStyle(money);
            dupPriceCell7.setCellValue(rs.getLong("복수7"));
            HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell8.setCellStyle(money);
            dupPriceCell8.setCellValue(rs.getLong("복수8"));
            HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell9.setCellStyle(money);
            dupPriceCell9.setCellValue(rs.getLong("복수9"));
            HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell10.setCellStyle(money);
            dupPriceCell10.setCellValue(rs.getLong("복수10"));
            HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell11.setCellStyle(money);
            dupPriceCell11.setCellValue(rs.getLong("복수11"));
            HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell12.setCellStyle(money);
            dupPriceCell12.setCellValue(rs.getLong("복수12"));
            HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell13.setCellStyle(money);
            dupPriceCell13.setCellValue(rs.getLong("복수13"));
            HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell14.setCellStyle(money);
            dupPriceCell14.setCellValue(rs.getLong("복수14"));
            HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell15.setCellStyle(money);
            dupPriceCell15.setCellValue(rs.getLong("복수15"));
            HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell1.setCellStyle(money);
            dupComCell1.setCellValue(rs.getLong("복참1"));
            HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell2.setCellStyle(money);
            dupComCell2.setCellValue(rs.getLong("복참2"));
            HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell3.setCellStyle(money);
            dupComCell3.setCellValue(rs.getLong("복참3"));
            HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell4.setCellStyle(money);
            dupComCell4.setCellValue(rs.getLong("복참4"));
            HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell5.setCellStyle(money);
            dupComCell5.setCellValue(rs.getLong("복참5"));
            HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell6.setCellStyle(money);
            dupComCell6.setCellValue(rs.getLong("복참6"));
            HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell7.setCellStyle(money);
            dupComCell7.setCellValue(rs.getLong("복참7"));
            HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell8.setCellStyle(money);
            dupComCell8.setCellValue(rs.getLong("복참8"));
            HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell9.setCellStyle(money);
            dupComCell9.setCellValue(rs.getLong("복참9"));
            HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell10.setCellStyle(money);
            dupComCell10.setCellValue(rs.getLong("복참10"));
            HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell11.setCellStyle(money);
            dupComCell11.setCellValue(rs.getLong("복참11"));
            HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell12.setCellStyle(money);
            dupComCell12.setCellValue(rs.getLong("복참12"));
            HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell13.setCellStyle(money);
            dupComCell13.setCellValue(rs.getLong("복참13"));
            HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell14.setCellStyle(money);
            dupComCell14.setCellValue(rs.getLong("복참14"));
            HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell15.setCellStyle(money);
            dupComCell15.setCellValue(rs.getLong("복참15"));
            row.createCell(cellIndex++).setCellValue(rs.getInt("참가수"));
            if (rs.getString("개찰일시") != null) {
                String dd = rs.getString("개찰일시");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
                row.createCell(cellIndex++).setCellValue(dd);
            }
            else row.createCell(cellIndex++).setCellValue("-");
            row.createCell(cellIndex++).setCellValue(rs.getString("공고기관"));
            row.createCell(cellIndex++).setCellValue(rs.getString("수요기관"));
            row.createCell(cellIndex++).setCellValue(rs.getString("심사기준"));
            row.createCell(cellIndex++).setCellValue("-");
            row.createCell(cellIndex++).setCellValue(rs.getString("계약방법"));
            row.createCell(cellIndex++).setCellValue(rs.getString("낙찰자선정방식"));
            row.createCell(cellIndex++).setCellValue(rs.getString("낙찰하한율"));
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue(rs.getString("예가방식"));
            index++;
        }

        adjustColumns();

        toFile();

        System.out.println("File created.");
    }

    public void railBidToExcel() throws ClassNotFoundException, SQLException, IOException {
        connectDB();

        labelColumns();

        String sql = "SELECT * FROM korailbidinfo WHERE ";
        if ((sd != null) && (ed != null)) {
            sql += "개찰일시 >= \"" + sd + "\" AND 개찰일시 <= \"" + ed + "\" AND ";
        }
        if (org != null) {
            sql += "발주부서=\"" + org + "\" AND ";
        }
        if (workType != null) {
            sql += "공고분류=\"" + workType + "\" AND ";
        }
        sql += "완료=1 ";

        sql += "UNION SELECT * FROM korailbidinfo WHERE ";
        if (org != null) {
            sql += "발주부서=\"" + org + "\" AND ";
        }
        if (workType != null) {
            sql += "공고분류=\"" + workType + "\" AND ";
        }
        sql += "개찰일시 >= \"" + today + "\" ORDER BY 개찰일시, 공고번호;";

        System.out.println(sql);
        rs = st.executeQuery(sql);

        int cellIndex = 0;
        int index = 1;
        while(rs.next()) {
            if (!Util.checkDataValidity(rs, site)) continue;

            Row row = sheet.createRow(rowIndex++);
            cellIndex = 0;
            row.createCell(cellIndex++).setCellValue(index);
            row.createCell(cellIndex++).setCellValue(rs.getString("공고번호"));
            String od = rs.getString("개찰일시");
            od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
            row.createCell(cellIndex++).setCellValue(od);
            row.createCell(cellIndex++).setCellValue(rs.getString("공고구분"));
            HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
            basePriceCell.setCellStyle(money);
            basePriceCell.setCellValue(rs.getLong("기초금액"));
            HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
            expectedPriceCell.setCellStyle(money);
            expectedPriceCell.setCellValue(rs.getLong("예정가격"));
            HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
            bidPriceCell.setCellStyle(money);
            bidPriceCell.setCellValue(rs.getLong("투찰금액"));
            HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell1.setCellStyle(money);
            dupPriceCell1.setCellValue(rs.getLong("복수1"));
            HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell2.setCellStyle(money);
            dupPriceCell2.setCellValue(rs.getLong("복수2"));
            HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell3.setCellStyle(money);
            dupPriceCell3.setCellValue(rs.getLong("복수3"));
            HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell4.setCellStyle(money);
            dupPriceCell4.setCellValue(rs.getLong("복수4"));
            HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell5.setCellStyle(money);
            dupPriceCell5.setCellValue(rs.getLong("복수5"));
            HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell6.setCellStyle(money);
            dupPriceCell6.setCellValue(rs.getLong("복수6"));
            HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell7.setCellStyle(money);
            dupPriceCell7.setCellValue(rs.getLong("복수7"));
            HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell8.setCellStyle(money);
            dupPriceCell8.setCellValue(rs.getLong("복수8"));
            HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell9.setCellStyle(money);
            dupPriceCell9.setCellValue(rs.getLong("복수9"));
            HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell10.setCellStyle(money);
            dupPriceCell10.setCellValue(rs.getLong("복수10"));
            HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell11.setCellStyle(money);
            dupPriceCell11.setCellValue(rs.getLong("복수11"));
            HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell12.setCellStyle(money);
            dupPriceCell12.setCellValue(rs.getLong("복수12"));
            HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell13.setCellStyle(money);
            dupPriceCell13.setCellValue(rs.getLong("복수13"));
            HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell14.setCellStyle(money);
            dupPriceCell14.setCellValue(rs.getLong("복수14"));
            HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell15.setCellStyle(money);
            dupPriceCell15.setCellValue(rs.getLong("복수15"));
            HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell1.setCellStyle(money);
            dupComCell1.setCellValue(rs.getLong("복참1"));
            HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell2.setCellStyle(money);
            dupComCell2.setCellValue(rs.getLong("복참2"));
            HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell3.setCellStyle(money);
            dupComCell3.setCellValue(rs.getLong("복참3"));
            HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell4.setCellStyle(money);
            dupComCell4.setCellValue(rs.getLong("복참4"));
            HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell5.setCellStyle(money);
            dupComCell5.setCellValue(rs.getLong("복참5"));
            HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell6.setCellStyle(money);
            dupComCell6.setCellValue(rs.getLong("복참6"));
            HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell7.setCellStyle(money);
            dupComCell7.setCellValue(rs.getLong("복참7"));
            HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell8.setCellStyle(money);
            dupComCell8.setCellValue(rs.getLong("복참8"));
            HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell9.setCellStyle(money);
            dupComCell9.setCellValue(rs.getLong("복참9"));
            HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell10.setCellStyle(money);
            dupComCell10.setCellValue(rs.getLong("복참10"));
            HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell11.setCellStyle(money);
            dupComCell11.setCellValue(rs.getLong("복참11"));
            HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell12.setCellStyle(money);
            dupComCell12.setCellValue(rs.getLong("복참12"));
            HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell13.setCellStyle(money);
            dupComCell13.setCellValue(rs.getLong("복참13"));
            HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell14.setCellStyle(money);
            dupComCell14.setCellValue(rs.getLong("복참14"));
            HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell15.setCellStyle(money);
            dupComCell15.setCellValue(rs.getLong("복참15"));
            row.createCell(cellIndex++).setCellValue(rs.getInt("참가수"));
            if (rs.getString("투찰종료일시") != null) {
                String dd = rs.getString("투찰종료일시");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,10);
                row.createCell(cellIndex++).setCellValue(dd);
            }
            else row.createCell(cellIndex++).setCellValue("-");
            row.createCell(cellIndex++).setCellValue(rs.getString("입찰방식"));
            row.createCell(cellIndex++).setCellValue(rs.getString("공고분류"));
            row.createCell(cellIndex++).setCellValue(rs.getString("발주부서"));
            row.createCell(cellIndex++).setCellValue(rs.getString("계약방법"));
            row.createCell(cellIndex++).setCellValue(rs.getString("입찰방식"));
            row.createCell(cellIndex++).setCellValue(rs.getString("재입찰허용여부"));
            row.createCell(cellIndex++).setCellValue(rs.getString("예가방식"));
            row.createCell(cellIndex++).setCellValue(rs.getInt("총예가갯수"));
            HSSFCell chosen1 = (HSSFCell) row.createCell(cellIndex++);
            chosen1.setCellStyle(money);
            chosen1.setCellValue(rs.getLong("선택1"));
            HSSFCell chosen2 = (HSSFCell) row.createCell(cellIndex++);
            chosen2.setCellStyle(money);
            chosen2.setCellValue(rs.getLong("선택2"));
            HSSFCell chosen3 = (HSSFCell) row.createCell(cellIndex++);
            chosen3.setCellStyle(money);
            chosen3.setCellValue(rs.getLong("선택3"));
            HSSFCell chosen4 = (HSSFCell) row.createCell(cellIndex++);
            chosen4.setCellStyle(money);
            chosen4.setCellValue(rs.getLong("선택4"));
            row.createCell(cellIndex++).setCellValue(rs.getString("낙찰하한율"));
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue(rs.getString("예가범위"));
            index++;
        }

        adjustColumns();

        toFile();

        System.out.println("File created.");
    }

    public void exBidToExcel() throws ClassNotFoundException, SQLException, IOException  {
        connectDB();

        labelColumns();

        if (cachedRowSet == null) {
            rs = st.executeQuery(sql);
        } else {
            rs = cachedRowSet.getOriginal();
        }

        int cellIndex = 0;
        int index = 1;
        while(rs.next()) {
            if (!Util.checkDataValidity(rs, site)) continue;

            Row row = sheet.createRow(rowIndex++);
            cellIndex = 0;
            row.createCell(cellIndex++).setCellValue(index);
            row.createCell(cellIndex++).setCellValue(rs.getString("공고번호"));
            String od = rs.getString("개찰일시");
            od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
            row.createCell(cellIndex++).setCellValue(od);
            row.createCell(cellIndex++).setCellValue(rs.getString("분류"));
            HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
            basePriceCell.setCellStyle(money);
            basePriceCell.setCellValue(rs.getLong("설계금액"));
            HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
            expectedPriceCell.setCellStyle(money);
            expectedPriceCell.setCellValue(rs.getLong("예정가격"));
            HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
            bidPriceCell.setCellStyle(money);
            bidPriceCell.setCellValue(rs.getLong("투찰금액"));
            HSSFCell aPriceCell = (HSSFCell) row.createCell(cellIndex++);
            aPriceCell.setCellStyle(money);
            aPriceCell.setCellValue(rs.getLong("A값"));
            HSSFCell purePriceCell = (HSSFCell) row.createCell(cellIndex++);
            purePriceCell.setCellStyle(money);
            purePriceCell.setCellValue(rs.getLong("순공사원가"));
            HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell1.setCellStyle(money);
            dupPriceCell1.setCellValue(rs.getLong("복수1"));
            HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell2.setCellStyle(money);
            dupPriceCell2.setCellValue(rs.getLong("복수2"));
            HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell3.setCellStyle(money);
            dupPriceCell3.setCellValue(rs.getLong("복수3"));
            HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell4.setCellStyle(money);
            dupPriceCell4.setCellValue(rs.getLong("복수4"));
            HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell5.setCellStyle(money);
            dupPriceCell5.setCellValue(rs.getLong("복수5"));
            HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell6.setCellStyle(money);
            dupPriceCell6.setCellValue(rs.getLong("복수6"));
            HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell7.setCellStyle(money);
            dupPriceCell7.setCellValue(rs.getLong("복수7"));
            HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell8.setCellStyle(money);
            dupPriceCell8.setCellValue(rs.getLong("복수8"));
            HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell9.setCellStyle(money);
            dupPriceCell9.setCellValue(rs.getLong("복수9"));
            HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell10.setCellStyle(money);
            dupPriceCell10.setCellValue(rs.getLong("복수10"));
            HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell11.setCellStyle(money);
            dupPriceCell11.setCellValue(rs.getLong("복수11"));
            HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell12.setCellStyle(money);
            dupPriceCell12.setCellValue(rs.getLong("복수12"));
            HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell13.setCellStyle(money);
            dupPriceCell13.setCellValue(rs.getLong("복수13"));
            HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell14.setCellStyle(money);
            dupPriceCell14.setCellValue(rs.getLong("복수14"));
            HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell15.setCellStyle(money);
            dupPriceCell15.setCellValue(rs.getLong("복수15"));
            HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell1.setCellStyle(money);
            dupComCell1.setCellValue(rs.getLong("복참1"));
            HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell2.setCellStyle(money);
            dupComCell2.setCellValue(rs.getLong("복참2"));
            HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell3.setCellStyle(money);
            dupComCell3.setCellValue(rs.getLong("복참3"));
            HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell4.setCellStyle(money);
            dupComCell4.setCellValue(rs.getLong("복참4"));
            HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell5.setCellStyle(money);
            dupComCell5.setCellValue(rs.getLong("복참5"));
            HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell6.setCellStyle(money);
            dupComCell6.setCellValue(rs.getLong("복참6"));
            HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell7.setCellStyle(money);
            dupComCell7.setCellValue(rs.getLong("복참7"));
            HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell8.setCellStyle(money);
            dupComCell8.setCellValue(rs.getLong("복참8"));
            HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell9.setCellStyle(money);
            dupComCell9.setCellValue(rs.getLong("복참9"));
            HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell10.setCellStyle(money);
            dupComCell10.setCellValue(rs.getLong("복참10"));
            HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell11.setCellStyle(money);
            dupComCell11.setCellValue(rs.getLong("복참11"));
            HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell12.setCellStyle(money);
            dupComCell12.setCellValue(rs.getLong("복참12"));
            HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell13.setCellStyle(money);
            dupComCell13.setCellValue(rs.getLong("복참13"));
            HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell14.setCellStyle(money);
            dupComCell14.setCellValue(rs.getLong("복참14"));
            HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell15.setCellStyle(money);
            dupComCell15.setCellValue(rs.getLong("복참15"));
            row.createCell(cellIndex++).setCellValue(rs.getInt("참가수"));
            if (rs.getString("공고일자") != null) {
                String dd = rs.getString("공고일자");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,10);
                row.createCell(cellIndex++).setCellValue(dd);
            }
            else row.createCell(cellIndex++).setCellValue("-");
            row.createCell(cellIndex++).setCellValue(rs.getString("복수예가여부"));
            row.createCell(cellIndex++).setCellValue(rs.getString("재입찰허용여부"));
            row.createCell(cellIndex++).setCellValue(rs.getString("전자입찰여부"));
            row.createCell(cellIndex++).setCellValue(rs.getString("공동수급가능여부"));
            row.createCell(cellIndex++).setCellValue(rs.getString("현장설명실시여부"));
            row.createCell(cellIndex++).setCellValue(rs.getString("공동수급의무여부"));
            row.createCell(cellIndex++).setCellValue(rs.getString("과업관련문의"));
            row.createCell(cellIndex++).setCellValue(rs.getString("계약관련문의"));
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue(rs.getString("계약방법"));
            row.createCell(cellIndex++).setCellValue(rs.getString("계약방법2"));
            row.createCell(cellIndex++).setCellValue(rs.getString("계약방법3"));
            row.createCell(cellIndex++).setCellValue(rs.getString("지역"));
            row.createCell(cellIndex++).setCellValue(rs.getString("분류"));
            index++;
        }

        adjustColumns();

        toFile();

        System.out.println("File created.");
    }

    public void lhBidToExcel() throws ClassNotFoundException, SQLException, IOException {
        connectDB();

        labelColumns();

        if (cachedRowSet == null) {
            rs = st.executeQuery(sql);
        } else {
            System.out.println("getting original");
            rs = cachedRowSet.getOriginal();
            System.out.println("got original");
        }

        int cellIndex = 0;
        int index = 1;
        while(rs.next()) {
            if (!Util.checkDataValidity(rs, site)) {
                continue;
            }

            Row row = sheet.createRow(rowIndex++);
            cellIndex = 0;
            row.createCell(cellIndex++).setCellValue(index++);
            row.createCell(cellIndex++).setCellValue(rs.getString("공고번호"));
            String od = rs.getString("개찰일시");
            if (!(od == null)){
                od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
            }
            else od = "";
            row.createCell(cellIndex++).setCellValue(od);
            row.createCell(cellIndex++).setCellValue(rs.getString("요구면허"));
            HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
            basePriceCell.setCellStyle(money);
            basePriceCell.setCellValue(rs.getLong("기초금액"));
            HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
            expectedPriceCell.setCellStyle(money);
            expectedPriceCell.setCellValue(rs.getLong("예정금액"));
            HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
            bidPriceCell.setCellStyle(money);
            bidPriceCell.setCellValue(rs.getLong("투찰금액"));
            HSSFCell aPriceCell = (HSSFCell) row.createCell(cellIndex++);
            aPriceCell.setCellStyle(money);
            aPriceCell.setCellValue(rs.getLong("A값"));
            HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell1.setCellStyle(money);
            dupPriceCell1.setCellValue(rs.getLong("복수1"));
            HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell2.setCellStyle(money);
            dupPriceCell2.setCellValue(rs.getLong("복수2"));
            HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell3.setCellStyle(money);
            dupPriceCell3.setCellValue(rs.getLong("복수3"));
            HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell4.setCellStyle(money);
            dupPriceCell4.setCellValue(rs.getLong("복수4"));
            HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell5.setCellStyle(money);
            dupPriceCell5.setCellValue(rs.getLong("복수5"));
            HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell6.setCellStyle(money);
            dupPriceCell6.setCellValue(rs.getLong("복수6"));
            HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell7.setCellStyle(money);
            dupPriceCell7.setCellValue(rs.getLong("복수7"));
            HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell8.setCellStyle(money);
            dupPriceCell8.setCellValue(rs.getLong("복수8"));
            HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell9.setCellStyle(money);
            dupPriceCell9.setCellValue(rs.getLong("복수9"));
            HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell10.setCellStyle(money);
            dupPriceCell10.setCellValue(rs.getLong("복수10"));
            HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell11.setCellStyle(money);
            dupPriceCell11.setCellValue(rs.getLong("복수11"));
            HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell12.setCellStyle(money);
            dupPriceCell12.setCellValue(rs.getLong("복수12"));
            HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell13.setCellStyle(money);
            dupPriceCell13.setCellValue(rs.getLong("복수13"));
            HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell14.setCellStyle(money);
            dupPriceCell14.setCellValue(rs.getLong("복수14"));
            HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell15.setCellStyle(money);
            dupPriceCell15.setCellValue(rs.getLong("복수15"));
            HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell1.setCellStyle(money);
            dupComCell1.setCellValue(rs.getLong("복참1"));
            HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell2.setCellStyle(money);
            dupComCell2.setCellValue(rs.getLong("복참2"));
            HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell3.setCellStyle(money);
            dupComCell3.setCellValue(rs.getLong("복참3"));
            HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell4.setCellStyle(money);
            dupComCell4.setCellValue(rs.getLong("복참4"));
            HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell5.setCellStyle(money);
            dupComCell5.setCellValue(rs.getLong("복참5"));
            HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell6.setCellStyle(money);
            dupComCell6.setCellValue(rs.getLong("복참6"));
            HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell7.setCellStyle(money);
            dupComCell7.setCellValue(rs.getLong("복참7"));
            HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell8.setCellStyle(money);
            dupComCell8.setCellValue(rs.getLong("복참8"));
            HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell9.setCellStyle(money);
            dupComCell9.setCellValue(rs.getLong("복참9"));
            HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell10.setCellStyle(money);
            dupComCell10.setCellValue(rs.getLong("복참10"));
            HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell11.setCellStyle(money);
            dupComCell11.setCellValue(rs.getLong("복참11"));
            HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell12.setCellStyle(money);
            dupComCell12.setCellValue(rs.getLong("복참12"));
            HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell13.setCellStyle(money);
            dupComCell13.setCellValue(rs.getLong("복참13"));
            HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell14.setCellStyle(money);
            dupComCell14.setCellValue(rs.getLong("복참14"));
            HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell15.setCellStyle(money);
            dupComCell15.setCellValue(rs.getLong("복참15"));
            row.createCell(cellIndex++).setCellValue(rs.getInt("참가수"));
            if (rs.getString("입찰마감일자") != null) {
                String dd = rs.getString("입찰마감일자");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
                row.createCell(cellIndex++).setCellValue(dd);
            }
            else row.createCell(cellIndex++).setCellValue("-");
            row.createCell(cellIndex++).setCellValue(rs.getString("업무"));
            row.createCell(cellIndex++).setCellValue(rs.getString("계약방법"));
            row.createCell(cellIndex++).setCellValue(rs.getString("입찰방법"));
            row.createCell(cellIndex++).setCellValue(rs.getString("입찰방식"));
            row.createCell(cellIndex++).setCellValue(rs.getString("낙찰자선정방법"));
            row.createCell(cellIndex++).setCellValue(rs.getString("재입찰"));
            HSSFCell chosenPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
            chosenPriceCell1.setCellStyle(money);
            chosenPriceCell1.setCellValue(rs.getLong("선택가격1"));
            HSSFCell chosenPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
            chosenPriceCell2.setCellStyle(money);
            chosenPriceCell2.setCellValue(rs.getLong("선택가격2"));
            HSSFCell chosenPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
            chosenPriceCell3.setCellStyle(money);
            chosenPriceCell3.setCellValue(rs.getLong("선택가격3"));
            HSSFCell chosenPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
            chosenPriceCell4.setCellStyle(money);
            chosenPriceCell4.setCellValue(rs.getLong("선택가격4"));
            HSSFCell sitePriceCell = (HSSFCell) row.createCell(cellIndex++);
            sitePriceCell.setCellStyle(money);
            sitePriceCell.setCellValue(rs.getLong("기존예정가격"));
            row.createCell(cellIndex++).setCellValue(rs.getString("개찰내역"));
            row.createCell(cellIndex++).setCellValue(rs.getString("분류"));
            row.createCell(cellIndex++).setCellValue(rs.getString("지역본부"));
            row.createCell(cellIndex++).setCellValue(rs.getString("업무"));
        }

        adjustColumns();

        toFile();

        System.out.println("File created.");
    }

    public void letsrunBidToExcel() throws ClassNotFoundException, SQLException, IOException {
        connectDB();

        labelColumns();

        if (cachedRowSet == null) {
            rs = st.executeQuery(sql);
        } else {
            rs = cachedRowSet.getOriginal();
        }

        int cellIndex = 0;
        int index = 1;
        while(rs.next()) {
            if (!Util.checkDataValidity(rs, site)) continue;

            Row row = sheet.createRow(rowIndex++);
            cellIndex = 0;
            row.createCell(cellIndex++).setCellValue(index);
            row.createCell(cellIndex++).setCellValue(rs.getString("공고번호"));
            String od = rs.getString("개찰일시");
            od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
            row.createCell(cellIndex++).setCellValue(od);
            row.createCell(cellIndex++).setCellValue(rs.getString("입찰구분"));
            HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
            basePriceCell.setCellStyle(money);
            basePriceCell.setCellValue(rs.getLong("예비가격기초금액"));
            HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
            expectedPriceCell.setCellStyle(money);
            expectedPriceCell.setCellValue(rs.getLong("예정가격"));
            HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
            bidPriceCell.setCellStyle(money);
            bidPriceCell.setCellValue(rs.getLong("투찰금액"));
            HSSFCell aPriceCell = (HSSFCell) row.createCell(cellIndex++);
            aPriceCell.setCellStyle(money);
            aPriceCell.setCellValue(rs.getLong("A값"));
            HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell1.setCellStyle(money);
            dupPriceCell1.setCellValue(rs.getLong("복수1"));
            HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell2.setCellStyle(money);
            dupPriceCell2.setCellValue(rs.getLong("복수2"));
            HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell3.setCellStyle(money);
            dupPriceCell3.setCellValue(rs.getLong("복수3"));
            HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell4.setCellStyle(money);
            dupPriceCell4.setCellValue(rs.getLong("복수4"));
            HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell5.setCellStyle(money);
            dupPriceCell5.setCellValue(rs.getLong("복수5"));
            HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell6.setCellStyle(money);
            dupPriceCell6.setCellValue(rs.getLong("복수6"));
            HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell7.setCellStyle(money);
            dupPriceCell7.setCellValue(rs.getLong("복수7"));
            HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell8.setCellStyle(money);
            dupPriceCell8.setCellValue(rs.getLong("복수8"));
            HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell9.setCellStyle(money);
            dupPriceCell9.setCellValue(rs.getLong("복수9"));
            HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell10.setCellStyle(money);
            dupPriceCell10.setCellValue(rs.getLong("복수10"));
            HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell11.setCellStyle(money);
            dupPriceCell11.setCellValue(rs.getLong("복수11"));
            HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell12.setCellStyle(money);
            dupPriceCell12.setCellValue(rs.getLong("복수12"));
            HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell13.setCellStyle(money);
            dupPriceCell13.setCellValue(rs.getLong("복수13"));
            HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell14.setCellStyle(money);
            dupPriceCell14.setCellValue(rs.getLong("복수14"));
            HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell15.setCellStyle(money);
            dupPriceCell15.setCellValue(rs.getLong("복수15"));
            HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell1.setCellStyle(money);
            dupComCell1.setCellValue(rs.getLong("복참1"));
            HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell2.setCellStyle(money);
            dupComCell2.setCellValue(rs.getLong("복참2"));
            HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell3.setCellStyle(money);
            dupComCell3.setCellValue(rs.getLong("복참3"));
            HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell4.setCellStyle(money);
            dupComCell4.setCellValue(rs.getLong("복참4"));
            HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell5.setCellStyle(money);
            dupComCell5.setCellValue(rs.getLong("복참5"));
            HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell6.setCellStyle(money);
            dupComCell6.setCellValue(rs.getLong("복참6"));
            HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell7.setCellStyle(money);
            dupComCell7.setCellValue(rs.getLong("복참7"));
            HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell8.setCellStyle(money);
            dupComCell8.setCellValue(rs.getLong("복참8"));
            HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell9.setCellStyle(money);
            dupComCell9.setCellValue(rs.getLong("복참9"));
            HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell10.setCellStyle(money);
            dupComCell10.setCellValue(rs.getLong("복참10"));
            HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell11.setCellStyle(money);
            dupComCell11.setCellValue(rs.getLong("복참11"));
            HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell12.setCellStyle(money);
            dupComCell12.setCellValue(rs.getLong("복참12"));
            HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell13.setCellStyle(money);
            dupComCell13.setCellValue(rs.getLong("복참13"));
            HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell14.setCellStyle(money);
            dupComCell14.setCellValue(rs.getLong("복참14"));
            HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell15.setCellStyle(money);
            dupComCell15.setCellValue(rs.getLong("복참15"));
            row.createCell(cellIndex++).setCellValue(rs.getInt("참여수"));
            if (rs.getString("입찰마감") != null) {
                String dd = rs.getString("입찰마감");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
                row.createCell(cellIndex++).setCellValue(dd);
            }
            else row.createCell(cellIndex++).setCellValue("-");
            row.createCell(cellIndex++).setCellValue(rs.getString("공고상태"));
            row.createCell(cellIndex++).setCellValue(rs.getString("개찰상태"));
            HSSFCell minPriceCell = (HSSFCell) row.createCell(cellIndex++);
            minPriceCell.setCellStyle(money);
            minPriceCell.setCellValue(rs.getLong("낙찰하한금액"));
            row.createCell(cellIndex++).setCellValue(rs.getString("낙찰하한율"));
            row.createCell(cellIndex++).setCellValue(rs.getString("계약방법"));
            row.createCell(cellIndex++).setCellValue(rs.getString("예정가격방식"));
            row.createCell(cellIndex++).setCellValue(rs.getString("낙찰자선정방법"));
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue(rs.getString("입찰방식"));
            index++;
        }

        adjustColumns();

        toFile();

        System.out.println("File created.");
    }

    public void dapaNegoToExcel() throws SQLException, ClassNotFoundException, IOException {
        connectDB();

        labelColumns();

        String sql = "SELECT * FROM dapanegoinfo WHERE ";
        if ((sd != null) && (ed != null)) {
            sql += "개찰일시 >= \"" + sd + "\" AND 개찰일시 <= \"" + ed + "\" AND ";
        }
        if (org != null) {
            sql += "발주기관 = \"" + org + "\" AND ";
        }
        if ((lowerBound != null) && (upperBound != null)) {
            sql += "하한=\"" + lowerBound + "\" AND 상한=\"" + upperBound + "\" AND ";
        }
        sql += "완료=1 ";

        sql += "UNION SELECT * FROM dapanegoinfo WHERE ";
        if (org != null) {
            sql += "발주기관 = \"" + org + "\" AND ";
        }
        if ((lowerBound != null) && (upperBound != null)) {
            sql += "하한=\"" + lowerBound + "\" AND 상한=\"" + upperBound + "\" AND ";
        }
        sql += "개찰일시 >= \"" + today + "\" ORDER BY 개찰일시, 공고번호;";

        rs = st.executeQuery(sql);

        int cellIndex = 0;
        int index = 1;
        while(rs.next()) {
            if (!Util.checkDataValidity(rs, site)) continue;

            Row row = sheet.createRow(rowIndex++);
            cellIndex = 0;
            row.createCell(cellIndex++).setCellValue(index++);
            row.createCell(cellIndex++).setCellValue(rs.getString("공고번호") + "-" + rs.getInt("차수"));
            if (rs.getString("실제개찰일시") != null) {
                String dd = rs.getString("실제개찰일시");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
                row.createCell(cellIndex++).setCellValue(dd);
            } else {
                String od = rs.getString("개찰일시");
                od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
                row.createCell(cellIndex++).setCellValue(od);
            }
            row.createCell(cellIndex++).setCellValue(rs.getString("면허명칭"));
            HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
            basePriceCell.setCellStyle(money);
            basePriceCell.setCellValue(rs.getLong("기초예비가격"));
            HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
            expectedPriceCell.setCellStyle(money);
            expectedPriceCell.setCellValue(rs.getLong("예정가격"));
            HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
            bidPriceCell.setCellStyle(money);
            bidPriceCell.setCellValue(rs.getLong("투찰금액"));
            HSSFCell aPriceCell = (HSSFCell) row.createCell(cellIndex++);
            aPriceCell.setCellStyle(money);
            aPriceCell.setCellValue(rs.getLong("A값"));
            HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell1.setCellStyle(money);
            dupPriceCell1.setCellValue(rs.getLong("복수1"));
            HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell2.setCellStyle(money);
            dupPriceCell2.setCellValue(rs.getLong("복수2"));
            HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell3.setCellStyle(money);
            dupPriceCell3.setCellValue(rs.getLong("복수3"));
            HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell4.setCellStyle(money);
            dupPriceCell4.setCellValue(rs.getLong("복수4"));
            HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell5.setCellStyle(money);
            dupPriceCell5.setCellValue(rs.getLong("복수5"));
            HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell6.setCellStyle(money);
            dupPriceCell6.setCellValue(rs.getLong("복수6"));
            HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell7.setCellStyle(money);
            dupPriceCell7.setCellValue(rs.getLong("복수7"));
            HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell8.setCellStyle(money);
            dupPriceCell8.setCellValue(rs.getLong("복수8"));
            HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell9.setCellStyle(money);
            dupPriceCell9.setCellValue(rs.getLong("복수9"));
            HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell10.setCellStyle(money);
            dupPriceCell10.setCellValue(rs.getLong("복수10"));
            HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell11.setCellStyle(money);
            dupPriceCell11.setCellValue(rs.getLong("복수11"));
            HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell12.setCellStyle(money);
            dupPriceCell12.setCellValue(rs.getLong("복수12"));
            HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell13.setCellStyle(money);
            dupPriceCell13.setCellValue(rs.getLong("복수13"));
            HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell14.setCellStyle(money);
            dupPriceCell14.setCellValue(rs.getLong("복수14"));
            HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell15.setCellStyle(money);
            dupPriceCell15.setCellValue(rs.getLong("복수15"));
            HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell1.setCellStyle(money);
            dupComCell1.setCellValue(rs.getLong("복참1"));
            HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell2.setCellStyle(money);
            dupComCell2.setCellValue(rs.getLong("복참2"));
            HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell3.setCellStyle(money);
            dupComCell3.setCellValue(rs.getLong("복참3"));
            HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell4.setCellStyle(money);
            dupComCell4.setCellValue(rs.getLong("복참4"));
            HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell5.setCellStyle(money);
            dupComCell5.setCellValue(rs.getLong("복참5"));
            HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell6.setCellStyle(money);
            dupComCell6.setCellValue(rs.getLong("복참6"));
            HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell7.setCellStyle(money);
            dupComCell7.setCellValue(rs.getLong("복참7"));
            HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell8.setCellStyle(money);
            dupComCell8.setCellValue(rs.getLong("복참8"));
            HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell9.setCellStyle(money);
            dupComCell9.setCellValue(rs.getLong("복참9"));
            HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell10.setCellStyle(money);
            dupComCell10.setCellValue(rs.getLong("복참10"));
            HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell11.setCellStyle(money);
            dupComCell11.setCellValue(rs.getLong("복참11"));
            HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell12.setCellStyle(money);
            dupComCell12.setCellValue(rs.getLong("복참12"));
            HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell13.setCellStyle(money);
            dupComCell13.setCellValue(rs.getLong("복참13"));
            HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell14.setCellStyle(money);
            dupComCell14.setCellValue(rs.getLong("복참14"));
            HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell15.setCellStyle(money);
            dupComCell15.setCellValue(rs.getLong("복참15"));
            row.createCell(cellIndex++).setCellValue(rs.getInt("참여수"));
            String od = rs.getString("개찰일시");
            od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
            row.createCell(cellIndex++).setCellValue(od);
            if (rs.getString("견적서제출마감일시") != null) {
                String dd = rs.getString("견적서제출마감일시");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
                row.createCell(cellIndex++).setCellValue(dd);
            }
            else row.createCell(cellIndex++).setCellValue("-");
            row.createCell(cellIndex++).setCellValue(rs.getString("협상형태"));
            row.createCell(cellIndex++).setCellValue(rs.getString("발주기관"));
            row.createCell(cellIndex++).setCellValue(rs.getString("낙찰자결정방법"));
            row.createCell(cellIndex++).setCellValue(rs.getString("입찰방법"));
            row.createCell(cellIndex++).setCellValue(rs.getString("기초예가적용여부"));
            if (rs.getString("견적서제출마감일시") != null) {
                String dd = rs.getString("견적서제출마감일시");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
                row.createCell(cellIndex++).setCellValue(dd);
            }
            else row.createCell(cellIndex++).setCellValue("-");
            row.createCell(cellIndex++).setCellValue(rs.getString("협상형태"));
            row.createCell(cellIndex++).setCellValue(rs.getString("진행상태"));
            String lbound = rs.getString("낙찰하한율");
            lbound = lbound.replaceAll("[^\\d.]", "");
            if (!lbound.equals("")) lbound += "%";
            row.createCell(cellIndex++).setCellValue(lbound);
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue(rs.getString("하한") + " ~ " + rs.getString("상한"));
        }

        adjustColumns();

        toFile();

        System.out.println("File created.");
    }

    public void dapaBidToExcel() throws ClassNotFoundException, SQLException, IOException {
        connectDB();

        labelColumns();

        if (cachedRowSet == null) {
            rs = st.executeQuery(sql);
        } else {
            rs = cachedRowSet.getOriginal();
        }

        int cellIndex = 0;
        int index = 1;
        while(rs.next()) {
            if (!Util.checkDataValidity(rs, site)) continue;

            Row row = sheet.createRow(rowIndex++);
            cellIndex = 0;
            row.createCell(cellIndex++).setCellValue(index++);
            row.createCell(cellIndex++).setCellValue(rs.getString("공고번호") + "-" + rs.getInt("차수"));
            if (rs.getString("실제개찰일시") != null) {
                String dd = rs.getString("실제개찰일시");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
                row.createCell(cellIndex++).setCellValue(dd);
            } else {
                String od = rs.getString("개찰일시");
                od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
                row.createCell(cellIndex++).setCellValue(od);
            }
            row.createCell(cellIndex++).setCellValue(rs.getString("면허명칭"));
            HSSFCell basePriceCell = (HSSFCell) row.createCell(cellIndex++);
            basePriceCell.setCellStyle(money);
            basePriceCell.setCellValue(rs.getLong("기초예비가격"));
            HSSFCell expectedPriceCell = (HSSFCell) row.createCell(cellIndex++);
            expectedPriceCell.setCellStyle(money);
            expectedPriceCell.setCellValue(rs.getLong("예정가격"));
            HSSFCell bidPriceCell = (HSSFCell) row.createCell(cellIndex++);
            bidPriceCell.setCellStyle(money);
            bidPriceCell.setCellValue(rs.getLong("투찰금액"));
            HSSFCell aPriceCell = (HSSFCell) row.createCell(cellIndex++);
            aPriceCell.setCellStyle(money);
            aPriceCell.setCellValue(rs.getLong("A값"));
            HSSFCell dupPriceCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell1.setCellStyle(money);
            dupPriceCell1.setCellValue(rs.getLong("복수1"));
            HSSFCell dupPriceCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell2.setCellStyle(money);
            dupPriceCell2.setCellValue(rs.getLong("복수2"));
            HSSFCell dupPriceCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell3.setCellStyle(money);
            dupPriceCell3.setCellValue(rs.getLong("복수3"));
            HSSFCell dupPriceCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell4.setCellStyle(money);
            dupPriceCell4.setCellValue(rs.getLong("복수4"));
            HSSFCell dupPriceCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell5.setCellStyle(money);
            dupPriceCell5.setCellValue(rs.getLong("복수5"));
            HSSFCell dupPriceCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell6.setCellStyle(money);
            dupPriceCell6.setCellValue(rs.getLong("복수6"));
            HSSFCell dupPriceCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell7.setCellStyle(money);
            dupPriceCell7.setCellValue(rs.getLong("복수7"));
            HSSFCell dupPriceCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell8.setCellStyle(money);
            dupPriceCell8.setCellValue(rs.getLong("복수8"));
            HSSFCell dupPriceCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell9.setCellStyle(money);
            dupPriceCell9.setCellValue(rs.getLong("복수9"));
            HSSFCell dupPriceCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell10.setCellStyle(money);
            dupPriceCell10.setCellValue(rs.getLong("복수10"));
            HSSFCell dupPriceCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell11.setCellStyle(money);
            dupPriceCell11.setCellValue(rs.getLong("복수11"));
            HSSFCell dupPriceCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell12.setCellStyle(money);
            dupPriceCell12.setCellValue(rs.getLong("복수12"));
            HSSFCell dupPriceCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell13.setCellStyle(money);
            dupPriceCell13.setCellValue(rs.getLong("복수13"));
            HSSFCell dupPriceCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell14.setCellStyle(money);
            dupPriceCell14.setCellValue(rs.getLong("복수14"));
            HSSFCell dupPriceCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupPriceCell15.setCellStyle(money);
            dupPriceCell15.setCellValue(rs.getLong("복수15"));
            HSSFCell dupComCell1 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell1.setCellStyle(money);
            dupComCell1.setCellValue(rs.getLong("복참1"));
            HSSFCell dupComCell2 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell2.setCellStyle(money);
            dupComCell2.setCellValue(rs.getLong("복참2"));
            HSSFCell dupComCell3 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell3.setCellStyle(money);
            dupComCell3.setCellValue(rs.getLong("복참3"));
            HSSFCell dupComCell4 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell4.setCellStyle(money);
            dupComCell4.setCellValue(rs.getLong("복참4"));
            HSSFCell dupComCell5 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell5.setCellStyle(money);
            dupComCell5.setCellValue(rs.getLong("복참5"));
            HSSFCell dupComCell6 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell6.setCellStyle(money);
            dupComCell6.setCellValue(rs.getLong("복참6"));
            HSSFCell dupComCell7 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell7.setCellStyle(money);
            dupComCell7.setCellValue(rs.getLong("복참7"));
            HSSFCell dupComCell8 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell8.setCellStyle(money);
            dupComCell8.setCellValue(rs.getLong("복참8"));
            HSSFCell dupComCell9 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell9.setCellStyle(money);
            dupComCell9.setCellValue(rs.getLong("복참9"));
            HSSFCell dupComCell10 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell10.setCellStyle(money);
            dupComCell10.setCellValue(rs.getLong("복참10"));
            HSSFCell dupComCell11 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell11.setCellStyle(money);
            dupComCell11.setCellValue(rs.getLong("복참11"));
            HSSFCell dupComCell12 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell12.setCellStyle(money);
            dupComCell12.setCellValue(rs.getLong("복참12"));
            HSSFCell dupComCell13 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell13.setCellStyle(money);
            dupComCell13.setCellValue(rs.getLong("복참13"));
            HSSFCell dupComCell14 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell14.setCellStyle(money);
            dupComCell14.setCellValue(rs.getLong("복참14"));
            HSSFCell dupComCell15 = (HSSFCell) row.createCell(cellIndex++);
            dupComCell15.setCellStyle(money);
            dupComCell15.setCellValue(rs.getLong("복참15"));
            row.createCell(cellIndex++).setCellValue(rs.getInt("참여수"));
            String od = rs.getString("개찰일시");
            od = od.substring(2,4) + od.substring(5,7) + od.substring(8,16);
            row.createCell(cellIndex++).setCellValue(od);
            if (rs.getString("입찰서제출마감일시") != null) {
                String dd = rs.getString("입찰서제출마감일시");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
                row.createCell(cellIndex++).setCellValue(dd);
            }
            else row.createCell(cellIndex++).setCellValue("-");
            if (rs.getString("입찰종류") == "경쟁") {
                row.createCell(cellIndex++).setCellValue(rs.getString("입찰종류") + "입찰" + rs.getString("공고종류"));
            } else {
                row.createCell(cellIndex++).setCellValue(rs.getString("입찰종류"));
            }
            row.createCell(cellIndex++).setCellValue(rs.getString("발주기관"));
            row.createCell(cellIndex++).setCellValue(rs.getString("계약방법"));
            row.createCell(cellIndex++).setCellValue(rs.getString("입찰방법"));
            row.createCell(cellIndex++).setCellValue(rs.getString("기초예가적용여부"));
            row.createCell(cellIndex++).setCellValue(rs.getString("사전심사"));
            row.createCell(cellIndex++).setCellValue(rs.getString("낙찰자결정방법"));
            if (rs.getString("입찰서제출마감일시") != null) {
                String dd = rs.getString("입찰서제출마감일시");
                if (dd.length() > 1) dd = dd.substring(2,4) + dd.substring(5,7) + dd.substring(8,16);
                row.createCell(cellIndex++).setCellValue(dd);
            }
            else row.createCell(cellIndex++).setCellValue("-");
            String lbound = rs.getString("낙찰하한율");
            lbound = lbound.replaceAll("[^\\d.]", "");
            if (!lbound.equals("")) lbound += "%";
            else lbound = "-";
            row.createCell(cellIndex++).setCellValue(lbound);
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            row.createCell(cellIndex++).setCellValue("");
            String rate = rs.getString("사정률");
            if (rate.equals("") || rate.equals("-")) {
                String lowerrate = rs.getString("하한");
                if (lowerrate == null || lowerrate.equals("") || lowerrate.equals("-")) rate = "";
                else rate = lowerrate + " ~ " + rs.getString("상한");
            }
            row.createCell(cellIndex++).setCellValue(rate);
        }

        adjustColumns();

        toFile();

        System.out.println("File created.");
    }

    class CellRangeAddress extends CellAddress {

        private CellAddress start;
        private CellAddress end;

        public CellRangeAddress(final CellAddress start, final CellAddress end) {
            super(start);
            this.start = start;
            this.end = end;
        }


        @Override
        public String formatAsString() {
            if (end != null) {
                return start.formatAsString() + ":" + end.formatAsString();
            }
            return super.formatAsString();
        }
    }
}