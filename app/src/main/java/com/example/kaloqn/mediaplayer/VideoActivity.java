package com.example.kaloqn.mediaplayer;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.MediaController;
import android.widget.VideoView;

/**
 * Created by Kaloyan on 3/16/17. The activity where the video is displayed.
 */

public class VideoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        String videoUri = getIntent().getStringExtra("uri");

        MediaController mediaController = new MediaController(this);

        VideoView videoView = (VideoView)findViewById(R.id.video);
        videoView.setVideoURI(Uri.parse(videoUri));
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.start();
    }

}
