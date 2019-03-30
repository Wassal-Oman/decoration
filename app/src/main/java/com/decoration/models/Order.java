package com.decoration.models;

public class Order {

    // attributes
    private String id;
    private String user_id;
    private String user_name;
    private String product_id;
    private String product_name;
    private String seller_id;
    private String count;
    private String latitude;
    private String longitude;
    private String payment_status;

    // constructors
    public Order() {

    }

    public Order(String id, String user_id, String user_name, String product_id, String product_name, String seller_id, String count, String latitude, String longitude, String payment_status) {
        this.id = id;
        this.user_id = user_id;
        this.user_name = user_name;
        this.product_id = product_id;
        this.product_name = product_name;
        this.seller_id = seller_id;
        this.count = count;
        this.latitude = latitude;
        this.longitude = longitude;
        this.payment_status = payment_status;
    }

    // getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public String getSeller_id() {
        return seller_id;
    }

    public void setSeller_id(String seller_id) {
        this.seller_id = seller_id;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getPayment_status() {
        return payment_status;
    }

    public void setPayment_status(String payment_status) {
        this.payment_status = payment_status;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getProduct_name() {
        return product_name;
    }

    public void setProduct_name(String product_name) {
        this.product_name = product_name;
    }
}
