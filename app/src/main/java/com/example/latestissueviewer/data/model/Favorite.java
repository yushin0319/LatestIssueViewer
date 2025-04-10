package com.example.latestissueviewer.data.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * Realmに保存するお気に入り本のモデルクラス。
 * 各お気に入りはISBN（itemCode）をidとして一意に管理。
 */
@RealmClass
public class Favorite extends RealmObject {

    // 本の一意な識別子（楽天APIのISBN）。主キーとして指定。
    @PrimaryKey
    private String id;

    // 著者名。通知や新刊リストの取得に使われる。
    private String author;

    // Realm用に必要なデフォルトコンストラクタ
    public Favorite() {}

    // idのGetter
    public String getId() { return id; }

    // authorのGetter
    public String getAuthor(){ return author; }

    // authorのSetter
    public void setAuthor(String author){ this.author = author; }
}
