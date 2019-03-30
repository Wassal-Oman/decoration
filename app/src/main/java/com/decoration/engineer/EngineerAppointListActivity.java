package com.decoration.engineer;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.decoration.R;
import com.decoration.models.Appointment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class EngineerAppointListActivity extends AppCompatActivity {

    // widgets
    ListView lvAppointments;

    // data
    List<Appointment> appointments;
    MyAdapter adapter;

    // firebase
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_engineer_appoint_list);

        // initialize
        lvAppointments = findViewById(R.id.lv_appointments);
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

        // load list of items
        loadListOfAppointments();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadListOfAppointments() {
        // clear list
        appointments.clear();
        lvAppointments.setAdapter(null);

        // fetch all items from database
        final ProgressDialog dialog = ProgressDialog.show(this, "Loading Appointments", "Please wait...", false, false);
        db.collection("appointments").whereEqualTo("engineer_id", auth.getCurrentUser().getUid()).whereEqualTo("appointment_status", "Not Accept").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                dialog.dismiss();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Appointment appointment = new Appointment();
                    appointment.setId(document.getId());
                    appointment.setUser_id(document.get("user_id").toString());
                    appointment.setEngineer_id(document.get("engineer_id").toString());
                    appointment.setService_id(document.get("service_id").toString());
                    appointment.setAppointment_date(document.get("appointment_date").toString());
                    appointment.setAppointment_time(document.get("appointment_time").toString());
                    appointment.setAppointment_status(document.get("appointment_status").toString());
                    appointments.add(appointment);
                }

                // view list of items
                viewListOfAppointments(appointments);
            }
        });
    }

    private void viewListOfAppointments(List<Appointment> appointments) {
        // check if appointments available
        if(appointments.size() > 0) {
            adapter = new MyAdapter(this, 0, appointments);
            lvAppointments.setAdapter(adapter);
            lvAppointments.setDivider(null);

            // detect changes in Array Adapter
            adapter.notifyDataSetChanged();

            // go to details page based on user selection
            lvAppointments.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    // get selected item
                    int pos = (int) adapter.getItemId(position);
                    Appointment _appointment = (Appointment) adapter.getItem(pos);

                    // go to details activity
                    Intent intent = new Intent(EngineerAppointListActivity.this, EngineerAppointDetailsActivity.class);
                    intent.putExtra("id", _appointment.getId());
                    intent.putExtra("customer_id", _appointment.getUser_id());
                    intent.putExtra("engineer_id", _appointment.getEngineer_id());
                    intent.putExtra("service_id", _appointment.getService_id());
                    intent.putExtra("appointment_date", _appointment.getAppointment_date());
                    intent.putExtra("appointment_time", _appointment.getAppointment_time());
                    intent.putExtra("appointment_status", _appointment.getAppointment_status());
                    startActivity(intent);
                }
            });

        } else {
            Toast.makeText(this, "No Appointments Available", Toast.LENGTH_SHORT).show();
        }
    }

    // private class to view data in ListView as custom list items
    class MyAdapter extends ArrayAdapter {

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
            public TextView date;
            public TextView time;
        }

        @NonNull
        @Override
        public View getView(int i, View view, @NonNull ViewGroup viewGroup) {
            @SuppressLint("ViewHolder") View v = inflater.inflate(R.layout.custom_appointment_layout, null);
            MyAdapter.ViewHolder holder = new MyAdapter.ViewHolder();

            if(v != null){
                holder.date = v.findViewById(R.id.tv_appointment_date);
                holder.time = v.findViewById(R.id.tv_appointment_time);
            }

            if(data.size() > 0){
                appointment = data.get(i);
                holder.date.setText(appointment.getAppointment_date());
                holder.time.setText(appointment.getAppointment_time());
            }

            return v;
        }

    }
}
