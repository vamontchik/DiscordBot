package instance;

import TrackScheduler.TrackScheduler;
import audio.LavaPlayerAudioProvider;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;

public final class ServerInstance {
    private final AudioPlayerManager playerManager;
    private final AudioPlayer player;
    private final AudioProvider provider;
    private final TrackScheduler trackScheduler;

    private VoiceConnection currVoiceConnection;
    private VoiceChannel currVoiceChannel;

    private final String guildId;

    public ServerInstance(String id)
    {
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);
        player = playerManager.createPlayer();
        provider = new LavaPlayerAudioProvider(player);
        trackScheduler = new TrackScheduler(player);
        player.addListener(trackScheduler);

        currVoiceChannel = null;
        guildId = id;
    }

    public AudioPlayerManager getPlayerManager() { return playerManager; }
    public AudioPlayer getPlayer() { return player; }
    public AudioProvider getProvider() { return provider; }
    public TrackScheduler getTrackScheduler() { return trackScheduler; }
    public VoiceChannel getCurrVoiceChannel() { return currVoiceChannel; }
    public void setCurrVoiceChannel(VoiceChannel v) { currVoiceChannel = v; }
    public VoiceConnection getCurrVoiceConnection() { return currVoiceConnection; }
    public void setCurrVoiceConnection(VoiceConnection v) { currVoiceConnection = v; }
    public String getGuildId() { return guildId; }
}
