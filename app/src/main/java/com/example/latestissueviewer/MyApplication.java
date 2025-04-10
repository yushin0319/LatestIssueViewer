package com.example.latestissueviewer;

import android.app.Application;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import com.example.latestissueviewer.data.model.FavoriteModule;

// アプリ全体の初期設定を行うクラス（AndroidのApplicationクラスを継承）
public class MyApplication extends Application {

    // アプリ起動時に一度だけ呼ばれるメソッド
    @Override
    public void onCreate() {
        super.onCreate();

        // Realmの初期化（このアプリでRealmを使う宣言）
        Realm.init(this);

        // Realmの構成を定義する
        RealmConfiguration config = new RealmConfiguration.Builder()
                .name("latestissue.realm") // DBファイルの名前
                .schemaVersion(1) // スキーマのバージョン（変更があればアップする）
                .modules(new FavoriteModule()) // 利用するカスタムモジュール（Favoriteクラスだけを使用）
                .allowWritesOnUiThread(true) // UIスレッドでの書き込みを許可
                .deleteRealmIfMigrationNeeded() // マイグレーション失敗時にRealmファイルを削除
                .build();

        // この構成をデフォルト設定として登録
        Realm.setDefaultConfiguration(config);
    }
}
