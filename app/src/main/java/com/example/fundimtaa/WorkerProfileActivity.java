package com.example.fundimtaa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class WorkerProfileActivity extends AppCompatActivity {
    private static final String TAG = WorkerProfileActivity.class.getSimpleName();

    private LinearLayout layoutRatingsReviews;
    private FirebaseAuth mAuth;
    private Button btnUpdateProfile;
    private ImageView profilePicture;
    private TextView textViewAverageRating;
    private List<QueryDocumentSnapshot> jobReviewsList = new ArrayList<>();
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_profile);

        mAuth = FirebaseAuth.getInstance();
        profilePicture = findViewById(R.id.profilePicture);
        layoutRatingsReviews = findViewById(R.id.layoutRatingsReviews);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        textViewAverageRating = findViewById(R.id.textViewAverageRating);

        // Initialize UI elements
       toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btnUpdateProfile.setOnClickListener(v -> {
            Intent intent = new Intent(WorkerProfileActivity.this, UpdateProfileActivity.class);
            startActivity(intent);
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String workerId = currentUser != null ? currentUser.getUid() : null;

        if (workerId != null) {
            loadWorkerDetails(workerId);
            loadInitialReviews(workerId);
        }

        applyFadeInAnimation(textViewAverageRating);
        applyFadeInAnimation(btnUpdateProfile);
    }

    // Handle back arrow click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close this activity and go back to the previous one
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyFadeInAnimation(View view) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(1000);
        view.startAnimation(fadeIn);
    }

    private void loadWorkerDetails(String workerId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(workerId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(WorkerProfileActivity.this, "Failed to fetch worker details.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching worker details: ", e);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    TextView textViewName = findViewById(R.id.textViewName);
                    TextView textViewEmail = findViewById(R.id.textViewEmail);
                    TextView textViewPhoneNumber = findViewById(R.id.textViewPhoneNumber);
                    TextView textViewLocation = findViewById(R.id.textViewLocation);
                    TextView textViewExperience = findViewById(R.id.textViewExperience);
                    TextView textViewSpecialization = findViewById(R.id.textViewSpecialization);

                    String profilePictureUrl = documentSnapshot.getString("profilePictureUrl");
                    textViewName.setText("Name: " + documentSnapshot.getString("name"));
                    textViewEmail.setText("Email: " + documentSnapshot.getString("email"));
                    textViewPhoneNumber.setText("Phone Number: " + documentSnapshot.getString("phoneNumber"));
                    textViewLocation.setText("Location: " + documentSnapshot.getString("location"));
                    textViewExperience.setText("Work Experience: " + documentSnapshot.getString("experience"));
                    textViewSpecialization.setText("Specialization: " + documentSnapshot.getString("specialization"));
                    Glide.with(WorkerProfileActivity.this).load(profilePictureUrl).into(profilePicture);
                }
            }
        });
    }

    private void loadInitialReviews(String workerId) {
        layoutRatingsReviews.removeAllViews();
        jobReviewsList.clear();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("RatingsAndReviews")
                .whereEqualTo("workerId", workerId)
              //  .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Sort by timestamp in descending order
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        float totalRating = 0;
                        int ratingCount = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            jobReviewsList.add(document);
                            addReviewToLayout(document);
                            totalRating += document.getDouble("rating").floatValue();
                            ratingCount++;
                        }

                        if (ratingCount > 0) {
                            textViewAverageRating.setText("Average Rating: " + (totalRating / ratingCount));
                        } else {
                            textViewAverageRating.setText("Average Rating: N/A");
                        }
                    } else {
                        Toast.makeText(this, "Failed to fetch job ratings and reviews.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error fetching job ratings and reviews: ", task.getException());
                    }
                });
    }

    private void addReviewToLayout(QueryDocumentSnapshot document) {
        String jobName = document.getString("jobName");
        float rating = document.getDouble("rating").floatValue();
        String review = document.getString("review");

        TextView textViewJobName = new TextView(this);
        textViewJobName.setText("Job Name: " + jobName);

        TextView textViewReview = new TextView(this);
        textViewReview.setText("Review: " + review);

        RatingBar ratingBar = new RatingBar(this, null, android.R.attr.ratingBarStyleSmall);
        ratingBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ratingBar.setRating(rating);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1.0f);
        ratingBar.setIsIndicator(true);

        layoutRatingsReviews.addView(textViewJobName);
        layoutRatingsReviews.addView(ratingBar);
        layoutRatingsReviews.addView(textViewReview);
    }
}
