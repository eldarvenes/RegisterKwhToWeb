package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterKwhToWeb implements Job {

    final static Logger logger = LoggerFactory.getLogger(RegisterKwhToWeb.class);

    YouLessService youLessService = new YouLessService();
    SendNmaMessage sendNmaMessage = new SendNmaMessage();
    String p_name;
    String p_pass;

    String maalerstand = "";

    String loginResult = "";
    private final String USER_AGENT = "Mozilla/5.0";


    HttpClient client = HttpClientBuilder.create()
            .setRedirectStrategy(new LaxRedirectStrategy())
            .build();


    public RegisterKwhToWeb() {
        loadProperties();
    }

    private void prepareDataFromYouless(){
        maalerstand = youLessService.getTotalKwhAsString();
        maalerstand = maalerstand.replace(",", "");
    }

    private String getLoginPage() throws IOException {
        URL url = new URL("https://kundeweb.sognekraft.no/pls/kundeweb_sognekraft/webuser.login.login?p_eltilbyder_id=3&p_kundetype=1");
        URLConnection con = url.openConnection();
        Pattern p = Pattern.compile("text/html;\\s+charset=([^\\s]+)\\s*");
        Matcher m = p.matcher(con.getContentType());

        String charset = m.matches() ? m.group(1) : "ISO-8859-1";
        Reader r = new InputStreamReader(con.getInputStream(), charset);
        StringBuilder buf = new StringBuilder();
        while (true) {
            int ch = r.read();
            if (ch < 0)
                break;
            buf.append((char) ch);
        }
        String str = buf.toString();

        return str;
    }

    public void login() throws Exception {
        String urlLogin = "https://kundeweb.sognekraft.no/pls/kundeweb_sognekraft/webuser.login.login_submit";
        HttpPost request = new HttpPost(urlLogin);
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .build();

        request.setConfig(requestConfig);
        request.setHeader("User-Agent", USER_AGENT);

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("p_navn", p_name));
        urlParameters.add(new BasicNameValuePair("p_pass", p_pass));
        urlParameters.add(new BasicNameValuePair("p_applikasjons_id", "3"));
        urlParameters.add(new BasicNameValuePair("p_kundetype", "1"));
        urlParameters.add(new BasicNameValuePair("p_tilprosedyre", ""));

        request.setEntity(new UrlEncodedFormEntity(urlParameters));

        HttpResponse response = client.execute(request);

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));


        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        loginResult = result.toString();
    }

    public void sendAvlesing() throws Exception {
        String dato = new SimpleDateFormat("dd.MM.yyyy").format(new Date());

        String urlAvlesning = "https://kundeweb.sognekraft.no/pls/kundeweb_sognekraft/webuser.avlesning.submit";

        HttpPost post = new HttpPost(urlAvlesning);

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("p_session_id", getSessionId(loginResult)));
        urlParameters.add(new BasicNameValuePair("p_avlesDato", dato));
        urlParameters.add(new BasicNameValuePair("p_maalarStand", maalerstand));

        post.setEntity(new UrlEncodedFormEntity(urlParameters));

        HttpResponse response = client.execute(post);

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));


        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
    }

    public void godkjennAvlesing() throws Exception {

        String urlAvlesning = "https://kundeweb.sognekraft.no/pls/kundeweb_sognekraft/webuser.godkjenn_avlesning.submit";

        HttpPost post = new HttpPost(urlAvlesning);

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("p_session_id", getSessionId(loginResult)));
        urlParameters.add(new BasicNameValuePair("p_funksjon", "1"));

        post.setEntity(new UrlEncodedFormEntity(urlParameters));

        HttpResponse response = client.execute(post);

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));


        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        sendNmaMessage.sendMessage("Maaleravlesing", "Registrering av straumforbruk til Sognekraft, OK, total maalerstand var: " + maalerstand);
    }

    public String getSessionId(String textToSearchIn) {
        String searchString = "p_session_Id=";

        int start = textToSearchIn.indexOf(searchString);
        String str = textToSearchIn;

        return (str.substring(start + 18, start + 24));
    }

    public void register() {
        try {
            if (youLessService.isConnectionOk()) {
                prepareDataFromYouless();
                getLoginPage();
                login();
                sendAvlesing();
                godkjennAvlesing();
            }else{
                logger.error("Could not register to web because YouLess service is not connected");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("Register kwh to web");
        register();
        logger.info("Registration success");
    }

    private void loadProperties() {
        Properties prop = new Properties();
        try {
            prop.load(RegisterKwhToWeb.class.getClassLoader()
                    .getResourceAsStream("configuration.properties"));
            p_name = prop.getProperty("p_name");
            p_pass = prop.getProperty("p_pass");
        } catch (Exception e) {

        }
    }
}
