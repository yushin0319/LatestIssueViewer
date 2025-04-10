package com.example.latestissueviewer.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 書籍情報を表すデータモデルクラス。
 *
 * Rakuten Books APIのレスポンスで使われるフィールドに対応しており、
 * 書籍のタイトル・著者・ISBNコード・発売日・表紙画像のURLなどを持つ。
 *
 * アプリ内部では、お気に入り状態（isFavorite）も一時的に管理する。
 * ※このフラグはRealmには保存されない一時的な状態として使われる。
 */
public class BookItem {

    // 書籍タイトル
    public String title;

    // 著者名
    public String author;

    // ISBN（APIのJSON上では "isbn" というキーになっている）
    @SerializedName("isbn")
    public String itemCode;

    // 発売日（例："2025年04月12日"）
    public String salesDate;

    // 表紙画像のURL
    public String largeImageUrl;

    // コンストラクタ（手動生成用）
    public BookItem(String title, String author, String salesDate) {
        this.title = title;
        this.author = author;
        this.salesDate = salesDate;
    }

    // Realmでは使わない、一時的な状態管理用
    private boolean isFavorite;

    // --- Getter系 ---
    public String getTitle() { return title; }

    public String getAuthor() { return author; }

    public String getItemCode(){ return itemCode; }

    public String getLargeImageUrl() { return largeImageUrl; }

    public String getSalesDate() { return salesDate; }

    // お気に入りフラグの状態を取得
    public boolean isFavorite(){ return isFavorite; }

    // お気に入りフラグをトグルで切り替え
    public void toggleFavorite(){ isFavorite = !isFavorite; }

    // お気に入りフラグを明示的にセット
    public void setFavorite(boolean favorite){ this.isFavorite = favorite; }
}
