package com.n0xx1.livedetect.barcode;

public class Entity {

    final String imageUrl;
    final String title;
    final String price;
    Entity(String title, String price, String imageUrl) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.price = price;
    }
}