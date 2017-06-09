package command;

import permissions.Permission;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

import static util.Util.setUpPermission;

public abstract class Command {
    protected final IDiscordClient bot;
    protected final MessageReceivedEvent event;
    protected final Permission requiredPermission;
    protected final Permission obtainedPermission;

        this.bot = bot;
        this.event = event;
        this.obtainedPermission = setUpPermission(event.getAuthor());
    }

    public abstract void execute();

    public boolean checkPermission() {
        return (requiredPermission.getValue() == obtainedPermission.getValue());
    }
}
