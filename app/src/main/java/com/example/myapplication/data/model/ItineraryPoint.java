package com.example.myapplication.data.model;

import java.io.Serializable;

public class ItineraryPoint implements Serializable {
    private final String name;
    private final String address;
    private final int position;

    public ItineraryPoint(String name, String address, int position) {
        this.name = name;
        this.address = address;
        this.position = position;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public int getPosition() { return position; }
}

