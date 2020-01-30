package BOT.Objects;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.Arrays;

public class SQL {
    private static final Logger logger = LoggerFactory.getLogger(SQL.class);
    private static Connection connection;
    private static Connection loggingConnection;
    private static Statement loggingStatement;
    private static ResultSet resultSet6;
    private static String driverName;
    private static String url;
    private static String user;
    private static String password;

    public SQL() {
        //init
        StringBuilder SQLPassword = new StringBuilder();
        try {
            File file = new File("C:\\DiscordServerBotSecrets\\rito-bot\\SQLPassword.txt");
            FileReader fileReader = new FileReader(file);
            int singalCh;
            while((singalCh = fileReader.read()) != -1) {
                SQLPassword.append((char) singalCh);
            }
        } catch (Exception e) {

            StackTraceElement[] eStackTrace = e.getStackTrace();
            StringBuilder a = new StringBuilder();
            for (StackTraceElement stackTraceElement : eStackTrace) {
                a.append(stackTraceElement).append("\n");
            }
            logger.warn(a.toString());
        }
        StringBuilder endPoint = new StringBuilder();
        try {
            File file = new File("C:\\DiscordServerBotSecrets\\rito-bot\\endPoint.txt");
            FileReader fileReader = new FileReader(file);
            int singalCh;
            while((singalCh = fileReader.read()) != -1) {
                endPoint.append((char) singalCh);
            }
        } catch (Exception e) {

            StackTraceElement[] eStackTrace = e.getStackTrace();
            StringBuilder a = new StringBuilder();
            for (StackTraceElement stackTraceElement : eStackTrace) {
                a.append(stackTraceElement).append("\n");
            }
            logger.warn(a.toString());
        }
        driverName = "com.mysql.cj.jdbc.Driver";
        url = "jdbc:mysql://" + endPoint.toString() + "/ritobotDB?serverTimezone=UTC";
        user = "admin";
        password = SQLPassword.toString();
        System.out.println(url);
        System.out.println(password);
        try {
            connection = DriverManager.getConnection(url, user, password);
            loggingConnection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static boolean loggingMessageUpLoad(String guildId, String messageId, String contentRaw, String authorId) {
        String queryString = "INSERT INTO messageLogging VALUE (" + guildId + ","+ messageId + ", '" + contentRaw + "'," + authorId +");";
        System.out.println(queryString);
        try {
            Class.forName(driverName);

            loggingStatement = loggingConnection.createStatement();
            loggingStatement.executeUpdate(queryString);
            loggingStatement.close();
        } catch (Exception e) {
            StackTraceElement[] eStackTrace = e.getStackTrace();
            StringBuilder a = new StringBuilder();
            for (StackTraceElement stackTraceElement : eStackTrace) {
                a.append(stackTraceElement).append("\n");
            }
            logger.warn(a.toString());
            try {
                loggingConnection = DriverManager.getConnection(url, user, password);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        }
        return true;
    }

    public static boolean loggingMessageUpdate(String guildId, String messageId, String contentRaw) {
        String queryString = "UPDATE messageLogging SET ContentRaw = '" + contentRaw + "' WHERE GuildId = '" + guildId + "' AND MessageId = '" + messageId + "';";
        System.out.println(queryString);
        try {
            Class.forName(driverName);

            loggingStatement = loggingConnection.createStatement();
            loggingStatement.executeUpdate(queryString);
            loggingStatement.close();
        } catch (Exception e) {
            StackTraceElement[] eStackTrace = e.getStackTrace();
            StringBuilder a = new StringBuilder();
            for (StackTraceElement stackTraceElement : eStackTrace) {
                a.append(stackTraceElement).append("\n");
            }
            logger.warn(a.toString());
            try {
                loggingConnection = DriverManager.getConnection(url, user, password);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        }
        return true;
    }

    @NotNull
    public static String[] loggingMessageDownLoad(String guildId, String messageId) {
        String[] data = new String[2];
        String queryString = "SELECT * FROM messageLogging WHERE MessageId=" + messageId + " AND GuildId=" + guildId + ";";
        System.out.println(queryString);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            loggingStatement = loggingConnection.createStatement();
            ResultSet loggingResultSet = loggingStatement.executeQuery(queryString);
            while (loggingResultSet.next()) {
                data[0] = loggingResultSet.getString("ContentRaw");
                data[1] = loggingResultSet.getString("Author");
            }
        } catch (Exception e) {
            StackTraceElement[] eStackTrace = e.getStackTrace();
            StringBuilder a = new StringBuilder();
            for (StackTraceElement stackTraceElement : eStackTrace) {
                a.append(stackTraceElement).append("\n");
            }
            logger.warn(a.toString());
            try {
                loggingConnection = DriverManager.getConnection(url, user, password);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        try {
            loggingStatement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return data;
    }
    public static final int textLogging = 7;
    public static final int channelLogging = 8;
    public static final int memberLogging = 9;

    @NotNull
    public static String[] configDownLoad(int option) {
        String[] return_data = new String[] {"error"};
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String queryString;
            int i = 0;
            switch (option) {
                case textLogging:
                    queryString = "SELECT * FROM ritobot_config.logging_enable WHERE text_logging = 1;";
                    Statement statement = connection.createStatement();
                    resultSet6 = statement.executeQuery(queryString);
                    return_data = new String[resultSet6.getFetchSize()];
                    while (resultSet6.next()) {
                        if (i == 0) {
                            return_data = new String[] {
                                    resultSet6.getString("guildId")};
                            i++;
                        } else {
                            String[] newArray = Arrays.copyOf(return_data, return_data.length + 1);
                            newArray[return_data.length] = resultSet6.getString("guildId");
                            return_data = newArray;
                        }
                    }
                    break;
                case channelLogging:
                    queryString = "SELECT * FROM ritobot_config.logging_enable WHERE channel_logging = 1;";
                    statement = connection.createStatement();
                    resultSet6 = statement.executeQuery(queryString);
                    return_data = new String[resultSet6.getFetchSize()];
                    while (resultSet6.next()) {
                        if (i == 0) {
                            return_data = new String[] {
                                    resultSet6.getString("guildId")};
                            i++;
                        } else {
                            String[] newArray = Arrays.copyOf(return_data, return_data.length + 1);
                            newArray[return_data.length] = resultSet6.getString("guildId");
                            return_data = newArray;
                        }
                    }
                    break;
                case memberLogging:
                    queryString = "SELECT * FROM ritobot_config.logging_enable WHERE member_logging = 1;";
                    statement = connection.createStatement();
                    resultSet6 = statement.executeQuery(queryString);
                    return_data = new String[resultSet6.getFetchSize()];
                    while (resultSet6.next()) {
                        if (i == 0) {
                            return_data = new String[] {
                                    resultSet6.getString("guildId")};
                            i++;
                        } else {
                            String[] newArray = Arrays.copyOf(return_data, return_data.length + 1);
                            newArray[return_data.length] = resultSet6.getString("guildId");
                            return_data = newArray;
                        }
                    }
                    break;
            }
            resultSet6.close();
        } catch (Exception e) {
            e.printStackTrace();
            return_data = new String [] {"error"};
        }
        return return_data;
    }

    public static Connection getConnection() {
        return connection;
    }

    public static void setConnection(Connection connection) {
        SQL.connection = connection;
    }

    public static void setLoggingConnection(Connection loggingConnection) {
        SQL.loggingConnection = loggingConnection;
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }

    public static String getPassword() {
        return password;
    }
}
