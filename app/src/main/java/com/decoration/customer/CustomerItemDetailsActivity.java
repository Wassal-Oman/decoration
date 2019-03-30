package com.decoration.customer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.decoration.R;
import com.decoration.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomerItemDetailsActivity extends AppCompatActivity {

    // widgets
    CircleImageView ivItem;
    TextView tvName, tvWidth, tvHeight, tvColor, tvPrice, tvSeller;
    EditText etCount;

    // attributes
    String id, name, width, height, color, price, image, user_id;
    int count = 1;
    final int MAP_CODE = 123;
    float lat , lng;

    // firebase
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_item_details);

        // initialize
        tvName = findViewById(R.id.tv_product_name);
        tvWidth = findViewById(R.id.tv_product_width);
        tvHeight = findViewById(R.id.tv_product_height);
        tvColor = findViewById(R.id.tv_product_color);
        tvPrice = findViewById(R.id.tv_product_price);
        tvSeller = findViewById(R.id.tv_product_seller);
        etCount = findViewById(R.id.et_count);
        ivItem = findViewById(R.id.iv_product_image);

        db = FirebaseFirestore.getInstance();

        // check for passed data
        if(getIntent() != null) {
            id = getIntent().getStringExtra("id");
            name = getIntent().getStringExtra("name");
            width = getIntent().getStringExtra("width");
            height = getIntent().getStringExtra("height");
            color = getIntent().getStringExtra("color");
            price = getIntent().getStringExtra("price");
            image = getIntent().getStringExtra("image");
            user_id = getIntent().getStringExtra("user_id");
        }

        // set widgets
        tvName.setText(name);
        tvWidth.setText(width);
        tvHeight.setText(height);
        tvColor.setText(color);
        tvPrice.setText(price);

        // set count
        etCount.setText(String.valueOf(count));

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
                    tvSeller.setText(user.getName());
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

    // method to trigger map button
    public void goToMap(View view) {
        Intent intent = new Intent(this, CustomerMapActivity.class);
        startActivityForResult(intent, MAP_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check for result
        if(requestCode == MAP_CODE && resultCode == Activity.RESULT_OK) {
            if(data != null) {
                lat = data.getFloatExtra("latitude", 0.0f);
                lng = data.getFloatExtra("longitude", 0.0f);
            } else {
                Toast.makeText(this, "No location was selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // method to trigger subtract button
    public void subtract(View view) {
        if(count > 1) {
            count -= 1;
        } else {
            count = 1;
        }

        etCount.setText(String.valueOf(count));
    }

    // method to trigger add button
    public void add(View view) {
        count += 1;
        etCount.setText(String.valueOf(count));
    }

    // method to trigger order now button
    public void orderProduct(View view) {
        if(lat != 0.0f && lng != 0.0f) {
            Intent intent = new Intent(this, CustomerPaymentActivity.class);
            intent.putExtra("id", id);
            intent.putExtra("user_id", user_id);
            intent.putExtra("count", count);
            intent.putExtra("latitude", lat);
            intent.putExtra("longitude", lng);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Location has not been selected", Toast.LENGTH_SHORT).show();
        }
    }
}
