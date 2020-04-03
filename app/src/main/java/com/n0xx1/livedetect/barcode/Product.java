package com.n0xx1.livedetect.barcode;

public class Product {

    final String imageUrl;
    final String title;
    final String price;
    Product(String title, String price, String imageUrl) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.price = price;
    }
}