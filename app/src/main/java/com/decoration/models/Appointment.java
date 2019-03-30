package com.decoration.models;

public class Appointment {

    // attributes
    private String id;
    private String user_id;
    private String engineer_id;
    private String service_id;
    private String appointment_date;
    private String appointment_time;
    private String appointment_status;
    private String customer_name;

    // constructors
    public Appointment() {

    }

    public Appointment(String id,String user_id, String engineer_id, String service_id, String appointment_date, String appointment_time, String appointment_status, String customer_name) {
        this.id = id;
        this.user_id = user_id;
        this.engineer_id = engineer_id;
        this.service_id = service_id;
        this.appointment_date = appointment_date;
        this.appointment_time = appointment_time;
        this.appointment_status = appointment_status;
        this.customer_name = customer_name;
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

    public String getEngineer_id() {
        return engineer_id;
    }

    public void setEngineer_id(String engineer_id) {
        this.engineer_id = engineer_id;
    }

    public String getService_id() {
        return service_id;
    }

    public void setService_id(String service_id) {
        this.service_id = service_id;
    }

    public String getAppointment_date() {
        return appointment_date;
    }

    public void setAppointment_date(String appointment_date) {
        this.appointment_date = appointment_date;
    }

    public String getAppointment_time() {
        return appointment_time;
    }

    public void setAppointment_time(String appointment_time) {
        this.appointment_time = appointment_time;
    }

    public String getAppointment_status() {
        return appointment_status;
    }

    public void setAppointment_status(String appointment_status) {
        this.appointment_status = appointment_status;
    }

    public String getCustomer_name() {
        return customer_name;
    }

    public void setCustomer_name(String customer_name) {
        this.customer_name = customer_name;
    }
}
