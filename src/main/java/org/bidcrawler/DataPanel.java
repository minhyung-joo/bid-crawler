package org.bidcrawler;

/**
 * Created by ravenjoo on 6/24/17.
 */

import org.bidcrawler.utils.Util;
import org.jdatepicker.DatePicker;
import org.jdatepicker.JDatePicker;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataPanel extends JPanel {
    // For SQL setup.
    Connection con;
    java.sql.Statement st;
    ResultSet rs;

    JPanel optionPanel;
    JComboBox<String> siteDrop;

    JTable data;
    ArrayList<SearchOptionPanel> searchPanels;

    JPanel bottomPanel;

    public DataPanel() {
        super();

        this.setLayout(new BorderLayout());

        searchPanels = new ArrayList<SearchOptionPanel>(10);

        optionPanel = new JPanel();
        siteDrop = new JComboBox<String>(Util.SITES);
        siteDrop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String site = siteDrop.getSelectedItem().toString();
                for (int i = 0; i < 10; i++) {
                    searchPanels.get(i).changeWork(site);
                }
            }
        });
        optionPanel.add(new JLabel("사이트 : "));
        optionPanel.add(siteDrop);

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        data = new JTable(new DefaultTableModel(Util.COLUMNS, 0));
        for (int i = 0; i < data.getColumnCount(); i++) {
            data.getColumn(Util.COLUMNS[i]).setCellRenderer(rightRender);
        }
        data.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                data.scrollRectToVisible(data.getCellRect(data.getRowCount() - 1, 0, true));
            }
        });
        data.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        data.setIntercellSpacing(new Dimension(1, 1));
        //data.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scroll = new JScrollPane(data);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.PAGE_AXIS));
        for (int i = 0; i < 10; i++) {
            SearchOptionPanel sop = new SearchOptionPanel();
            bottomPanel.add(sop);
            searchPanels.add(sop);
        }

        JScrollPane bottomScroll = new JScrollPane(bottomPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        bottomScroll.setPreferredSize(new Dimension(this.getWidth(), 400));

        this.add(optionPanel, BorderLayout.NORTH);
        this.add(scroll, BorderLayout.CENTER);
        this.add(bottomScroll, BorderLayout.SOUTH);
    }

    public void adjustColumns() {
        data.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
        data.setRowHeight(20);
        final TableColumnModel columnModel = data.getColumnModel();
        for (int i = 0; i < 17; i++) {
            int width = 50;
            for (int j = 0; j < data.getRowCount(); j++) {
                TableCellRenderer renderer = data.getCellRenderer(j, i);
                Component comp = data.prepareRenderer(renderer, j, i);
                width = Math.max(comp.getPreferredSize().width + 1, width);
            }
            DefaultTableCellRenderer leftRender = new DefaultTableCellRenderer();
            leftRender.setHorizontalAlignment(SwingConstants.LEFT);
            if ((i < 4) || (i > 9)) {
                columnModel.getColumn(i).setCellRenderer(leftRender);
            }

            if (i == 3) width = 50;
            if (i > 13 && i < 16) {
                if (width > 100) width = 100;
            }
            if (width > 150) width = 150;

            columnModel.getColumn(i).setPreferredWidth(width);
        }
    }

    private class SearchOptionPanel extends JPanel {

        JComboBox<String> workDrop;
        JTextField orgInput;
        JButton orgSearch;
        JCheckBox dateCheck;
        DatePicker startDate;
        DatePicker endDate;
        JCheckBox rateCheck;
        JTextField upperInput;
        JTextField lowerInput;
        JButton searchButton;
        JButton excelButton;

        public SearchOptionPanel() {
            super();
            workDrop = new JComboBox<String>();
            orgInput = new JTextField(15);
            orgSearch = new JButton("검색");
            orgSearch.addActionListener(new OrgListener());
            dateCheck = new JCheckBox();
            startDate = new JDatePicker(Calendar.getInstance().getTime());
            startDate.setTextEditable(true);
            endDate = new JDatePicker(Calendar.getInstance().getTime());
            endDate.setTextEditable(true);
            rateCheck = new JCheckBox();
            upperInput = new JTextField(4);
            upperInput.setText("0.00");
            lowerInput = new JTextField(4);
            lowerInput.setText("-3.00");
            searchButton = new JButton("검색");
            searchButton.addActionListener(new SearchListener());
            excelButton = new JButton("엑셀저장");
            excelButton.addActionListener(new ExcelListener());

            this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            this.add(new JLabel("구분 : "));
            this.add(workDrop);
            JLabel o = new JLabel("발주기관 : ");
            o.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            this.add(o);
            this.add(orgInput);
            this.add(orgSearch);
            JLabel d = new JLabel("개찰일시 ");
            d.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            this.add(d);
            this.add(dateCheck);
            this.add((JComponent) startDate);
            this.add(new JLabel(" ~ "));
            this.add((JComponent) endDate);
            JLabel r = new JLabel("사정률 ");
            r.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
            this.add(r);
            this.add(rateCheck);
            this.add(upperInput);
            this.add(new JLabel(" ~ "));
            this.add(lowerInput);
            this.add(searchButton);
            this.add(excelButton);
        }

        public void changeWork(String site) {
            workDrop.removeAllItems();
            if (site.equals("LH공사")) {
                DefaultComboBoxModel model = new DefaultComboBoxModel(Util.LH_WORKS);
                workDrop.setModel(model);
            }
            else if (site.equals("한국마사회")) {
                DefaultComboBoxModel model = new DefaultComboBoxModel(Util.LETS_WORKS);
                workDrop.setModel(model);
            }
            else if (site.equals("도로공사")) {
                DefaultComboBoxModel model = new DefaultComboBoxModel(Util.EX_WORKS);
                workDrop.setModel(model);
            }
        }

        private class OrgListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                String site = siteDrop.getSelectedItem().toString();
                try {
                    OrgFrame o = new OrgFrame(orgInput, site, false);
                } catch (ClassNotFoundException | SQLException e1) {
                    Logger.getGlobal().log(Level.WARNING, e1.getMessage(), e1);
                    e1.printStackTrace();
                }
            }
        }

        private class SearchListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String sd = null;
                String ed = null;
                String lowerBound = null;
                String upperBound = null;
                String today = sdf.format(new Date()) + " 00:00:00";

                if (dateCheck.isSelected()) {
                    if ((startDate.getModel().getValue() != null) && (endDate.getModel().getValue() != null)) {
                        sd = sdf.format(startDate.getModel().getValue());
                        ed = sdf.format(endDate.getModel().getValue());
                    }
                    else {
                        JOptionPane.showMessageDialog(null, "날짜를 바르게 입력해주십시오.");
                        return;
                    }
                }

                if (rateCheck.isSelected() && siteDrop.getSelectedItem().toString().equals("국방조달청")) {
                    lowerBound = Util.parseRate(lowerInput.getText());
                    upperBound = Util.parseRate(upperInput.getText());
                }

                // Set up SQL connection.
                Connection con = null;
                try {
                    String site = siteDrop.getSelectedItem().toString();
                    String workType = "";
                    if (!(site.equals("국방조달청") || site.equals("철도시설공단"))) workType = workDrop.getSelectedItem().toString();
                    String org = orgInput.getText();
                    String tableName = "";

                    con = DriverManager.getConnection(
                            "jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8",
                            Util.DB_ID,
                            Util.DB_PW
                    );
                    st = con.createStatement();
                    rs = null;

                    if (site.equals("LH공사")) tableName = "lhbidinfo";
                    else if (site.equals("국방조달청")) tableName = "dapabidinfo";
                    else if (site.equals("한국마사회")) tableName = "letsrunbidinfo";
                    else if (site.equals("도로공사")) tableName = "exbidinfo";
                    else if (site.equals("철도시설공단")) tableName = "railnetbidinfo";

                    String sql = "";
                    sql = "SELECT * FROM " + tableName + " WHERE ";
                    if (!org.equals("")) {
                        if (site.equals("LH공사")) sql += "지역본부=\"" + org + "\" AND ";
                        else if (site.equals("국방조달청")) sql += "발주기관=\"" + org + "\" AND ";
                        else if (site.equals("도로공사")) sql += "지역=\"" + org + "\" AND ";
                        else if (site.equals("한국마사회")) sql += "사업장=\"" + org + "\" AND ";
                    }
                    if (dateCheck.isSelected()) {
                        sql += "개찰일시 >= \"" + sd + "\" AND 개찰일시 <= \"" + ed + "\" AND ";
                    }
                    if (!site.equals("국방조달청") && !workType.equals("전체")) {
                        if (site.equals("LH공사")) sql += "업무=\"" + workType + "\" AND ";
                        else if (site.equals("도로공사")) sql += "분류=\"" + workType + "\" AND ";
                        else if (site.equals("한국마사회")) sql += "입찰구분=\"" + workType + "\" AND ";
                    }
                    if (rateCheck.isSelected() && site.equals("국방조달청")) {
                        String rate = lowerBound + " ~ " + upperBound;
                        sql += "사정률=\"" + rate + "\" AND ";
                    }
                    sql += "완료 > 0 ";

                    // Add unopened notis
                    sql += "UNION SELECT * FROM " + tableName + " WHERE ";
                    if (!org.equals("")) {
                        if (site.equals("LH공사")) sql += "지역본부=\"" + org + "\" AND ";
                        else if (site.equals("국방조달청")) sql += "발주기관=\"" + org + "\" AND ";
                        else if (site.equals("도로공사")) sql += "지역=\"" + org + "\" AND ";
                        else if (site.equals("한국마사회")) sql += "사업장=\"" + org + "\" AND ";
                    }
                    if (!site.equals("국방조달청") && !workType.equals("전체")) {
                        if (site.equals("LH공사")) sql += "업무=\"" + workType + "\" AND ";
                        else if (site.equals("도로공사")) sql += "분류=\"" + workType + "\" AND ";
                        else if (site.equals("한국마사회")) sql += "입찰구분=\"" + workType + "\" AND ";
                    }
                    if (rateCheck.isSelected() && site.equals("국방조달청")) {
                        sql += "하한=\"" + lowerBound + "\" AND 상한=\"" + upperBound + "\" AND ";
                    }
                    sql += "개찰일시 >= \"" + today + "\" ORDER BY 개찰일시, 공고번호";

                    System.out.println(sql);
                    rs = st.executeQuery(sql);

                    DefaultTableModel m = (DefaultTableModel) data.getModel();
                    m.setRowCount(0);
                    int index = 1;
                    while(rs.next()) {
                        boolean valid = true;
                        String bPrice = "";
                        double baseValue = 0;
                        if (site.equals("LH공사")) bPrice = rs.getString("기초금액");
                        else if (site.equals("국방조달청")) bPrice = rs.getString("기초예비가격");
                        else if (site.equals("도로공사") || site.equals("철도시설공단")) bPrice = rs.getString("설계금액");
                        else if (site.equals("한국마사회")) bPrice = rs.getString("예비가격기초금액");
                        if (bPrice == null) bPrice = "";
                        if (!bPrice.equals("") && !(bPrice.equals("0") || bPrice.equals("0.00"))) {
                            double amount = Double.parseDouble(bPrice);
                            baseValue = amount;
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            bPrice = formatter.format(amount);
                        }
                        else valid = false;

                        double[] ratios = new double[15];
                        for (int i = 1; i <= 15; i++) {
                            String dupPrice = rs.getString("복수" + i);
                            if (dupPrice == null || dupPrice.equals("")) {
                                valid = false;
                                break;
                            }
                            double dupValue = Double.parseDouble(dupPrice);
                            if (dupValue == 0) {
                                valid = false;
                                break;
                            }

                            double ratio = (dupValue / baseValue - 1) * 100;
                            ratios[i-1] = ratio;
                        }
                        Arrays.sort(ratios);
                        double largestRatio = ratios[14];
                        double smallestRatio = ratios[0];

                        double largestLB = 0;
                        double largestUB = 0;
                        double smallestLB = 0;
                        double smallestUB = 0;
                        if (site.equals("LH공사")) {
                            largestLB = 1.70;
                            largestUB = 2.00;
                            smallestLB = -2.00;
                            smallestUB = -1.70;

                            if (largestRatio >= largestUB || largestRatio <= largestLB) valid = false;
                            if (smallestRatio >= smallestUB || smallestRatio <= smallestLB) valid = false;
                        }
                        else if (site.equals("철도시설공단")) {
                            largestLB = 2.00;
                            largestUB = 2.70;
                            smallestLB = -2.70;
                            smallestUB = -2.10;

                            if (largestRatio >= largestUB || largestRatio <= largestLB) valid = false;
                            if (smallestRatio >= smallestUB || smallestRatio <= smallestLB) valid = false;
                        }
                        else if (site.equals("도로공사") || site.equals("한국마사회")) {
                            largestLB = 2.50;
                            largestUB = 3.001;
                            smallestLB = -3.00;
                            smallestUB = -2.50;

                            if (largestRatio >= largestUB || largestRatio <= largestLB) valid = false;
                            if (smallestRatio >= smallestUB || smallestRatio <= smallestLB) valid = false;
                        }

                        Date dateCheck = rs.getDate("개찰일시");
                        Calendar passCalendar = Calendar.getInstance();
                        passCalendar.set(Calendar.HOUR_OF_DAY, 0);
                        passCalendar.set(Calendar.MINUTE, 0);
                        passCalendar.set(Calendar.SECOND, 0);
                        passCalendar.set(Calendar.MILLISECOND, 0);
                        Date passDate = passCalendar.getTime();
                        if (dateCheck.after(passDate)) valid = true;

                        if (!valid) {
                            continue;
                        }

                        String bidno = rs.getString("공고번호");

                        String date = rs.getString("개찰일시");
                        if (date.length() == 21) {
                            date = date.substring(2, 4) + date.substring(5, 7) + date.substring(8, 10) + " " + date.substring(11, 16);
                        }

                        String limit = "-";
                        if (site.equals("국방조달청")) limit = rs.getString("면허명칭");
                        else if (site.equals("나라장터") || site.equals("도로공사")) limit = rs.getString("업종제한사항");

                        String ePrice = "";
                        if (site.equals("LH공사")) ePrice = rs.getString("예정금액");
                        else ePrice = rs.getString("예정가격");
                        if (ePrice == null) ePrice = "";
                        if (!ePrice.equals("") && !(ePrice.equals("0") || ePrice.equals("0.00"))) {
                            double amount = Double.parseDouble(ePrice);
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            ePrice = formatter.format(amount);
                        }
                        else ePrice = "-";

                        String tPrice = rs.getString("투찰금액");
                        if (tPrice == null) tPrice = "";
                        if (!tPrice.equals("") && !(tPrice.equals("0") || tPrice.equals("0.00"))) {
                            double amount = Double.parseDouble(tPrice);
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            tPrice = formatter.format(amount);
                        }
                        else tPrice = "-";

                        String dPrice1 = rs.getString("복수1");
                        if (dPrice1 == null) dPrice1 = "";
                        if (!dPrice1.equals("") && !(dPrice1.equals("0") || dPrice1.equals("0.00"))) {
                            double amount = Double.parseDouble(dPrice1);
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            dPrice1 = formatter.format(amount);
                        }
                        else dPrice1 = "-";

                        String dPrice2 = rs.getString("복수15");
                        if (dPrice2 == null) dPrice2 = "";
                        if (!dPrice2.equals("") && !(dPrice2.equals("0") || dPrice2.equals("0.00"))) {
                            double amount = Double.parseDouble(dPrice2);
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            dPrice2 = formatter.format(amount);
                        }
                        else dPrice2 = "-";

                        String comp = "";
                        if (site.equals("LH공사") || site.equals("도로공사")) comp = rs.getString("참가수");
                        else if (site.equals("국방조달청") || site.equals("한국마사회") || site.equals("나라장터")) comp = rs.getString("참여수");
                        if (comp == null) comp = "";
                        if (!comp.equals("") && !comp.equals("0")) {
                            double amount = Double.parseDouble(comp);
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            comp = formatter.format(amount);
                        }
                        else comp = "-";

                        String eDate = "";
                        eDate = rs.getString("개찰일시");
                        if (eDate != null) {
                            if (eDate.length() == 21) {
                                eDate = eDate.substring(2, 4) + eDate.substring(5, 7) + eDate.substring(8, 10) + " " + eDate.substring(11, 16);
                            }
                        }

                        String prog = "";
                        if (site.equals("LH공사")) prog = rs.getString("개찰내역");
                        else if (site.equals("한국마사회")) prog = rs.getString("개찰상태");
                        else if (site.equals("도로공사")) prog = rs.getString("결과상태");
                        else if (site.equals("국방조달청")) prog = rs.getString("입찰결과");
                        else if (site.equals("철도시설공단")) prog = rs.getString("개찰결과");

                        String annOrg = "";
                        if (site.equals("철도시설공단")) annOrg = rs.getString("공고기관");
                        else if (site.equals("LH공사")) annOrg = rs.getString("지역본부");
                        else if (site.equals("한국마사회")) annOrg = rs.getString("사업장");
                        else if (site.equals("국방조달청")) annOrg = rs.getString("발주기관");
                        else if (site.equals("도로공사")) annOrg = rs.getString("지역");

                        String demOrg = "";
                        if (site.equals("철도시설공단")) demOrg = rs.getString("수요기관");
                        else if (site.equals("LH공사")) demOrg = rs.getString("지역본부");
                        else if (site.equals("한국마사회")) demOrg = rs.getString("사업장");
                        else if (site.equals("국방조달청")) demOrg = rs.getString("발주기관");
                        else if (site.equals("도로공사")) demOrg = rs.getString("지역");

                        String bidType = "";
                        if (site.equals("나라장터") || site.equals("한국마사회") || site.equals("LH공사")) bidType = rs.getString("입찰방식");
                        else if (site.equals("국방조달청")) bidType = rs.getString("입찰방법");

                        String compType = rs.getString("계약방법");

                        String priceMethod = "";
                        if (site.equals("철도시설공단")) priceMethod = rs.getString("예가방식");
                        else if (site.equals("한국마사회")) priceMethod = rs.getString("예정가격방식");

                        m.addRow(new Object[] { index, bidno, date, limit, bPrice, ePrice, tPrice, dPrice1, dPrice2,
                                comp, eDate, prog, annOrg, demOrg, bidType, compType, priceMethod });
                        index++;
                    }
                    adjustColumns();

                    con.close();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        }

        private class ExcelListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                String sd = null;
                String ed = null;
                String lowerBound = null;
                String upperBound = null;

                if (dateCheck.isSelected()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                    sd = sdf.format(startDate.getModel().getValue());
                    ed = sdf.format(endDate.getModel().getValue());
                }

                if (rateCheck.isSelected() && siteDrop.getSelectedItem().toString().equals("국방조달청")) {
                    lowerBound = Util.parseRate(lowerInput.getText());
                    upperBound = Util.parseRate(upperInput.getText());
                }

                try {
                    String site = siteDrop.getSelectedItem().toString();
                    String org = orgInput.getText().equals("") ? null : orgInput.getText();
                    String workType = null;
                    if (!(site.equals("국방조달청") || site.equals("철도시설공단"))) {
                        if (!workDrop.getSelectedItem().toString().equals("전체")) {
                            workType = workDrop.getSelectedItem().toString();
                        }
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    String curDate = sdf.format(Calendar.getInstance().getTime());
                    ExcelWriter ew = new ExcelWriter(site, "입찰");
                    ew.setOptions(sd, ed, org, workType, lowerBound, upperBound);
                    ew.toExcel();
                } catch (Exception ex) {
                    Logger.getGlobal().log(Level.WARNING, ex.getMessage(), ex);
                    ex.printStackTrace();
                }
            }
        }
    }
}