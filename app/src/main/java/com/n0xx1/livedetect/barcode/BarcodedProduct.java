package com.n0xx1.livedetect.barcode;

public class BarcodedProduct {

    final String name;
    final int price;
    final String imageUrl;
    final String siteUrl;



    BarcodedProduct(String name, int price, String imageUrl, String siteUrl) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.siteUrl = siteUrl;

    }

    public String getName(){
        return name;
    }



}
