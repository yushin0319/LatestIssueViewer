package com.example.latestissueviewer.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Favorite extends RealmObject {
    @PrimaryKey
    private String itemCode;

    // コンストラクタ
    public Favorite() {}

    public Favorite(String itemCode) {
        this.itemCode = itemCode;
    }

    // Getter/Setter
    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }
}
