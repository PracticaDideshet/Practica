package com.neptune.practica;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    
    private ImageButton btnPlayPause;
    private SeekBar playerSeekBar;
    private TextView trackTitle, artistName;
    
    private MediaPlayer mediaPlayer;
    private List<Song> songList = new ArrayList<>();
    private int currentSongIndex = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        checkPermissions();
    }

    private void initViews() {
        btnPlayPause = findViewById(R.id.btnPlayPause);
        ImageButton btnNext = findViewById(R.id.btnNext);
        ImageButton btnPrevious = findViewById(R.id.btnPrevious);
        playerSeekBar = findViewById(R.id.playerSeekBar);
        trackTitle = findViewById(R.id.trackTitle);
        artistName = findViewById(R.id.artistName);

        mediaPlayer = new MediaPlayer();

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> playNext());
        btnPrevious.setOnClickListener(v -> playPrevious());

        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void checkPermissions() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU 
                ? Manifest.permission.READ_MEDIA_AUDIO 
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        } else {
            loadSongs();
        }
    }

    private void loadSongs() {
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);

        if (songCursor != null && songCursor.moveToFirst()) {
            int titleColumn = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int dataColumn = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            do {
                String title = songCursor.getString(titleColumn);
                String artist = songCursor.getString(artistColumn);
                String path = songCursor.getString(dataColumn);
                songList.add(new Song(title, artist, path));
            } while (songCursor.moveToNext());
            songCursor.close();
        }

        if (!songList.isEmpty()) {
            prepareSong(currentSongIndex);
        } else {
            Toast.makeText(this, "No songs found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void prepareSong(int index) {
        try {
            Song song = songList.get(index);
            trackTitle.setText(song.getTitle());
            artistName.setText(song.getArtist());
            
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepare();
            playerSeekBar.setMax(mediaPlayer.getDuration());
            updateSeekBar();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mediaPlayer.start();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            updateSeekBar();
        }
    }

    private void playNext() {
        if (songList.isEmpty()) return;
        currentSongIndex = (currentSongIndex + 1) % songList.size();
        prepareSong(currentSongIndex);
        mediaPlayer.start();
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
    }

    private void playPrevious() {
        if (songList.isEmpty()) return;
        currentSongIndex = (currentSongIndex - 1 + songList.size()) % songList.size();
        prepareSong(currentSongIndex);
        mediaPlayer.start();
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            playerSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs();
        } else {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    // Вспомогательный класс для песни
    private static class Song {
        private String title;
        private String artist;
        private String path;

        public Song(String title, String artist, String path) {
            this.title = title;
            this.artist = artist;
            this.path = path;
        }

        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getPath() { return path; }
    }
}
