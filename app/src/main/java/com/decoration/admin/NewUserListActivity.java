package com.decoration.admin;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.decoration.R;
import com.decoration.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class NewUserListActivity extends AppCompatActivity {

    // widgets7
    ListView lvUsers;
    SearchView searchView;

    // data
    List<User> users;
    MyAdapter adapter;

    // firebase
    private FirebaseFirestore db;

    // attributes
    String status;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user_list);

        // initialize
        lvUsers = findViewById(R.id.lv_users);
        searchView = findViewById(R.id.search_view);
        users = new ArrayList<>();

        db = FirebaseFirestore.getInstance();

        // get type of user
        if(getIntent() != null) {
            status = getIntent().getStringExtra("status");
        }

        // enable home button
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // clear old data
        users.clear();

        // load user of users based on passed value
        if(!status.isEmpty()) {
            final ProgressDialog dialog = ProgressDialog.show(this, "Loading Users", "Please wait...", false, false);
            db.collection("users").whereEqualTo("status", status).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    dialog.dismiss();
                    if(task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            users.add(user);
                        }

                        // load list of users
                        loadListOfUsers(users);

                    } else {
                        Toast.makeText(NewUserListActivity.this, "Failed To Load List of Users", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            });
        } else {
            Toast.makeText(this, "No Users To Load", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadListOfUsers(final List<User> users) {

        adapter = new MyAdapter(this, 0, users);
        lvUsers.setTextFilterEnabled(true);
        lvUsers.setAdapter(adapter);

        // search through ListView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                // filter
                adapter.getFilter().filter(s);
                return false;
            }
        });

        // detect changes in Array Adapter
        adapter.notifyDataSetChanged();


        // add click listener to next button
        lvUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // get user details
                User _user = users.get(position);

                // go to details page
                Intent intent = new Intent(NewUserListActivity.this, UserAdmissionActivity.class);
                intent.putExtra("id", _user.getId());
                intent.putExtra("name", _user.getName());
                intent.putExtra("email", _user.getEmail());
                intent.putExtra("phone", _user.getPhone());
                intent.putExtra("password", _user.getPassword());
                intent.putExtra("type", _user.getType());
                intent.putExtra("status", _user.getStatus());

                startActivity(intent);
            }
        });
    }

    // private class to view data in ListView as custom list items
    class MyAdapter extends ArrayAdapter implements Filterable {

        private List<User> data;
        private LayoutInflater inflater = null;
        User user = null;

        public MyAdapter(@NonNull Context context, int resource, @NonNull List list) {
            super(context, resource, list);
            this.data = list;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public long getItemId(int i) {
            int id;

            // original list will be null only if we haven't filtered yet
            if (users == null) {
                id = i;
            } else {
                id = users.indexOf(data.get(i));
            }
            return id;
        }

        public class ViewHolder{

            public ImageView next;
            public TextView name;
        }

        @NonNull
        @Override
        public View getView(int i, View view, @NonNull ViewGroup viewGroup) {
            @SuppressLint("ViewHolder") View v = inflater.inflate(R.layout.custom_user_item_layout, null);
            MyAdapter.ViewHolder holder = new MyAdapter.ViewHolder();

            if(v != null){
                holder.next = v.findViewById(R.id.iv_next);
                holder.name = v.findViewById(R.id.tv_user_name);
            }

            if(data.size() <= 0){
                Toast.makeText(NewUserListActivity.this, "No Data Available", Toast.LENGTH_LONG).show();
            } else {
                user = data.get(i);
                holder.name.setText(user.getName());
            }

            return v;
        }

        @Override
        public Filter getFilter() {

            // return filtered list
            return new Filter() {
                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {

                    data = (List<User>) results.values;
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    FilterResults results = new FilterResults();
                    ArrayList<User> FilteredArrayNames = new ArrayList<>();

                    // perform your search here using the searchConstraint String.
                    constraint = constraint.toString().toLowerCase();

                    for (int i = 0; i < users.size(); i++) {
                        User _user = users.get(i);
                        if (_user.getName().toLowerCase().startsWith(constraint.toString()))  {
                            FilteredArrayNames.add(_user);
                        }
                    }

                    results.count = FilteredArrayNames.size();
                    results.values = FilteredArrayNames;

                    return results;
                }
            };
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }
    }
}
