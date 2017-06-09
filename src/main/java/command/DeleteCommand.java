package command;

import permissions.Permission;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import static util.Util.buildAndSendMessage;

public class DeleteCommand extends Command{
    public DeleteCommand(IDiscordClient bot, MessageReceivedEvent event) {
        super(bot, event, new Permission(Permission.Value.MEOWERS));
    }

    /*
        Deletes the specified messages. Not yet implemented.
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
