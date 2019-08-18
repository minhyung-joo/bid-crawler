package org.bidcrawler;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executors;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import org.bidcrawler.utils.Util;

public class DayCheckFrame extends CheckFrame {
    DayCheckFrame pointer;

    public DayCheckFrame(String site)
    {
        super(site + " 일자별조회");
        this.site = site;
        pointer = this;

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        toggle.addActionListener(new UpdateListener());

        JPanel datePanel = new JPanel();
        datePanel.setLayout(new BoxLayout(datePanel, 3));

        JPanel sdPanel = new JPanel();
        sdPanel.add(new JLabel("시작일자 : "));
        sdPanel.add((JComponent)startDatePicker);
        JPanel edPanel = new JPanel();
        edPanel.add(new JLabel("종료일자 : "));
        edPanel.add((JComponent)endDatePicker);
        JPanel rePanel = new JPanel();
        rePanel.add(new JLabel("조회간격 : "));
        rePanel.add(reps);
        rePanel.add(new JLabel("초"));

        datePanel.add(sdPanel);
        datePanel.add(edPanel);
        datePanel.add(rePanel);
        datePanel.add(toggle);

        JScrollPane scroll = new JScrollPane(table);

        panel.add(autoCheck, "North");
        panel.add(scroll, "Center");
        panel.add(datePanel, "South");

        add(panel);
        setSize(500, 800);
        setResizable(false);
        setDefaultCloseOperation(2);
        setVisible(true);
    }

    private class UpdateListener implements ActionListener {
        public ArrayList<String> queryByDay(String sd, String ed) throws ParseException {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar sdate = Calendar.getInstance();
            sdate.setTime(sdf.parse(sd));
            Calendar edate = Calendar.getInstance();
            edate.setTime(sdf.parse(ed));
            ArrayList<String> dates = new ArrayList();
            do {
                dates.add(sdf.format(sdate.getTime()));
                sdate.add(5, 1);
                if (sdate.equals(edate)) {
                    dates.add(sdf.format(sdate.getTime()));
                }
            } while ((edate.after(sdate)) && (dates.size() < 31));
            return dates;
        }

        public void actionPerformed(ActionEvent e) {
            SimpleDateFormat sdf;
            if (!running) {
                sdf = new SimpleDateFormat("yyyy-MM-dd");
                DefaultTableModel tm = (DefaultTableModel)table.getModel();
                es = Executors.newFixedThreadPool(31);
                progressArray.clear();
                totalArray.clear();
                parsers.clear();

                if ((startDatePicker.getModel().getValue() == null) || (endDatePicker.getModel().getValue() == null)) {
                    JOptionPane.showMessageDialog(null, "날짜를 설정해주십시오.");
                    return;
                }

                startDate = sdf.format(startDatePicker.getModel().getValue());
                endDate = sdf.format(endDatePicker.getModel().getValue());


                configureAutoMode();
                try
                {
                    timeFrames = queryByDay(startDate, endDate);
                    con = DriverManager.getConnection("jdbc:mysql://localhost/bid_db_2?useUnicode=true&characterEncoding=euckr", Util.DB_ID, Util.DB_PW);
                    st = con.createStatement();
                    rs = null;

                    int threadIndex = 0;
                    tm.setRowCount(0);
                    for (String sday : timeFrames) {
                        int dbcount = 0;
                        int svcount = 0;
                        String sql = "";
                        Parser parser = null;
                        if (site.equals("국방조달청")) {
                            parser = new NewDapaParser("", "", "", null, pointer);
                            sql = "SELECT COUNT(*) FROM dapabidinfo WHERE 개찰일시 BETWEEN \"" + sday + " 00:00:00\" AND \"" + sday + " 23:59:59\" AND 결과=1;";
                        }
                        else if (site.equals("한국마사회")) {
                            parser = new LetsParser("", "", "", null, pointer);
                            parser.setOption("결과");
                            sql = "SELECT COUNT(*) FROM letsrunbidinfo WHERE 개찰일시 BETWEEN \"" + sday + " 00:00:00\" AND \"" + sday + " 23:59:59\" AND 완료=1;";
                        }
                        else if (site.equals("도로공사")) {
                            parser = new ExParser("", "", "", null, pointer);
                            sql = "SELECT COUNT(DISTINCT 공고번호) FROM exbidinfo WHERE 개찰일시 BETWEEN \"" + sday + " 00:00:00\" AND \"" + sday + " 23:59:59\" AND 중복번호=1 AND 완료=1;";
                        }
                        else if (site.equals("LH공사")) {
                            parser = new NewLHParser("", "", "", null, pointer);
                            sql = "SELECT COUNT(*) FROM lhbidinfo WHERE 개찰일시 BETWEEN \"" + sday + " 00:00:00\" AND \"" + sday + " 23:59:59\" AND 완료=1;";
                        }
                        else if (site.equals("철도시설공단")) {
                            parser = new NewRailnetParser("", "", "", null, pointer);
                            sql = "SELECT COUNT(*) FROM railnetbidinfo WHERE 개찰일시 BETWEEN \"" + sday + " 00:00:00\" AND \"" + sday + " 23:59:59\" AND 완료=1;";
                        }

                        rs = st.executeQuery(sql);
                        if (rs.next()) {
                            dbcount = rs.getInt(1);
                        }

                        parser.setDate(sday, sday);
                        svcount = parser.getTotal();
                        int diff = svcount - dbcount;
                        String progressText = "";

                        if (diff > 0) {
                            parsers.add(parser);
                            es.submit(parser);
                            progressText = "0/" + svcount;
                        }
                        if (diff < 0) {
                            parser.setOption("건수차이");
                            parsers.add(parser);
                            es.submit(parser);
                        }

                        progressArray.add(Integer.valueOf(0));
                        totalArray.add(Integer.valueOf(svcount));
                        parser.setThreadIndex(threadIndex++);
                        tm.addRow(new Object[] { sday, Integer.valueOf(svcount), Integer.valueOf(dbcount), Integer.valueOf(diff), progressText });
                    }
                } catch (ClassNotFoundException|SQLException|ParseException|IOException e1) { int threadIndex;
                    e1.printStackTrace();
                }

                adjustColumns();

                if (parsers.size() > 0) {
                    counter = 0;
                    threadNum = parsers.size();
                    running = true;
                    toggle.setText("중지");
                }
            }
            else if (running) {
                for (Parser p : parsers) {
                    p.shutdownNow();
                }

                progressArray.clear();
                totalArray.clear();
                parsers.clear();
                es.shutdownNow();
                running = false;
                toggle.setText("시작");
            }
        }
    }
}
