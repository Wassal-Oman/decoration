package com.decoration.models;

public class Item {

    // attributes
    private String id;
    private String name;
    private String price;
    private String color;
    private String width;
    private String height;
    private String count;
    private String image;
    private String user_id;

    // constructors
    public Item() {

    }

    public Item(String id, String name, String price, String color, String width, String height, String count, String image, String user_id) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.color = color;
        this.width = width;
        this.height = height;
        this.count = count;
        this.image = image;
        this.user_id = user_id;
    }

    // getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
}
