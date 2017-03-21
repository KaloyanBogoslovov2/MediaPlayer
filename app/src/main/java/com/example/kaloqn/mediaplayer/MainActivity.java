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
import android.os.Handler;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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
    private static HashMap<String, Uri> songsMap = new HashMap<String, Uri>();
    private static MediaPlayer mediaPlayer=null;

    private int totalSongs =0;
    private int currentSongNumber=0;

    private ImageButton playButton;
    private ListView listView;

    private Uri prevSong = null;
    private Uri nextSong = null;
    private ArrayList<String> allSongsNames= null;
    private LinearLayout buttonPanel;
    private Handler mHandler = new Handler();
    private SeekBar seekBar;

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

            if(currentFormatType.equals(FORMAT_MUSIC)){
                Bundle formatBundle = getBundle(VIDEO_URI.toString(),VIDEO_SELECTION);
                getSupportLoaderManager().restartLoader(MEDIA_LOADER, formatBundle, this);

                if (mediaPlayer!=null && mediaPlayer.isPlaying()) {
                    setPlayButtonPaused();
                    mediaPlayer.pause();
                }
                currentFormatType = FORMAT_VIDEO;
                buttonPanel.setVisibility(LinearLayout.GONE);
                Toast.makeText(this,"Format type changed to video",Toast.LENGTH_SHORT).show();

            }else if (currentFormatType.equals(FORMAT_VIDEO)){
                Bundle formatBundle = getBundle(MUSIC_URI.toString(),MUSIC_SELECTION);
                getSupportLoaderManager().restartLoader(MEDIA_LOADER, formatBundle, this);

                currentFormatType = FORMAT_MUSIC;
                buttonPanel.setVisibility(LinearLayout.VISIBLE);
                Toast.makeText(this,"Format type changed to music",Toast.LENGTH_SHORT).show();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Bundle getBundle(String uri, String selection){
        Bundle formatBundle = new Bundle();
        formatBundle.putString("uri", uri);
        formatBundle.putString("selection", selection);

        return formatBundle;
    }

    private void setPlayButtonPaused(){
        playButton.setImageResource(android.R.drawable.ic_media_play);
        playButtonPressed = false;
    }


    private void initListView(){

        ArrayAdapter<String> adapter= new ArrayAdapter<String>(
                this,
                R.layout.file_name,
                R.id.file_name_text_view,
                new String[]{}
        );

        listView = (ListView) findViewById(R.id.files_list);
        TextView emptyList = (TextView) findViewById(R.id.empty);
        listView.setEmptyView(emptyList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                String songName =(String) adapterView.getItemAtPosition(position);
                if (songName.equals("Loading..."))return;
                final Uri mediaUri = songsMap.get(songName);

                if (currentFormatType.equals(FORMAT_VIDEO)){
                    Intent intent = new Intent(MainActivity.this,VideoActivity.class);
                    intent.putExtra("uri",mediaUri.toString());
                    startActivity(intent);

                }else if(currentFormatType.equals(FORMAT_MUSIC)){
                    try {
                        playMusic(mediaUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                setPrevAndNextSong(adapterView,position);
                currentSongNumber = position;
            }
        });
    }

    private void playMusic(Uri songUri) throws IOException {
        if (mediaPlayer==null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(getApplicationContext(), songUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
           // initAndSetSeekBar();
            setPlayButtonPlaying();

        }else {
            startDifferentSong(songUri);
        }
        initAndSetSeekBar();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (nextSong==null)return;
                startNextSong();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
               // seekBar.setMax(mp.getDuration());
            }
        });
    }

    private void initAndSetSeekBar(){
        seekBar = (SeekBar) findViewById(R.id.seek_bar);

        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if(mediaPlayer != null){
                    int mCurrentPosition = mediaPlayer.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                }
                mHandler.postDelayed(this, 1000);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mediaPlayer != null && fromUser){
                    mediaPlayer.seekTo(progress * 1000);
                }
            }
        });
    }

    private void setPlayButtonPlaying(){
        playButton.setImageResource(android.R.drawable.ic_media_pause);
        playButtonPressed = true;
    }

    private void startNextSong(){
        if (allSongsNames!=null&&currentSongNumber>0){
            currentSongNumber++;
            if (currentSongNumber<totalSongs){
                nextSong = songsMap.get(allSongsNames.get(currentSongNumber));
                prevSong = songsMap.get(allSongsNames.get(currentSongNumber-1));
            }
        }
        startDifferentSong(nextSong);
    }

    private void startDifferentSong(Uri songUri){
        if (mediaPlayer.isPlaying()|| mediaPlayer!=null){
            if(mediaPlayer!=null) setPlayButtonPlaying();
            mediaPlayer.setLooping(false);
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer=MediaPlayer.create(getApplicationContext(), songUri);
            mediaPlayer.start();

        }
    }

    private void setPrevAndNextSong(AdapterView<?> adapterView,int position){
        if (position>1&&position< totalSongs)prevSong = songsMap.get(adapterView.getItemAtPosition(position-1));
        if (position>0&&position< totalSongs)nextSong = songsMap.get(adapterView.getItemAtPosition(position+1));
    }



    private void checkForPermissionsAndLoadSongs(){

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_MP3_FILES);
        }else{
            initLoader();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_MP3_FILES: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initLoader();
                } else {
                    Toast.makeText(this,"permission denied",Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void initLoader(){
        if (currentFormatType.equals(FORMAT_MUSIC)) {
            initLoaderWithAudio();
        }else if (currentFormatType.equals(FORMAT_VIDEO)){
            initLoaderWithVideo();
        }
    }

    private void initLoaderWithAudio(){
        buttonPanel.setVisibility(LinearLayout.VISIBLE);
        Bundle formatBundle = getBundle(MUSIC_URI.toString(),MUSIC_SELECTION);
        getSupportLoaderManager().initLoader(MEDIA_LOADER, formatBundle, this);
    }

    private void initLoaderWithVideo(){
        buttonPanel.setVisibility(LinearLayout.GONE);
        Bundle formatBundle = getBundle(VIDEO_URI.toString(),VIDEO_SELECTION);
        getSupportLoaderManager().initLoader(MEDIA_LOADER, formatBundle, this);
    }

    //change to previous song button
    public void changeToPreviousSong(View v){
        if(prevSong==null)return;

        if (allSongsNames!=null&&currentSongNumber>0){
            currentSongNumber--;
            if (currentSongNumber<totalSongs){
                prevSong = songsMap.get(allSongsNames.get(currentSongNumber));
                nextSong = songsMap.get(allSongsNames.get(currentSongNumber+1));
            }
        }
        startDifferentSong(prevSong);
    }

    //change to random song
    public void changeToRandomSong(View v){

        RandomSong randomSong = getRandomSong();
        Uri songUri = randomSong.getSongUri();
        int songIndex = randomSong.getSongNumber();
        try {
            playMusic(songUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (songIndex>1&&songIndex< totalSongs)prevSong = songsMap.get(allSongsNames.get(songIndex-1));
        if (songIndex>0&&songIndex< totalSongs)nextSong = songsMap.get(allSongsNames.get(songIndex+1));
        currentSongNumber = songIndex;


    }

    private RandomSong getRandomSong(){
        int randomIndex = new Random().nextInt(allSongsNames.size());
        RandomSong randomSong = new RandomSong();
        String songName = allSongsNames.get(randomIndex);
        randomSong.setSongUri(songsMap.get(songName));
        randomSong.setSongNumber(randomIndex);
        return randomSong;
    }

    //play/stop button
    public void playStopSong(View v){
        if (mediaPlayer==null)return;
        if (playButtonPressed){
            //stop song
            setPlayButtonPaused();
            mediaPlayer.pause();

        }else{
            //start song
            setPlayButtonPlaying();
            mediaPlayer.start();

        }
    }

    //change to next song button
    public void changeToNextSong(View v){
        if (nextSong==null)return;
        startNextSong();

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
                songsMap.put(songName,songUri);
                allSongsNames.add(songName);
            } while (cursor.moveToNext());
            totalSongs = songsMap.size();
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
