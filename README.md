# LatestIssueViewer

楽天ブックスAPIを使った新刊検索アプリ。  
お気に入り登録・新刊通知などの機能があります。

## 技術スタック
- Java / Kotlin（Android）
- Realm
- OkHttp
- Gson
- Glide
- 楽天ブックスAPI

## セットアップ
1. `gradle.properties` に `RAKUTEN_APP_ID=your_api_key` を設定
2. Android Studio でプロジェクトを開いてビルド

## 主な機能
- 本の検索
- お気に入り登録（ローカルDB）
- お気に入り著者の新刊一覧表示
- ローカル通知による発売日お知らせ
