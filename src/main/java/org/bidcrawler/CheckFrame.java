package org.bidcrawler;

import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import org.jdatepicker.DatePicker;
import org.jdatepicker.JDatePicker;

public abstract class CheckFrame extends JFrame
{
    String[] columns = { "개찰일시", "사이트", "DB", "차수", "진행상태" };

    Connection con;
    java.sql.Statement st;
    java.sql.ResultSet rs;
    ExecutorService es;

    ArrayList<Parser> parsers;
    ArrayList<String> timeFrames;
    ArrayList<Integer> progressArray;
    ArrayList<Integer> totalArray;
    boolean running;
    int counter;
    int threadNum;
    String startDate;
    String endDate;
    Timer auto;
    boolean automode;
    JCheckBox autoCheck;
    DatePicker startDatePicker;
    DatePicker endDatePicker;
    JTextArea reps;
    JTable table;
    DefaultTableModel tableModel;
    String site;
    JButton toggle;

    public CheckFrame(String name)
    {
        super(name);

        parsers = new ArrayList();
        auto = new Timer();
        automode = false;
        running = false;
        autoCheck = new JCheckBox("자동");

        // Initialize date pickers
        startDatePicker = new JDatePicker(Calendar.getInstance().getTime());
        startDatePicker.setTextfieldColumns(12);
        startDatePicker.setTextEditable(true);
        endDatePicker = new JDatePicker(Calendar.getInstance().getTime());
        endDatePicker.setTextfieldColumns(12);
        endDatePicker.setTextEditable(true);


        reps = new JTextArea(1, 5);
        reps.setText("60");
        toggle = new JButton("시작");
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);

        parsers = new ArrayList();
        progressArray = new ArrayList();
        totalArray = new ArrayList();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                for (Parser p : parsers) {
                    p.shutdownNow();
                    p = null;
                }
                parsers.clear();

                if (es != null) es.shutdownNow();
                auto.cancel();
            }
        });
    }

    protected void configureAutoMode() {
        if ((org.bidcrawler.utils.Util.isInteger(reps.getText())) && (!reps.equals("0"))) {
            int rep = Integer.parseInt(reps.getText());
            if ((!automode) && (autoCheck.isSelected())) {
                auto.scheduleAtFixedRate(new java.util.TimerTask() {
                    public void run() {
                        if (!running) {
                            automode = true;
                            toggle.doClick();
                        }
                        if (!autoCheck.isSelected()) {
                            automode = false;
                            auto.cancel(); } } }, rep * 1000, rep * 1000);
            }
        }
    }

    public synchronized void signalFinish() {
        if (threadNum > 0) {
            counter += 1;
            if (counter >= threadNum) {
                toggle.doClick();
            }
        }
    }

    public synchronized void updateProgress(int index) {
        progressArray.set(index, progressArray.get(index) + 1);
        String updatedProgress = progressArray.get(index) + "/" + totalArray.get(index);
        tableModel.setValueAt(updatedProgress, index, 4);
    }

    public void adjustColumns() {
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(120);
        columnModel.getColumn(1).setPreferredWidth(80);
        columnModel.getColumn(2).setPreferredWidth(80);
        columnModel.getColumn(3).setPreferredWidth(80);
        columnModel.getColumn(4).setPreferredWidth(140);
    }
}
