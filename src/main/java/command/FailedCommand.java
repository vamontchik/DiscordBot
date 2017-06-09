package command;

import permissions.Permission;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import static util.Util.buildAndSendMessage;

public class FailedCommand extends Command {
    private final String errMessage;

    public FailedCommand(IDiscordClient bot, String errMessage, MessageReceivedEvent event) {
        super(bot, event, new Permission(Permission.Value.MEOWERS));
        this.errMessage = errMessage;
    }

    /*
        Displays an error message, with a reason.
     */
    @Override
    public void execute() {
        buildAndSendMessage(
                bot,
                event.getAuthor().mention() + " Could not execute command. Reason: " + errMessage,
                event.getChannel()
        );
    }
}
