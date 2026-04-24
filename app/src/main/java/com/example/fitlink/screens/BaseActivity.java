package com.example.fitlink.screens;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.fitlink.models.User;
import com.example.fitlink.screens.dialogs.LogoutDialog;
import com.example.fitlink.screens.dialogs.NoInternetDialog;
import com.example.fitlink.services.AuthService;
import com.example.fitlink.services.DatabaseService;
import com.example.fitlink.utils.SharedPreferencesUtil;
import com.google.firebase.database.ValueEventListener;

public class BaseActivity extends AppCompatActivity {

    // משתנה סטטי ששומר האם המשתמש כבר אישר את מצב האופליין (תקף לכל המסכים)
    private static boolean hasAcknowledgedOffline = false;
    protected DatabaseService databaseService;
    private ConnectivityManager.NetworkCallback networkCallback;
    private NoInternetDialog customNoInternetDialog;
    // מאזינים לשינויים בזמן אמת במשתמש (למקרה של מחיקה)
    private ValueEventListener currentUserListener;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // נועל את האפליקציה על theme בהיר
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        databaseService = DatabaseService.getInstance();
        currentUserId = SharedPreferencesUtil.getUserId(this);

        // אם יש לנו משתמש מחובר, נתחיל להאזין לו בזמן אמת
        if (currentUserId != null && !currentUserId.isEmpty()) {
            startListeningToUserStatus();
        }
    }

    private void startListeningToUserStatus() {
        currentUserListener = databaseService.listenToUser(currentUserId, new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(User user) {
                // המשתמש נמחק מ-Firebase! (על ידי מנהל או על ידי עצמו)
                if (user == null) {
                    Toast.makeText(BaseActivity.this, "Your account has been deleted by an admin.", Toast.LENGTH_LONG).show();

                    // מחיקת נתוני ההתחברות המקומיים
                    SharedPreferencesUtil.signOutUser(BaseActivity.this);

                    // העברה מיידית למסך ההתחברות וניקוי כל היסטוריית המסכים (Back Stack)
                    Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailed(Exception e) {
                // מתעלמים משגיאות כאן (למשל ניתוק מהרשת) כדי לא להפריע למשתמש
            }
        });
    }

    // הוספנו onResume כדי להאזין לרשת רק כשהמסך באמת מוצג למשתמש
    @Override
    protected void onResume() {
        super.onResume();
        registerNetworkCallback();

        // בדיקה יזומה ברגע שהמסך עולה - אם אנחנו באופליין והמשתמש עוד לא אישר את זה
        if (!isNetworkAvailable() && !hasAcknowledgedOffline) {
            showNoInternetDialog();
        }
    }

    // העברנו את ביטול ההאזנה ל-onPause כדי שמסכים ברקע לא יקפיצו דיאלוגים
    @Override
    protected void onPause() {
        super.onPause();
        if (networkCallback != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                try {
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            networkCallback = null; // איפוס המאזין
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ניקוי מאזין המשתמש כשהמסך נסגר למניעת זליגות זיכרון (Memory Leaks)
        if (currentUserId != null && currentUserListener != null) {
            databaseService.removeUserListener(currentUserId, currentUserListener);
        }
    }

    protected void logout() {
        AuthService authService = new AuthService(this);

        new LogoutDialog(this, () -> {
            String email = authService.logout();
            Toast.makeText(this, "התנתקת בהצלחה", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("userEmail", email);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }).show();
    }

    protected void registerNetworkCallback() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    // האינטרנט חזר! נאפס את הזיכרון כדי שבפעם הבאה שיתנתק הדיאלוג יקפוץ שוב
                    hasAcknowledgedOffline = false;

                    runOnUiThread(() -> {
                        if (customNoInternetDialog != null && customNoInternetDialog.isShowing()) {
                            customNoInternetDialog.dismiss();
                        }
                    });
                }

                @Override
                public void onLost(@NonNull Network network) {
                    runOnUiThread(() -> showNoInternetDialog());
                }
            };
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    private void showNoInternetDialog() {
        // אם המשתמש כבר אישר שהוא ממשיך באופליין, לא נציג שוב שום דבר
        if (hasAcknowledgedOffline) {
            return;
        }

        if (customNoInternetDialog != null && customNoInternetDialog.isShowing()) {
            return;
        }

        customNoInternetDialog = new NoInternetDialog(this,
                "Offline Mode",
                "You have lost your internet connection.\nYou can continue using FitLink in offline mode. Some online features may be temporarily unavailable.",
                "Continue Offline",
                () -> {
                    // ברגע שהמשתמש לחץ על המשך, אנחנו מסמנים שהוא יודע שהוא באופליין
                    hasAcknowledgedOffline = true;
                },
                null,
                null
        );

        customNoInternetDialog.show();
    }

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
}