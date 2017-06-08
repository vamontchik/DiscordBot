package command;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import static util.Util.buildAndSendMessage;

public class LogOutCommand implements Command {
    private final IDiscordClient bot;
    private final MessageReceivedEvent event;

    public LogOutCommand(IDiscordClient bot, MessageReceivedEvent event) {
        this.bot = bot;
        this.event = event;
    }

    /*
        Disconnects the bot.
     */
    @Override
    public void execute() {
        buildAndSendMessage(
                bot,
                event.getAuthor().mention() + " Logging out...",
                event.getChannel()
        );
        bot.logout();
    }
}
