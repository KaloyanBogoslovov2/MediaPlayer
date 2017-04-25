package com.example.kaloqn.mediaplayer;

import android.content.Context;
import android.content.Intent;

/**
 * Created by Kaloyan on 25/04/2017.
 */

public class MusicIntentReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent.getAction().equals(
                android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {

        }
    }
}