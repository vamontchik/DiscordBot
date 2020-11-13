import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import instance.ServerInstance;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static Util.Util.*;

public final class Bot {
    @SuppressWarnings("FieldCanBeLocal")
    private static GatewayDiscordClient client;

    @SuppressWarnings("FieldCanBeLocal")
    private static String DISCORD_API_KEY = "";

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private static String GOOGLE_API_KEY = "";

    private static final Map<String, BiFunction<MessageCreateEvent, String[], String>> checkFns;
    private static final Map<String, BiConsumer<MessageCreateEvent, String[]>> execFns;
    private static final List<String> actions;
    private static final Map<String, ServerInstance> serverToAudio;

    // TODO: impl support for twitter
    static
    {
        serverToAudio = new HashMap<>();

        actions = List.of("play", "skip", "queue", "check", "search", "search-r", "disconnect");

        checkFns = new HashMap<>();
        populateCheckFns();

        execFns = new HashMap<>();
        populateExecFns();
    }

    private static void populateExecFns()
    {
        execFns.put("play", (e, tokens) ->
        {
            var guildId = e.getGuildId().get().asString(); // TODO: nullable ?
            var serverInst = GetOrSetupServerInstance(guildId);

            var chn = e.getMessage().getChannel().block();
            if (!ChannelNullCheck(chn)) return;

            var voiceChannel = GetVoiceChannel(e, chn, serverInst);
            voiceChannel.ifPresent(vc -> {
                var url = tokens[2];
                var connection = vc.join(spec -> spec.setProvider(serverInst.getProvider())).block();
                serverInst.setCurrVoiceConnection(connection);
                serverInst.getPlayerManager().loadItem(url, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        serverInst.getTrackScheduler().setOutputMessageChannel(chn);
                        serverInst.getTrackScheduler().queue(track);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        serverInst.getTrackScheduler().setOutputMessageChannel(chn);
                        for (AudioTrack track : playlist.getTracks()) {
                            serverInst.getTrackScheduler().queue(track);
                        }
                    }

                    @Override
                    public void noMatches() {
                        if (chn != null) sendMessageAsEmbed(chn, "No matches found! Get fucked, m8.");
                    }

                    @Override
                    public void loadFailed(FriendlyException throwable) {
                        if (chn != null) sendMessageAsEmbed(chn, throwable.toString());
                    }
                });
            });
        });
        execFns.put("skip", (e, tokens) ->
        {
            var guildId = e.getGuildId().get().asString(); // TODO: nullable ?
            var serverInst = GetOrSetupServerInstance(guildId);
            serverInst.getTrackScheduler().nextTrack();
        });
        execFns.put("queue", (e, tokens) ->
        {
            var guildId = e.getGuildId().get().asString(); // TODO: nullable ?
            var serverInst = GetOrSetupServerInstance(guildId);

            var chn = e.getMessage().getChannel().block();
            if (!ChannelNullCheck(chn)) return;

            serverInst.getTrackScheduler().setOutputMessageChannel(chn);
            serverInst.getTrackScheduler().printQueue();
        });
        execFns.put("check", (e,tokens) ->
        {
            var chn = e.getMessage().getChannel().block();
            if (!ChannelNullCheck(chn)) return;

            var maxMemory = Runtime.getRuntime().maxMemory();
            var maxMemoryStr = convert(maxMemory);

            var totalMemory = Runtime.getRuntime().totalMemory();
            var totalMemoryStr = convert(totalMemory);

            var availMemory = Runtime.getRuntime().freeMemory();
            var freeMemoryStr = convert(availMemory);

            var usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            var usedMemoryStr = convert(usedMemory);

            var str =
                "Server instance count: " + serverToAudio.size() + System.lineSeparator() +
                "JVM max memory limit: " + maxMemoryStr + System.lineSeparator() +
                "JVM memory currently allocated: " + totalMemoryStr + System.lineSeparator() +
                "JVM free memory currently available: " + freeMemoryStr + System.lineSeparator() +
                "JVM free memory currently used: " + usedMemoryStr + System.lineSeparator();
            //noinspection ConstantConditions
            sendMessageAsEmbedWithTitle(chn, str, "Process Info:");
        });
        execFns.put("search", (e,tokens) ->
        {
            var full = Arrays.asList(tokens);
            var argToStr = new StringBuilder();
            for (int i = 2; i < full.size(); ++i)
            {
                argToStr.append(full.get(i)).append(" ");
            }
            var searchArg = argToStr.toString();
            searchArg = searchArg.replaceAll(" ", "");

            var maxResults = 100; // TODO: allow end user to change this?
            var youtubeSearchURL =
                "https://youtube.googleapis.com/youtube/v3/search?" +
                "part=snippet&" +
                "maxResults=" + maxResults + "&" +
                "q=" + searchArg +
                "&key=" + GOOGLE_API_KEY;

            String jsonResponse = "";
            try {
                var client = HttpClient.newHttpClient();
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(youtubeSearchURL))
                        .headers("Accept", "application/json")
                        .build();
                var response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                jsonResponse = response.body();
            } catch (IOException | InterruptedException e1) {
                e1.printStackTrace();
                return;
            }

            var fullObj = new JSONObject(jsonResponse);
            var allItems = fullObj.getJSONArray("items");
            var validURLs = new ArrayList<String>();
            for (int i = 0; i < allItems.length(); ++i)
            {
                var curr = allItems.getJSONObject(i);
                var idObj = curr.getJSONObject("id");
                var kind = idObj.getString("kind");
                if (kind.equals("youtube#video"))
                {
                    validURLs.add(idObj.getString("videoId"));
                    break;
                }
            }
            // String randomURL = validURLs.get(new Random().nextInt(validURLs.size()));
            String mostRelevantURL = validURLs.get(0);

            String fullURL = "https://www.youtube.com/watch?v=" + mostRelevantURL;

            MessageChannel out = e.getMessage().getChannel().block();
            if (!ChannelNullCheck(out)) return;
            //noinspection ConstantConditions
            sendMessagePlain(out, fullURL);

            String[] newTokens = new String[3];
            List.of("$meow", "play", "https://www.youtube.com/watch?v=" + mostRelevantURL).toArray(newTokens);

            execFns.get("play").accept(e, newTokens);
        });
        execFns.put("search-r", (e,tokens) ->
        {
            // TODO: this is copy-pasted from search...

            var full = Arrays.asList(tokens);
            var argToStr = new StringBuilder();
            for (int i = 2; i < full.size(); ++i)
            {
                argToStr.append(full.get(i)).append(" ");
            }
            var searchArg = argToStr.toString();
            searchArg = searchArg.replaceAll(" ", "");

            var maxResults = 100; // TODO: allow end user to change this?
            var youtubeSearchURL =
                    "https://youtube.googleapis.com/youtube/v3/search?" +
                            "part=snippet&" +
                            "maxResults=" + maxResults + "&" +
                            "q=" + searchArg +
                            "&key=" + GOOGLE_API_KEY;

            String jsonResponse = "";
            try {
                var client = HttpClient.newHttpClient();
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(youtubeSearchURL))
                        .headers("Accept", "application/json")
                        .build();
                var response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                jsonResponse = response.body();
            } catch (IOException | InterruptedException e1) {
                e1.printStackTrace();
                return;
            }

            var fullObj = new JSONObject(jsonResponse);
            var allItems = fullObj.getJSONArray("items");
            var validURLs = new ArrayList<String>();
            for (int i = 0; i < allItems.length(); ++i)
            {
                var curr = allItems.getJSONObject(i);
                var idObj = curr.getJSONObject("id");
                var kind = idObj.getString("kind");
                if (kind.equals("youtube#video"))
                {
                    validURLs.add(idObj.getString("videoId"));
                }
            }
            String randomURL = validURLs.get(new Random().nextInt(validURLs.size()));

            String fullURL = "https://www.youtube.com/watch?v=" + randomURL;

            MessageChannel out = e.getMessage().getChannel().block();
            if (!ChannelNullCheck(out)) return;
            //noinspection ConstantConditions
            sendMessagePlain(out, fullURL);

            String[] newTokens = new String[3];
            List.of("$meow", "play", "https://www.youtube.com/watch?v=" + randomURL).toArray(newTokens);

            execFns.get("play").accept(e, newTokens);
        });
        execFns.put("disconnect", (e, tokens) ->
        {
            var guildId = e.getGuildId().get().asString(); // TODO: nullable ?
            ServerInstance inst = GetOrSetupServerInstance(guildId);

            var connection = inst.getCurrVoiceConnection();
            if (connection == null) return;

            inst.getCurrVoiceConnection().disconnect().block();

            // NOTE: queue is emptied AND server inst values set in event handler...
        });
    }

    private static void populateCheckFns()
    {
        checkFns.put("play", (e, tokens) ->
        {
            if (tokens.length != 3)
            {
                return "usage: $meow play <url>";
            }
            return "";
        });
        checkFns.put("skip", (e, tokens) ->
        {
            if (tokens.length != 2)
            {
                return "usage: $meow skip";
            }
            return "";
        });
        checkFns.put("queue", (e, tokens) ->
        {
            if (tokens.length != 2)
            {
                return "usage: $meow queue";
            }
            return "";
        });
        checkFns.put("check", (e, tokens) ->
        {
            AtomicReference<String> ret = new AtomicReference<>("");

            e.getMember().ifPresent(m ->
            {
                if (!m.getId().asString().equals("472019929340575744"))
                {
                    ret.set("Invalid permissions!");
                }
            });

            if (tokens.length != 2)
            {
                return "usage: $meow check";
            }

            return ret.get();
        });
        checkFns.put("search", (e, tokens) ->
        {
            // $meow search <args>
            if (tokens.length <= 2)
            {
                return "usage: $meow search <args...>";
            }
            return "";
        });
        checkFns.put("search-r", (e, tokens) ->
        {
            // $meow search-r <args>
            if (tokens.length <= 2)
            {
                return "usage: $meow search-r <args...>";
            }
            return "";
        });
        checkFns.put("disconnect", (e, tokens) ->
        {
            if (tokens.length != 2)
            {
                return "usage: $meow disconnect";
            }
            return "";
        });
    }

    private static ServerInstance GetOrSetupServerInstance(String guildID)
    {
        var serverInst = serverToAudio.get(guildID);
        if (serverInst == null)
        {
            serverInst = new ServerInstance(guildID);
            serverToAudio.put(guildID, serverInst);
        }
        return serverInst;
    }

    public static void main(String[] args)
    {
        DISCORD_API_KEY = args[0];
        GOOGLE_API_KEY = args[1];

        client = DiscordClientBuilder.create(DISCORD_API_KEY).build().login().block();
        if (client == null)
        {
            throw new RuntimeException("client was null!");
        }
        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event ->
        {
            var msg = event.getMessage();
            var chn = msg.getChannel().block();
            if (!ChannelNullCheck(chn)) return;

            var tokens = event.getMessage().getContent().split("\\s+");
            if (!ZeroLengthCheck(chn, tokens)) return;

            var base = tokens[0];
            if (!BaseCheck(base)) return;

            if (!OneLengthTokensCheck(chn, tokens)) return;
            var action = tokens[1];

            if (!ValidActionCheck(action, chn)) return;

            var checkFn = checkFns.get(action);
            var res = checkFn.apply(event, tokens);
            if (!CheckFnCheck(chn, res)) return;

            var execFn = execFns.get(action);
            execFn.accept(event, tokens);
        });
        client.getEventDispatcher().on(VoiceStateUpdateEvent.class).subscribe(event ->
        {
            // TODO : https://github.com/Discord4J/Discord4J/commit/87548f882c29b96a037ee17e041a97b26bcd996a
            //       can probably migrate to a later version when it is released , for now
            //       copy the code from the commit ...
            boolean voiceLeaveEvent = event.getCurrent().getChannelId().isEmpty() && event.getOld().isPresent();
            if (voiceLeaveEvent)
            {
                var guildId = event.getOld().get().getGuildId().asString(); // TODO: getOld().get() can throw exception ?
                var inst = GetOrSetupServerInstance(guildId);
                inst.setCurrVoiceConnection(null);
                inst.setCurrVoiceChannel(null);
                inst.getTrackScheduler().emptyQueue();
            }
        });
        client.onDisconnect().block();
    }

    private static boolean ValidActionCheck(String action, MessageChannel chn)
    {
        if (!actions.contains(action))
        {
            sendMessageAsEmbed(chn, "Invalid action! Available actions: " + actions);
            return false;
        }
        return true;
    }

    private static Optional<VoiceChannel> GetVoiceChannel(MessageCreateEvent event,
                                                          MessageChannel chn,
                                                          ServerInstance inst)
    {
        var ifPresent = inst.getCurrVoiceChannel();
        if (ifPresent == null)
        {
            var m = event.getMember().orElse(null);
            if (m == null)
            {
                sendMessageAsEmbed(chn, "Internal Error: event.getMember() was null...");
                return Optional.empty();
            }
            var voiceState = m.getVoiceState().block();
            if (voiceState == null)
            {
                sendMessageAsEmbed(chn, "You need to join a voice channel first!");
                return Optional.empty();
            }
            var voiceChannel = voiceState.getChannel().block();
            if (voiceChannel == null)
            {
                sendMessageAsEmbed(chn, "Internal Error: voiceState.getChannel() was null...");
                return Optional.empty();
            }

            inst.setCurrVoiceChannel(voiceChannel);
            return Optional.of(voiceChannel);
        }
        return Optional.of(ifPresent);
    }

    private static boolean CheckFnCheck(MessageChannel chn, String res)
    {
        if (!res.isEmpty())
        {
            sendMessageAsEmbed(chn, res);
            return false;
        }
        return true;
    }

    private static boolean OneLengthTokensCheck(MessageChannel chn, String[] tokens)
    {
        if (tokens.length == 1)
        {
            String res = "usage: $meow <action> <args?>" +
                    System.lineSeparator() +
                    "Available actions: " + actions;
            sendMessageAsEmbed(chn, res);
            return false;
        }
        return true;
    }

    private static boolean BaseCheck(String base)
    {
        if (!base.equals("$meow"))
        {
            System.out.println("skipping this message, since it doesn't start with '$meow'...");
            return false;
        }
        return true;
    }

    private static boolean ZeroLengthCheck(MessageChannel chn, String[] tokens)
    {
        if (tokens.length == 0)
        {
            sendMessageAsEmbed(chn, "Internal Error: tokens.length == 0");
            return false;
        }
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean ChannelNullCheck(MessageChannel chn)
    {
        if (chn == null)
        {
            System.err.println("chn was null...");
            return false;
        }
        return true;
    }
}
