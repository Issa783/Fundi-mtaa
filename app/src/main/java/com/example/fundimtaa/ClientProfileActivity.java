package com.example.fundimtaa;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.squareup.picasso.Picasso; // Add Picasso dependency in your build.gradle

public class ClientProfileActivity extends AppCompatActivity {
    private static final String TAG = ClientProfileActivity.class.getSimpleName();
    private String clientId;

    private Button btnUpdateProfile;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_profile);
        ImageView imageViewBackArrow = findViewById(R.id.imageViewBackArrow);
        imageViewBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to the previous activity
                onBackPressed();
            }
        });


        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        btnUpdateProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ClientProfileActivity.this, UpdateClientProfileActivity.class);
            startActivity(intent);
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        clientId = currentUser != null ? currentUser.getUid() : null;

        if (clientId != null) {
            fetchClientDetails();
        }
    }

    private void fetchClientDetails() {
        if (clientId == null) {
            Log.e(TAG, "Client ID is null.");
            return;
        }

        db.collection("users")
                .document(clientId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@NonNull DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(ClientProfileActivity.this, "Failed to fetch client details.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error fetching client details: ", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String phoneNumber = documentSnapshot.getString("phoneNumber");
                            String location = documentSnapshot.getString("location");
                            String about = documentSnapshot.getString("about");
                            String profilePicture = documentSnapshot.getString("profilePicture");

                            TextView textViewName = findViewById(R.id.textViewName);
                            textViewName.setText("Name: " + name);

                            TextView textViewEmail = findViewById(R.id.textViewEmail);
                            textViewEmail.setText("Email: " + email);

                            TextView textViewPhoneNumber = findViewById(R.id.textViewPhoneNumber);
                            textViewPhoneNumber.setText("Phone Number: " + phoneNumber);

                            TextView textViewLocation = findViewById(R.id.textViewLocation);
                            textViewLocation.setText("Location: " + location);

                            TextView textViewAbout = findViewById(R.id.textViewAbout);
                            textViewAbout.setText("About: " + about);

                            ImageView imageViewProfilePicture = findViewById(R.id.imageViewProfilePicture);
                            if (profilePicture != null && !profilePicture.isEmpty()) {
                                Picasso.get().load(profilePicture).into(imageViewProfilePicture);
                            }
                        }
                    }
                });
    }
}
