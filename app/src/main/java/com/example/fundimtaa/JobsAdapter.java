package com.example.fundimtaa;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class JobsAdapter extends RecyclerView.Adapter<JobsAdapter.JobViewHolder> {

    private List<Job> jobList;
    private OnItemClickListener listener;

    public JobsAdapter(List<Job> jobList, OnItemClickListener listener) {
        this.jobList = jobList;
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(Job job);
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job_admin, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Job job = jobList.get(position);
        holder.jobName.setText("Job Name: " + job.getJobName());
        holder.jobDescription.setText("Job Description: " + job.getJobDescription());
        holder.location.setText("Location: " + job.getLocation());
        holder.price.setText("Price: " + job.getPrice());
        holder.minExperience.setText("Minimum Experience: " + job.getMinExperience());
        holder.jobStartDate.setText("Job Start Date: " + job.getJobStartDate());
        holder.bind(job, listener);
    }

    @Override
    public int getItemCount() {
        return jobList.size();
    }

    public static class JobViewHolder extends RecyclerView.ViewHolder {
        TextView jobName, jobDescription, location, price, minExperience, jobStartDate;

        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            jobName = itemView.findViewById(R.id.textViewJobName);
            jobDescription = itemView.findViewById(R.id.textViewJobDescription);
            location = itemView.findViewById(R.id.textViewLocation);
            price = itemView.findViewById(R.id.textViewPrice);
            minExperience = itemView.findViewById(R.id.textViewMinExperience);
            jobStartDate = itemView.findViewById(R.id.textViewJobStartDate);
        }

        public void bind(final Job job, final OnItemClickListener listener) {
            itemView.setOnClickListener(v -> listener.onItemClick(job));
        }
    }
}
