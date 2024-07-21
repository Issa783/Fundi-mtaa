package com.example.fundimtaa;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class PostJobActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_job);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();
        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Find EditText fields and Post Job Button
        EditText editTextJobName = findViewById(R.id.editTextJobName);
        EditText editTextJobStartDate = findViewById(R.id.editTextJobStartDate);
        EditText editTextJobDescription = findViewById(R.id.editTextJobDescription);
        EditText editTextMinExperience = findViewById(R.id.editTextMinExperience);
        EditText editTextLocation = findViewById(R.id.editTextLocation);
        EditText editTextPrice = findViewById(R.id.editTextPrice);
        ImageView imageViewBackArrow = findViewById(R.id.imageViewBackArrow);
        imageViewBackArrow.setOnClickListener(v -> onBackPressed());

        Button buttonPostJob = findViewById(R.id.buttonPostJob);

        // Set OnClickListener for the Job Start Date EditText
        editTextJobStartDate.setOnClickListener(v -> showDatePickerDialog(editTextJobStartDate));

        // Set OnClickListener for the Post Job Button
        buttonPostJob.setOnClickListener(v -> postJob());
    }

    private void postJob() {
        // Retrieve job details from EditText fields
        EditText editTextJobName = findViewById(R.id.editTextJobName);
        EditText editTextJobStartDate = findViewById(R.id.editTextJobStartDate);
        EditText editTextJobDescription = findViewById(R.id.editTextJobDescription);
        EditText editTextMinExperience = findViewById(R.id.editTextMinExperience);
        EditText editTextLocation = findViewById(R.id.editTextLocation);
        EditText editTextPrice = findViewById(R.id.editTextPrice);

        String jobName = editTextJobName.getText().toString().trim();
        String jobStartDate = editTextJobStartDate.getText().toString().trim();
        String jobDescription = editTextJobDescription.getText().toString().trim();
        String minExperience = editTextMinExperience.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();
        String price = editTextPrice.getText().toString().trim();

        // Check if all fields are filled
        if (jobName.isEmpty() || jobStartDate.isEmpty() || jobDescription.isEmpty() || minExperience.isEmpty()) {
            Toast.makeText(PostJobActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Retrieve the current user ID
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String clientId = currentUser != null ? currentUser.getUid() : null;

        if (clientId != null) {
            // Fetch client details from Firestore
            db.collection("users").document(clientId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Extract client details
                            String clientName = documentSnapshot.getString("name");
                            String clientEmail = documentSnapshot.getString("email");
                            String clientPhoneNumber = documentSnapshot.getString("phoneNumber");
                            String clientLocation = documentSnapshot.getString("location");

                            // Create a HashMap to hold the job details
                            Map<String, Object> jobDetails = new HashMap<>();
                            jobDetails.put("jobName", jobName);
                            jobDetails.put("jobStartDate", jobStartDate);
                            jobDetails.put("jobDescription", jobDescription);
                            jobDetails.put("minExperience", minExperience);
                            jobDetails.put("location", location);
                            jobDetails.put("price", price);
                            jobDetails.put("clientId", clientId); // Add the client ID to the job details
                            jobDetails.put("clientName", clientName); // Add client name
                            jobDetails.put("clientEmail", clientEmail); // Add client email
                            jobDetails.put("clientPhoneNumber", clientPhoneNumber); // Add client phone number
                            jobDetails.put("clientLocation", clientLocation); // Add client location
                            jobDetails.put("isAssigned", false);
                            jobDetails.put("timestamp", Timestamp.now());

                            // Add the job details to Firestore with a unique jobId
                            db.collection("jobs")
                                    .add(jobDetails)
                                    .addOnSuccessListener(documentReference -> {
                                        // Job details added successfully
                                        String jobId = documentReference.getId(); // Retrieve the generated document ID

                                        // Update the job document with the jobId
                                        documentReference.update("jobId", jobId)
                                                .addOnSuccessListener(aVoid -> {
                                                    // Document updated successfully with jobId
                                                    Toast.makeText(PostJobActivity.this, "Job posted successfully", Toast.LENGTH_SHORT).show();
                                                    // Clear EditText fields after posting job
                                                    editTextJobName.setText("");
                                                    editTextJobStartDate.setText("");
                                                    editTextJobDescription.setText("");
                                                    editTextMinExperience.setText("");
                                                    editTextLocation.setText("");
                                                    editTextPrice.setText("");
                                                })
                                                .addOnFailureListener(e -> {
                                                    // Failed to update document with jobId
                                                    Toast.makeText(PostJobActivity.this, "Failed to update document with jobId: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        // Failed to add job details
                                        Toast.makeText(PostJobActivity.this, "Failed to post job: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(PostJobActivity.this, "Client details not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Failed to fetch client details
                        Toast.makeText(PostJobActivity.this, "Failed to fetch client details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showDatePickerDialog(final EditText editText) {
        // Get current date to set as default in the DatePickerDialog
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        // Create DatePickerDialog and set the default date
        DatePickerDialog datePickerDialog = new DatePickerDialog(PostJobActivity.this,
                (view, year1, month1, dayOfMonth1) -> {
                    // Set the selected date in the EditText field
                    calendar.set(Calendar.YEAR, year1);
                    calendar.set(Calendar.MONTH, month1);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth1);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                    String selectedDate = dateFormat.format(calendar.getTime());
                    editText.setText(selectedDate);
                }, year, month, dayOfMonth);

        // Show DatePickerDialog
        datePickerDialog.show();
    }
}
