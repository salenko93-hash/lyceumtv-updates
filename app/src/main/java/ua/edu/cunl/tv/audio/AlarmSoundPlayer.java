package ua.edu.cunl.tv.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

public final class AlarmSoundPlayer {
    private MediaPlayer player;

    public boolean start(Context context, float volume) {
        stop();
        int id = context.getResources().getIdentifier(
                "trivoga", "raw", context.getPackageName());
        if (id == 0) return false;

        try {
            player = MediaPlayer.create(context, id,
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    0);
            if (player == null) return false;

            float v = Math.max(0f, Math.min(1f, volume));
            player.setVolume(v, v);
            player.setLooping(false);
            player.setOnCompletionListener(mp -> stop());
            player.setOnErrorListener((mp, what, extra) -> {
                stop();
                return true;
            });
            player.start();
            return true;
        } catch (Exception e) {
            stop();
            return false;
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
