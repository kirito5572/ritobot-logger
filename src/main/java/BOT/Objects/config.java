package BOT.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class config {
    private SQL sql;
    public config(SQL sql) {
        this.sql = sql;
    }
    private static String[] textLoggingEnable;
    private static String[] channelLoggingEnable;
    private static String[] memberLoggingEnable;

    private static final Logger logger = LoggerFactory.getLogger(config.class);
    public void config_load() {
        textLoggingEnable = sql.configDownLoad(SQL.textLogging);
        channelLoggingEnable = sql.configDownLoad(SQL.channelLogging);
        memberLoggingEnable = sql.configDownLoad(SQL.memberLogging);
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
