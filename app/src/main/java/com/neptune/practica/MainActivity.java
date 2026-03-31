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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Главная активность музыкального плеера Neptune.
 * Отвечает за интерфейс, поиск музыки и управление воспроизведением.
 */
public class MainActivity extends AppCompatActivity {

    // Код для запроса разрешений на чтение памяти
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    // Элементы интерфейса
    private ImageButton btnPlayPause;
    private SeekBar playerSeekBar;
    private TextView trackTitle, artistName;
    
    // Объекты для работы с музыкой
    private MediaPlayer mediaPlayer;
    private List<Song> songList = new ArrayList<>(); // Список найденных песен
    private int currentSongIndex = 0; // Индекс текущей песни в списке
    private Handler handler = new Handler(); // Обработчик для обновления SeekBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Включение отображения контента "от края до края" (EdgeToEdge)
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Настройка отступов для системных панелей (статус-бар, навигация)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews(); // Инициализация UI компонентов
        checkPermissions(); // Проверка разрешений при запуске
    }

    /**
     * Инициализация кнопок, текста и обработчиков событий.
     */
    private void initViews() {
        btnPlayPause = findViewById(R.id.btnPlayPause);
        ImageButton btnNext = findViewById(R.id.btnNext);
        ImageButton btnPrevious = findViewById(R.id.btnPrevious);
        ImageButton btnOpenPlaylist = findViewById(R.id.btnOpenPlaylist);
        playerSeekBar = findViewById(R.id.playerSeekBar);
        trackTitle = findViewById(R.id.trackTitle);
        artistName = findViewById(R.id.artistName);

        mediaPlayer = new MediaPlayer();

        // Слушатели нажатий для кнопок управления
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> playNext());
        btnPrevious.setOnClickListener(v -> playPrevious());
        btnOpenPlaylist.setOnClickListener(v -> showPlaylistDialog());

        // Слушатель изменения SeekBar (перемотка трека)
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

    /**
     * Отображение списка песен в нижнем всплывающем окне (BottomSheet).
     */
    private void showPlaylistDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(android.R.layout.list_content, null);
        ListView listView = view.findViewById(android.R.id.list);
        
        // Подготовка названий песен для адаптера
        List<String> songTitles = new ArrayList<>();
        for (Song song : songList) {
            songTitles.add(song.getTitle() + " - " + song.getArtist());
        }

        // Настройка списка в BottomSheet
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, songTitles);
        listView.setAdapter(adapter);

        // Обработка выбора песни из списка
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            currentSongIndex = position;
            prepareSong(currentSongIndex);
            mediaPlayer.start();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    /**
     * Проверка и запрос разрешений на чтение файлов в зависимости от версии Android.
     */
    private void checkPermissions() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU 
                ? Manifest.permission.READ_MEDIA_AUDIO 
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        } else {
            loadSongs(); // Если разрешение уже есть, загружаем музыку
        }
    }

    /**
     * Сканирование памяти устройства для поиска аудиофайлов.
     */
    private void loadSongs() {
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);

        if (songCursor != null && songCursor.moveToFirst()) {
            int titleColumn = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int dataColumn = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);

            songList.clear();
            do {
                String title = songCursor.getString(titleColumn);
                String artist = songCursor.getString(artistColumn);
                String path = songCursor.getString(dataColumn);
                songList.add(new Song(title, artist, path));
            } while (songCursor.moveToNext());
            songCursor.close();
        }

        // Если песни найдены, подготавливаем первую к воспроизведению
        if (!songList.isEmpty()) {
            prepareSong(currentSongIndex);
        } else {
            Toast.makeText(this, "Музыка не найдена!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Подготовка MediaPlayer к проигрыванию конкретной песни.
     */
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

    /**
     * Переключение режимов Воспроизведение / Пауза.
     */
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

    /**
     * Переход к следующей песне.
     */
    private void playNext() {
        if (songList.isEmpty()) return;
        currentSongIndex = (currentSongIndex + 1) % songList.size();
        prepareSong(currentSongIndex);
        mediaPlayer.start();
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
    }

    /**
     * Переход к предыдущей песне.
     */
    private void playPrevious() {
        if (songList.isEmpty()) return;
        currentSongIndex = (currentSongIndex - 1 + songList.size()) % songList.size();
        prepareSong(currentSongIndex);
        mediaPlayer.start();
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
    }

    /**
     * Обновление позиции SeekBar каждую секунду во время игры.
     */
    private void updateSeekBar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            playerSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    /**
     * Обработка результата запроса разрешений.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSongs();
        } else {
            Toast.makeText(this, "Доступ к памяти запрещен!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Освобождение ресурсов MediaPlayer при уничтожении активности.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * Вспомогательный класс для хранения данных о песне.
     */
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
