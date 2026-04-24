package com.example.fitlink.screens.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.example.fitlink.R;
import com.google.android.material.button.MaterialButton;

public class UserFilterDialog extends Dialog {

    // אופציות ל-Spinner
    private static final String[] ROLE_OPTIONS = {"All Roles", "Admin", "Regular"};
    private Spinner spinnerRole;
    private EditText etEmail, etPhone;
    private MaterialButton btnApply, btnClear;
    private OnUserFilterAppliedListener listener;
    private String selectedRole = "All Roles";
    private String selectedEmail = "";
    private String selectedPhone = "";

    public UserFilterDialog(@NonNull Context context) {
        super(context);
    }

    public void setListener(OnUserFilterAppliedListener listener) {
        this.listener = listener;
    }

    public void setInitialCriteria(String role, String email, String phone) {
        this.selectedRole = (role != null) ? role : "All Roles";
        this.selectedEmail = (email != null) ? email : "";
        this.selectedPhone = (phone != null) ? phone : "";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_filter_users);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        initViews();
        setupSpinner();
        restoreSelections();
        setupClickListeners();
    }

    private void initViews() {
        spinnerRole = findViewById(R.id.spinner_filter_role);
        etEmail = findViewById(R.id.edit_filter_email);
        etPhone = findViewById(R.id.edit_filter_phone);
        btnApply = findViewById(R.id.btn_apply_filters);
        btnClear = findViewById(R.id.btn_reset_filters);
    }

    private void setupSpinner() {
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, ROLE_OPTIONS);
        spinnerRole.setAdapter(roleAdapter);
    }

    private void restoreSelections() {
        if (selectedRole != null) {
            for (int i = 0; i < ROLE_OPTIONS.length; i++) {
                if (ROLE_OPTIONS[i].equals(selectedRole)) {
                    spinnerRole.setSelection(i);
                    break;
                }
            }
        }
        etEmail.setText(selectedEmail);
        etPhone.setText(selectedPhone);
    }

    private void setupClickListeners() {
        btnClear.setOnClickListener(v -> clearFilters());
        btnApply.setOnClickListener(v -> applyFilters());
    }

    private void clearFilters() {
        spinnerRole.setSelection(0);
        etEmail.setText("");
        etPhone.setText("");
    }

    private void applyFilters() {
        int rolePos = spinnerRole.getSelectedItemPosition();
        String finalRole = (rolePos > 0) ? ROLE_OPTIONS[rolePos] : null;
        String finalEmail = etEmail.getText().toString().trim();
        String finalPhone = etPhone.getText().toString().trim();

        if (listener != null) {
            listener.onFilterApplied(finalRole, finalEmail, finalPhone);
        }
        dismiss();
    }

    public interface OnUserFilterAppliedListener {
        void onFilterApplied(String role, String email, String phone);
    }
}