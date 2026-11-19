package com.hanse.SoftBrain;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

/**
 * [화면 1] 시작 화면 (SplashActivity)
 * - 앱을 처음 실행했을 때 로고와 "지출 데이터를 불러오는 중..." 로딩 스피너가 표시됩니다.
 * - 저장된 다크모드 설정을 먼저 적용한 뒤, 1.5초 후 자동으로 홈 화면(MainActivity)으로 전환됩니다.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500L; // 1.5초 로딩 후 전환

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 앱 시작 시 저장된 다크모드 설정 적용
        SharedPreferences prefs = getSharedPreferences("SoftBrainPrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_splash);

        // 1.5초 뒤에 홈 화면(MainActivity)으로 이동하고 시작 화면은 종료(finish)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
