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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
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

    /*
     * We only have a lock for serverToAudio since those are the only *real* cases
     * of concurrency issues: checkFns and execFns are always populated
     * in the static initializer block and all other calls are just .get(),
     * so we should be OK without locks for checkFns and execFns.
     *
     * The same idea holds for actions: only read calls happen to actions,
     * and actions is defined in the static initializer block as well.
     */
    private static final ReentrantLock serverToAudioLock;

    // TODO: impl support for twitter ...
    // TODO: search / search-r functionality put into one function?

    static
    {
        serverToAudio = new HashMap<>();
        serverToAudioLock = new ReentrantLock();

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
            var guildId = e.getGuildId().orElse(null);
            if (IsNull(guildId)) return;

            var serverInst = GetOrSetupServerInstance(guildId.asString());

            var chn = e.getMessage().getChannel().block();
            if (IsNull(chn)) return;

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
                        sendMessageAsEmbed(chn, "No matches found! Get fucked, m8.");
                    }

                    @Override
                    public void loadFailed(FriendlyException throwable) {
                        sendMessageAsEmbed(chn, throwable.toString());
                    }
                });
            });
        });
        execFns.put("skip", (e, tokens) ->
        {
            var guildId = e.getGuildId().orElse(null);
            if (IsNull(guildId)) return;

            var serverInst = GetOrSetupServerInstance(guildId.asString());

            serverInst.getTrackScheduler().nextTrack();
        });
        execFns.put("queue", (e, tokens) ->
        {
            var guildId = e.getGuildId().orElse(null);
            if (IsNull(guildId)) return;

            var serverInst = GetOrSetupServerInstance(guildId.asString());

            var chn = e.getMessage().getChannel().block();
            if (IsNull(chn)) return;

            serverInst.getTrackScheduler().setOutputMessageChannel(chn);
            serverInst.getTrackScheduler().printQueue();
        });
        execFns.put("check", (e,tokens) ->
        {
            var chn = e.getMessage().getChannel().block();
            if (IsNull(chn)) return;

            var maxMemory = Runtime.getRuntime().maxMemory();
            var maxMemoryStr = convert(maxMemory);

            var totalMemory = Runtime.getRuntime().totalMemory();
            var totalMemoryStr = convert(totalMemory);

            var availMemory = Runtime.getRuntime().freeMemory();
            var freeMemoryStr = convert(availMemory);

            var usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            var usedMemoryStr = convert(usedMemory);

            serverToAudioLock.lock();
            try
            {
                var str = "Server instance count: " + serverToAudio.size() + System.lineSeparator() +
                    "JVM max memory limit: " + maxMemoryStr + System.lineSeparator() +
                    "JVM memory currently allocated: " + totalMemoryStr + System.lineSeparator() +
                    "JVM free memory currently available: " + freeMemoryStr + System.lineSeparator() +
                    "JVM free memory currently used: " + usedMemoryStr + System.lineSeparator();
                sendMessageAsEmbedWithTitle(chn, str, "Process Info:");
            }
            finally
            {
                serverToAudioLock.unlock();
            }
        });
        execFns.put("search", (e,tokens) ->
        {
            var searchArg = BuildSearchArgFromTokens(tokens);
            var youtubeSearchUrl = BuildYoutubeSearchUrl(searchArg);
            var jsonResponse = GetYoutubeApiResponse(youtubeSearchUrl);
            var validURLs = GetVideoUrls(jsonResponse);

            if (validURLs.isEmpty())
            {
                sendMessageAsEmbed(e.getMessage().getChannel().block(), "No search results found!");
                return;
            }

            // youtube sorts by relevance by default, so pick the first one
            // for the most relevant video
            var mostRelevantURL = validURLs.get(0);
            var fullURL = "https://www.youtube.com/watch?v=" + mostRelevantURL;

            var out = e.getMessage().getChannel().block();
            if (!IsNull(out))
            {
                sendMessagePlain(out, fullURL);
            }

            var newTokens = new String[3];
            List.of("$meow", "play", "https://www.youtube.com/watch?v=" + mostRelevantURL).toArray(newTokens);

            execFns.get("play").accept(e, newTokens);
        });
        execFns.put("search-r", (e,tokens) ->
        {
            var searchArg = BuildSearchArgFromTokens(tokens);
            var youtubeSearchUrl = BuildYoutubeSearchUrl(searchArg);
            var jsonResponse = GetYoutubeApiResponse(youtubeSearchUrl);

            var validURLs = GetVideoUrls(jsonResponse);

            if (validURLs.isEmpty())
            {
                sendMessageAsEmbed(e.getMessage().getChannel().block(), "No search results found!");
                return;
            }

            var randomURL = validURLs.get(new Random().nextInt(validURLs.size()));
            var fullURL = "https://www.youtube.com/watch?v=" + randomURL;

            var out = e.getMessage().getChannel().block();
            if (!IsNull(out))
            {
                sendMessagePlain(out, fullURL);
            }

            var newTokens = new String[3];
            List.of("$meow", "play", "https://www.youtube.com/watch?v=" + randomURL).toArray(newTokens);

            execFns.get("play").accept(e, newTokens);
        });
        execFns.put("disconnect", (e, tokens) ->
        {
            var guildId = e.getGuildId().orElse(null);
            if (IsNull(guildId)) return;

            ServerInstance inst = GetOrSetupServerInstance(guildId.asString());

            var connection = inst.getCurrVoiceConnection();
            if (IsNull(connection)) return;
            connection.disconnect().block();

            // NOTE: queue is emptied AND server inst values set in event handler...
        });
    }

    private static List<String> GetVideoUrls(String jsonResponse)
    {
        var validUrls = new ArrayList<String>();

        JSONObject fullObj;
        try
        {
            fullObj = new JSONObject(jsonResponse);
        }
        catch (org.json.JSONException e)
        {
            return validUrls; // exit early
        }

        JSONArray allItems;
        try
        {
            allItems = fullObj.getJSONArray("items");
        }
        catch (org.json.JSONException e)
        {
            return validUrls; // exit early
        }

        for (int i = 0; i < allItems.length(); ++i)
        {
            var curr = allItems.getJSONObject(i);

            JSONObject idObj;
            try
            {
                idObj = curr.getJSONObject("id");
            }
            catch (org.json.JSONException e)
            {
                continue; // skip this item, continue to next
            }

            String kind;
            try
            {
                kind = idObj.getString("kind");
            }
            catch (org.json.JSONException e)
            {
                continue; // skip this item, continue to next
            }

            if (kind.equals("youtube#video"))
            {
                try
                {
                    var videoURL = idObj.getString("videoId");
                    validUrls.add(videoURL);
                }
                catch (org.json.JSONException e)
                {
                    // do nothing, just continue with loop...
                }
            }
        }

        return validUrls;
    }

    private static String GetYoutubeApiResponse(String youtubeSearchUrl)
    {
        String resp;
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(youtubeSearchUrl))
                .headers("Accept", "application/json")
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e); // rethrow upwards as unchecked
        }
        resp = response.body();
        return resp;
    }

    private static String BuildYoutubeSearchUrl(String searchArg)
    {
        var maxResults = 100; // TODO: allow end user to change this?

        var url =
            "https://youtube.googleapis.com/youtube/v3/search?" +
            "part=snippet&" +
            "maxResults=" + maxResults + "&";

        if (IsNull(searchArg))
        {
            url += "q=null&";
        }
        else
        {
            url += "q=" + searchArg + "&";
        }

        url += "key=" + GOOGLE_API_KEY;

        return url;
    }

    private static String BuildSearchArgFromTokens(String[] tokens)
    {
        if (IsNull(tokens)) return "";

        var full = Arrays.asList(tokens);
        var argToStr = new StringBuilder();
        for (int i = 2; i < full.size(); ++i)
        {
            argToStr.append(full.get(i)).append(" ");
        }

        return argToStr.toString().replaceAll(" ", "");
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
        serverToAudioLock.lock();
        try
        {
            var serverInst = serverToAudio.get(guildID);
            if (IsNull(serverInst))
            {
                serverInst = new ServerInstance(guildID);
                serverToAudio.put(guildID, serverInst);
            }
            return serverInst;
        }
        finally
        {
            serverToAudioLock.unlock();
        }
    }

    public static void main(String[] args)
    {
        DISCORD_API_KEY = args[0];
        GOOGLE_API_KEY = args[1];

        client = DiscordClientBuilder.create(DISCORD_API_KEY).build().login().block();
        if (IsNull(client))
        {
            System.err.println("client was null = failed to be built or logged in!");
            return;
        }

        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event ->
        {
            if (IsNull(event))
            {
                System.err.println("MessageCreateEvent event object was null!");
                return;
            }

            var msg = event.getMessage();

            var chn = msg.getChannel().block();
            if (IsNull(chn))
            {
                System.err.println("event.getMessage().getChannel() was null!");
                return;
            }

            var msgContents = msg.getContent();

            var tokens = msgContents.split("\\s+");
            if (AreTokensLengthZero(tokens))
            {
                sendMessageAsEmbed(chn, "Internal Error: tokens.length == 0");
                return;
            }

            var base = tokens[0];
            if (!IsBotBaseStr(base)) return;

            if (AreTokensLengthOne(tokens))
            {
                var res = "usage: $meow <action> <args?>" + System.lineSeparator() + "Available actions: " + actions;
                sendMessageAsEmbed(chn, res);
                return;
            }

            var action = tokens[1];
            if (!IsValidAction(action))
            {
                sendMessageAsEmbed(chn, "Invalid action! Available actions: " + actions);
                return;
            }

            var checkFn = checkFns.get(action);
            var res = checkFn.apply(event, tokens);
            if (!DidCheckFnSucceed(res))
            {
                sendMessageAsEmbed(chn, res);
                return;
            }

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
                // .get() cannot throw exception since we do the .getOld().isPresent() check earlier!
                var guildId = event.getOld().get().getGuildId().asString();
                var inst = GetOrSetupServerInstance(guildId);
                inst.setCurrVoiceConnection(null);
                inst.setCurrVoiceChannel(null);
                inst.getTrackScheduler().emptyQueue();
            }
        });

        client.onDisconnect().block();
    }

    private static Optional<VoiceChannel> GetVoiceChannel(MessageCreateEvent event,
                                                          MessageChannel chn,
                                                          ServerInstance inst)
    {
        if (IsNull(event) || IsNull(inst)) return Optional.empty();
        var ifPresent = inst.getCurrVoiceChannel();
        if (IsNull(ifPresent))
        {
            var m = event.getMember().orElse(null);
            if (IsNull(m))
            {
                if (!IsNull(chn)) sendMessageAsEmbed(chn, "Internal Error: event.getMember() was null...");
                return Optional.empty();
            }
            var voiceState = m.getVoiceState().block();
            if (IsNull(voiceState))
            {
                if (!IsNull(chn)) sendMessageAsEmbed(chn, "You need to join a voice channel first!");
                return Optional.empty();
            }
            var voiceChannel = voiceState.getChannel().block();
            if (IsNull(voiceChannel))
            {
                if (!IsNull(chn)) sendMessageAsEmbed(chn, "Internal Error: voiceState.getChannel() was null...");
                return Optional.empty();
            }
            inst.setCurrVoiceChannel(voiceChannel);
            return Optional.of(voiceChannel);
        }
        return Optional.of(ifPresent);
    }

    private static boolean IsValidAction(String action)
    {
        if (IsNull(action)) return false;
        return actions.contains(action);
    }

    private static boolean DidCheckFnSucceed(String res)
    {
        if (IsNull(res)) return false;
        return res.isEmpty();
    }

    private static boolean AreTokensLengthOne(String[] tokens)
    {
        if (IsNull(tokens)) return false; // TODO: what values should this have?
        return tokens.length == 1;
    }

    private static boolean IsBotBaseStr(String base)
    {
        if (IsNull(base)) return false;
        return base.equals("$meow");
    }

    private static boolean AreTokensLengthZero(String[] tokens)
    {
        if (IsNull(tokens)) return false; // TODO: what value should this be?
        return (tokens.length == 0);
    }
}
