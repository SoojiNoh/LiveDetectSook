package com.n0xx1.livedetect.entitysearch;

public class Entity {

    final String imageUrl;
    final String title;
    final String subtitle;

    Entity(String imageUrl, String title, String subtitle) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getTitle(){
        return title;
    }

}