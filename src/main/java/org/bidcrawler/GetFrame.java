package org.bidcrawler;

/**
 * Created by ravenjoo on 6/25/17.
 */
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.jdatepicker.DatePicker;
import org.jdatepicker.JDatePicker;

public class GetFrame extends JFrame {
    ExecutorService es;
    Future state;
    Timer stateCheck;
    Timer auto;
    Parser parser;
    GetFrame frame;

    JCheckBox autoCheck;
    DatePicker startDate;
    DatePicker endDate;
    JTextArea reps;
    JLabel count;
    JLabel code;
    JButton toggle;

    boolean automode;
    boolean running;
    String totalCount;
    String curCount;
    String site;

    public GetFrame(String site) {
        super(site + " 업데이트");
        this.site = site;
        frame = this;

        stateCheck = new Timer();
        stateCheck.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (running) {
                    boolean done = true;

                    if (!state.isDone()) done = false;

                    if (done) {
                        toggle.doClick();

                        if (autoCheck.isSelected()) {
                            long delay = Long.parseLong(reps.getText());

                            auto.schedule(new TimerTask() {
                                public void run() {
                                    toggle.doClick();
                                }
                            }, delay);
                        }
                    }
                }
            }
        }, 2000, 2000);

        auto = new Timer();

        running = false;
        automode = false;
        totalCount = "0";
        curCount = "0";

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
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
        count = new JLabel(curCount + " / " + totalCount);
        code = new JLabel("-");

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
        JPanel coPanel = new JPanel();
        coPanel.add(count);
        JPanel codePanel = new JPanel();
        codePanel.add(code);

        datePanel.add(sdPanel);
        datePanel.add(edPanel);
        datePanel.add(rePanel);
        datePanel.add(coPanel);
        datePanel.add(codePanel);

        panel.add(autoCheck, BorderLayout.NORTH);
        panel.add(datePanel, BorderLayout.CENTER);
        panel.add(toggle, BorderLayout.SOUTH);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (es != null) {
                    es.shutdownNow();
                }
                if (parser != null) {
                    parser.shutdownNow();
                    parser = null;
                }
                auto.cancel();
            }
        });
        this.add(panel);
        this.setSize(250, 250);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setVisible(true);
    }

    public void updateInfo(String c, boolean cur) {
        code.setText(c);
        if (cur) {
            int ind = Integer.parseInt(curCount);
            ind++;
            curCount = ind + "";
            count.setText(curCount + " / " + totalCount);
        }

        this.repaint();
    }

    private class UpdateListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (!running) {
                String sd = "";
                String ed = "";
                es = Executors.newFixedThreadPool(1);
                curCount = "0";

                if ((startDate.getModel().getValue() == null) || (endDate.getModel().getValue() == null)) {
                    JOptionPane.showMessageDialog(null, "날짜를 설정해주십시오.");
                    return;
                }
                else {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sd = sdf.format(startDate.getModel().getValue());
                    ed = sdf.format(endDate.getModel().getValue());
                }

                try {
                    if (site.equals("국방조달청")) {
                        parser = new DapaParser(sd, ed, "", frame);
                    }
                    else if (site.equals("한국마사회")) {
                        parser = new LetsParser(sd, ed, "", frame);
                    }
                    else if (site.equals("LH공사")) {
                        sd = sd.replaceAll("-", "/");
                        ed = ed.replaceAll("-", "/");
                        parser = new NewLHParser(sd, ed, "", frame);
                    }
                    else if (site.equals("도로공사")) {
                        parser = new ExParser(sd, ed, "", frame);
                    }
                    else if (site.equals("철도시설공단")) {
                        parser = new RailnetParser(sd, ed, "", frame);
                    }

                    totalCount = "" + parser.getTotal();

                    state = es.submit(parser);

                } catch (ClassNotFoundException | SQLException | IOException e1) {
                    Logger.getGlobal().log(Level.WARNING, e1.getMessage());
                    e1.printStackTrace();
                }

                count.setText(curCount + " / " + totalCount);
                running = true;
                toggle.setText("중지");
            }
            else if (running) {
                if (parser != null) {
                    parser.shutdownNow();
                }
                es.shutdownNow();
                running = false;
                toggle.setText("시작");
            }
        }
    }
}