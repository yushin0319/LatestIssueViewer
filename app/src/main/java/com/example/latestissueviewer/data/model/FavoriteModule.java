package com.example.latestissueviewer.data.model;

import io.realm.annotations.RealmModule;

/**
 * Realmの構成をカスタマイズするためのモジュール定義。
 * ここでは Favorite クラスのみを対象にしたRealmモジュールを定義している。
 *
 * → モジュールを使うことで、アプリ内で使う Realm モデルを明示的に指定できる。
 *    例えばモジュールを分けて管理することで、無駄に全モデルをロードしないようにできる。
 */
@RealmModule(classes = { Favorite.class })
public class FavoriteModule {}
