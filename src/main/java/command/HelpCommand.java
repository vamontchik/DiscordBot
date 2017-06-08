package command;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import static util.Util.buildAndSendMessage;

public class HelpCommand implements Command {
    private final IDiscordClient bot;
    private final MessageReceivedEvent event;

    public HelpCommand(IDiscordClient bot, MessageReceivedEvent event) {
        this.bot = bot;
        this.event = event;
    }

    /*
        Returns an Embed with a list of the supported commands. Not yet implemented.
     */
    @Override
    public void execute() {
        buildAndSendMessage(
                bot,
                event.getAuthor().mention() + " Unimplemented command.",
                event.getChannel()
        );
    }
}
