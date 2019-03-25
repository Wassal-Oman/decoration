package com.decoration.customer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.decoration.R;
import com.decoration.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomerItemDetailsActivity extends AppCompatActivity {

    // widgets
    CircleImageView ivItem;
    EditText etName, etWidth, etHeight, etColor, etPrice, etSeller;

    // attributes
    String id, name, width, height, color, price, count, image, user_id;

    // firebase
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_item_details);

        // initialize
        etName = findViewById(R.id.et_item_name);
        etWidth = findViewById(R.id.et_item_width);
        etHeight = findViewById(R.id.et_item_height);
        etColor = findViewById(R.id.et_item_location);
        etPrice = findViewById(R.id.et_item_price);
        etSeller = findViewById(R.id.et_item_seller);
        ivItem = findViewById(R.id.iv_item_image);

        db = FirebaseFirestore.getInstance();

        etName.setEnabled(false);
        etWidth.setEnabled(false);
        etHeight.setEnabled(false);
        etColor.setEnabled(false);
        etPrice.setEnabled(false);
        etSeller.setEnabled(false);
        ivItem.setEnabled(false);

        // check for passed data
        if(getIntent() != null) {
            id = getIntent().getStringExtra("id");
            name = getIntent().getStringExtra("name");
            width = getIntent().getStringExtra("width");
            height = getIntent().getStringExtra("height");
            color = getIntent().getStringExtra("color");
            price = getIntent().getStringExtra("price");
            count = getIntent().getStringExtra("count");
            image = getIntent().getStringExtra("image");
            user_id = getIntent().getStringExtra("user_id");
        }

        // set widgets
        etName.setText(name);
        etWidth.setText(width);
        etHeight.setText(height);
        etColor.setText(color);
        etPrice.setText(price);

        // show image
        Picasso.get()
                .load(image)
                .resize(200, 200)
                .centerCrop()
                .into(ivItem);

        // load seller name
        loadSellerName();
    }

    // load to seller name
    private void loadSellerName() {
        // progress dialog
        final ProgressDialog dialog = ProgressDialog.show(this, "Loading Item Details", "Please wait...", false, false);

        // load from database
        db.collection("users").document(user_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                dialog.dismiss();

                // get user details
                User user = documentSnapshot.toObject(User.class);

                // check if user exists
                if(user != null) {
                    etSeller.setText(user.getName());
                } else {
                    finish();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Toast.makeText(CustomerItemDetailsActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // method to trigger back button
    public void goBack(View view) {
        finish();
    }

    // method to trigger order now button
    public void orderProduct(View view) {
        Intent intent = new Intent(this, CustomerOrderActivity.class);
        intent.putExtra("id", id);
        intent.putExtra("user_id", user_id);
        startActivity(intent);
    }
}
