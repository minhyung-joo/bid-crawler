package org.bidcrawler;

/**
 * Created by ravenjoo on 6/25/17.
 */
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import org.bidcrawler.utils.*;
import org.jdatepicker.DatePicker;
import org.jdatepicker.JDatePicker;

@SuppressWarnings("serial")
public class MonthCheckFrame extends JFrame {
    String[] columns = { "개찰일시", "사이트", "데이터베이스", "차수" };

    // For SQL setup.
    Connection con;
    java.sql.Statement st;
    ResultSet rs;

    ExecutorService es;
    Timer stateCheck;
    Timer auto;
    ArrayList<Future> states;
    ArrayList<Parser> parsers;

    boolean automode;
    boolean running;
    ArrayList<String> months;

    JCheckBox autoCheck;
    DatePicker startDate;
    DatePicker endDate;
    JTextArea reps;
    JTable table;
    String site;
    JButton toggle;

    public MonthCheckFrame(String site) {
        super(site + " 월별조회");
        this.site = site;

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        parsers = new ArrayList<Parser>();
        states = new ArrayList<Future>();
        stateCheck = new Timer();
        stateCheck.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (running) {
                    boolean done = true;
                    for (int i = 0; i < states.size(); i++) {
                        Future f = states.get(i);
                        if (!f.isDone()) done = false;
                        System.out.println(f.isDone());
                    }
                    if (done || states.size() == 0) {
                        states.clear();
                        toggle.doClick();
                    }
                }
            }
        }, 2000, 2000);

        auto = new Timer();

        automode = false;
        running = false;
        months = new ArrayList<String>();

        autoCheck = new JCheckBox("자동");
        startDate = new JDatePicker(Calendar.getInstance().getTime());
        startDate.setTextfieldColumns(12);
        startDate.setTextEditable(true);
        endDate = new JDatePicker(Calendar.getInstance().getTime());
        endDate.setTextfieldColumns(12);
        endDate.setTextEditable(true);
        reps = new JTextArea(1, 5);
        reps.setText("60");
        toggle = new JButton("시작");
        toggle.addActionListener(new UpdateListener());

        JPanel datePanel = new JPanel();
        datePanel.setLayout(new BoxLayout(datePanel, BoxLayout.PAGE_AXIS));

        JPanel sdPanel = new JPanel();
        sdPanel.add(new JLabel("시작일자 : "));
        sdPanel.add((JComponent) startDate);
        JPanel edPanel = new JPanel();
        edPanel.add(new JLabel("종료일자 : "));
        edPanel.add((JComponent) endDate);
        JPanel rePanel = new JPanel();
        rePanel.add(new JLabel("조회간격 : "));
        rePanel.add(reps);
        rePanel.add(new JLabel("초"));

        datePanel.add(sdPanel);
        datePanel.add(edPanel);
        datePanel.add(rePanel);
        datePanel.add(toggle);

        table = new JTable(new DefaultTableModel(columns, 0));
        JScrollPane scroll = new JScrollPane(table);

        panel.add(autoCheck, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(datePanel, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                states.clear();
                for (Parser p : parsers) {
                    p.shutdownNow();
                    p = null;
                }
                parsers.clear();

                if (es != null) es.shutdownNow();
                auto.cancel();
            }
        });

        this.add(panel);
        this.setSize(300, 410);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setVisible(true);
    }

    private class UpdateListener implements ActionListener {
        public ArrayList<String> queryByMonth(String sd, String ed) throws ParseException {
            SimpleDateFormat forq = new SimpleDateFormat("yyyy-MM");
            Calendar sdate = Calendar.getInstance();
            sdate.setTime(forq.parse(sd));
            Calendar edate = Calendar.getInstance();
            edate.setTime(forq.parse(ed));
            ArrayList<String> dates = new ArrayList<String>();
            do {
                dates.add(forq.format(sdate.getTime()));
                sdate.add(Calendar.MONTH, 1);
                if (sdate.equals(edate)) {
                    dates.add(forq.format(sdate.getTime()));
                }
            } while (edate.after(sdate) && dates.size() < 12);
            return dates;
        }

        public void actionPerformed(ActionEvent e) {
            if (!running) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                DefaultTableModel tm = (DefaultTableModel) table.getModel();
                String sd = "";
                String ed = "";
                es = Executors.newFixedThreadPool(12);

                if ((startDate.getModel().getValue() == null) || (endDate.getModel().getValue() == null)) {
                    JOptionPane.showMessageDialog(null, "날짜를 설정해주십시오.");
                    return;
                }
                else {
                    sd = sdf.format(startDate.getModel().getValue());
                    ed = sdf.format(endDate.getModel().getValue());
                }

                if (Util.isInteger(reps.getText()) && !reps.equals("0")) {
                    int rep = Integer.parseInt(reps.getText());
                    if (!automode && autoCheck.isSelected()) {
                        auto.scheduleAtFixedRate(new TimerTask() {
                            public void run() {
                                if (!running) {
                                    automode = true;
                                    toggle.doClick();
                                }
                                if (!autoCheck.isSelected()) {
                                    automode = false;
                                    auto.cancel();
                                }
                            }
                        }, rep * 1000, rep * 1000);
                    }
                }

                try {
                    months = queryByMonth(sd, ed);
                    con = DriverManager.getConnection(
                            "jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8",
                            Util.DB_ID,
                            Util.DB_PW
                    );
                    st = con.createStatement();
                    rs = null;

                    tm.setRowCount(0); // Empty the table.
                    for (String sm : months) {
                        Calendar sc = Calendar.getInstance();
                        int dbcount = 0;
                        int svcount = 0;

                        sm += "-01";
                        sc.setTime(sdf.parse(sm));
                        sc.add(Calendar.MONTH, 1);
                        sc.add(Calendar.DAY_OF_MONTH, -1);
                        String em = sdf.format(sc.getTime());

                        Parser parser = null;
                        if (site.equals("국방조달청")) {
                            parser = new DapaParser(sd, ed, "", null);

                            String sql = "SELECT COUNT(*) FROM dapabidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 완료=1;";
                            rs = st.executeQuery(sql);
                            if (rs.next()) {
                                dbcount = rs.getInt(1);
                            }
                            sql = "SELECT COUNT(*) FROM dapanegoinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 완료=1;";
                            rs = st.executeQuery(sql);
                            if (rs.next()) {
                                dbcount += rs.getInt(1);
                            }
                        }
                        else if (site.equals("한국마사회")) {
                            parser = new LetsParser(sd, ed, "", null);
                            rs = st.executeQuery("SELECT COUNT(*) FROM letsrunbidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 완료=1;");
                            if (rs.next()) {
                                dbcount = rs.getInt(1);
                            }
                        }
                        else if (site.equals("도로공사")) {
                            parser = new ExParser(sd, ed, "", null);
                            rs = st.executeQuery("SELECT COUNT(DISTINCT 공고번호) FROM exbidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 중복번호=1 AND 완료=1;");
                            if (rs.next()) {
                                dbcount = rs.getInt(1);
                            }
                        }
                        parser.setDate(sm, em);
                        svcount = parser.getTotal();
                        sm = sm.substring(0, sm.length()-3);
                        int diff = svcount - dbcount;
                        tm.addRow(new Object[] { sm, svcount, dbcount, diff });

                        if (diff > 0) {
                            parsers.add(parser);
                            Future f = es.submit(parser);
                            states.add(f);
                        }
                        if (diff < 0) {
                            parser.setOption("건수차이");
                            parsers.add(parser);
                            Future f = es.submit(parser);
                            states.add(f);
                        }
                    }
                } catch (ClassNotFoundException | SQLException | ParseException | IOException e1) {
                    e1.printStackTrace();
                }

                running = true;
                toggle.setText("중지");
            }
            else if (running) {
                states.clear();
                for (Parser p : parsers) {
                    p.shutdownNow();
                }
                es.shutdownNow();
                toggle.setText("시작");
                running = false;
            }
        }
    }
}