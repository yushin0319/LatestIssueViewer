package com.example.latestissueviewer.data.model;

import java.util.List;

/**
 * 楽天ブックスAPIのレスポンスを受け取るためのクラス。
 * Items配列の中にItemというキーがあり、その中身がBookItemに対応している。
 */
public class BookResponse {
    public List<ItemWrapper> Items;

    public static class ItemWrapper {
        public BookItem Item;
    }
}