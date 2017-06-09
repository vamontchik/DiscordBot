package command;

import permissions.Permission;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public class IgnoreCommand extends Command {
    public IgnoreCommand(IDiscordClient bot, MessageReceivedEvent event) {
        super(bot, event, new Permission(Permission.Value.ALL));
    }

    @Override
    public void execute() {
        //do nothing
    }
}
