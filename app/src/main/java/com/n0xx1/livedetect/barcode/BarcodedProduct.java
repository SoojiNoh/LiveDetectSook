package com.n0xx1.livedetect.barcode;

import android.util.Log;

public class BarcodedProduct {

    final String name;
    final String price;
    final String imageUrl;
    final String siteUrl;



    BarcodedProduct(String name, String price, String imageUrl, String siteUrl) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.siteUrl = siteUrl;
        Log.d("logcat", "***imageUrl: "+imageUrl);

    }

    public String getName(){
        return name;
    }



}
