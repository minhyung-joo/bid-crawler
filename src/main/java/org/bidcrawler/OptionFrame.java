package org.bidcrawler;

/**
 * Created by ravenjoo on 6/24/17.
 */

import org.bidcrawler.utils.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@SuppressWarnings("serial")
public class OptionFrame extends JFrame {

    JLabel topLabel;
    JTextArea dbid;
    JPasswordField dbpw;
    JTextArea basePath;
    JTextArea schema;
    JButton confirm;

    public OptionFrame() {
        super("설정");

        topLabel = new JLabel("환경설정");
        topLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));

        dbid = new JTextArea(1, 10);
        dbid.setText(Util.DB_ID);
        dbid.setBorder(BorderFactory.createLineBorder(Color.black));
        dbpw = new JPasswordField(10);
        dbpw.setText(Util.DB_PW);
        dbpw.setBorder(BorderFactory.createLineBorder(Color.black));
        schema = new JTextArea(1, 10);
        schema.setText(Util.SCHEMA);
        schema.setBorder(BorderFactory.createLineBorder(Color.black));
        basePath = new JTextArea(1, 10);
        basePath.setText(Util.BASE_PATH);
        basePath.setBorder(BorderFactory.createLineBorder(Color.black));
        confirm = new JButton("확인");
        confirm.addActionListener(new ConfirmListener());

        JPanel idPanel = new JPanel();
        idPanel.add(new JLabel("MySQL ID : "));
        idPanel.add(dbid);
        JPanel pwPanel = new JPanel();
        pwPanel.add(new JLabel("MySQL PW : "));
        pwPanel.add(dbpw);
        JPanel schemaPanel = new JPanel();
        schemaPanel.add(new JLabel("DB Schema : "));
        schemaPanel.add(schema);
        JPanel pathPanel = new JPanel();
        pathPanel.add(new JLabel("저장폴더 : "));
        pathPanel.add(basePath);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
        centerPanel.add(idPanel);
        centerPanel.add(pwPanel);
        centerPanel.add(schemaPanel);
        centerPanel.add(pathPanel);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(confirm);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(topLabel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        this.setSize(600, 350);
        this.setResizable(false);
        this.add(mainPanel);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setVisible(true);
    }

    private class ConfirmListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Util.setValues(dbid.getText(), new String(dbpw.getPassword()), schema.getText(), basePath.getText());
            closeFrame();
        }
    }

    public void closeFrame() {
        this.dispose();
    }
}