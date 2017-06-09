package command;

import permissions.Permission;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import static util.Util.buildAndSendMessage;

public class LogOutCommand extends Command {
    public LogOutCommand(IDiscordClient bot, MessageReceivedEvent event) {
        super(bot, event, new Permission(Permission.Value.MEOWERS));
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
