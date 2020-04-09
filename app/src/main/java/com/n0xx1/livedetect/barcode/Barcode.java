package com.n0xx1.livedetect.barcode;

public class Barcode {

    final String name;
    final String barcode;
    final String description;


    Barcode(String name, String barcode, String description) {
        this.name = name;
        this.barcode = barcode;
        this.description = description;
    }

    public String getName(){
        return name;
    }

    public String getDescription(){
        return description;
    }

}
