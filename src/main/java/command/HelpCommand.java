package command;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import static util.Util.buildAndSendEmbed;

public class HelpCommand implements Command {
    private final IDiscordClient bot;
    private final MessageReceivedEvent event;

    public HelpCommand(IDiscordClient bot, MessageReceivedEvent event) {
        this.bot = bot;
        this.event = event;
    }

    /*
        Returns an Embed with a list of the supported commands.
     */
    @Override
    public void execute() {
        buildAndSendEmbed(
                bot,
                new String[]{
                        "!delete",
                        "!logout",
                        "!help"},
                new String[]{
                        "Deletes the specified messages. Not yet implemented.",
                        "Disconnects the bot.",
                        "Returns an Embed with a list of the supported commands."},
                "List of Commands",
                "A quick run-down of all available commands.",
                event.getChannel()
        );
    }
}
