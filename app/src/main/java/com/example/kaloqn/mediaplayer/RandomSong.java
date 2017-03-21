package com.example.kaloqn.mediaplayer;

import android.net.Uri;

/**
 * Created by kaloqn on 3/21/17.
 */

public class RandomSong {

    private Uri songUri;
    private int songNumber;

    public Uri getSongUri() {
        return songUri;
    }

    public void setSongUri(Uri songUri) {
        this.songUri = songUri;
    }

    public int getSongNumber() {
        return songNumber;
    }

    public void setSongNumber(int songNumber) {
        this.songNumber = songNumber;
    }

}
