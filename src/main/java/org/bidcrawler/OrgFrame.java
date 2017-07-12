package org.bidcrawler;

/**
 * Created by ravenjoo on 6/24/17.
 */
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import org.bidcrawler.utils.*;

public class OrgFrame extends JFrame {
    // For SQL setup.
    Connection con;
    java.sql.Statement st;
    ResultSet rs;

    String site;
    boolean nego;

    JList list;
    DefaultListModel listModel;
    JTextField searchInput;
    JButton searchButton;
    JTextField ref;

    public OrgFrame(JTextField orgInput, String site, boolean nego) throws ClassNotFoundException, SQLException {
        super("기관검색");
        ref = orgInput;
        this.site = site;
        this.nego = nego;

        fillList();

        searchInput = new JTextField(15);
        searchInput.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
                searchOrg();
            }
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchButton.doClick();
                }
            }
            public void keyReleased(KeyEvent e) {

            }
        });
        searchButton = new JButton("검색");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setOrg(list.getSelectedValue().toString());
            }
        });
        JScrollPane listPane = new JScrollPane(list);
        listPane.setPreferredSize(new Dimension(350, 250));
        JPanel mainPanel = new JPanel();
        mainPanel.add(listPane);
        mainPanel.add(searchInput);
        mainPanel.add(searchButton);

        this.setSize(380, 330);
        this.setResizable(false);
        this.add(mainPanel);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setVisible(true);
    }

    public void setOrg(String o) {
        ref.setText(o);
        this.dispose();
    }

    public void searchOrg() {
        for (int i = 0; i < listModel.getSize(); i++) {
            String s = listModel.get(i).toString();
            if (s.startsWith(searchInput.getText())) {
                list.setSelectedIndex(i);
                list.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private void fillList() throws ClassNotFoundException, SQLException {
        con = DriverManager.getConnection(
                "jdbc:mysql://localhost/" + Util.SCHEMA + "?characterEncoding=utf8",
                Util.DB_ID,
                Util.DB_PW
        );
        st = con.createStatement();
        rs = null;

        listModel = new DefaultListModel();

        String sql = "";
        String table = selectTable();
        String org = selectOrg();
        sql = "SELECT DISTINCT " + org + " FROM " + table + " ORDER BY " + org;
        rs = st.executeQuery(sql);

        while (rs.next()) {
            listModel.addElement(rs.getString(1));
        }
        list = new JList(listModel);
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList)evt.getSource();
                if (evt.getClickCount() >= 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    setOrg(list.getModel().getElementAt(index).toString());
                }
            }
        });
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(8);
    }

    private String selectTable() {
        String t = "";
        if (site.equals("나라장터")) {
            t = "narabidinfo";
        }
        else if (site.equals("국방조달청")) {
            if (nego) t = "dapanegoinfo";
            else t = "dapabidinfo";
        }
        else if (site.equals("LH공사")) {
            t = "lhbidinfo";
        }
        else if (site.equals("한국마사회")) {
            t = "letsrunbidinfo";
        }
        else if (site.equals("도로공사")) {
            t = "exbidinfo";
        }
        return t;
    }

    private String selectOrg() {
        String o = "";
        if (site.equals("나라장터")) {
            o = "수요기관";
        }
        else if (site.equals("국방조달청")) {
            o = "발주기관";
        }
        else if (site.equals("LH공사")) {
            o = "지역본부";
        }
        else if (site.equals("한국마사회")) {
            o = "사업장";
        }
        else if (site.equals("도로공사")) {
            o = "지역";
        }
        return o;
    }
}