package command;

import permissions.Permission;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import static util.Util.buildAndSendEmbed;

public class HelpCommand extends Command {
    public HelpCommand(IDiscordClient bot, MessageReceivedEvent event) {
        super(bot, event, new Permission(Permission.Value.ALL));

    }

    /*
        Returns an Embed with a list of the supported commands.
     */
    @Override
    public void execute() {
        buildAndSendEmbed(
                bot,
                new String[]{
                        "!rps [rock/paper/scissors]",
                        "!logout",
                        "!help"},
                new String[]{
                        "A game of rock-paper-scissors with the bot. The second argument is which choice the user has made.",
                        "[Meowers] Disconnects the bot.",
                        "Returns an Embed with a list of the supported commands."},
                "List of Commands",
                "A quick run-down of all available commands.",
                event.getChannel()
        );
    }
}
