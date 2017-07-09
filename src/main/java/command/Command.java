package command;

import permissions.Permission;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IUser;

import java.io.*;

public abstract class Command {
    protected final IDiscordClient bot;
    protected final MessageReceivedEvent event;
    protected final Permission requiredPermission;
    protected final Permission obtainedPermission;

    public Command(IDiscordClient bot, MessageReceivedEvent event, Permission requiredPermission) {
        this.bot = bot;
        this.event = event;
        this.requiredPermission = requiredPermission;
        this.obtainedPermission = setUpPermission(event.getAuthor());
    }

    /*
        Sets up proper permission, depending on what IUser is passed in.
    */
    private Permission setUpPermission(IUser user) {
        File file = new File("properties/id.txt");
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new Permission(Permission.Value.ALL);
        }

        String read;
        try {
            read = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return new Permission(Permission.Value.ALL);
        }

        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (user.getStringID().equals(read)) {
            return new Permission(Permission.Value.MEOWERS);
        } else {
            return new Permission(Permission.Value.ALL);
        }
    }

    public abstract void execute();

    public final boolean checkPermission() {
        return obtainedPermission.equalsOrGreater(requiredPermission);
    }
}
