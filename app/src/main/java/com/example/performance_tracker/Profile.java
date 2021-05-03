package com.example.performance_tracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.core.utilities.Tree;

import java.io.Serializable;
import java.sql.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class Profile extends AppCompatActivity {
    // All Button Declaration Goes here
    private Button logOut;
    private Button addFriendButton;
    private Button addClassButton;
    private Button addTaskButton;
    private Button addProject;
    private Button viewMoreTask;

    // All Listview goes in here for Task
    private ListView listview;
    private ArrayList<String> values;
    private ArrayList<String> taskUid;
    private SimpleDateFormat currentDate;
    private HashMap<String,HashMap<String, Task>> taskLists;
    private ArrayAdapter<String> arrayAdapter, arrayAdapter2;

    // All User Variables Goes in Here
    private FirebaseUser loggedInUser;
    private DatabaseReference reference;
    private String loggedInUserId;

    // View user streak history
    private TextView viewStreakHistory;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // for the loggedInUser
        loggedInUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users");

        loggedInUserId = loggedInUser.getUid();

        // Home-Screen Content

        final TextView firstNameTextView = (TextView) findViewById(R.id.first_name); // the first name
        final TextView streakPoints = (TextView) findViewById(R.id.streak);

        // for the task list
        // for populating the pending task field
        listview = (ListView) findViewById(R.id.task_list);
        listview.setScrollContainer(false);
        this.values = new ArrayList<String>();
        this.taskUid = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, values);
        arrayAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, taskUid);
        listview.setAdapter(arrayAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent n = new Intent(getApplicationContext(), MarkCompleteTask.class);
                String taskId = taskUid.get(position);
                Bundle bundle = new Bundle();
                bundle.putString("taskId", taskId);
                currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String loggedInDate= currentDate.format(new Date());
                bundle.putString("taskDate", loggedInDate);
                n.putExtras(bundle);
                startActivityForResult(n,1);
            }
        });

        reference.child(loggedInUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userProfile = snapshot.getValue(User.class);
                if (userProfile!= null){
                    String firstName = userProfile.firstName;
                    firstNameTextView.setText("Hi "+firstName+"!");

                    currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String loggedInDate= currentDate.format(new Date());

                    TreeMap<String, Streak> streakHistory = new TreeMap<>(userProfile.streakHistory);
                    if (!streakHistory.containsKey(loggedInDate)){
                        Streak newStreakAdded = new Streak(loggedInDate);
                        newStreakAdded.setHighPriorityStreak(Objects.requireNonNull(streakHistory.lastEntry()).getValue().getHighPriorityStreak());
                        newStreakAdded.setMediumPriorityStreak(Objects.requireNonNull(streakHistory.lastEntry()).getValue().getMediumPriorityStreak());
                        newStreakAdded.setLowPriorityStreak(Objects.requireNonNull(streakHistory.lastEntry()).getValue().getLowPriorityStreak());
                        streakHistory.put(loggedInDate,newStreakAdded);
                        reference.child(loggedInUserId).child("streakHistory").setValue(streakHistory);
                        reference.child(loggedInUserId).child("userStreaks").setValue(newStreakAdded);
                    }
                    reference.child(loggedInUserId).child("userStreaks").setValue(streakHistory.get(loggedInDate));
                    HashMap<String, Task> task = new HashMap<>();
                    if (snapshot.child("taskLists").exists()){
                        taskLists = userProfile.taskLists;
                        TreeMap<String, HashMap<String, Task>> sortedTaskLists = new TreeMap<String, HashMap<String, Task>>(taskLists);
                        if (taskLists.containsKey(loggedInDate)){
                            arrayAdapter.clear();
                            arrayAdapter2.clear();
                            task = taskLists.get(loggedInDate);
                            int count = 0;
                            assert task != null;
                            for (Map.Entry<String, Task> set: task.entrySet()){
                                values.add(set.getValue().getTaskName());
                                taskUid.add(set.getValue().uuid);
                                count ++;
                                if (count >4) break;
                            }
                        }
                    } else {
                        taskLists=null;
                        arrayAdapter.clear();
                    }
                    arrayAdapter.notifyDataSetChanged();
                    String streakPoint = userProfile.userStreaks.totalStreak().toString();
                    streakPoints.setText(streakPoint);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Profile.this, "Error while accessing user account! Please retry", Toast.LENGTH_SHORT).show();
            }
        });


        // for pop up User Streak
        relativeLayout = (RelativeLayout) findViewById(R.id.streak_relative_layout);
        viewStreakHistory = (TextView) findViewById(R.id.view_full_streak);
        viewStreakHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Profile.this, streak_history.class));
            }
        });

        // for loading more tasks
        viewMoreTask =  (Button) findViewById(R.id.view_more_task);
        viewMoreTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (taskLists!=null){
                    Intent intent = new Intent(Profile.this, LoadAllTasks.class);
                    intent.putExtra("taskList",taskLists);
                    startActivity(intent);
                } else {
                    Toast.makeText(Profile.this, "You don't have any tasks ", Toast.LENGTH_SHORT).show();
                }
            }
        });


        // All footer goes in here
        // Adding New Project
        addProject =(Button) findViewById(R.id.open_project_activity);
        addProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Profile.this, AddProject.class));
            }
        });


        // Adding New Friends
        addFriendButton =(Button) findViewById(R.id.open_friend_activity);
        addFriendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Profile.this, AddFriends.class));
            }
        });

        // Adding New Task
        addTaskButton =(Button) findViewById(R.id.open_task_activity);
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Profile.this, AddTask.class));
            }
        });


        // Adding New Class
        addClassButton =(Button) findViewById(R.id.open_add_class_activity);
        addClassButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Profile.this, AddClass.class));
            }
        });


        // LogOut the Session
        logOut =(Button) findViewById(R.id.log_out);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(Profile.this, MainActivity.class));
            }
        });


    }
    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == 1 && resultCode==RESULT_OK){
                finish();
                overridePendingTransition(0,0);
                startActivity(getIntent());
                overridePendingTransition(0,0);
        }
    }



}