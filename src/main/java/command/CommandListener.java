package command;

import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public class CommandListener {
    private final String commandIdentifier;
    private final IDiscordClient bot;
    private boolean isReady;

    public CommandListener(IDiscordClient bot) {
        commandIdentifier = "!";
        isReady = false;
        this.bot = bot;
    }

    @EventSubscriber
    public void onReadyEvent(ReadyEvent event) {
        isReady = true;
    }

    @EventSubscriber
    public void onMessageReceivedEvent(MessageReceivedEvent event) {
        if (isReady) {
            Command command = parseForCommand(event.getMessage().getContent(), event);
            command.execute();
        }
    }

    private Command parseForCommand(String message, MessageReceivedEvent event) {
        /*
            content[0] should be the identifier and command literal to work properly
            content[1...n] should be the list of arguments
         */
        String[] content = message.split(" ");

        //Ignores the Permissions set-up, but this is allowed since this command simply ignores the user input
        if (!content[0].startsWith(commandIdentifier)) {
            return new IgnoreCommand(bot, event);
        }

        String command = content[0].substring(1).toLowerCase();
        Command temp;
        switch (command) {
            case "logout":
                temp = new LogOutCommand(bot, event);
                break;
            case "help":
                temp = new HelpCommand(bot, event);
                break;
            default:
                temp = new FailedCommand(bot, "unrecognized command", event);
                break;
        }

        if (!(temp instanceof FailedCommand) && !temp.checkPermission()) {
            temp = new FailedCommand(bot, "invalid permissions", event);
        }

        return temp;
    }
}
