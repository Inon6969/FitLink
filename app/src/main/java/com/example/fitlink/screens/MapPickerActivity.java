package com.example.fitlink.screens;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitlink.R;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapPickerActivity extends AppCompatActivity {

    private MapView map = null;

    private EditText etSearchAddress;
    private TextView tvSelectedAddress;

    private GeoPoint finalSelectedPoint;
    private String finalAddressString = "";

    // מנהל תהליכי רקע + Handler כדי לזהות מתי המשתמש סיים לגלול את המפה
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler delayHandler = new Handler(Looper.getMainLooper());
    private Runnable fetchAddressRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map_picker);

        map = findViewById(R.id.map);
        Button btnConfirm = findViewById(R.id.btnConfirmLocation);
        etSearchAddress = findViewById(R.id.etSearchAddress);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);

        map.setMultiTouchControls(true);
        map.getController().setZoom(16.0);
        GeoPoint startPoint = new GeoPoint(32.0853, 34.7818); // תל אביב כברירת מחדל
        map.getController().setCenter(startPoint);

        // מאזין שמזהה תנועה (גרירה/זום) במפה
        map.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                handleMapMovement();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                handleMapMovement();
                return true;
            }
        });

        // קריאה ראשונית למיקום התחלתי
        fetchAddressForCenterMap();

        // מאזין לחיפוש במקלדת
        etSearchAddress.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                String query = etSearchAddress.getText().toString().trim();
                if (!query.isEmpty()) {
                    hideKeyboard();
                    searchAddressFromText(query);
                }
                return true;
            }
            return false;
        });

        btnConfirm.setOnClickListener(v -> {
            if (finalSelectedPoint != null && !finalAddressString.isEmpty() && !finalAddressString.equals("Moving map...")) {
                returnResult();
            } else {
                Toast.makeText(this, "Please wait for location to load", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // פונקציה שנקראת בכל פעם שהמפה זזה ומתאפסת כל עוד היא בתנועה
    private void handleMapMovement() {
        if (fetchAddressRunnable != null) {
            delayHandler.removeCallbacks(fetchAddressRunnable); // מבטל את הבקשה הקודמת
        }

        tvSelectedAddress.setText("Moving map...");

        // יוצר בקשה חדשה שתרוץ חצי שנייה אחרי שהמפה תפסיק לזוז
        fetchAddressRunnable = this::fetchAddressForCenterMap;
        delayHandler.postDelayed(fetchAddressRunnable, 500);
    }

    // לוקח את הנקודה המדויקת שנמצאת באמצע המסך (מתחת לסיכה) ומביא את הכתובת שלה
    private void fetchAddressForCenterMap() {
        IGeoPoint center = map.getMapCenter();
        finalSelectedPoint = new GeoPoint(center.getLatitude(), center.getLongitude());

        tvSelectedAddress.setText("Loading address...");

        executorService.execute(() -> {
            Geocoder geocoder = new Geocoder(MapPickerActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(finalSelectedPoint.getLatitude(), finalSelectedPoint.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    finalAddressString = addresses.get(0).getAddressLine(0);
                } else {
                    finalAddressString = "Unknown Location";
                }
            } catch (IOException e) {
                e.printStackTrace();
                finalAddressString = "Location Selected (" + finalSelectedPoint.getLatitude() + ", " + finalSelectedPoint.getLongitude() + ")";
            }

            runOnUiThread(() -> tvSelectedAddress.setText(finalAddressString));
        });
    }

    private void searchAddressFromText(String locationName) {
        tvSelectedAddress.setText("Searching...");

        executorService.execute(() -> {
            Geocoder geocoder = new Geocoder(MapPickerActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    GeoPoint newPoint = new GeoPoint(address.getLatitude(), address.getLongitude());

                    // מזיז את המפה לנקודה החדשה (הכתובת כבר תתעדכן אוטומטית בזכות ה-MapListener!)
                    runOnUiThread(() -> map.getController().animateTo(newPoint));
                } else {
                    runOnUiThread(() -> {
                        tvSelectedAddress.setText("Address not found. Try again.");
                        Toast.makeText(MapPickerActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> tvSelectedAddress.setText("Network error. Try again."));
            }
        });
    }

    private void returnResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("address", finalAddressString);
        resultIntent.putExtra("lat", finalSelectedPoint.getLatitude());
        resultIntent.putExtra("lng", finalSelectedPoint.getLongitude());

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}