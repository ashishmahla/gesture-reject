package com.gesturecaller.models;

/**
 * +Created by Ashish on 2/14/2018.
 */

public class ExceptionContact {

    private int id;
    private String contactName;
    private String contact;

    public ExceptionContact() {
    }

    public ExceptionContact(String contactName, String contact) {
        this.contactName = contactName;
        this.contact = contact;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }
}