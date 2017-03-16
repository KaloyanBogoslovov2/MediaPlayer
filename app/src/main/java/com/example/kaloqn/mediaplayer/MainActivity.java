package com.example.kaloqn.mediaplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static com.example.kaloqn.mediaplayer.Constants.COL_DISPLAY_NAME;
import static com.example.kaloqn.mediaplayer.Constants.COL_ID;
import static com.example.kaloqn.mediaplayer.Constants.FORMAT_MUSIC;
import static com.example.kaloqn.mediaplayer.Constants.FORMAT_VIDEO;
import static com.example.kaloqn.mediaplayer.Constants.MEDIA_LOADER;
import static com.example.kaloqn.mediaplayer.Constants.MUSIC_SELECTION;
import static com.example.kaloqn.mediaplayer.Constants.MUSIC_URI;
import static com.example.kaloqn.mediaplayer.Constants.MY_PERMISSIONS_REQUEST_MP3_FILES;
import static com.example.kaloqn.mediaplayer.Constants.PROJECTION;
import static com.example.kaloqn.mediaplayer.Constants.VIDEO_SELECTION;
import static com.example.kaloqn.mediaplayer.Constants.VIDEO_URI;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    public static String currentFormatType =FORMAT_MUSIC;
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
    private LinearLayout buttonPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonPanel = (LinearLayout) findViewById(R.id.button_panel);
        playButton = (ImageButton) findViewById(R.id.play_stop_song);

        initListView();
        checkForPermissionsAndLoadSongs();
        }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.change_data_format,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id= item.getItemId();

        if(id==R.id.change_data_format){

            Bundle formatBundle = new Bundle();
            if(currentFormatType.equals(FORMAT_MUSIC)){
                currentFormatType = FORMAT_VIDEO;
                formatBundle.putString("uri", VIDEO_URI.toString());
                formatBundle.putString("selection",VIDEO_SELECTION);

                if (mediaPlayer!=null && mediaPlayer.isPlaying()) {
                    playButton.setImageResource(android.R.drawable.ic_media_play);
                    mediaPlayer.pause();
                    playButtonPressed = false;
                }
                buttonPanel.setVisibility(LinearLayout.GONE);

                Toast.makeText(this,"Format type changed to video",Toast.LENGTH_SHORT).show();
                getSupportLoaderManager().restartLoader(MEDIA_LOADER, formatBundle, this);
            }else if (currentFormatType.equals(FORMAT_VIDEO)){
                buttonPanel.setVisibility(LinearLayout.VISIBLE);
                currentFormatType = FORMAT_MUSIC;
                formatBundle.putString("uri", MUSIC_URI.toString());
                formatBundle.putString("selection", MUSIC_SELECTION);

                //buttonPanel.setVisibility(LinearLayout.);

                Toast.makeText(this,"Format type changed to music",Toast.LENGTH_SHORT).show();
                getSupportLoaderManager().restartLoader(MEDIA_LOADER, formatBundle, this);
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
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
                    if (songName.equals("Loading..."))return;
                        final Uri songUri = songsList.get(songName);
                    if (currentFormatType.equals(FORMAT_VIDEO)){

                        Intent intent = new Intent(MainActivity.this,VideoActivity.class);
                        intent.putExtra("uri",songUri.toString());
                        startActivity(intent);

                    }else{
                        if (mediaPlayer==null) {
                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.setDataSource(getApplicationContext(), songUri);
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                            playButton.setImageResource(android.R.drawable.ic_media_pause);
                            playButtonPressed = true;
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){

                                @Override
                                public void onCompletion(MediaPlayer mp) {
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
                            });
                        }else {
                            startDifferentSong(songUri);
                        }
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

            if (currentFormatType.equals(FORMAT_MUSIC)) {
                buttonPanel.setVisibility(LinearLayout.VISIBLE);
                Bundle formatBundle = new Bundle();
                formatBundle.putString("uri", MUSIC_URI.toString());
                formatBundle.putString("selection", MUSIC_SELECTION);
                getSupportLoaderManager().initLoader(MEDIA_LOADER, formatBundle, this);
            }else if (currentFormatType.equals(FORMAT_VIDEO)){
                buttonPanel.setVisibility(LinearLayout.GONE);
                Bundle formatBundle = new Bundle();
                formatBundle.putString("uri", VIDEO_URI.toString());
                formatBundle.putString("selection", VIDEO_SELECTION);
                getSupportLoaderManager().initLoader(MEDIA_LOADER, formatBundle, this);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_MP3_FILES: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (currentFormatType.equals(FORMAT_MUSIC)) {
                        buttonPanel.setVisibility(LinearLayout.VISIBLE);
                        Bundle formatBundle = new Bundle();
                        formatBundle.putString("uri", MUSIC_URI.toString());
                        formatBundle.putString("selection", MUSIC_SELECTION);
                        getSupportLoaderManager().initLoader(MEDIA_LOADER, formatBundle, this);
                    }else if (currentFormatType.equals(FORMAT_VIDEO)){
                        buttonPanel.setVisibility(LinearLayout.GONE);
                        Bundle formatBundle = new Bundle();
                        formatBundle.putString("uri", VIDEO_URI.toString());
                        formatBundle.putString("selection", VIDEO_SELECTION);
                        getSupportLoaderManager().initLoader(MEDIA_LOADER, formatBundle, this);
                    }
                } else {
                    Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    public void changeToPreviousSong(View v){
        if(prevSong==null||currentFormatType.equals(FORMAT_VIDEO))return;

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
        if (mediaPlayer==null||currentFormatType.equals(FORMAT_VIDEO))return;
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
        if (nextSong==null||currentFormatType.equals(FORMAT_VIDEO))return;
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
        Uri uri = Uri.parse(args.getString("uri"));
        String selection = args.getString("selection");

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
                Uri songUri=null;
                if (currentFormatType.equals(FORMAT_MUSIC)) {
                    songUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
                }else if (currentFormatType.equals(FORMAT_VIDEO)){
                    songUri = ContentUris.withAppendedId(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, songId);
                }
                songsList.put(songName,songUri);
                allSongsNames.add(songName);
            } while (cursor.moveToNext());
            totalSongs = songsList.size();
            updateAdapter();
        }
    }

    private void updateAdapter(){

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
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
