package util;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.*;

public class Util {
    /*
        Constructs a message, using MessageBuilder, to be sent to the specified IChannel by means of the specified IDiscordClient.
        Utilizes RequestBuilder to handle RateLimitExceptions and MissingPermissionsExceptions should the occasion arise.
     */
    public static void buildAndSendMessage(IDiscordClient bot, String message, IChannel channel) {
        MessageBuilder builder = new MessageBuilder(bot);
        builder.withContent(message);
        builder.withChannel(channel);

        RequestBuilder requestBuilder = new RequestBuilder(bot).shouldBufferRequests(true);
        requestBuilder.onMissingPermissionsError(e -> {
            //swallow exception silently...
        });
        requestBuilder.doAction(() -> {
           builder.send();
           return true;
        });
        requestBuilder.execute();
    }

    /*
    Creates an instance of the discord bot
    */
    public static IDiscordClient createClient(String token) {
        ClientBuilder clientBuilderWithToken = new ClientBuilder().withToken(token);
        try {
            return clientBuilderWithToken.login();
        } catch (DiscordException e) {
            e.printStackTrace();
            return null; //will throw an exception further down the line, when the program tries to access the client
        }
    }
}
