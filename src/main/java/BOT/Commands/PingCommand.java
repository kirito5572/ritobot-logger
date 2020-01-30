package BOT.Commands;

import BOT.App;
import BOT.Objects.ICommand;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PingCommand implements ICommand {
    @Override
    public void handle(List<String> args, @NotNull GuildMessageReceivedEvent event) {
        event.getChannel().sendMessage("퐁!").queue((message) ->
                message.editMessageFormat("logger_core\n" +
                        "결과: %sms", event.getJDA().getGatewayPing()).queue()
        );
    }

    @NotNull
    @Override
    public String getHelp() {
        return "레이턴시 ㄴㅇㄱ!\n" +
                "명령어: `" + App.getPREFIX() + getInvoke() + "`";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "핑";
    }

    @NotNull
    @Override
    public String getSmallHelp() {
        return "null";
    }
}