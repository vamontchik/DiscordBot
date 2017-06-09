package util;

import permissions.Permission;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class Util {
    /*
        Constructs a message, using MessageBuilder, to be sent to the specified IChannel by means of the specified IDiscordClient.
        Utilizes RequestBuilder to handle RateLimitExceptions and MissingPermissionsExceptions should the occasion arise.
     */
    public static void buildAndSendMessage(IDiscordClient bot, String message, IChannel channel) {
        MessageBuilder messageBuilder = new MessageBuilder(bot);
        messageBuilder.withContent(message);
        messageBuilder.withChannel(channel);

        RequestBuilder requestBuilder = new RequestBuilder(bot).shouldBufferRequests(true);
        requestBuilder.onMissingPermissionsError(e -> {
            //swallow exception silently...
        });
        requestBuilder.doAction(() -> {
           messageBuilder.send();
           return true;
        });
        requestBuilder.execute();
    }


    /*
        Constructs and sends an Embed, using EmbedBuilder, to be sent to the specified IChannel by means of a specified IDiscordClient.
        It is assumed that lineTitles[n] and lineFields[n] correspond to the same lines, and will be displayed together.
            As a consequence of this assumption, it can be inferred that lineTitles and lineFields are of the same size.
        It is assumed that the arrays lineTitles and lineFields are non-empty and do not contain null.
        Utilizes RequestBuilder to handle RateLimitExceptions should the occasion arise.
    */
    public static void buildAndSendEmbed(IDiscordClient bot,
                                         String[] lineTitles,
                                         String[] lineFields,
                                         String embedTitle,
                                         String embedDescription,
                                         IChannel channel) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.withTitle(embedTitle);
        embedBuilder.withDescription(embedDescription);
        for (int i = 0; i < lineTitles.length; i++) {
            embedBuilder.appendField(lineTitles[i], lineFields[i], false);
        }
        embedBuilder.withColor(0, 0, 255); //blue
        embedBuilder.withFooterText("github.com/meowingmeowers/meowingBot");
        embedBuilder.withTimestamp(ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime());

        MessageBuilder messageBuilder = new MessageBuilder(bot);
        messageBuilder.withEmbed(embedBuilder.build());
        messageBuilder.withChannel(channel);
        
        RequestBuilder requestBuilder = new RequestBuilder(bot).shouldBufferRequests(true);
        requestBuilder.onMissingPermissionsError(e -> {
            //swallow exception silently...
        });
        requestBuilder.doAction(() -> {
            messageBuilder.send();
            return true;
        });
        requestBuilder.execute();
    }
}
