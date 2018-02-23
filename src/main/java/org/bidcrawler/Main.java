package org.bidcrawler;
/**
 * Created by ravenjoo on 6/24/17.
 */

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.bidcrawler.utils.*;

public class Main extends JFrame {
    private static Logger logger = Logger.getGlobal();

    private JMenuBar menuBar;
    private JButton settingMenu;
    private JTabbedPane tabbedPane;
    private JComponent dataPanel;
    private JComponent negoPanel;
    private JComponent updatePanel;

    public Main() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException {
        super();

        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }

        UIManager.getLookAndFeel().getDefaults().put("defaultFont", new Font(null, Font.PLAIN, 20));

        initializeMenuBar();
        initializeTabbedPane();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenSize.setSize(screenSize.getWidth(), screenSize.getHeight() - 50);
        this.setSize(screenSize);
        this.setLayout(new GridLayout(1, 1));
        this.setTitle("입찰정보");
        this.setJMenuBar(menuBar);
        this.add(tabbedPane);
        this.setExtendedState(Frame.MAXIMIZED_BOTH);
        this.setVisible(true);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public void initializeMenuBar() {
        menuBar = new JMenuBar();
        settingMenu = new JButton("설정");
        settingMenu.addActionListener(event -> {
            OptionFrame of = new OptionFrame();
        });
        menuBar.add(settingMenu);
    }

    public void initializeTabbedPane() throws IOException {
        tabbedPane = new JTabbedPane();

        dataPanel = new DataPanel("BID");
        negoPanel = new DataPanel("NEGO");
        updatePanel = new UpdatePanel();

        tabbedPane.addTab("데이터 조회", dataPanel);
        tabbedPane.addTab("협상건 조회", negoPanel);
        tabbedPane.addTab("업데이트 센터", updatePanel);
    }

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, IOException {
        //Disable unnecessary error logs.
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);

        try {
            FileHandler fh = new FileHandler("mylog.txt");
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.WARNING);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

        Util.initialize();
        System.out.println(Util.DB_ID);
        System.out.println(Util.DB_PW);
        System.out.println(Util.SCHEMA);
        System.out.println(Util.BASE_PATH);
        Main m = new Main();
    }
}
