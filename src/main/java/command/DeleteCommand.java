package command;

import permissions.Permission;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.MessageHistory;
import sx.blah.discord.util.RequestBuilder;

import java.util.List;

import static util.Util.buildAndSendMessage;
import static util.Util.logger;

public class DeleteCommand extends Command {
    private final List<String> argsList;
    private final int MAX_LENGTH;

    public DeleteCommand(IDiscordClient bot, MessageReceivedEvent event, List<String> argsList) {
        super(bot, event, new Permission(Permission.Value.MEOWERS));
        this.argsList = argsList;
        MAX_LENGTH = 10;
    }

    @Override
    public void execute() {
        //Empty Argument List
        if (argsList.isEmpty()) {
            buildAndSendMessage(bot, event.getAuthor().mention() + " Usage: !delete [channelName] [amount]", event.getChannel());
        }

        //Usage of First Argument (and error handling)
        List<IChannel> channelLocations = event.getGuild().getChannelsByName(argsList.get(0)); //return all channels with that name
        if (channelLocations.isEmpty()) {
            buildAndSendMessage(bot, event.getAuthor().mention() + " Please supply a valid channel name!", event.getChannel());
            return;
        }

        //Usage of Second Argument (and error handling)
        int amountToDelete = 0;
        try {
            amountToDelete = Integer.parseInt(argsList.get(1));
            if (amountToDelete <= 0) {
                buildAndSendMessage(bot, event.getAuthor().mention() + " Please specify a non-negative amount for deletion size!", event.getChannel());
                return;
            }
            if (amountToDelete > MAX_LENGTH) {
                amountToDelete = MAX_LENGTH;
                buildAndSendMessage(bot, event.getAuthor().mention() + " Max deletion request is of size " + MAX_LENGTH + "!", event.getChannel());
            }
        } catch (NumberFormatException e) {
            buildAndSendMessage(bot, event.getAuthor().mention() + " Could not parse the specified amount to delete by!", event.getChannel());
            return;
        } catch (IndexOutOfBoundsException e) {
            buildAndSendMessage(bot, event.getAuthor().mention() + " Usage: !delete [channelName] [amount]", event.getChannel());
            return;
        }

        //Execution
        for (IChannel channel : channelLocations) {
            MessageHistory messageHistory = channel.getMessageHistory();
            for (int i = 0; i < amountToDelete; i++) {
                final int get = i;
                RequestBuilder builder = new RequestBuilder(bot)
                        .shouldBufferRequests(true)
                        .onMissingPermissionsError(e -> {
                            logger.debug("A MissingPermissionsException was thrown.", e);
                        }).onDiscordError(e -> {
                            logger.debug("A DiscordError was thrown!", e);
                        })
                        .doAction(() -> {
                            messageHistory.delete(get); //MessageHistory.delete(int index) can throw a DiscordException, RateLimitException, or MissingPermissionsException
                            return true;
                        });
                builder.execute();
            }
        }
    }
}
