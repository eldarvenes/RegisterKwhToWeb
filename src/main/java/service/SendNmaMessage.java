package service;
import lib.NMAClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SendNmaMessage {

    final static Logger logger = LoggerFactory.getLogger(SendNmaMessage.class);

    public SendNmaMessage(){
        loadProperties();
    }

    private NMAClient nmaClient = new NMAClient();
    String MyId;


    public int sendMessage(String event, String body) {

        if(NMAClient.verify(MyId) == 1){
            logger.debug("Verify ok for: " + MyId);
            NMAClient.notify("Energy", event, body, MyId);
            logger.debug("Event: "+ event + " and " + "Body: " + body + " sent to device");
        }else {
            logger.debug("Could not verify: " + MyId);
        }
        return 0;
    }

    private void loadProperties() {
        Properties prop = new Properties();
        try {
            prop.load(YouLessService.class.getClassLoader()
                    .getResourceAsStream("configuration.properties"));
            MyId = prop.getProperty("NotifyMyAndroidIdEldar");
        } catch (Exception e) {

        }
    }

}
