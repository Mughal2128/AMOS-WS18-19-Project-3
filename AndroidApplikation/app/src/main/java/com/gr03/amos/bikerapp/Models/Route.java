package com.gr03.amos.bikerapp.Models;

import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.LinkingObjects;
import io.realm.annotations.PrimaryKey;

public class Route extends RealmObject {
    @PrimaryKey
    private long id_route;
    private long id_user;
    private String name;
    private String description;
    private Start start;
    private End end;
    private Address end_address;
    private Address start_address;

    @LinkingObjects("route")
    private final RealmResults<Friend> friend = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Start getStart() {
        return start;
    }

    public void setStart(Start start) {
        this.start = start;
    }

    public End getEnd() {
        return end;
    }

    public void setEnd(End end) {
        this.end = end;
    }

    public Address getEndAddress() {
        return end_address;
    }

    public void setEndAddress(Address end_address) {
        this.end_address = end_address;
    }

    public Address getStartAddress() {
        return start_address;
    }

    public void setStartAddress(Address start_address) {
        this.start_address = start_address;
    }

    public long getId_route() {
        return id_route;
    }

    public void setId_route(long id_route) {
        this.id_route = id_route;
    }

    public long getId_user() {
        return id_user;
    }

    public void setId_user(long id_user) {
        this.id_user = id_user;
    }

    public RealmResults<Friend> getFriend() {
        return friend;
    }

}


