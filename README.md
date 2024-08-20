
# LogErrors - Automated Error Logging and Emailing

This project, LogErrors, is designed to automate the process of capturing browser console logs and network responses from a web page, filtering them for errors, saving the logs to a CSV file, and then sending an email with the CSV file attached.



## Features

- Capture Browser Console Logs
- LNetwork Error Logging
- CSV Log Saving
- Emailing Logs


## Prequisites
Java Development Kit (JDK) 8 or higher

Maven for dependency management

Latest ChromeDriver

A Google account for Gmail API integration

credentials.json file containing Google API credentials
## Installation

1. Clone the Repository: Clone this repository to your local machine.

2. Install Dependencies: Use Maven to install the necessary dependencies.


3. Download ChromeDriver: Download the appropriate version of ChromeDriver for your operating system and ensure it is in your system's PATH.

4. Google API Credentials: Obtain your Google API credentials (Google OAuth 2.0 )and save them in a file named credentials.json.
Place the details.json file in the src/main/resources folder.


    
## Configuration
URL: Modify the URL in the driver.get() method to capture logs from the desired webpage.

Email Recipient: Change the recipient email address in the sendEmailWithAttachment method.

## Running Program
1. Create a maven project on eclipse
2. Package and class name be LogErrors
3. Copy the whole code
4. Run your java selenium code




## Documentation

- main(String[] args): Entry point for the application.

- sendEmailWithAttachment(String to, String filename): Sends an email with the specified CSV file attached.

- getCredentials(NetHttpTransport HTTP_TRANSPORT): Retrieves Google API credentials.

- createEmailWithAttachment(String to, String from, String subject, String bodyText, String filePath): Creates an email with an attachment.

- scrollPage(ChromeDriver driver, List<LogEntry> allLogEntries): Scrolls the page to capture all logs.

- collectLogs(ChromeDriver driver, List<LogEntry> allLogEntries): Collects logs from the browser.

- collectLogEntries(ChromeDriver driver, String logType, List<LogEntry> allLogEntries): Collects log entries of a specific type.

- saveLogsToCSV(List<LogEntry> logEntries, List<NetworkLogEntry> networkLogEntries, String fileName): Saves logs to a CSV file.

