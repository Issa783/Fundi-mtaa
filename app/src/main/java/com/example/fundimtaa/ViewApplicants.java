package com.example.fundimtaa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.appcompat.widget.SearchView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
public class ViewApplicants extends AppCompatActivity {
    private boolean isJobAssigned = false;
    private RecyclerView recyclerViewApplicants;
    private WorkerAdapter workerAdapter;
    private List<Worker> workerList;
    private FirebaseFirestore db;
    private ImageView imageViewFilter;

    private String jobId;
    private String clientId;
    private String jobName;
    private ViewApplicantsViewModel viewModel;
    private Map<String, Integer> workerAssignedJobsCountMap = new HashMap<>();
    private Map<String, Set<String>> assignedJobsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_applicants);

        ImageView imageViewBackArrow = findViewById(R.id.imageViewBackArrow);
        imageViewBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to the previous activity
                onBackPressed();
            }
        });

        imageViewFilter = findViewById(R.id.imageViewFilter);
        db = FirebaseFirestore.getInstance();
        viewModel = new ViewModelProvider(this).get(ViewApplicantsViewModel.class);

        imageViewFilter.setOnClickListener(v -> showFilterDialog());

        recyclerViewApplicants = findViewById(R.id.recyclerWorkerViewApplicants);
        recyclerViewApplicants.setHasFixedSize(true);
        recyclerViewApplicants.setLayoutManager(new LinearLayoutManager(this));
        workerList = new ArrayList<>();
        jobId = getIntent().getStringExtra("jobId");
        clientId = getIntent().getStringExtra("clientId");
        jobName = getIntent().getStringExtra("jobName");
        String startDate = getIntent().getStringExtra("jobStartDate");
        String minExperience = getIntent().getStringExtra("minExperience");
        String location = getIntent().getStringExtra("location");
        String price = getIntent().getStringExtra("price");
        String jobDescription = getIntent().getStringExtra("jobDescription");
        String documentId = getIntent().getStringExtra("documentId");

        // Initialize adapter
        workerAdapter = new WorkerAdapter(workerList, jobId, jobName, startDate, minExperience, location, price, jobDescription, clientId, documentId);
        recyclerViewApplicants.setAdapter(workerAdapter);
        loadWorkers();

        // Observe data from ViewModel
        viewModel.getWorkersLiveData().observe(this, new Observer<List<Worker>>() {
            @Override
            public void onChanged(List<Worker> workers) {
                workerList.clear();
                workerList.addAll(workers);
                workerAdapter.notifyDataSetChanged();
            }
        });

        recommendWorkers();
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_filter, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        TextView textViewExperience = dialogView.findViewById(R.id.textViewExperience);
        TextView textViewRating = dialogView.findViewById(R.id.textViewRating);
        TextView textViewAvailability = dialogView.findViewById(R.id.textViewAvailability);
        TextView textViewClose = dialogView.findViewById(R.id.textViewClose);

        textViewAvailability.setOnClickListener(v -> {
            textViewAvailability.setTextColor(ContextCompat.getColor(this, R.color.selectedText));
            textViewExperience.setTextColor(ContextCompat.getColor(this, R.color.defaultText));
            textViewRating.setTextColor(ContextCompat.getColor(this, R.color.defaultText));

            Collections.sort(workerList, (worker1, worker2) -> Integer.compare(worker1.getAssignedJobs(), worker2.getAssignedJobs()));
            workerAdapter.notifyDataSetChanged();
            dialog.dismiss();
        });

        textViewExperience.setOnClickListener(v -> {
            textViewExperience.setTextColor(ContextCompat.getColor(this, R.color.selectedText));
            textViewRating.setTextColor(ContextCompat.getColor(this, R.color.defaultText));
            textViewAvailability.setTextColor(ContextCompat.getColor(this, R.color.defaultText));

            Collections.sort(workerList, (worker1, worker2) -> {
                int experience1 = parseExperience(worker1.getExperience());
                int experience2 = parseExperience(worker2.getExperience());
                return Integer.compare(experience2, experience1);
            });
            workerAdapter.notifyDataSetChanged();
            dialog.dismiss();
        });

        textViewRating.setOnClickListener(v -> {
            textViewRating.setTextColor(ContextCompat.getColor(this, R.color.selectedText));
            textViewExperience.setTextColor(ContextCompat.getColor(this, R.color.defaultText));
            textViewAvailability.setTextColor(ContextCompat.getColor(this, R.color.defaultText));

            db.collection("RatingsAndReviews")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Map<String, List<Double>> workerRatingsMap = new HashMap<>();

                            // Collect ratings for each worker
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String workerId = document.getString("workerId");
                                double rating = document.getDouble("rating");

                                if (!workerRatingsMap.containsKey(workerId)) {
                                    workerRatingsMap.put(workerId, new ArrayList<>());
                                }
                                workerRatingsMap.get(workerId).add(rating);
                            }

                            // Update workerList with filtered and sorted workers
                            List<Worker> filteredWorkers = new ArrayList<>();
                            for (Worker worker : workerList) {
                                List<Double> ratings = workerRatingsMap.get(worker.getWorkerId());
                                if (ratings != null && !ratings.isEmpty()) {
                                    // Calculate average rating
                                    double averageRating = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                                    worker.setRating(averageRating);

                                    // Check if average rating is 3 or greater
                                    if (averageRating >= 3.0) {
                                        filteredWorkers.add(worker);
                                    }
                                }
                            }

                            // Sort filtered workers by rating (descending) and assigned jobs (ascending)
                            Collections.sort(filteredWorkers, (worker1, worker2) -> {
                                int compareRatings = Double.compare(worker2.getRating(), worker1.getRating());
                                if (compareRatings == 0) {
                                    return Integer.compare(worker1.getAssignedJobs(), worker2.getAssignedJobs());
                                }
                                return compareRatings;
                            });

                            // Update workerList with filtered and sorted workers
                            workerList.clear();
                            workerList.addAll(filteredWorkers);
                            workerAdapter.notifyDataSetChanged();
                        }
                    });

            dialog.dismiss();
        });


        textViewClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Helper method to parse experience strings into integers
    private int parseExperience(String experience) {
        return Integer.parseInt(experience.replaceAll("[^0-9]", ""));
    }

    private void loadWorkers() {
        db.collection("job_applications")
                .whereEqualTo("jobId", jobId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(ViewApplicants.this, "Failed to load workers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        workerList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String workerId = document.getString("workerId");
                            String name = document.getString("name");
                            String phoneNumber = document.getString("phoneNumber");
                            String location = document.getString("location");
                            String dateOfApplication = document.getString("dateOfApplication");
                            String experience = document.getString("experience");
                            Timestamp timestamp = document.getTimestamp("timestamp");
                            boolean isCanceled = document.getBoolean("isCanceled") != null && document.getBoolean("isCanceled");
                            Worker worker = new Worker(workerId, name, phoneNumber, location, dateOfApplication, experience, timestamp, isCanceled);
                            workerList.add(worker);
                        }

                        // Fetch assigned jobs count and ratings for each worker
                        fetchAssignedJobsCountForAllWorkers();
                    }
                });
    }



    private void fetchAssignedJobsCountForAllWorkers() {
        for (Worker worker : workerList) {
            fetchAssignedJobsCountForWorker(worker);
        }
    }

    private void fetchAssignedJobsCountForWorker(Worker worker) {
        db.collection("AssignedJobsCount")
                .document(worker.getWorkerId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Long numberOfAssignedJobs = document.getLong("numberOfAssignedJobs");
                            if (numberOfAssignedJobs != null) {
                                int assignedJobsCount = numberOfAssignedJobs.intValue();
                                worker.setAssignedJobs(assignedJobsCount);
                                workerAssignedJobsCountMap.put(worker.getWorkerId(), assignedJobsCount);
                                // Set availability based on the assigned jobs count
                                worker.setAvailable(assignedJobsCount == 0);
                                workerAdapter.notifyDataSetChanged();
                            } else {
                                Log.d("FetchAssignedJobsCount", "numberOfAssignedJobs is null for worker " + worker.getWorkerId());
                            }
                        } else {
                            Log.d("FetchAssignedJobsCount", "No such document for worker " + worker.getWorkerId());
                        }
                    } else {
                        Log.e("FetchAssignedJobsCount", "Error fetching document: ", task.getException());
                    }
                });
    }

    private void recommendWorkers() {
        db.collection("RatingsAndReviews")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, List<Double>> workerRatingsMap = new HashMap<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String workerId = document.getString("workerId");
                            double rating = document.getDouble("rating");

                            if (!workerRatingsMap.containsKey(workerId)) {
                                workerRatingsMap.put(workerId, new ArrayList<>());
                            }
                            workerRatingsMap.get(workerId).add(rating);
                        }

                        for (Worker worker : workerList) {
                            List<Double> ratings = workerRatingsMap.get(worker.getWorkerId());
                            if (ratings != null && !ratings.isEmpty()) {
                                double averageRating = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                                worker.setRating(averageRating);
                            } else {
                                worker.setRating(0.0); // Set default rating to 0.0 if no ratings are found
                            }
                        }

                        // Wait until assigned jobs are fetched and then sort workers
                        new Handler().postDelayed(() -> {
                            sortAndDisplayWorkers();
                            workerAdapter.notifyDataSetChanged();
                        }, 2000); // Adjust delay as necessary to ensure assigned jobs are fetched
                    }
                });
    }


    private void sortAndDisplayWorkers() {
        // Separate workers with and without ratings
        List<Worker> workersWithRatings = new ArrayList<>();
        List<Worker> workersWithoutRatings = new ArrayList<>();

        for (Worker worker : workerList) {
            if (worker.getRating() >= 3.0) {
                workersWithRatings.add(worker);
            } else {
                workersWithoutRatings.add(worker);
            }
        }

        // Sort workers with ratings by rating (descending) and assigned jobs (ascending)
        Collections.sort(workersWithRatings, (worker1, worker2) -> {
            int compareRatings = Double.compare(worker2.getRating(), worker1.getRating());
            if (compareRatings == 0) {
                return Integer.compare(worker1.getAssignedJobs(), worker2.getAssignedJobs());
            }
            return compareRatings;
        });

        // Combine the lists: workers with ratings first, followed by workers without ratings
        workerList.clear();
        workerList.addAll(workersWithRatings);
        workerList.addAll(workersWithoutRatings);

        workerAdapter.notifyDataSetChanged();
    }

    private class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder> {

        private List<Worker> workerList;
        private String jobId;
        private String jobName;
        private String startDate;
        private String minExperience;
        private String location;
        private String price;
        private String jobDescription;
        private String clientId;
        private String documentId;


        public WorkerAdapter(List<Worker> workerList, String jobId, String jobName, String startDate, String minExperience, String location, String price, String jobDescription, String clientId, String documentId) {
            this.workerList = workerList;
            this.jobId = jobId;
            this.jobName = jobName;
            this.startDate = startDate;
            this.minExperience = minExperience;
            this.location = location;
            this.price = price;
            this.jobDescription = jobDescription;
            this.clientId = clientId;
            this.documentId = documentId;
        }

        @NonNull
        @Override
        public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_applicants, parent, false);
            return new WorkerViewHolder(itemView);
        }
        @Override
        public void onBindViewHolder(@NonNull WorkerViewHolder holder, int position) {
            Worker worker = workerList.get(position);
            holder.textViewWorkerName.setText("Worker Name: " + worker.getName());
            holder.textViewDateOfApplication.setText("Applied on: " + worker.getDateOfApplication());
            holder.textViewExperience.setText("Experience: " + worker.getExperience());
            holder.textViewAssignedJobs.setText("Assigned Jobs: " + worker.getAssignedJobs());

            // Display availability
            if (worker.isAvailable()) {
                holder.textViewAvailable.setVisibility(View.VISIBLE);
                holder.textViewAvailable.setText("Available");
                // Start pulsating animation
                Animation pulseAnimation = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.pulse);
                holder.indicatorAvailable.setVisibility(View.VISIBLE);
                holder.indicatorAvailable.startAnimation(pulseAnimation);
                holder.buttonAssignJob.setEnabled(true); // Enable button if available
            } else {
                holder.textViewAvailable.setVisibility(View.VISIBLE);
                holder.textViewAvailable.setText("Not Available");
                holder.indicatorAvailable.setVisibility(View.GONE);
                holder.indicatorAvailable.clearAnimation(); // Clear animation when not visible
                holder.buttonAssignJob.setEnabled(false); // Disable button if not available
            }

            if (worker.getRating() >= 3.0 && position < 3) {
                holder.textViewRecommended.setVisibility(View.VISIBLE);
                holder.itemView.setBackgroundColor(ContextCompat.getColor(ViewApplicants.this, R.color.recommended_worker_bg));
            } else {
                holder.textViewRecommended.setVisibility(View.GONE);
                holder.itemView.setBackgroundColor(ContextCompat.getColor(ViewApplicants.this, R.color.default_worker_bg));
            }

            // Disable the assign job button if the job is canceled for this worker
            holder.buttonAssignJob.setEnabled(!worker.isCanceled());

            holder.buttonViewProfile.setOnClickListener(v -> {
                Intent intent = new Intent(ViewApplicants.this, ViewProfileActivity.class);
                intent.putExtra("workerId", worker.getWorkerId());
                startActivity(intent);
            });

            holder.buttonAssignJob.setOnClickListener(v -> {
                assignJob(worker, jobName, startDate, minExperience, location, price, jobDescription);
            });

            holder.buttonRejectJob.setOnClickListener(v -> {
                rejectJob(worker, jobName, jobId, startDate);
            });

            holder.buttonCancelJob.setOnClickListener(v -> {
                cancelJob(worker, jobId, startDate, position);
            });
        }

        @Override
        public int getItemCount() {
            return workerList.size();
        }

        public class WorkerViewHolder extends RecyclerView.ViewHolder {
            TextView textViewWorkerName, textViewDateOfApplication, textViewExperience, textViewAssignedJobs, textViewRecommended,textViewAvailable;
            Button buttonViewProfile, buttonAssignJob,buttonRejectJob, buttonCancelJob;
            ImageView indicatorAvailable; // Add this line

            public WorkerViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewWorkerName = itemView.findViewById(R.id.textViewName);
                textViewDateOfApplication = itemView.findViewById(R.id.textViewDateOfApplication);
                textViewExperience = itemView.findViewById(R.id.textViewExperience);
                textViewAssignedJobs = itemView.findViewById(R.id.textViewAssignedJobs);
                textViewRecommended = itemView.findViewById(R.id.textViewRecommended); // TextView for recommended tag
                textViewAvailable = itemView.findViewById(R.id.textViewAvailable); // TextView for availability
                buttonViewProfile = itemView.findViewById(R.id.buttonViewProfile);
                buttonAssignJob = itemView.findViewById(R.id.buttonAssignJob);
                buttonRejectJob = itemView.findViewById(R.id.buttonRejectJob);
                buttonCancelJob = itemView.findViewById(R.id.buttonCancelJob);
                indicatorAvailable = itemView.findViewById(R.id.indicatorAvailable); // Add this line

            }
        }
    }
    private void assignJob(Worker worker, String jobName, String startDate, String minExperience, String location, String price, String jobDescription) {
        // Check if the worker is available
        if (!worker.isAvailable()) {
            Toast.makeText(ViewApplicants.this, "Worker " + worker.getName() + " is not available", Toast.LENGTH_SHORT).show();
            return; // Exit the method if the worker is not available
        }
        // First, check if the job is already assigned or rejected
        db.collection("AssignedJobs")
                .whereEqualTo("clientId", clientId)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("workerId", worker.getWorkerId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Job is already assigned to this worker
                        Toast.makeText(ViewApplicants.this, "You have already assigned this job to " + worker.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        // Check if the job was rejected for this worker
                        db.collection("RejectedJobs")
                                .whereEqualTo("jobId", jobId)
                                .whereEqualTo("workerId", worker.getWorkerId())
                                .get()
                                .addOnCompleteListener(rejectTask -> {
                                    if (rejectTask.isSuccessful() && !rejectTask.getResult().isEmpty()) {
                                        // Worker has been rejected for this job
                                        Toast.makeText(ViewApplicants.this, "Cannot assign job to " + worker.getName() + " since you have rejected.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Proceed with the assignment
                                        proceedWithJobAssignment(worker, jobName, startDate, minExperience, location, price, jobDescription);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ViewApplicants.this, "Error checking rejected jobs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewApplicants.this, "Error checking existing assignment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void proceedWithJobAssignment(Worker worker, String jobName, String startDate, String minExperience, String location, String price, String jobDescription) {
        FirebaseFirestore.getInstance().collection("users").document(clientId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String clientName = documentSnapshot.getString("name");
                        String clientPhoneNumber = documentSnapshot.getString("phoneNumber");
                        String clientLocation = documentSnapshot.getString("location");
                        String clientEmail = documentSnapshot.getString("email");

                        Map<String, Object> assignedJob = new HashMap<>();
                        assignedJob.put("clientId", clientId);
                        assignedJob.put("clientName", clientName);
                        assignedJob.put("clientPhoneNumber", clientPhoneNumber);
                        assignedJob.put("clientLocation", clientLocation);
                        assignedJob.put("clientEmail", clientEmail);
                        assignedJob.put("workerId", worker.getWorkerId());
                        assignedJob.put("workerName", worker.getName());
                        assignedJob.put("jobId", jobId);
                        assignedJob.put("jobName", jobName);
                        assignedJob.put("jobStartDate", startDate);
                        assignedJob.put("minExperience", minExperience);
                        assignedJob.put("location", location);
                        assignedJob.put("price", price);
                        assignedJob.put("jobDescription", jobDescription);
                        assignedJob.put("timestamp", Timestamp.now());
                        assignedJob.put("isCanceled", false);

                        String documentId = jobId;

                        // Store complete job details in AssignedJobs collection
                        FirebaseFirestore.getInstance().collection("AssignedJobs")
                                .document(documentId)
                                .set(assignedJob)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("AssignedJobs", "Assigned job ID: " + documentId);

                                    FirebaseFirestore.getInstance().collection("AssignedJobsCount")
                                            .document(worker.getWorkerId())
                                            .update("numberOfAssignedJobs", FieldValue.increment(1))
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("AssignedJobsCount", "Updated number of assigned jobs for worker " + worker.getWorkerId());
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("AssignedJobsCount", "Failed to update number of assigned jobs: " + e.getMessage());
                                                // If the document doesn't exist, create it
                                                Map<String, Object> assignedJobsCount = new HashMap<>();
                                                assignedJobsCount.put("workerId", worker.getWorkerId());
                                                assignedJobsCount.put("workerName", worker.getName());
                                                assignedJobsCount.put("numberOfAssignedJobs", 1);

                                                FirebaseFirestore.getInstance().collection("AssignedJobsCount")
                                                        .document(worker.getWorkerId())
                                                        .set(assignedJobsCount)
                                                        .addOnSuccessListener(newDocumentReference -> {
                                                            Log.d("AssignedJobsCount", "Created new document for worker " + worker.getWorkerId());
                                                        })
                                                        .addOnFailureListener(newError -> {
                                                            Log.e("AssignedJobsCount", "Failed to create document: " + newError.getMessage());
                                                        });
                                            });

                                    String key = clientId + "_" + jobId;
                                    Set<String> assignedWorkers = assignedJobsMap.getOrDefault(key, new HashSet<>());
                                    assignedWorkers.add(worker.getWorkerId());
                                    assignedJobsMap.put(key, assignedWorkers);

                                    saveAssignmentState(true);
                                    Toast.makeText(ViewApplicants.this, "Job assigned to " + worker.getName(), Toast.LENGTH_SHORT).show();
                                    worker.setAssignedJobs(worker.getAssignedJobs() + 1);
                                    // worker.setAvailable(false); // Mark as unavailable
                                    db.collection("jobs").document(jobId).update("isAssigned", true);
                                    db.collection("users").document(worker.getWorkerId()).update("isAvailable", false);
                                    workerAdapter.notifyDataSetChanged();
                                    notifyJobAssignment(clientId, worker.getWorkerId(), jobName);

                                    // Store ratings and reviews
                                    Map<String, Object> ratingsAndReviews = new HashMap<>();
                                    ratingsAndReviews.put("rating", 0);
                                    ratingsAndReviews.put("review", "");
                                    ratingsAndReviews.put("workerId", worker.getWorkerId());
                                    ratingsAndReviews.put("jobId", jobId);
                                    ratingsAndReviews.put("jobName", jobName);
                                    ratingsAndReviews.put("minExperience", minExperience);
                                    ratingsAndReviews.put("timestamp",Timestamp.now());

                                    FirebaseFirestore.getInstance().collection("RatingsAndReviews")
                                            .document(documentId)
                                            .set(ratingsAndReviews)
                                            .addOnSuccessListener(ratingsAndReviewsRef -> {
                                                Log.d("RatingsAndReviews", "Ratings and Reviews stored successfully");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("RatingsAndReviews", "Failed to store Ratings and Reviews: " + e.getMessage());
                                            });

                                    // Store job details in WorkersJobHistory collection
                                    Map<String, Object> jobHistory = new HashMap<>();
                                    jobHistory.put("jobId", jobId);
                                    jobHistory.put("workerId",worker.getWorkerId());
                                    jobHistory.put("jobName", jobName);
                                    jobHistory.put("jobStartDate", startDate);
                                    jobHistory.put("minExperience", minExperience);
                                    jobHistory.put("location", location);
                                    jobHistory.put("price", price);
                                    jobHistory.put("jobDescription", jobDescription);
                                    jobHistory.put("timestamp", Timestamp.now());

                                    FirebaseFirestore.getInstance().collection("WorkersJobHistory")
                                            .document(documentId)
                                            .set(jobHistory)
                                            .addOnSuccessListener(jobHistoryRef -> {
                                                Log.d("WorkersJobHistory", "Job history stored successfully for job ID: " + documentId);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("WorkersJobHistory", "Failed to store job history: " + e.getMessage());
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ViewApplicants.this, "Failed to assign job: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(ViewApplicants.this, "Client details not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewApplicants.this, "Error fetching client details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    private void rejectJob(Worker worker, String jobName, String jobId, String startDate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("RejectedJobs")
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("workerId", worker.getWorkerId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Job has already been rejected by this worker
                        Toast.makeText(ViewApplicants.this, "You have already rejected " + worker.getName() + " for this job.", Toast.LENGTH_SHORT).show();
                    } else {
                        // Proceed with rejecting the job
                        proceedWithJobRejection(worker, jobName, jobId, startDate);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RejectedJobs", "Failed to check rejected jobs: " + e.getMessage());
                });
    }

    private void proceedWithJobRejection(Worker worker, String jobName, String jobId, String startDate) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> rejectedJob = new HashMap<>();
        rejectedJob.put("workerId", worker.getWorkerId());
        rejectedJob.put("jobId", jobId);
        rejectedJob.put("jobName", jobName);
        rejectedJob.put("jobStartDate", startDate);
        rejectedJob.put("workerName", worker.getName());
        rejectedJob.put("rejected", true);

        db.collection("RejectedJobs")
                .document(worker.getWorkerId() + "_" + jobId)
                .set(rejectedJob)
                .addOnSuccessListener(aVoid -> {
                    Log.d("RejectedJobs", "Job rejected: " + jobId);
                    Toast.makeText(ViewApplicants.this, "You have rejected " + worker.getName() + " for this job.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("RejectedJobs", "Failed to reject job: " + e.getMessage());
                });
    }

    private void cancelJob(Worker worker, String jobId, String startDate, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("AssignedJobs").document(jobId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isCanceled = documentSnapshot.getBoolean("isCanceled");
                        if (isCanceled != null && isCanceled) {
                            Toast.makeText(ViewApplicants.this, "You have already canceled this job", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Update the job to canceled
                        documentSnapshot.getReference().update("isCanceled", true)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("PostedJobs", "Job canceled successfully");

                                    // Update assigned jobs count
                                    db.collection("AssignedJobsCount")
                                            .document(worker.getWorkerId())
                                            .update("numberOfAssignedJobs", FieldValue.increment(-1))
                                            .addOnSuccessListener(aVoid1 -> {
                                                Log.d("AssignedJobsCount", "Assigned jobs count updated successfully");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("AssignedJobsCount", "Failed to update assigned jobs count", e);
                                            });

                                    // Store details in RejectedJobs collection
                                    Map<String, Object> rejectedJob = new HashMap<>();
                                    rejectedJob.put("workerId", worker.getWorkerId());
                                    rejectedJob.put("workerName", worker.getName());
                                    rejectedJob.put("jobId", jobId);
                                    rejectedJob.put("jobName", jobName); // Ensure jobName is defined
                                    rejectedJob.put("jobStartDate", startDate);
                                    rejectedJob.put("timestamp", Timestamp.now());

                                    db.collection("RejectedJobs")
                                            .add(rejectedJob)
                                            .addOnSuccessListener(documentReference -> {
                                                Log.d("RejectedJobs", "Rejected job ID: " + documentReference.getId());

                                                // Remove job from WorkersJobHistory collection
                                                db.collection("WorkersJobHistory")
                                                        .document(jobId)
                                                        .delete()
                                                        .addOnSuccessListener(aVoid3 -> {
                                                            Log.d("WorkersJobHistory", "Job removed from worker's job history collection");
                                                            Toast.makeText(ViewApplicants.this, "Job canceled and removed successfully", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e("WorkersJobHistory", "Failed to remove job from worker's job history: " + e.getMessage());
                                                            Toast.makeText(ViewApplicants.this, "Error removing job from worker's job history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("RejectedJobs", "Error adding rejected job", e);
                                                Toast.makeText(ViewApplicants.this, "Error adding rejected job: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });

                                    // Update the isCanceled field in job_applications
                                    db.collection("job_applications")
                                            .document(worker.getWorkerId() + "_" + jobId) // Assuming unique document ID format for job applications
                                            .update("isCanceled", true)
                                            .addOnSuccessListener(aVoid2 -> {
                                                Log.d("job_applications", "Job application marked as canceled");

                                                // Update the job status to available
                                                db.collection("jobs")
                                                        .document(jobId)
                                                        .update("isAssigned", false)
                                                        .addOnSuccessListener(aVoid3 -> {
                                                            Log.d("PostedJobs", "Job status updated to available");
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e("PostedJobs", "Failed to update job status", e);
                                                            Toast.makeText(ViewApplicants.this, "Error updating job status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        });

                                                // Update the worker's canceled state
                                                worker.setCanceled(true);
                                                workerList.set(position, worker);
                                                workerAdapter.notifyItemChanged(position);

                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("job_applications", "Failed to update job application", e);
                                                Toast.makeText(ViewApplicants.this, "Error updating job application: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("PostedJobs", "Failed to cancel job", e);
                                    Toast.makeText(ViewApplicants.this, "Error canceling job: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error retrieving job details", e);
                    Toast.makeText(ViewApplicants.this, "Error retrieving job details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




    private void saveAssignmentState(boolean isAssigned) {
        getSharedPreferences("ViewApplicantsPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isJobAssigned_" + jobId, isAssigned)
                .apply();
    }
    private void notifyJobAssignment(String clientId, String workerId, String jobId) {
        // Log the request parameters
        Log.d("NotifyJobApplication", "clientId: " + clientId);
        Log.d("NotifyJobApplication", "workerId: " + workerId);
        Log.d("NotifyJobApplication", "jobName: " + jobName);
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String jsonBody = "{\"clientId\":\"" + clientId + "\", \"workerId\":\"" + workerId + "\",\"jobName\":\"" + jobName + "\"}";
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url("https://notify-1-wk1o.onrender.com/notify-job-assignment")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Log.e("NotifyJobApplication", "Notification failed: " + e.getMessage());
                    Toast.makeText(ViewApplicants.this, "Notification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Log.d("NotifyJobApplication", "Notification sent successfully");
                        Toast.makeText(ViewApplicants.this, "Notification sent successfully", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        Log.e("NotifyJobApplication", "Notification failed: " + response.message());
                        Toast.makeText(ViewApplicants.this, "Notification failed: " + response.message(), Toast.LENGTH_SHORT).show();
                    });
                }
                response.close(); // Always close the response
            }
        });

    }
    }