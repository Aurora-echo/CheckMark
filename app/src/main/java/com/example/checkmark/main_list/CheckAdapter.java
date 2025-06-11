package com.example.checkmark.main_list;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.checkmark.R;
import com.example.checkmark.main_list.CheckItem;

import java.util.List;

public class CheckAdapter extends RecyclerView.Adapter<CheckAdapter.TodoViewHolder> {

    private List<CheckItem> CheckItem;
    private OnItemClickListener listener;
    private static final String TAG = "Log--------->>>>";

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onStatusClick(int position);
    }

    public CheckAdapter(List<CheckItem> CheckItem, OnItemClickListener listener) {
        this.CheckItem = CheckItem;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.checklist, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        CheckItem item = CheckItem.get(position);
        Log.d(TAG, "绑定数据: " + item.getText() + ", 状态: " + item.isCompleted());
        holder.textView.setText(item.getText());
        holder.statusView.setImageResource(
                item.isCompleted() ? R.drawable.ic_checked : R.drawable.ic_unchecked
        );

        // 整个项点击事件
        holder.itemView.setOnClickListener(v -> {
            listener.onItemClick(position);
        });

        // 状态图标点击事件
        holder.statusView.setOnClickListener(v -> {
            listener.onStatusClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return CheckItem.size();
    }

    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView statusView;

        public TodoViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.todo_text);
            statusView = itemView.findViewById(R.id.todo_status);
        }
    }
}