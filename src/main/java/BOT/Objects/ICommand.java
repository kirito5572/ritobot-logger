package BOT.Objects;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import javax.annotation.Nonnull;
import java.util.List;

public interface ICommand {

    void handle(List<String> args, @Nonnull GuildMessageReceivedEvent event);

    String getHelp();

    String getInvoke();

    String getSmallHelp();

}
