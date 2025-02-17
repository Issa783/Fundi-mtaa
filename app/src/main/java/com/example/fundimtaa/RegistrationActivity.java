package com.example.fundimtaa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegistrationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText editTextName = findViewById(R.id.editTextName);
        EditText editTextEmail = findViewById(R.id.editTextEmail);
        EditText editTextPassword = findViewById(R.id.editTextPassword);
        EditText editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        EditText editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);

        RadioGroup radioGroupRole = findViewById(R.id.radioGroupUserType);

        Button buttonRegister = findViewById(R.id.buttonRegister);
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editTextName.getText().toString().trim();
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();
                String confirmPassword = editTextConfirmPassword.getText().toString().trim();
                String phoneNumber = editTextPhoneNumber.getText().toString().trim();

                if (!validateFields(name, email, password, confirmPassword, phoneNumber, radioGroupRole)) {
                    return;
                }

                int selectedRoleId = radioGroupRole.getCheckedRadioButtonId();
                String role;
                Class<?> nextActivity;
                if (selectedRoleId == R.id.radioButtonClient) {
                    role = "client";
                    nextActivity = ClientActivity.class;
                } else if (selectedRoleId == R.id.radioButtonWorker) {
                    role = "worker";
                    nextActivity = WorkerActivity.class;
                } else {
                    Toast.makeText(RegistrationActivity.this, "Please select a role", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(RegistrationActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    String userId = mAuth.getCurrentUser().getUid();
                                    // Create a new User object based on the role selected
                                    User user;
                                    if (role.equals("client")) {
                                        // If the role is client, set clientId to userId and workerId to null
                                        user = new User(userId, name, email, phoneNumber, role, userId, null);
                                    } else {
                                        // If the role is worker, set workerId to userId and clientId to null
                                        user = new User(userId, name, email, phoneNumber, role, null, userId);
                                    }

                                    db.collection("users").document(userId)
                                            .set(user)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(RegistrationActivity.this, "Data stored successfully", Toast.LENGTH_SHORT).show();
                                                        Intent intent = new Intent(RegistrationActivity.this, nextActivity);
                                                        intent.putExtra("userId", userId);
                                                        startActivity(intent);
                                                        finish();
                                                    } else {
                                                        Toast.makeText(RegistrationActivity.this, "Failed to store user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                } else {
                                    Toast.makeText(RegistrationActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    private boolean validateFields(String name, String email, String password, String confirmPassword, String phoneNumber, RadioGroup radioGroupRole) {
        if (name.isEmpty() || name.length() < 3) {
            Toast.makeText(this, "Name must be at least 3 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (phoneNumber.isEmpty() || phoneNumber.length() != 10 || !Patterns.PHONE.matcher(phoneNumber).matches()) {
            Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (radioGroupRole.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

}
