package com.example.fitlink.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fitlink.R;

public class AdminActivity extends BaseActivity {

    LinearLayout cardUsers, cardFoods, cardCarts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        initViews();
        setupToolbar(); // הפונקציה החדשה שמוסיפה את כפתור החזור
        setupClickListeners();
    }

    private void initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        cardUsers = findViewById(R.id.card_users);
        cardFoods = findViewById(R.id.card_foods);
        cardCarts = findViewById(R.id.card_carts);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            // מאפשר את הצגת כפתור החזור (החץ)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // הגדרת הפעולה בלחיצה על החץ - סגירת המסך הנוכחי וחזרה אחורה
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupClickListeners() {
        cardUsers.setOnClickListener(v -> {
            Intent intent = new Intent(this, UsersListActivity.class);
            startActivity(intent);
        });

        /*
        cardFoods.setOnClickListener(v -> {
            Intent intent = new Intent(this, FoodItemsActivity.class);
            startActivity(intent);
        });

        cardCarts.setOnClickListener(v -> {
            Intent intent = new Intent(this, AllCartsActivity.class);
            startActivity(intent);
        });
        */
    }
}