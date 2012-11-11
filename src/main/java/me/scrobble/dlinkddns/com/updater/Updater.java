package me.scrobble.dlinkddns.com.updater;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Updater {

    private final static Logger LOGGER = LoggerFactory.getLogger(Updater.class);

    /**
     *  @param args Start parameters.
     */
    public static void main(String[] args) {
        Options options = new Options();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            LOGGER.debug("Parsing parameters");
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.out.println("This is an updater for http://www.dlinkddns.com. Please use the correct arguments:\n");
            parser.printUsage(System.out);
            System.out.println("\nFor more information visit http://www.scrobble.me");
            return;
        }
        try {
            update(options);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            LOGGER.debug(e.getMessage(), e);
        }
    }

    private static void update(Options options) throws ClientProtocolException, IOException {
        HttpClient client = new DefaultHttpClient();
        String loginResult = sendPostRequest(client, "https://www.dlinkddns.com/login", new BasicNameValuePair(
                "username", options.username), new BasicNameValuePair("pw", options.password));
        if (loginResult.contains("Username and password did not match")) {
            throw new IllegalArgumentException("Username and password did not match.");
        }
        LOGGER.info("Logged in as " + options.username);
        String clientUrl = "https://www.dlinkddns.com/host/" + options.host + "." + options.baseUrl;
        String updatePage = sendGetRequest(client, clientUrl);
        updatePage = updatePage.replaceAll("\\s+", " ");
        updatePage = updatePage.replace("&nbsp;", "");
        updatePage = updatePage.replace("> <", "><");
        String dnsIpAddress = StringUtils.substringBetween(updatePage, "IP in DNS</th><td><span class=\"data\">",
                "</span>");
        String browserIpAddress = StringUtils.substringBetween(updatePage,
                "Browser IP Address</th><td><span class=\"data\">", "</span>");
        String lastModified = StringUtils.substringBetween(updatePage, "Last Modified</th><td><span class=\"data\">",
                "</span>");
        LOGGER.info("Before update: Ip in DNS is {} and your current IP address is {} as of {} (server time).",
                new Object[]{dnsIpAddress, browserIpAddress, lastModified});
        sendPostRequest(client, clientUrl,
                new BasicNameValuePair("modify", options.host),
                new BasicNameValuePair("host", options.host),
                new BasicNameValuePair("ip", browserIpAddress),
                new BasicNameValuePair("commit", "Save"),
                new BasicNameValuePair("domain", options.baseUrl));
        updatePage = sendGetRequest(client, clientUrl);
        updatePage = updatePage.replaceAll("\\s+", " ");
        updatePage = updatePage.replace("&nbsp;", "");
        updatePage = updatePage.replace("> <", "><");
        dnsIpAddress = StringUtils.substringBetween(updatePage, "IP in DNS</th><td><span class=\"data\">", "</span>");
        lastModified = StringUtils.substringBetween(updatePage, "Last Modified</th><td><span class=\"data\">",
                "</span>");
        LOGGER.info("After update: Ip in DNS is {} as of {} (server time).", dnsIpAddress, lastModified);
    }

    private static String sendGetRequest(HttpClient client, String url, NameValuePair... parameters)
            throws ClientProtocolException, IOException {
        List<NameValuePair> queryParameters = new ArrayList<NameValuePair>();
        if (parameters != null && parameters.length > 0) {
            queryParameters.addAll(Arrays.asList(parameters));
            if (url.contains("?")) {
                url += "&";
            } else {
                url += "?";
            }
        }
        url += URLEncodedUtils.format(queryParameters, "UTF-8");
        HttpGet request = new HttpGet(url);
        HttpResponse reponse = client.execute(request);
        return EntityUtils.toString(reponse.getEntity());
    }

    private static String sendPostRequest(HttpClient client, String url, NameValuePair... parameters)
            throws ClientProtocolException, IOException {
        HttpPost request = new HttpPost(url);
        if (parameters != null && parameters.length > 0) {
            List<NameValuePair> formParameters = new ArrayList<NameValuePair>();
            formParameters.addAll(Arrays.asList(parameters));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParameters, "UTF-8");
            request.setEntity(entity);
        }
        HttpResponse reponse = client.execute(request);
        return EntityUtils.toString(reponse.getEntity());
    }

    /**
     * Options.
     */
    private static class Options {

        @Option(name = "-u", aliases = {"-user"}, required = true, usage = "(required) Your user id.")
        private String username;
        @Option(name = "-p", aliases = {"-password"}, required = true, usage = "(required) Your password.")
        private String password;
        @Option(name = "-h", aliases = {"-host"}, required = true, usage = "(required) Your hostname, i.e. \"myhost\" for \"myhost.dlinkddns.com\".")
        private String host;
        @Option(name = "-b", aliases = {"-baseurl"}, usage = "(optional) The base url, default \"dlinkddns.com\".", metaVar = "dlinkddns.com")
        private String baseUrl = "dlinkddns.com";
    }
}
