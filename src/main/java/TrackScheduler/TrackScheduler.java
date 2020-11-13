package TrackScheduler;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.ArrayDeque;
import java.util.Queue;

import static Util.Util.*;

public final class TrackScheduler extends AudioEventAdapter {
    private final Queue<AudioTrack> q;
    private final AudioPlayer player;
    private MessageChannel output;

    @Override
    public void onPlayerPause(AudioPlayer player) {
        // Player was paused
        if (output != null) sendMessageAsEmbed(output, "Paused!");
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        // Player was resumed
        if (output != null) sendMessageAsEmbed(output, "Resumed!");
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // A track started playing
        if (output != null) sendMessageAsEmbedWithTitle(output,
            track.getInfo().author + " - " + track.getInfo().title,
            "New Track:"
        );
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            // Start next track
            nextTrack();
        }

        // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
        // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
        // endReason == STOPPED: The player was stopped.
        // endReason == REPLACED: Another track started playing while this had not finished
        // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
        //                       clone of this back to your queue
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
        if (output != null) sendMessageAsEmbed(output, exception.toString());
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        // Audio track has been unable to provide us any audio, might want to just start a new track
        if (thresholdMs > 5000)
        {
            if (output != null) sendMessageAsEmbed(output, "Audio stuck for more than 5 seconds! Skipping to next track...");
            nextTrack();
        }
    }

    public TrackScheduler(AudioPlayer player)
    {
        q = new ArrayDeque<>();
        this.player = player;
        output = null;
    }

    public void setOutputMessageChannel(MessageChannel output)
    {
        this.output = output;
    }

    public void queue(AudioTrack a)
    {
        try {
            q.add(a);
            player.startTrack(a, true);
        } catch (Exception e) {
            if (output != null) sendMessageAsEmbed(output, e.toString());
        }
    }

    public void nextTrack()
    {
        q.poll();
        player.startTrack(q.peek(), false);
    }

    public void printQueue()
    {
        if (output != null)
        {
            StringBuilder res = new StringBuilder();
            int i = 1;
            for (AudioTrack a : q)
            {
                res.append('`')
                    .append(i)
                    .append("` ")
                    .append(a.getInfo().author)
                    .append(" - ")
                    .append(a.getInfo().title)
                    .append(System.lineSeparator());
                if (i == 1)
                {
                    long currPosInMS = a.getPosition();
                    String currPosTimestamp = toTimestamp(currPosInMS);
                    long totalLengthInMS = a.getInfo().length;
                    String totalLengthTimestamp = toTimestamp(totalLengthInMS);

                    res.append(":play_pause:").append(" ");

                    res.append(currPosTimestamp)
                        .append("/")
                        .append(totalLengthTimestamp)
                        .append(System.lineSeparator());
                    res.append(System.lineSeparator());
                }
                ++i;
            }
            sendMessageAsEmbedWithTitle(output, res.toString(), "Song Queue:");
        }
    }

    public void emptyQueue()
    {
        q.clear();
    }
}