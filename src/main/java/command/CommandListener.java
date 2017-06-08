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

        if (content.length == 0) {
            return new FailedCommand(bot, "empty contents array", event);
        }

        if (!content[0].startsWith(commandIdentifier)) {
            return new FailedCommand(bot, "incorrect command identifier", event);
        }

        String command = content[0].substring(1).toLowerCase();
        switch (command) {
            case "logout":
                return new LogOutCommand(bot, event);
            case "help":
                return new HelpCommand(bot, event);
            case "delete":
                return new DeleteCommand(bot, event);
            default:
                return new FailedCommand(bot, "unrecognized command", event);
        }
    }
}
