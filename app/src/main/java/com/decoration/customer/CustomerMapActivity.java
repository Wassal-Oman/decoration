package com.decoration.customer;

import android.app.Activity;
import android.content.Intent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.MenuItem;
import android.view.View;

import com.decoration.HttpHelper;
import com.decoration.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class CustomerMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // attributes
    private GoogleMap mMap;
    SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        // initialize
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // get default toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // add a marker
        LatLng latLng = new LatLng(HttpHelper.LATITUDE, HttpHelper.LONGITUDE);
        MarkerOptions marker = new MarkerOptions().position(latLng).title("My Location");

        // config map
        mMap.addMarker(marker);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    // method to trigger select location button
    public void selectLocation(View view) {
        Intent intent = new Intent();
        intent.putExtra("latitude", HttpHelper.LATITUDE);
        intent.putExtra("longitude", HttpHelper.LONGITUDE);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
