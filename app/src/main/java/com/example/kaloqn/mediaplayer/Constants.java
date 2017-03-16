package com.example.kaloqn.mediaplayer;

import android.net.Uri;
import android.provider.MediaStore;

/**
 * Created by kaloqn on 3/16/17.
 */

public class Constants {

    public static final int MY_PERMISSIONS_REQUEST_MP3_FILES=0;

    public static final int MEDIA_LOADER = 1;

    public static final Uri MUSIC_URI = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    public static final Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

    public static final String MUSIC_SELECTION = MediaStore.Audio.Media.IS_MUSIC + " != 0";
    public static final String VIDEO_SELECTION = null;

    public static final String FORMAT_MUSIC = "music";
    public static final String FORMAT_VIDEO = "video";


    public static final String[] PROJECTION = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME
    };

    public static final int COL_ID = 0;
    public static final int COL_DATA = 1;
    public static final int COL_DISPLAY_NAME = 2;

}
