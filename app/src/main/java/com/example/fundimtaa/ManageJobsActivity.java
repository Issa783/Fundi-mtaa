package com.example.fundimtaa;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class ManageJobsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private JobsAdapter jobsAdapter;
    private List<Job> jobList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_jobs);

        recyclerView = findViewById(R.id.recyclerViewJobs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        jobList = new ArrayList<>();
        jobsAdapter = new JobsAdapter(jobList, new JobsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Job job) {
                showClientDetailsDialog(job);
            }

        });
        recyclerView.setAdapter(jobsAdapter);

        db = FirebaseFirestore.getInstance();

        loadJobs();
    }

    private void loadJobs() {
        db.collection("jobs")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(ManageJobsActivity.this, "Error loading jobs", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        jobList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Job job = document.toObject(Job.class);
                            job.setDocumentId(document.getId()); // Use setDocumentId instead of setId
                            jobList.add(job);
                        }
                        jobsAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void showClientDetailsDialog(Job job) {
        String clientId = job.getClientId();

        if (clientId == null || clientId.isEmpty()) {
            Toast.makeText(ManageJobsActivity.this, "Client ID is invalid", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(clientId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String role = document.getString("role");
                                Log.d("Firestore", "User role: " + role); // Log the role

                                if ("client".equals(role)) {
                                    String clientName = document.getString("name");
                                    String clientEmail = document.getString("email");
                                    String clientPhoneNumber = document.getString("phoneNumber");
                                    String clientLocation = document.getString("location");

                                    String clientDetails = "Client Name: " + clientName + "\n" +
                                            "Client Email: " + clientEmail + "\n" +
                                            "Client Phone: " + clientPhoneNumber + "\n" +
                                            "Client Location: " + clientLocation;

                                    new AlertDialog.Builder(ManageJobsActivity.this)
                                            .setTitle("Client Details")
                                            .setMessage(clientDetails)
                                            .setPositiveButton("OK", null)
                                            .show();
                                } else {
                                    Toast.makeText(ManageJobsActivity.this, "The user is not a client", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(ManageJobsActivity.this, "Client not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ManageJobsActivity.this, "Error fetching client details", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }




}
