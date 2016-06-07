package service;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Properties;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YouLessService {

    final static Logger logger = LoggerFactory.getLogger(RegisterKwhToWeb.class);

    String youLessURL;

    public YouLessService(){
        loadProperties();
    }

    public double getTotalKwhFromYouLess() {
        JSONObject json = readJSONFromYouLess();
        String kwh = json.getString("cnt");
        kwh = kwh.replace(',', '.');
        double valuekwh = Double.valueOf(kwh);
        logger.debug("Read total kwh from YouLess: " + valuekwh);
        return valuekwh;
    }

    private JSONObject readJSONFromYouLess() {
        URL youless = null;
        try {
            youless = new URL(youLessURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        BufferedReader in = null;

        try {
            in = new BufferedReader(new InputStreamReader(youless.openStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String inputLine = null;
        try {
            inputLine = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JSONObject(inputLine);
    }

    public String getTotalKwhAsString(){
        return getStringValueNullDecimals(getTotalKwhFromYouLess());
    }

    private String getStringValueNullDecimals(double kwh){

        NumberFormat nf = DecimalFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        String str = nf.format(kwh);

        return str.replace('\u00A0',' ').replaceAll("\\s","");
    }

    public boolean isConnectionOk(){
        logger.debug("Check if connection is ok to YouLess...");
        Boolean connected = false;
        int retries = 0;
        int signalStrength;

        while((connected == false) && (retries < 10)){
            logger.debug("Retries: " + retries);
            JSONObject json = readJSONFromYouLess();
            signalStrength = json.getInt("lvl");
            retries++;
            connected = signalStrength > 80;
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug("Connected: " + connected);
        return(connected);
    }

    private void loadProperties() {
        Properties prop = new Properties();
        try {
            prop.load(YouLessService.class.getClassLoader()
                    .getResourceAsStream("configuration.properties"));
            youLessURL = prop.getProperty("youLessURL");
        } catch (Exception e) {

        }
    }
}
