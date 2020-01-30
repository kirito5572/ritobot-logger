package BOT.Objects;

import BOT.App;
import BOT.Commands.PingCommand;
import BOT.Commands.VersionCommand;
import BOT.Commands.upTimeCommand;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

public class CommandManager {
    private final Logger logger = LoggerFactory.getLogger(CommandManager.class);

    private final Map<String, ICommand> commands = new HashMap<>();

    public CommandManager() {
        {
            addCommand(new PingCommand());
            addCommand(new PingCommand() {
                @NotNull
                @Override
                public String getInvoke() {
                    return "ping";
                }

                @NotNull
                @Override
                public String getSmallHelp() {
                    return "";
                }

                @NotNull
                @Override
                public String getHelp() {
                    return "Pong!\n" +
                            "Usage: `" + App.getPREFIX() + getInvoke() + "`";
                }
            });
        }
        //------------------------------------------------------------------//
        {
            addCommand(new VersionCommand());
            addCommand(new VersionCommand() {
                @NotNull
                @Override
                public String getInvoke() {
                    return "version";
                }

                @NotNull
                @Override
                public String getSmallHelp() {
                    return "";
                }

                @NotNull
                @Override
                public String getHelp() {
                    return "say bot's build version";
                }
            });
        }
        //------------------------------------------------------------------//
        {
            addCommand(new upTimeCommand());
            addCommand(new upTimeCommand() {
                @NotNull
                @Override
                public String getInvoke() {
                    return "upTime";
                }

                @NotNull
                @Override
                public String getSmallHelp() {
                    return "";
                }

                @NotNull
                @Override
                public String getHelp() {
                    return "Bot's uptime";
                }
            });
        }
    }

    private void addCommand(@NotNull ICommand command) {
        if(!commands.containsKey(command.getInvoke())) {
            commands.put(command.getInvoke(), command);
        }
    }


    @NotNull
    public Collection<ICommand> getCommands() {
        return commands.values();
    }

    public ICommand getCommand(String name) {
        return commands.get(name);
    }

    public void handleCommand(@NotNull GuildMessageReceivedEvent event) {
        final TextChannel channel = event.getChannel();
        final String[] split = event.getMessage().getContentRaw().replaceFirst(
                "(?i)" + Pattern.quote(App.getPREFIX()), "").split("\\s+");
        final String invoke = split[0].toLowerCase();

        if(commands.containsKey(invoke)) {
            final List<String> args = Arrays.asList(split).subList(1, split.length);

            channel.sendTyping().queue();
            commands.get(invoke).handle(args, event);
        }
    }
}
