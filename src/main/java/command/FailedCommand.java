package command;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import static util.Util.buildAndSendMessage;

public class FailedCommand implements Command {
    private final IDiscordClient bot;
    private final String errMessage;
    private final MessageReceivedEvent event;

    public FailedCommand(IDiscordClient bot, String errMessage, MessageReceivedEvent event) {
        this.bot = bot;
        this.errMessage = errMessage;
        this.event = event;
    }

    /*
        Displays an error message, with a reason.
     */
    @Override
    public void execute() {
        buildAndSendMessage(
                bot,
                event.getAuthor().mention() + " Could not construct command. Reason: " + errMessage,
                event.getChannel()
        );
    }
}
