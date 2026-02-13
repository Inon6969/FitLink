package com.example.fitlink.screens;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitlink.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity {

    private MapView map = null;
    private GeoPoint selectedPoint;
    private Marker selectedMarker;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // טעינת הגדרות OSM (חשוב להגדיר User Agent לפני ה-setContentView)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map_picker);

        btnConfirm = findViewById(R.id.btnConfirmLocation);
        map = findViewById(R.id.map); // וודא שב-XML סוג ה-View הוא org.osmdroid.views.MapView

        // הגדרות בסיסיות למפה
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(32.0853, 34.7818); // תל אביב
        map.getController().setCenter(startPoint);

        // הוספת שכבת האזנה ללחיצות על המפה
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                updateMarker(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });

        map.getOverlays().add(0, mapEventsOverlay);

        btnConfirm.setOnClickListener(v -> {
            if (selectedPoint != null) {
                returnResult();
            } else {
                Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMarker(GeoPoint p) {
        selectedPoint = p;
        if (selectedMarker == null) {
            selectedMarker = new Marker(map);
            selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(selectedMarker);
        }
        selectedMarker.setPosition(p);
        selectedMarker.setTitle("Selected Location");
        map.invalidate(); // ריענון המפה
    }

    private void returnResult() {
        String addressStr = "Unknown Location";
        // שימוש ב-Geocoder המובנה של המערכת (חינמי)
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(selectedPoint.getLatitude(), selectedPoint.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                addressStr = addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("address", addressStr);
        resultIntent.putExtra("lat", selectedPoint.getLatitude());
        resultIntent.putExtra("lng", selectedPoint.getLongitude());

        setResult(RESULT_OK, resultIntent);
        finish();
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