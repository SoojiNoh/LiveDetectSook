package com.n0xx1.livedetect.entitydetection;

public class Entity {

    final String imageUrl;
    final String title;
    final String subtitle;
    final String description;

    Entity(String imageUrl, String title, String subtitle, String description) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.subtitle = subtitle;
        if (description != null)
            this.description = description;
        else
            this.description = " ";
    }
}