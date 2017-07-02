package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class Util {
    public static final Logger logger;
    static {
        logger = LoggerFactory.getLogger(Util.class);
    }

    /*
        Constructs a message, using MessageBuilder, to be sent to the specified IChannel by means of the specified IDiscordClient.
        Utilizes RequestBuilder to handle RateLimitExceptions and MissingPermissionsExceptions should the occasion arise.
     */
    public static void buildAndSendMessage(IDiscordClient bot, String message, IChannel channel) {
        MessageBuilder messageBuilder = new MessageBuilder(bot)
                .withContent(message)
                .withChannel(channel);

        RequestBuilder requestBuilder = new RequestBuilder(bot)
                .shouldBufferRequests(true)
                .onMissingPermissionsError(e -> {
                    logger.debug("A MissingPermissionsException was thrown.", e);
                })
                .doAction(() -> {
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
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .withTitle(embedTitle)
                .withDescription(embedDescription)
                .withColor(0, 0, 255)
                .withFooterText("github.com/meowingmeowers/meowingBot")
                .withTimestamp(ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime());
        for (int i = 0; i < lineTitles.length; i++) {
            embedBuilder.appendField(lineTitles[i], lineFields[i], false);
        }

        MessageBuilder messageBuilder = new MessageBuilder(bot)
                .withEmbed(embedBuilder.build())
                .withChannel(channel);
        
        RequestBuilder requestBuilder = new RequestBuilder(bot)
                .shouldBufferRequests(true)
                .onMissingPermissionsError(e -> {
                    logger.debug("A MissingPermissionsException was thrown.", e);
                })
                .doAction(() -> {
                    messageBuilder.send();
                    return true;
                });
        requestBuilder.execute();
    }
}
