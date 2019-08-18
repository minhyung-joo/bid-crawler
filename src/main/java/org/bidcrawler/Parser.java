package org.bidcrawler;

/**
 * Created by ravenjoo on 6/25/17.
 */
import java.io.IOException;
import java.sql.SQLException;

public abstract class Parser implements Runnable {
    int threadIndex;
    volatile boolean shutdown = false;

    public abstract int getTotal() throws IOException, ClassNotFoundException, SQLException;

    public abstract void setDate(String sd, String ed);

    public abstract void setOption(String op);

    public abstract int getCur();

    public abstract void manageDifference(String sm, String em) throws SQLException, IOException;

    public void shutdownNow() {
        shutdown = true;
    }

    public void setThreadIndex(int threadIndex) {
        this.threadIndex = threadIndex;
    }
}