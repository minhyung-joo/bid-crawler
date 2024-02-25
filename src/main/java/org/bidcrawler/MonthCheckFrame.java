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

public class MonthCheckFrame extends CheckFrame {
    private MonthCheckFrame pointer;

    public MonthCheckFrame(String site)
    {
        super(site + " 월별조회");
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
        setSize(350, 450);
        setResizable(false);
        setDefaultCloseOperation(2);
        setVisible(true);
    }

    private class UpdateListener implements ActionListener { private UpdateListener() {}
        public ArrayList<String> queryByMonth(String sd, String ed) throws ParseException {
            SimpleDateFormat forq = new SimpleDateFormat("yyyy-MM");
            Calendar sdate = Calendar.getInstance();
            sdate.setTime(forq.parse(sd));
            Calendar edate = Calendar.getInstance();
            edate.setTime(forq.parse(ed));
            ArrayList<String> dates = new ArrayList();
            do {
                dates.add(forq.format(sdate.getTime()));
                sdate.add(2, 1);
                if (sdate.equals(edate)) {
                    dates.add(forq.format(sdate.getTime()));
                }
            } while ((edate.after(sdate)) && (dates.size() < 12));
            return dates;
        }

        public void actionPerformed(ActionEvent e) {
            if (!running) {
                new Thread(() -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    DefaultTableModel tm = (DefaultTableModel)table.getModel();
                    es = Executors.newFixedThreadPool(12);
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
                        timeFrames = queryByMonth(startDate, endDate);
                        con = DriverManager.getConnection(Util.DB_URL, Util.DB_ID, Util.DB_PW);
                        st = con.createStatement();
                        rs = null;

                        int threadIndex = 0;
                        tm.setRowCount(0);
                        for (String sm : timeFrames) {
                            Calendar sc = Calendar.getInstance();
                            int dbcount = 0;

                            sm = sm + "-01";
                            sc.setTime(sdf.parse(sm));
                            sc.add(2, 1);
                            sc.add(5, -1);
                            String em = sdf.format(sc.getTime());

                            String sql = "";
                            Parser parser = null;
                            if (site.equals("국방조달청")) {
                                parser = new NewDapaParser("", "", "", null, pointer);
                                sql = "SELECT COUNT(*) FROM dapabidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 결과=1;";
                            }
                            else if (site.equals("한국마사회")) {
                                parser = new LetsParser("", "", "", null, pointer);
                                sql = "SELECT COUNT(*) FROM letsrunbidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 완료=1;";
                            }
                            else if (site.equals("도로공사")) {
                                parser = new DoroParser("", "", "", null, pointer);
                                sql = "SELECT COUNT(DISTINCT 공고번호) FROM exbidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 중복번호=1 AND 완료=1;";
                            }
                            else if (site.equals("LH공사")) {
                                parser = new NewLHParser("", "", "", null, pointer);
                                sql = "SELECT COUNT(*) FROM lhbidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 완료=1;";
                            }
                            else if (site.equals("국가철도공단")) {
                                parser = new NewRailnetParser("", "", "", null, pointer);
                                sql = "SELECT COUNT(*) FROM railnetbidinfo WHERE 개찰일시 BETWEEN \"" + sm + " 00:00:00\" AND \"" + em + " 23:59:59\" AND 완료=1;";
                            }

                            rs = st.executeQuery(sql);
                            if (rs.next()) {
                                dbcount = rs.getInt(1);
                            }

                            parser.setDate(sm, em);
                            int svcount = parser.getTotal();
                            sm = sm.substring(0, sm.length() - 3);
                            String progressText = "";

                            int diff = svcount - dbcount;
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

                            progressArray.add(0);
                            totalArray.add(svcount);
                            parser.setThreadIndex(threadIndex++);
                            tm.addRow(new Object[] { sm, Integer.valueOf(svcount), Integer.valueOf(dbcount), Integer.valueOf(diff), progressText });
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

                    toggle.setEnabled(true);
                }).start();

                toggle.setEnabled(false);
            }
            else if (running) {
                for (Parser p : parsers) {
                    p.shutdownNow();
                }

                progressArray.clear();
                totalArray.clear();
                parsers.clear();
                es.shutdownNow();
                toggle.setText("시작");
                running = false;
            }
        }
    }
}
