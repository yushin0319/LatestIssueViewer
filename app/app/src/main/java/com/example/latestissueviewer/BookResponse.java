package com.example.latestissueviewer;
import java.util.List;

public class BookResponse {
    public List<ItemWrapper> Items;

    public static class ItemWrapper{
        public BookItem Item;
    }
}
