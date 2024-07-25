package com.example.fundimtaa;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewClientProfileActivity extends AppCompatActivity {
  //  private ImageView imageViewProfilePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_client_profile);
        ImageView imageViewBackArrow = findViewById(R.id.imageViewBackArrow);
        imageViewBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to the previous activity
                onBackPressed();
            }
        });

        // Retrieve client details passed from WorkerApplicationJobHistory activity
        String clientId = getIntent().getStringExtra("clientId");
        String clientName = getIntent().getStringExtra("clientName");
        String clientEmail = getIntent().getStringExtra("clientEmail");
        String clientPhoneNumber = getIntent().getStringExtra("clientPhoneNumber");
        String clientLocation = getIntent().getStringExtra("clientLocation");

        // Initialize TextViews to display client details
        TextView textViewClientName = findViewById(R.id.textViewClientName);
        TextView textViewClientEmail = findViewById(R.id.textViewClientEmail);
        TextView textViewClientPhoneNumber = findViewById(R.id.textViewClientPhoneNumber);
        TextView textViewClientLocation = findViewById(R.id.textViewClientLocation);
        // Initialize ImageView for profile picture
       // imageViewProfilePicture = findViewById(R.id.imageViewProfilePicture);


        // Set client details to TextViews
        textViewClientName.setText("Client Name: " + clientName);
        textViewClientEmail.setText("Client Email: " + clientEmail);
        textViewClientPhoneNumber.setText("Client Phone Number: " + clientPhoneNumber);
        textViewClientLocation.setText("Client Location: " + clientLocation);
        // Load animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);


        textViewClientName.startAnimation(fadeIn);
        textViewClientEmail.startAnimation(fadeIn);
        textViewClientPhoneNumber.startAnimation(fadeIn);
        textViewClientLocation.startAnimation(fadeIn);


        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        textViewClientName.startAnimation(slideIn);
        textViewClientEmail.startAnimation(slideIn);
        textViewClientPhoneNumber.startAnimation(slideIn);
        textViewClientLocation.startAnimation(slideIn);
        // Fetch profile picture from Firestore
       //fetchProfilePicture(clientId);
    }

    /*private void fetchProfilePicture(String clientId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(clientId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
                        // Load profile picture using Glide or any other image loading library
                        Glide.with(this)
                                .load(profilePictureUrl)
                                .placeholder(R.drawable.ic_profile) // Optional placeholder image
                                .into(imageViewProfilePicture);
                    } else {
                        Toast.makeText(ViewClientProfileActivity.this, "Profile picture not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewClientProfileActivity.this, "Failed to retrieve profile picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }*/
    }
