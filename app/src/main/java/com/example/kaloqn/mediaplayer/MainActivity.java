package com.example.kaloqn.mediaplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    private static final int MY_PERMISSIONS_REQUEST_MP3_FILES=0;
    private static final int SONGS_LOADER = 1;

    private static final String[] PROJECTION = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME
    };

    private static final int COL_ID = 0;
    private static final int COL_DATA = 1;
    private static final int COL_DISPLAY_NAME = 2;

    private static boolean playButtonPressed = false;
    private static HashMap<String, Uri> songsList = new HashMap<String, Uri>();
    private static MediaPlayer mediaPlayer=null;

    private int totalSongs =0;
    private int currentSongNumber=0;

    private ImageButton playButton;
    private ListView listView;

    private Uri prevSong = null;
    private Uri nextSong = null;
    private ArrayList<String> allSongsNames= null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playButton = (ImageButton) findViewById(R.id.play_stop_song);

        initListView();
        checkForPermissionsAndLoadSongs();
        }

    private void initListView(){

        ArrayAdapter<String> adapter= new ArrayAdapter<String>(
                this,
                R.layout.file_name,
                R.id.file_name_text_view,
                new String[]{"Loading..."}
        );

        listView = (ListView) findViewById(R.id.files_list);
        TextView emptyList = (TextView) findViewById(R.id.empty);
        listView.setEmptyView(emptyList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                try {
                        String songName =(String) adapterView.getItemAtPosition(position);
                        Uri songUri = songsList.get(songName);

                    if (mediaPlayer==null) {
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mediaPlayer.setDataSource(getApplicationContext(), songUri);
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        playButton.setImageResource(android.R.drawable.ic_media_pause);
                        playButtonPressed = true;

                    }else {
                        startDifferentSong(songUri);
                    }

                    if (position>1&&position< totalSongs)prevSong = songsList.get(adapterView.getItemAtPosition(position-1));
                    if (position>0&&position< totalSongs)nextSong = songsList.get(adapterView.getItemAtPosition(position+1));
                    currentSongNumber = position;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startDifferentSong(Uri songUri){
        if (mediaPlayer.isPlaying()|| mediaPlayer!=null){
            if(mediaPlayer!=null){
                playButton.setImageResource(android.R.drawable.ic_media_pause);
                playButtonPressed = true;
            }
            mediaPlayer.setLooping(false);
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer=MediaPlayer.create(getApplicationContext(), songUri);
            mediaPlayer.start();
        }
    }

    private void checkForPermissionsAndLoadSongs(){

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_MP3_FILES);
        }else{
            getSupportLoaderManager().initLoader(SONGS_LOADER, null, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_MP3_FILES: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getSupportLoaderManager().initLoader(SONGS_LOADER, null, this);
                } else {
                    Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    public void changeToPreviousSong(View v){
        if(prevSong==null)return;

        if (allSongsNames!=null&&currentSongNumber>0){
            currentSongNumber--;
            if (currentSongNumber<totalSongs){
                prevSong = songsList.get(allSongsNames.get(currentSongNumber));
                nextSong = songsList.get(allSongsNames.get(currentSongNumber+1));
            }
        }
        startDifferentSong(prevSong);
    }

    public void playStopSong(View v){
        if (mediaPlayer==null)return;
        if (playButtonPressed){
            //stop song
            playButton.setImageResource(android.R.drawable.ic_media_play);
            mediaPlayer.pause();
            playButtonPressed = false;
        }else{
            //start song
            playButton.setImageResource(android.R.drawable.ic_media_pause);
            mediaPlayer.start();
            playButtonPressed = true;
        }
    }
    
    public void changeToNextSong(View v){
        if (nextSong==null)return;
        if (allSongsNames!=null&&currentSongNumber>0){
            currentSongNumber++;
            if (currentSongNumber<totalSongs){
                nextSong = songsList.get(allSongsNames.get(currentSongNumber));
                prevSong = songsList.get(allSongsNames.get(currentSongNumber-1));
            }
        }
        startDifferentSong(nextSong);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        return new CursorLoader(this,
                uri,
                PROJECTION,
                selection,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if(cursor.moveToFirst()) {
            allSongsNames = new ArrayList<>();
            do {
                String songName = cursor.getString(COL_DISPLAY_NAME);
                long songId = cursor.getLong(COL_ID);
                Uri songUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);

                songsList.put(songName,songUri);
                allSongsNames.add(songName);
            } while (cursor.moveToNext());
            totalSongs = songsList.size();
            updateAdapter();
        }
    }

    private void updateAdapter(){
        ArrayAdapter<String> adapter= new ArrayAdapter<String>(
                this,
                R.layout.file_name,
                R.id.file_name_text_view,
                allSongsNames
        );
        listView.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
