package com.decoration.seller;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.decoration.R;
import com.decoration.customer.CustomerItemDetailsActivity;
import com.decoration.models.Item;
import com.decoration.models.Order;
import com.decoration.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SellerOrderListActivity extends AppCompatActivity {

    // widgets
    ListView lvOrders;
    SearchView searchView;

    // data
    List<Order> orders;
    MyAdapter adapter;

    // firebase
    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_order_list);

        // initialize
        lvOrders = findViewById(R.id.lv_orders);
        searchView = findViewById(R.id.search_view);
        orders = new ArrayList<>();

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
        loadListOfOrders();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    // method to load orders
    private void loadListOfOrders() {
        // clear list
        orders.clear();
        lvOrders.setAdapter(null);

        // fetch all items from database
        final ProgressDialog dialog = ProgressDialog.show(this, "Loading Items", "Please wait...", false, false);
        db.collection("orders").whereEqualTo("seller_id", auth.getCurrentUser().getUid()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                dialog.dismiss();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Order order = new Order();
                    order.setId(document.getId());
                    order.setUser_id(document.get("user_id").toString());
                    order.setSeller_id(document.get("seller_id").toString());
                    order.setProduct_id(document.get("product_id").toString());
                    order.setCount(document.get("count").toString());
                    order.setLatitude(document.get("latitude").toString());
                    order.setLongitude(document.get("longitude").toString());
                    order.setPayment_status(document.get("payment_status").toString());
                    order.setUser_name(getUserName(document.get("user_id").toString()));
                    order.setProduct_name(getProductName(document.get("product_id").toString()));
                    orders.add(order);
                }

                // view list of items
                viewListOfOrders(orders);
            }
        });
    }

    // method to fetch customer name
    private String getUserName(String user_id) {
        // first clear any data in shared preferences
        SharedPreferences pref = getSharedPreferences("USER", MODE_PRIVATE);
        final SharedPreferences.Editor editor = pref.edit();

        // load data from database
        db.collection("users").document(user_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                User user = documentSnapshot.toObject(User.class);

                // store data inside shared preferences
                editor.putString("name", user.getName());
                editor.apply();
            }
        });

        // retrieve data from shared preferences
        Log.d("USER NAME", pref.getString("name", ""));
        return pref.getString("name", "");
    }

    // method to fetch customer name
    private String getProductName(String product_id) {
        // first clear any data in shared preferences
        SharedPreferences pref = getSharedPreferences("ITEM", MODE_PRIVATE);
        final SharedPreferences.Editor editor = pref.edit();

        // load data from database
        db.collection("items").document(product_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Item item = documentSnapshot.toObject(Item.class);
                // store data inside shared preferences
                editor.putString("name", item.getName());
                editor.apply();
            }
        });

        // retrieve data from shared preferences
        Log.d("ITEM NAME", pref.getString("name", ""));
        return pref.getString("name", "");
    }

    // method to view list of orders
    private void viewListOfOrders(List<Order> orders) {
        // check if orders available
        if(orders.size() > 0) {
            adapter = new MyAdapter(this, 0, orders);
            lvOrders.setTextFilterEnabled(true);
            lvOrders.setAdapter(adapter);
            lvOrders.setDivider(null);

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

            // go to details page based on user selection
            lvOrders.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    // get selected item
                    int pos = (int) adapter.getItemId(position);
                    Order _order = (Order) adapter.getItem(pos);

                    // go to details activity
                    Intent intent = new Intent(SellerOrderListActivity.this, CustomerItemDetailsActivity.class);
                    intent.putExtra("customer_id", _order.getUser_id());
                    intent.putExtra("seller_id", _order.getSeller_id());
                    intent.putExtra("product_id", _order.getProduct_id());
                    intent.putExtra("count", _order.getCount());
                    intent.putExtra("latitude", _order.getLongitude());
                    intent.putExtra("longitude", _order.getLongitude());
                    intent.putExtra("payment_status", _order.getPayment_status());
                    startActivity(intent);
                }
            });

        } else {
            Toast.makeText(this, "No Items Available", Toast.LENGTH_SHORT).show();
        }
    }

    // private class to view data in ListView as custom list items
    class MyAdapter extends ArrayAdapter implements Filterable {

        private List<Order> data;
        private LayoutInflater inflater = null;
        Order order = null;

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
            if (orders == null) {
                id = i;
            } else {
                id = orders.indexOf(data.get(i));
            }
            return id;
        }

        public class ViewHolder{
            public TextView customerName;
            public TextView itemName;
            public TextView count;
        }

        @NonNull
        @Override
        public View getView(int i, View view, @NonNull ViewGroup viewGroup) {
            @SuppressLint("ViewHolder") View v = inflater.inflate(R.layout.custom_order_layout, null);
            MyAdapter.ViewHolder holder = new MyAdapter.ViewHolder();

            if(v != null){
                holder.customerName = v.findViewById(R.id.tv_customer_name);
                holder.itemName = v.findViewById(R.id.tv_item_name);
                holder.count = v.findViewById(R.id.tv_quantity);
            }

            if(data.size() > 0){
                order = data.get(i);
                holder.customerName.setText(order.getUser_name());
                holder.itemName.setText(order.getProduct_name());
                holder.count.setText(order.getCount());
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

                    data = (List<Order>) results.values;
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {

                    FilterResults results = new FilterResults();
                    ArrayList<Order> FilteredArrayNames = new ArrayList<>();

                    // perform your search here using the searchConstraint String.
                    constraint = constraint.toString().toLowerCase();

                    for (int i = 0; i < orders.size(); i++) {
                        Order _order = orders.get(i);
                        if (_order.getProduct_name().toLowerCase().startsWith(constraint.toString()))  {
                            FilteredArrayNames.add(_order);
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
