package BOT.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class config {
    private static String[] textLoggingEnable;
    private static String[] channelLoggingEnable;
    private static String[] memberLoggingEnable;

    private static final Logger logger = LoggerFactory.getLogger(config.class);
    public static void config_load() {
        textLoggingEnable = SQL.configDownLoad(SQL.textLogging);
        channelLoggingEnable = SQL.configDownLoad(SQL.channelLogging);
        memberLoggingEnable = SQL.configDownLoad(SQL.memberLogging);
    }

    public static String[] getTextLoggingEnable() {
        return textLoggingEnable;
    }

    public static String[] getChannelLoggingEnable() {
        return channelLoggingEnable;
    }

    public static String[] getMemberLoggingEnable() {
        return memberLoggingEnable;
    }

}
