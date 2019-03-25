package com.decoration.customer;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.decoration.R;
import com.decoration.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomerServiceDetailsActivity extends AppCompatActivity {

    // widgets
    CircleImageView ivService;
    EditText etName, etDesc, etLocation, etPrice, etEngineer;

    // attributes
    String id, name, desc, location, price, image, user_id;

    // firebase
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_service_details);

        // initialize
        etName = findViewById(R.id.et_service_name);
        etDesc = findViewById(R.id.et_service_desc);
        etLocation = findViewById(R.id.et_service_location);
        etPrice = findViewById(R.id.et_service_price);
        etEngineer = findViewById(R.id.et_service_engineer);
        ivService = findViewById(R.id.iv_service_image);

        db = FirebaseFirestore.getInstance();

        etName.setEnabled(false);
        etDesc.setEnabled(false);
        etLocation.setEnabled(false);
        etPrice.setEnabled(false);
        etEngineer.setEnabled(false);
        ivService.setEnabled(false);

        // check for passed data
        if(getIntent() != null) {
            id = getIntent().getStringExtra("id");
            name = getIntent().getStringExtra("name");
            desc = getIntent().getStringExtra("description");
            price = getIntent().getStringExtra("price");
            location = getIntent().getStringExtra("location");
            image = getIntent().getStringExtra("image");
            user_id = getIntent().getStringExtra("user_id");
        }

        // set widgets
        etName.setText(name);
        etDesc.setText(desc);
        etLocation.setText(location);
        etPrice.setText(price);

        // show image
        Picasso.get()
                .load(image)
                .resize(200, 200)
                .centerCrop()
                .into(ivService);

        // load seller name
        loadSellerName();
    }

    private void loadSellerName() {
        // progress dialog
        final ProgressDialog dialog = ProgressDialog.show(this, "Loading Service Details", "Please wait...", false, false);

        // load from database
        db.collection("users").document(user_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                dialog.dismiss();

                // get user details
                User user = documentSnapshot.toObject(User.class);

                // check if user exists
                if(user != null) {
                    etEngineer.setText(user.getName());
                } else {
                    finish();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Toast.makeText(CustomerServiceDetailsActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // method to trigger back button
    public void goBack(View view) {
        finish();
    }

    // method to trigger make appointment button
    public void makeAppointment(View view) {

    }
}
