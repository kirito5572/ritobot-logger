package BOT.Listener;

import BOT.App;
import BOT.Constants;
import BOT.Objects.CommandManager;
import BOT.Objects.SQL;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Listener extends ListenerAdapter {
    private final CommandManager manager;
    private final Logger logger = LoggerFactory.getLogger(Listener.class);
    private static String ID1;
    private static String ID2;
    public Listener(CommandManager manager) {
        this.manager = manager;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info(String.format("logger_core 로그인 성공: %#s", event.getJDA().getSelfUser()));
        System.out.println(String.format("logger_core 로그인 성공: %#s", event.getJDA().getSelfUser()));
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        Message message = event.getMessage();
        StringBuilder IDreader = new StringBuilder();
        StringBuilder IDreader1 = new StringBuilder();
        try {
            File file = new File("C:\\DiscordServerBotSecrets\\rito-bot\\OWNER_ID.txt");
            File file1 = new File("C:\\DiscordServerBotSecrets\\rito-bot\\OWNER_ID1.txt");
            FileReader fileReader = new FileReader(file);
            FileReader fileReader1 = new FileReader(file1);
            int singalCh, singalCh1;
            while((singalCh = fileReader.read()) != -1) {
                IDreader.append((char) singalCh);
            }
            while((singalCh1 = fileReader1.read()) != -1) {
                IDreader1.append((char) singalCh1);
            }
        } catch (Exception e) {

            StackTraceElement[] eStackTrace = e.getStackTrace();
            StringBuilder a = new StringBuilder();
            for (StackTraceElement stackTraceElement : eStackTrace) {
                a.append(stackTraceElement).append("\n");
            }
            logger.warn(a.toString());
        }
        ID1 = IDreader.toString();
        ID2 = IDreader1.toString();
        if (event.getMessage().getContentRaw().equalsIgnoreCase(App.getPREFIX() + "종료 logger") &&
                (
                        (event.getAuthor().getIdLong() == Long.decode(ID1)) ||
                        (event.getAuthor().getIdLong() == Long.decode(ID2))
                )) {
            shutdown(event.getJDA(), event);
            return;

        }
        if(event.getAuthor().isBot()) {
            if(!event.getAuthor().getId().equals("617912267597676545")) {
                return;
            }
        }
        if(event.getMessage().isWebhookMessage()) {

            return;
        }
        String channelId = SQL.configDownLoad_botchannel(event.getGuild().getId());
        if (event.getMessage().getContentRaw().startsWith(Constants.PREFIX)) {
            if(!channelId.equals(event.getChannel().getId())) {
                if(!channelId.equals("error")) {
                    Member member = event.getMember();
                    assert member != null;
                    if (!(member.hasPermission(Permission.MESSAGE_MANAGE) ||
                            member.hasPermission(Permission.MANAGE_CHANNEL) ||
                            member.hasPermission(Permission.MANAGE_PERMISSIONS) ||
                            member.hasPermission(Permission.MANAGE_ROLES) ||
                            member.hasPermission(Permission.MANAGE_SERVER) ||
                            member.hasPermission(Permission.ADMINISTRATOR))) {
                        return;
                    }
                }
            }
            manager.handleCommand(event);
        }

    }
    private void shutdown(@NotNull JDA jda, @NotNull GuildMessageReceivedEvent event) {
        new Thread(() -> {
            event.getChannel().sendMessage("logger_core 종료 완료").queue();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

                StackTraceElement[] eStackTrace = e.getStackTrace();
                StringBuilder a = new StringBuilder();
                for (StackTraceElement stackTraceElement : eStackTrace) {
                    a.append(stackTraceElement).append("\n");
                }
                logger.warn(a.toString());
            }
            jda.shutdown();
            System.exit(0);
        }).start();
    }


    public static String getID1() {
        return ID1;
    }

    public static String getID2() {
        return ID2;
    }
}

