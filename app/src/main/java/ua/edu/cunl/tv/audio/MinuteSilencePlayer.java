package ua.edu.cunl.tv.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

public final class MinuteSilencePlayer {
    public interface Listener {
        void onCompleted();
        void onMissingAudio();
    }

    public static final long DOCUMENTED_DURATION_MS = 79_536L;

    private MediaPlayer player;

    public boolean start(Context context, Listener listener) {
        return start(context, 0.25f, listener);
    }

    public boolean start(Context context, float volume, Listener listener) {
        stop();
        int id = context.getResources().getIdentifier(
                "minute_silence", "raw", context.getPackageName());
        if (id == 0) {
            listener.onMissingAudio();
            return false;
        }
        try {
            player = MediaPlayer.create(context, id,
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    0);
            if (player == null) {
                listener.onMissingAudio();
                return false;
            }
            float v = Math.max(0f, Math.min(1f, volume));
            player.setVolume(v, v);
            player.setLooping(false);
            player.setOnCompletionListener(mp -> {
                stop();
                listener.onCompleted();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                stop();
                listener.onMissingAudio();
                return true;
            });
            player.start();
            return true;
        } catch (Exception e) {
            stop();
            listener.onMissingAudio();
            return false;
        }
    }

    public int durationMs() {
        try {
            return player == null ? 0 : player.getDuration();
        } catch (Exception e) {
            return 0;
        }
    }

    public void stop() {
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
    }

    public boolean isPlaying() {
        try { return player != null && player.isPlaying(); }
        catch (Exception e) { return false; }
    }
}
