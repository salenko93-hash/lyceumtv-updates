package ua.edu.cunl.tv.startup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ua.edu.cunl.tv.MainActivity;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Intent launch = new Intent(context, MainActivity.class);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);
        } catch (Exception ignored) {
            // Modern Android/TV firmware may block background activity starts.
        }
    }
}
