package com.example.latestissueviewer;

public class BookItem {
    public String title;
    public String author;

    public String itemCode;
    public String salesDate;
    public String itemUrl;
    public String largeImageUrl;

    private boolean isFavorite;

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getItemCode(){
        return itemCode;
    }

    public String getLargeImageUrl() {
        return largeImageUrl;
    }

    public boolean isFavorite(){
        return isFavorite;
    }

    public void toggleFavorite(){
        isFavorite = !isFavorite;
    }
}
