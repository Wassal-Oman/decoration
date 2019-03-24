package com.decoration.admin;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.decoration.HttpHelper;
import com.decoration.R;
import com.decoration.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class UserDetailsActivity extends AppCompatActivity {

    // widgets
    TextView tvName, tvEmail, tvPhone, tvType, tvStatus;

    // attributes
    String id, name, email, phone, type, status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        // initialize
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvType = findViewById(R.id.tv_type);
        tvStatus = findViewById(R.id.tv_status);

        // enable home button
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // get passed data
        if(getIntent() != null) {
            id = getIntent().getStringExtra("id");
            name = getIntent().getStringExtra("name");
            email = getIntent().getStringExtra("email");
            phone = getIntent().getStringExtra("phone");
            type = getIntent().getStringExtra("type");
            status = getIntent().getStringExtra("status");
        }

        // set TextView values
        tvName.setText(name);
        tvEmail.setText(email);
        tvPhone.setText(phone);
        tvType.setText(type);
        tvStatus.setText(status);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    // method to handle delete button click
    public void deleteUser(View view) {
        if(!id.isEmpty()) {
            // show alert dialog
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UserDetailsActivity.this);
            alertDialogBuilder.setMessage("Are you sure you want to delete this user?");
            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    delete(id);
                }
            });
            alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        } else {
            Toast.makeText(this, "Cannot delete this user", Toast.LENGTH_SHORT).show();
        }
    }

    // method to send a http request to delete user
    public void delete(final String id) {
        // check for internet
        if(HttpHelper.isOnline(this)) {

            // show progress dialog
            final ProgressDialog dialog = ProgressDialog.show(this, "Deleting user in process", "Please wait...", false, false);

            // make http request
            StringRequest request = new StringRequest(Request.Method.POST, HttpHelper.DELETE_USER_URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    dialog.dismiss();
                    Log.d("RESPONSE", response);

                    if(response.length() > 0) {
                        try {
                            // get json response and parse it
                            JSONObject json = new JSONObject(response);
                            String status = json.getString("status");
                            String message = json.getString("message");

                            Toast.makeText(UserDetailsActivity.this, message, Toast.LENGTH_SHORT).show();

                            if(status.equals("success")) {
                                finish();
                            }

                        } catch (JSONException e) {
                            Log.d("JSON EXCEPTION", "Cannot read JSON Object");
                        }
                    } else {
                        Log.d("RESPONSE", "No Response");
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dialog.dismiss();
                    Log.d("ERROR", String.valueOf(error));
                    Toast.makeText(UserDetailsActivity.this, "Cannot connect to firebase function", Toast.LENGTH_LONG).show();
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("id", id);
                    return params;
                }
            };

            // request policy
            request.setRetryPolicy(new DefaultRetryPolicy(0, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            // add request to request queue
            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }
    }
}
