package org.bidcrawler;

import java.io.IOException;
import java.sql.SQLException;

import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.WebDriver;

public class KepcoParser extends Parser {

    public static final String WEB_DRIVER_ID = "webdriver.chrome.driver";
    public static final String WEB_DRIVER_PATH = "C:\\Users\\user\\Downloads\\chromedriver.exe";

    public KepcoParser(String startDate, String endDate, String option, GetFrame frame, CheckFrame checkFrame)
            throws SQLException
    {

    }

    @Override
    public int getTotal() throws IOException, ClassNotFoundException, SQLException {
        return 0;
    }

    @Override
    public void setDate(String sd, String ed) {

    }

    @Override
    public void setOption(String op) {

    }

    @Override
    public int getCur() {
        return 0;
    }

    @Override
    public void manageDifference(String sm, String em) throws SQLException, IOException {

    }

    @Override
    public void run() {
        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
        WebDriver driver = new ChromeDriver();
        String url = "https://srm.kepco.net/index.do";
        driver.get(url);
        driver.switchTo().frame(driver.findElement(By.id("mdiiframe-1010-iframeEl")));
        driver.findElement(By.id("memberLogin")).click();
        driver.switchTo().frame(driver.findElement(By.id("kepcoLoginPop")));
        System.out.println(driver.getPageSource());
        while (true) {
            continue;
        }
    }
}
