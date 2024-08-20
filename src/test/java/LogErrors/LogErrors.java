package LogErrors;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v126.network.Network;
import org.openqa.selenium.devtools.v126.network.model.Response;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
//import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
//import com.google.api.*;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;
//import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
//import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class LogErrors {

    // Define constants
    private static final String APPLICATION_NAME = "LogsErrors";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/gmail.send");

    public static void main(String[] args) throws Exception {

        // Set up Chrome options to capture console logs
        ChromeOptions options = new ChromeOptions();
        LoggingPreferences logs = new LoggingPreferences();
        logs.enable(LogType.BROWSER, Level.ALL); // For browser logs
        logs.enable(LogType.PERFORMANCE, Level.ALL); // For performance logs (e.g., network errors)
        options.setCapability("goog:loggingPrefs", logs);

        // Create a new instance of the Chrome driver
        ChromeDriver driver = new ChromeDriver(options);

        // Enable Network Logging
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

        List<LogEntry> allLogEntries = new ArrayList<>();
        List<NetworkLogEntry> networkLogEntries = new ArrayList<>();

        // Capture network logs
        devTools.addListener(Network.responseReceived(), response -> {
            Response res = response.getResponse();
            if (res.getStatus() >= 400) {
                networkLogEntries.add(new NetworkLogEntry(res.getStatus(), res.getUrl()));
            }
        });

        try {
            // Navigate to the desired URL
            driver.get("https://timesofindia.indiatimes.com/");

            // Wait for some time to let the page load and generate logs
            Thread.sleep(10000); // 10 seconds

            // Scroll the page and collect all logs
            collectLogs(driver, allLogEntries); // Collect initial logs
            scrollPage(driver, allLogEntries);
            collectLogs(driver, allLogEntries); // Collect final logs

            // Wait for 10 seconds after scrolling the whole page
            Thread.sleep(10000);

            // Save all severe and warning logs to CSV
            saveLogsToCSV(allLogEntries, networkLogEntries, "severe_warnings_logs.csv");

            // Send email with the CSV attachment
            System.out.println("CSV file 'severe_warnings_logs.csv' created successfully.");
            sendEmailWithAttachment("RECIEVER EMAIL ID", "severe_warnings_logs.csv");

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            // Quit the driver
            driver.quit();
        }
    }

    private static void sendEmailWithAttachment(String to, String filename) throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Create the email content
        MimeMessage emailContent = createEmailWithAttachment(to, "YOUR EMAIL ID ", "Responses CSV", "Please find the attached CSV file.", filename);

        // Encode the email to base64
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();
        String encodedEmail = Base64.getEncoder().encodeToString(rawMessageBytes);

       Message message=new Message();
       message.setRaw(encodedEmail);

        // Send the email
        message = service.users().messages().send("me", message).execute();
        System.out.println("Email sent with ID: " + message.getId());
    }

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets from the same directory
        InputStream in = LogErrors.class.getClassLoader().getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static MimeMessage createEmailWithAttachment(String to, String from, String subject, String bodyText, String filePath) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(bodyText, "text/plain");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        MimeBodyPart attachment = new MimeBodyPart();
        DataSource source = new FileDataSource(filePath);

        attachment.setDataHandler(new DataHandler(source));
        attachment.setFileName(filePath);

        multipart.addBodyPart(attachment);
        email.setContent(multipart);

        return email;
    }

    private static void scrollPage(ChromeDriver driver, List<LogEntry> allLogEntries) throws InterruptedException {
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        Long pageHeight = (Long) jse.executeScript("return document.body.scrollHeight;");
        long scrollHeight = 0;
        while (scrollHeight < pageHeight) {
            jse.executeScript("window.scrollBy(0, 400);");
            Thread.sleep(2000);
            scrollHeight = (Long) jse.executeScript("return window.pageYOffset + window.innerHeight;");

            // Collect logs after each scroll
            collectLogs(driver, allLogEntries);
        }
    }

    private static void collectLogs(ChromeDriver driver, List<LogEntry> allLogEntries) throws InterruptedException {
        // Collect logs from all relevant types
        collectLogEntries(driver, LogType.BROWSER, allLogEntries);
        collectLogEntries(driver, LogType.PERFORMANCE, allLogEntries);
    }

    private static void collectLogEntries(ChromeDriver driver, String logType, List<LogEntry> allLogEntries) throws InterruptedException {
        LogEntries logEntries = driver.manage().logs().get(logType);
        for (LogEntry entry : logEntries) {
            allLogEntries.add(entry);
            if (entry.getLevel() == Level.SEVERE || entry.getLevel() == Level.WARNING) {
                // Print SEVERE and WARNING messages
                System.out.println(entry.getLevel() + " " + entry.getMessage());
                Thread.sleep(100); // Adding delay to avoid overwhelming output
            }
        }
    }

    private static void saveLogsToCSV(List<LogEntry> logEntries, List<NetworkLogEntry> networkLogEntries, String fileName) throws IOException {
        // Define regex pattern to extract URLs from log messages
        Pattern urlPattern = Pattern.compile("(https?://\\S+)");

        try (FileWriter fileWriter = new FileWriter(fileName); PrintWriter printWriter = new PrintWriter(fileWriter)) {
            // Write the CSV file header
            printWriter.println("Error Type,Error Url,Status Code,Status Text");

            // Write the log entries to the CSV file
            for (LogEntry entry : logEntries) {
                if (entry.getLevel() == Level.SEVERE || entry.getLevel() == Level.WARNING) {
                    String message = entry.getMessage();
                
                    // Extract URL
                    Matcher urlMatcher = urlPattern.matcher(message);
                    String url = urlMatcher.find() ? urlMatcher.group() : "N/A"; // Extract URL or use "N/A" if not found                    
                    // Find the corresponding network log entry
                    NetworkLogEntry matchingNetworkEntry = networkLogEntries.stream()
                        .filter(networkEntry -> networkEntry.getUrl().equals(url))
                        .findFirst()
                        .orElse(null);
                    // Prepare CSV entry
                    String errorType = entry.getLevel().toString();
                    int statusCode = matchingNetworkEntry != null ? matchingNetworkEntry.getStatusCode() : 0;
                    
                    // Write to CSV
                    printWriter.printf("\"%s\",\"%s\",\"%d\",\"%s\"%n", errorType, url, statusCode, message);
                }
            }
        }
    }

    static class NetworkLogEntry {
        private final int statusCode;
        private final String url;

        public NetworkLogEntry(int statusCode, String url) {
            this.statusCode = statusCode;
            this.url = url;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getUrl() {
            return url;
        }
    }
}
