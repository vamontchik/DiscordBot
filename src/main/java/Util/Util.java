package Util;

import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class Util {
    public static boolean IsNull(Object o) { return o == null; }

    public static void sendMessageAsEmbed(MessageChannel output, String msg)
    {
        if (IsNull(output) || IsNull(msg)) return;

        output.createMessage(messageSpec ->
            messageSpec.setEmbed(embedSpec ->
            {
                embedSpec.setColor(Color.ENDEAVOUR);
                embedSpec.setTimestamp(Instant.now());
                embedSpec.setFooter("bottom text", null);
                embedSpec.setDescription(msg);
            })
        ).block();
    }

    public static void sendMessageAsEmbedWithTitle(MessageChannel output, String msg, String title)
    {
        if (IsNull(output) || IsNull(msg) || IsNull(title)) return;

        output.createMessage(messageSpec ->
            messageSpec.setEmbed(embedSpec ->
            {
                embedSpec.setColor(Color.ENDEAVOUR);
                embedSpec.setTitle(title);
                embedSpec.setTimestamp(Instant.now());
                embedSpec.setFooter("bottom text", null);
                embedSpec.setDescription(msg);
            })
        ).block();
    }

    public static void sendMessagePlain(MessageChannel output, String msg)
    {
        if (IsNull(output) || IsNull(msg)) return;

        output.createMessage(msg).block();
    }

    public static String convert(long bytes)
    {
        var curr = bytes * 1.0;
        var suffixes = List.of("B", "KB", "MB", "GB");

        var pos = 0;
        while (curr >= 1024.0)
        {
            ++pos;
            curr /= 1024.0;
        }

        return Math.round(curr) + " " + suffixes.get(pos);
    }

    public static String toTimestamp(long ms)
    {
        Duration obj = Duration.ofMillis(ms);
        String strRep = "";
        int hours = obj.toHoursPart();
        int minutes = obj.toMinutesPart();
        int seconds = obj.toSecondsPart();
        if (hours > 0)
        {
            if (hours <= 9)
            {
                strRep += "0" + hours + ":";
            }
            else
            {
                strRep += hours + ":";
            }
        }
        if (minutes <= 9)
        {
            strRep += "0" + minutes + ":";
        }
        else
        {
            strRep += minutes + ":";
        }
        if (seconds <= 9)
        {
            strRep += "0" + seconds;
        }
        else
        {
            strRep += seconds;
        }
        return strRep;
    }
}
