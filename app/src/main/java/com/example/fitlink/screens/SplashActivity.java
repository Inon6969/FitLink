package com.example.fitlink.screens;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fitlink.R;
import com.example.fitlink.models.User;
import com.example.fitlink.screens.dialogs.NoInternetDialog;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;

public class SplashActivity extends AppCompatActivity {
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splashPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        startSplashProcess();
    }

    private void startSplashProcess() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            } finally {
                // אנחנו חייבים לעבור ל-UI Thread כדי להציג הודעות ולבצע מעברי מסך בצורה בטוחה
                runOnUiThread(this::checkLoginAndNetwork);
            }
        }).start();
    }

    private void checkLoginAndNetwork() {
        // 1. קודם כל בודקים אם יש בכלל אינטרנט
        if (!isNetworkAvailable()) {
            showNoInternetDialog();
            return; // עוצרים כאן ולא ממשיכים לבקשת הנתונים
        }

        // 2. אם יש אינטרנט, ממשיכים בלוגיקה הרגילה
        if (SharedPreferencesUtil.isUserLoggedIn(this)) {
            User current = SharedPreferencesUtil.getUser(this);
            if (current != null) {
                DatabaseService.getInstance().getUser(current.getId(), new DatabaseService.DatabaseCallback<User>() {
                    @Override
                    public void onCompleted(User user) {
                        if (user != null) {
                            SharedPreferencesUtil.saveUser(SplashActivity.this, user);
                            intent = new Intent(SplashActivity.this, MainActivity.class);
                        } else {
                            SharedPreferencesUtil.signOutUser(SplashActivity.this);
                            intent = new Intent(SplashActivity.this, LandingActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        SharedPreferencesUtil.signOutUser(SplashActivity.this);
                        intent = new Intent(SplashActivity.this, LandingActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });
            } else {
                navigateToLanding();
            }
        } else {
            navigateToLanding();
        }
    }

    private void navigateToLanding() {
        intent = new Intent(SplashActivity.this, LandingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // פונקציית עזר לבדיקת חיבור רשת פעיל
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        }
        return false;
    }

    // הצגת הדיאלוג המעוצב במקרה שאין אינטרנט
    private void showNoInternetDialog() {
        new NoInternetDialog(this,
                // מה קורה כשלוחצים על Retry
                () -> {
                    checkLoginAndNetwork(); // מנסים שוב את תהליך הבדיקה
                },
                // מה קורה כשלוחצים על Exit
                () -> {
                    finishAffinity(); // סוגר את האפליקציה לגמרי
                }
        ).show();
    }
}