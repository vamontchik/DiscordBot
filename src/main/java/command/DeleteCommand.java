package command;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import static util.Util.buildAndSendMessage;

public class DeleteCommand implements Command {
    private final IDiscordClient bot;
    private final MessageReceivedEvent event;

    public DeleteCommand(IDiscordClient bot, MessageReceivedEvent event) {
        this.bot = bot;
        this.event = event;
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
