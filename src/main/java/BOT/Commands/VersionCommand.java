package BOT.Commands;

import BOT.App;
import BOT.Objects.ICommand;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VersionCommand implements ICommand {
    private static final String version = "빌드 버젼 V 1.7.4_logger ";
    @Override
    public void handle(List<String> args, @NotNull GuildMessageReceivedEvent event) {
        if(App.isDEBUG_MODE() || App.isONLINE_DEBUG()) {
            event.getChannel().sendMessage("빌드 버젼 V 1.7.5_logger (" + App.getTime() + ")").queue();
            return;
        }
        event.getChannel().sendMessage(version + "(" + App.getTime() + ")").queue();
    }

    @NotNull
    @Override
    public String getHelp() {
        return "빌드 버젼을 알려줍니다. \n" +
                "사용법: `" + App.getPREFIX() + getInvoke() + "`";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "봇버젼";
    }

    @NotNull
    @Override
    public String getSmallHelp() {
        return "null";
    }
}
