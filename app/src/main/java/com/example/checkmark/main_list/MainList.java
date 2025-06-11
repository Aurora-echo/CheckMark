package com.example.checkmark.main_list;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.checkmark.R;
import com.example.checkmark.main_list.CheckItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import add_check.Add_Check;
import check_record.CheckRecord;

public class MainList extends AppCompatActivity {

    private List<CheckItem> CheckList = new ArrayList<>();
    private CheckAdapter CheckAdapter;
    private SharedPreferences sp;
    private static final String SP_NAME = "CheckListInfo";
    private static final String TASKS_KEY = "tasks";
    private static final String TAG = "Log--------->>>>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);

        // 初始化SharedPreferences
        sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);

        // 从SP加载数据
        loadTasksFromSharedPreferences();

        // 设置RecyclerView
        RecyclerView recyclerView = findViewById(R.id.todo_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        CheckAdapter = new CheckAdapter(CheckList, new CheckAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(MainList.this, CheckRecord.class);
                intent.putExtra("position", position);
                intent.putExtra("taskName", CheckList.get(position).getText());
                startActivity(intent);
            }

            @Override
            public void onStatusClick(int position) {
                Toast.makeText(MainList.this, "点击了第 " + (position + 1) + " 项的图标", Toast.LENGTH_SHORT).show();
                // 切换完成状态
                //CheckItem item = CheckList.get(position);
                //item.setCompleted(!item.isCompleted());
                //CheckAdapter.notifyItemChanged(position);
                // 更新SP中的完成状态
                //updateCompletionStatusInSP(position, item.isCompleted());
            }
        });
        recyclerView.setAdapter(CheckAdapter);

        // 添加按钮点击事件
        ImageButton btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainList.this, Add_Check.class);
            startActivityForResult(intent, 1); // 使用startActivityForResult以便刷新列表
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // 当从Add_Check返回时刷新列表
            loadTasksFromSharedPreferences();
            CheckAdapter.notifyDataSetChanged();
        }
    }

    //从SP中读取数据方法
    private void loadTasksFromSharedPreferences() {
        Log.i(TAG,"coming loadTasksFromSharedPreferences");
        CheckList.clear();
        String tasksJson = sp.getString(TASKS_KEY, "[]");
        Log.d(TAG, "从SP读取的原始JSON: " + tasksJson);
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson, type);

        for (Map<String, Object> task : tasks) {
            String name = (String) task.get("name");
            boolean isCompleted = task.containsKey("isCompleted") && (boolean) task.get("isCompleted");
            Log.d(TAG, "加载任务 - 名称: " + name + ", 状态: " + isCompleted);
            CheckList.add(new CheckItem(name, isCompleted));
        }
    }

    private void updateCompletionStatusInSP(int position, boolean isCompleted) {
        String tasksJson = sp.getString(TASKS_KEY, "[]");
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> tasks = new Gson().fromJson(tasksJson, type);

        if (position >= 0 && position < tasks.size()) {
            tasks.get(position).put("isCompleted", isCompleted);
            sp.edit().putString(TASKS_KEY, new Gson().toJson(tasks)).apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到界面时刷新数据
        loadTasksFromSharedPreferences();
        if (CheckAdapter != null) {
            CheckAdapter.notifyDataSetChanged();
        }
    }
}