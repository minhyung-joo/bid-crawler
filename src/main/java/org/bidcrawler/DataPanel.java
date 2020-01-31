package org.bidcrawler;

/**
 * Created by ravenjoo on 6/24/17.
 */
import com.sun.rowset.CachedRowSetImpl;
import org.bidcrawler.utils.Util;
import org.jdatepicker.DatePicker;
import org.jdatepicker.JDatePicker;

import javax.sql.rowset.CachedRowSet;
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
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataPanel extends JPanel {
    // For SQL setup.
    Connection con;
    java.sql.Statement st;
    ResultSet rs;
    CachedRowSet cachedRowSet;
    String lastSql;

    String type;
    JPanel optionPanel;
    JComboBox<String> siteDrop;

    JTable data;
    ArrayList<SearchOptionPanel> searchPanels;

    JPanel bottomPanel;

    public DataPanel(String type) throws SQLException {
        super();

        this.type = type;
        this.setLayout(new BorderLayout());

        cachedRowSet = new CachedRowSetImpl();
        searchPanels = new ArrayList<>(10);

        optionPanel = new JPanel();
        siteDrop = new JComboBox<>(Util.SITES);
        siteDrop.addActionListener(e -> {
            String site = siteDrop.getSelectedItem().toString();
            for (int i = 0; i < 10; i++) {
                searchPanels.get(i).changeWork(site);
                searchPanels.get(i).changeType(site);
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
        data.setIntercellSpacing(new Dimension(1, 1));
        data.setRowHeight(20);

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

        JComboBox typeDrop;
        JComboBox workDrop;
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
            typeDrop = new JComboBox();
            DefaultComboBoxModel typeModel = new DefaultComboBoxModel(Util.DAPA_TYPES);
            typeDrop.setModel(typeModel);
            workDrop = new JComboBox();
            DefaultComboBoxModel workModel = new DefaultComboBoxModel(Util.DAPA_WORKS);
            workDrop.setModel(workModel);
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
            this.add(new JLabel("입찰 : "));
            this.add(typeDrop);
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
            else if (site.equals("국방조달청")) {
                DefaultComboBoxModel model = new DefaultComboBoxModel(Util.DAPA_WORKS);
                workDrop.setModel(model);
            }
        }

        public void changeType(String site) {
            typeDrop.removeAllItems();
            if (site.equals("국방조달청")) {
                DefaultComboBoxModel model = new DefaultComboBoxModel(Util.DAPA_TYPES);
                typeDrop.setModel(model);
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
                String site = siteDrop.getSelectedItem().toString();
                String sd = null;
                String ed = null;
                String lowerBound = null;
                String upperBound = null;
                String bidType = typeDrop.getModel().getSize() > 0 ? typeDrop.getSelectedItem().toString() : null;
                String workType = workDrop.getModel().getSize() > 0 ? workDrop.getSelectedItem().toString() : null;
                String today = sdf.format(new Date());

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
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                } catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
                Connection con = null;
                try {
                    String org = orgInput.getText();

                    con = DriverManager.getConnection(
                            "jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8",
                            Util.DB_ID,
                            Util.DB_PW
                    );
                    st = con.createStatement();
                    rs = null;

                    String sql = null;
                    switch (site) {
                        case "국방조달청":
                            sql = Util.selectDapa(org, sd, ed, workType, lowerBound, upperBound, bidType, today);
                            break;
                        case "LH공사":
                            sql = Util.selectLh(org, sd, ed, workType, lowerBound, upperBound, bidType, today);
                            break;
                        case "한국마사회":
                            sql = Util.selectLets(org, sd, ed, workType, lowerBound, upperBound, bidType, today);
                            break;
                        case "도로공사":
                            sql = Util.selectEx(org, sd, ed, workType, lowerBound, upperBound, bidType, today);
                            break;
                        case "철도시설공단":
                            sql = Util.selectRailnet(org, sd, ed, workType, lowerBound, upperBound, bidType, today);
                            break;
                    }

                    System.out.println(sql);
                    rs = st.executeQuery(sql);
                    lastSql = sql;
                    cachedRowSet.release();
                    cachedRowSet.populate(rs);

                    DefaultTableModel m = (DefaultTableModel) data.getModel();
                    m.setRowCount(0);
                    int index = 1;
                    while(cachedRowSet.next()) {
                        if (!Util.checkDataValidity(cachedRowSet, site)) {
                            continue;
                        }

                        String bidno = cachedRowSet.getString("공고번호");

                        String date = cachedRowSet.getString("개찰일시");
                        if (date.length() == 21) {
                            date = date.substring(2, 4) + date.substring(5, 7) + date.substring(8, 10) + " " + date.substring(11, 16);
                        }

                        String limit = "-";
                        if (site.equals("국방조달청")) limit = cachedRowSet.getString("면허명칭");
                        else if (site.equals("나라장터") || site.equals("도로공사")) limit = cachedRowSet.getString("업종제한사항");

                        String bPrice = "";
                        if (site.equals("LH공사")) bPrice = cachedRowSet.getString("기초금액");
                        else if (site.equals("국방조달청")) bPrice = cachedRowSet.getString("기초예비가격");
                        else if (site.equals("도로공사") || site.equals("철도시설공단")) bPrice = cachedRowSet.getString("설계금액");
                        else if (site.equals("한국마사회")) bPrice = cachedRowSet.getString("예비가격기초금액");

                        String ePrice = "";
                        if (site.equals("LH공사")) ePrice = cachedRowSet.getString("예정금액");
                        else ePrice = cachedRowSet.getString("예정가격");
                        if (ePrice == null) ePrice = "";
                        if (!ePrice.equals("") && !(ePrice.equals("0") || ePrice.equals("0.00"))) {
                            double amount = Double.parseDouble(ePrice);
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            ePrice = formatter.format(amount);
                        }
                        else ePrice = "-";

                        String tPrice = cachedRowSet.getString("투찰금액");
                        if (tPrice == null) tPrice = "";
                        if (!tPrice.equals("") && !(tPrice.equals("0") || tPrice.equals("0.00"))) {
                            double amount = Double.parseDouble(tPrice);
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            tPrice = formatter.format(amount);
                        }
                        else tPrice = "-";

                        String dPrice1 = cachedRowSet.getString("복수1");
                        if (dPrice1 == null) dPrice1 = "";
                        if (!dPrice1.equals("") && !(dPrice1.equals("0") || dPrice1.equals("0.00"))) {
                            double amount = Double.parseDouble(dPrice1);
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            dPrice1 = formatter.format(amount);
                        }
                        else dPrice1 = "-";

                        String dPrice2 = cachedRowSet.getString("복수15");
                        if (dPrice2 == null) dPrice2 = "";
                        if (!dPrice2.equals("") && !(dPrice2.equals("0") || dPrice2.equals("0.00"))) {
                            double amount = Double.parseDouble(dPrice2);
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            dPrice2 = formatter.format(amount);
                        }
                        else dPrice2 = "-";

                        String comp = "";
                        if (site.equals("LH공사") || site.equals("도로공사")) comp = cachedRowSet.getString("참가수");
                        else if (site.equals("국방조달청") || site.equals("한국마사회") || site.equals("나라장터")) comp = cachedRowSet.getString("참여수");
                        if (comp == null) comp = "";
                        if (!comp.equals("") && !comp.equals("0")) {
                            double amount = Double.parseDouble(comp);
                            DecimalFormat formatter = new DecimalFormat("#,###");
                            comp = formatter.format(amount);
                        }
                        else comp = "-";

                        String eDate = "";
                        eDate = cachedRowSet.getString("개찰일시");
                        if (eDate != null) {
                            if (eDate.length() == 21) {
                                eDate = eDate.substring(2, 4) + eDate.substring(5, 7) + eDate.substring(8, 10) + " " + eDate.substring(11, 16);
                            }
                        }

                        String prog = "";
                        if (site.equals("LH공사")) prog = cachedRowSet.getString("개찰내역");
                        else if (site.equals("한국마사회")) prog = cachedRowSet.getString("개찰상태");
                        else if (site.equals("도로공사")) prog = cachedRowSet.getString("결과상태");
                        else if (site.equals("국방조달청")) prog = cachedRowSet.getString("입찰결과");
                        else if (site.equals("철도시설공단")) prog = cachedRowSet.getString("개찰결과");

                        String annOrg = "";
                        if (site.equals("철도시설공단")) annOrg = cachedRowSet.getString("공고기관");
                        else if (site.equals("LH공사")) annOrg = cachedRowSet.getString("지역본부");
                        else if (site.equals("한국마사회")) annOrg = cachedRowSet.getString("사업장");
                        else if (site.equals("국방조달청")) annOrg = cachedRowSet.getString("발주기관");
                        else if (site.equals("도로공사")) annOrg = cachedRowSet.getString("지역");

                        String demOrg = "";
                        if (site.equals("철도시설공단")) demOrg = cachedRowSet.getString("수요기관");
                        else if (site.equals("LH공사")) demOrg = cachedRowSet.getString("지역본부");
                        else if (site.equals("한국마사회")) demOrg = cachedRowSet.getString("사업장");
                        else if (site.equals("국방조달청")) demOrg = cachedRowSet.getString("발주기관");
                        else if (site.equals("도로공사")) demOrg = cachedRowSet.getString("지역");

                        bidType = "";
                        if (site.equals("나라장터") || site.equals("한국마사회") || site.equals("LH공사")) bidType = cachedRowSet.getString("입찰방식");
                        else if (site.equals("국방조달청")) bidType = cachedRowSet.getString("입찰방법");

                        String compType = cachedRowSet.getString("계약방법");

                        String priceMethod = "";
                        if (site.equals("철도시설공단")) priceMethod = cachedRowSet.getString("예가방식");
                        else if (site.equals("한국마사회")) priceMethod = cachedRowSet.getString("예정가격방식");

                        m.addRow(new Object[] { index, bidno, date, limit, bPrice, ePrice, tPrice, dPrice1, dPrice2,
                                comp, eDate, prog, annOrg, demOrg, bidType, compType, priceMethod });
                        index++;
                    }

                    adjustColumns();
                    data.setRowHeight(24);
                    con.close();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        }

        private class ExcelListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                SimpleDateFormat todayFormat = new SimpleDateFormat("yyyy-MM-dd");
                String today = todayFormat.format(new Date());
                String sd = null;
                String ed = null;
                String lowerBound = null;
                String upperBound = null;
                String bidType = typeDrop.getModel().getSize() > 0 ? typeDrop.getSelectedItem().toString() : null;
                String org = orgInput.getText();
                String workType = workDrop.getModel().getSize() > 0 ? workDrop.getSelectedItem().toString() : null;

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
                    String sql = null;
                    switch (site) {
                        case "국방조달청":
                            sql = Util.selectDapa(org, sd, ed, workType, lowerBound, upperBound, bidType, today);
                            break;
                        case "LH공사":
                            sql = Util.selectLh(org, sd, ed, workType, lowerBound, upperBound, bidType, today);
                            break;
                        case "한국마사회":
                            sql = Util.selectLets(org, sd, ed, workType, lowerBound, upperBound, bidType, today);
                            break;
                        case "도로공사":
                            sql = Util.selectEx(org, sd, ed, workType, lowerBound, upperBound, bidType, today);
                            break;
                        case "철도시설공단":
                            sql = Util.selectRailnet(org, sd, ed, workType, lowerBound, upperBound, bidType, today);
                            break;
                    }

                    ExcelWriter ew;
                    if (sql.equals(lastSql)) {
                        ew = new ExcelWriter(site, sql, cachedRowSet);
                        System.out.println("cached");
                    } else {
                        ew = new ExcelWriter(site, sql, null);
                    }

                    ew.toExcel();
                } catch (Exception ex) {
                    Logger.getGlobal().log(Level.WARNING, ex.getMessage(), ex);
                    ex.printStackTrace();
                }
            }
        }
    }
}