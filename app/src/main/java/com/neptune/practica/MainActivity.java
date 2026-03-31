package com.neptune.practica;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private boolean isPlaying = false;

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

        // Инициализация кнопок
        ImageButton btnPlayPause = findViewById(R.id.btnPlayPause);
        ImageButton btnNext = findViewById(R.id.btnNext);
        ImageButton btnPrevious = findViewById(R.id.btnPrevious);

        // Логика кнопки Play/Pause
        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                Toast.makeText(this, "Pause", Toast.LENGTH_SHORT).show();
            } else {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                Toast.makeText(this, "Playing...", Toast.LENGTH_SHORT).show();
            }
            isPlaying = !isPlaying;
        });

        // Логика кнопок Вперед/Назад
        btnNext.setOnClickListener(v -> Toast.makeText(this, "Next track", Toast.LENGTH_SHORT).show());
        btnPrevious.setOnClickListener(v -> Toast.makeText(this, "Previous track", Toast.LENGTH_SHORT).show());
    }
}
