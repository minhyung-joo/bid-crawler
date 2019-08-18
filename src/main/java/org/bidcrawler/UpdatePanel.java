package org.bidcrawler;

/**
 * Created by ravenjoo on 6/25/17.
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class UpdatePanel extends JPanel {

    public UpdatePanel() throws IOException {
        super();

        this.setLayout(new GridLayout(0, 3, 20, 20));
        this.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        initializePanel("국방조달청");
        initializePanel("LH공사");
        initializePanel("한국마사회");
        initializePanel("도로공사");
        initializePanel("철도시설공단");
    }

    public void initializePanel(String site) throws IOException {
        String path = "";
        if (site.equals("국방조달청")) path = "/org/bidcrawler/logos/dapa.PNG";
        else if (site.equals("LH공사")) path = "/org/bidcrawler/logos/lh.GIF";
        else if (site.equals("도로공사")) path = "/org/bidcrawler/logos/ex.PNG";
        else if (site.equals("한국마사회")) path = "/org/bidcrawler/logos/letsrun.PNG";
        else if (site.equals("철도시설공단")) path = "/org/bidcrawler/logos/railnet.JPG";

        InputStream stream = UpdatePanel.class.getResourceAsStream(path);
        BufferedImage logo = ImageIO.read(stream);
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.white);
        JPanel buttonPanel = new JPanel();
        JLabel label = new JLabel(new ImageIcon(logo));

        JButton updateButton = new JButton("업데이트");
        updateButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        updateButton.addActionListener(event -> {
            new GetFrame(site);
        });
        buttonPanel.add(updateButton);

        JButton dayButton = new JButton("일자별조회");
        dayButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        dayButton.addActionListener(event -> {
            new DayCheckFrame(site);
        });
        JButton monthButton = new JButton("월별조회");
        monthButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        monthButton.addActionListener(event -> {
            new MonthCheckFrame(site);
        });

        buttonPanel.add(monthButton);
        buttonPanel.add(dayButton);

        mainPanel.add(label, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEtchedBorder());

        this.add(mainPanel);
    }
}