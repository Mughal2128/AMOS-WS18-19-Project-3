package com.gr03.amos.bikerapp.Models;
import io.realm.RealmObject;

public class End extends RealmObject{
    private Address address;
    public Address getAddress() {
        return address;
    }
    public void setAddress(Start start) {
        this.address = address;
    }

}
