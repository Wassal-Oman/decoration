package com.decoration.engineer;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

import com.decoration.R;
import com.decoration.models.Appointment;
import com.decoration.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class EngineerAcceptedAppointListActivity extends AppCompatActivity {

    // widgets
    ListView lvAppointments;
    SearchView searchView;

    // data
    List<Appointment> appointments;
    MyAdapter adapter;

    // firebase
    FirebaseAuth auth;
    FirebaseFirestore db;

    // attributes
    String name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_engineer_accepted_appoint_list);

        // initialize
        lvAppointments = findViewById(R.id.lv_accepted_appointments);
        searchView = findViewById(R.id.search_view);
        appointments = new ArrayList<>();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // check for default toolbar
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // load list of appointments
        loadListOfAppointments();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    // method to load accepted appointments
    private void loadListOfAppointments() {
        // clear list
        appointments.clear();
        lvAppointments.setAdapter(null);

        // fetch all items from database
        final ProgressDialog dialog = ProgressDialog.show(this, "Loading Appointments", "Please wait...", false, false);
        db.collection("appointments").whereEqualTo("appointment_status", "Accept").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                dialog.dismiss();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    final Appointment appointment = new Appointment();
                    appointment.setId(document.getId());
                    appointment.setUser_id(document.get("user_id").toString());
                    appointment.setEngineer_id(document.get("engineer_id").toString());
                    appointment.setService_id(document.get("service_id").toString());
                    appointment.setAppointment_date(document.get("appointment_date").toString());
                    appointment.setAppointment_time(document.get("appointment_time").toString());
                    appointment.setAppointment_status(document.get("appointment_status").toString());
                    appointment.setCustomer_name(getCustomerName(document.get("user_id").toString()));
                    appointments.add(appointment);
                }

                // view list of items
                viewListOfAppointments(appointments);
            }
        });
    }

    private String getCustomerName(String user_id) {

        // first clear any data in shared preferences
        SharedPreferences pref = getSharedPreferences("USER", MODE_PRIVATE);
        final SharedPreferences.Editor editor = pref.edit();

        // load customer
        db.collection("users").document(user_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = documentSnapshot.toObject(User.class);

                // store data inside shared preferences
                editor.putString("name", user.getName());
                editor.apply();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("USER ERROR", e.getMessage());
            }
        });

        // retrieve data from shared preferences
        Log.d("USER NAME", pref.getString("name", ""));
        return pref.getString("name", "");
    }

    // method to view list of accepted appointments
    private void viewListOfAppointments(List<Appointment> appointments) {
        // check if appointments available
        if(appointments.size() > 0) {
            adapter = new MyAdapter(this, 0, appointments);
            lvAppointments.setTextFilterEnabled(true);
            lvAppointments.setAdapter(adapter);
            lvAppointments.setDivider(null);

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
        }
    }

    // private class to view data in ListView as custom list items
    class MyAdapter extends ArrayAdapter implements Filterable {

        private List<Appointment> data;
        private LayoutInflater inflater = null;
        Appointment appointment = null;

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
            if (appointments == null) {
                id = i;
            } else {
                id = appointments.indexOf(data.get(i));
            }
            return id;
        }

        public class ViewHolder{
            public TextView customer;
            public TextView date;
            public TextView time;
        }

        @NonNull
        @Override
        public View getView(int i, View view, @NonNull ViewGroup viewGroup) {
            @SuppressLint("ViewHolder") View v = inflater.inflate(R.layout.custom_accepted_appointment_list, null);
            MyAdapter.ViewHolder holder = new MyAdapter.ViewHolder();

            if(v != null){
                holder.customer = v.findViewById(R.id.tv_customer_name);
                holder.date = v.findViewById(R.id.tv_date);
                holder.time = v.findViewById(R.id.tv_time);
            }

            if(data.size() > 0){
                appointment = data.get(i);
                holder.customer.setText(appointment.getCustomer_name());
                holder.date.setText(appointment.getAppointment_date());
                holder.time.setText(appointment.getAppointment_time());
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

                    data = (List<Appointment>) results.values;
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    FilterResults results = new FilterResults();
                    ArrayList<Appointment> FilteredArrayNames = new ArrayList<>();

                    // perform your search here using the searchConstraint String.
                    constraint = constraint.toString().toLowerCase();

                    for (int i = 0; i < appointments.size(); i++) {
                        Appointment _appointment = appointments.get(i);
                        if (_appointment.getCustomer_name().toLowerCase().startsWith(constraint.toString()))  {
                            FilteredArrayNames.add(_appointment);
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
