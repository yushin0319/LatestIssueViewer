package com.example.latestissueviewer;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.latestissueviewer.adapter.BookAdapter;
import com.example.latestissueviewer.adapter.UpcomingBookAdapter;
import com.example.latestissueviewer.data.model.BookItem;
import com.example.latestissueviewer.data.model.BookResponse;
import com.example.latestissueviewer.data.model.Favorite;
import com.example.latestissueviewer.util.NotificationUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.realm.Realm;
import io.realm.RealmResults;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.example.latestissueviewer.BuildConfig;

public class MainActivity extends AppCompatActivity {

    String appID = BuildConfig.RAKUTEN_APP_ID;

    // UI部品：検索ワード入力欄と検索ボタン
    private EditText searchInput;
    private Button searchButton;

    // 検索結果表示用リストとアダプター（RecyclerView用）
    private List<BookItem> bookList = new ArrayList<>();
    private BookAdapter adapter;

    // 新刊表示用リストとアダプター（RecyclerView用）
    private List<BookItem> upcomingList = new ArrayList<>();
    private UpcomingBookAdapter upcomingAdapter;

    // お気に入り作家のカウント管理（author名 → 登録数）
    // 同じ著者を複数冊お気に入りにしてる時の参照数として使う
    private final HashMap<String, Integer> favAuthorsMap = new HashMap<>();

    // 作家名 → 新刊リスト（BookItem）のマップ
    private final HashMap<String, List<BookItem>> upcomingBooksMap = new HashMap<>();

    // HTTPクライアント（楽天APIの通信に使用）
    private final OkHttpClient client = new OkHttpClient();

    // "2025年04月12日" の形式で日付をパースするためのフォーマッタ
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.JAPAN);

    // 新刊情報をJSONで保存するファイル名
    private final String UPCOMING_FILE = "upcoming_books.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Android 12以降：正確なアラームを許可してもらうための設定 ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // 設定画面を開いて、ユーザーに許可してもらう
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        // --- Android 13以降：通知の表示許可をリクエスト（初回起動時など） ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        // --- Androidのエッジ処理：ステータスバーなどの余白を吸収してくれるやつ ---
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // ウィンドウインセット（画面上下のノッチ部分など）に対応するパディング設定
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- UIの初期化（検索系） ---
        searchInput = findViewById(R.id.search_input);
        searchButton = findViewById(R.id.search_button);

        // --- RecyclerViewの初期化（検索結果） ---
        RecyclerView recyclerView = findViewById(R.id.book_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookAdapter(bookList, this::handleFavoriteToggle); // 本のリストとお気に入り操作を渡す
        recyclerView.setAdapter(adapter);

        // --- RecyclerViewの初期化（新刊リスト） ---
        RecyclerView upcomingRecycler = findViewById(R.id.upcoming_list);
        upcomingRecycler.setLayoutManager(new LinearLayoutManager(this));
        upcomingAdapter = new UpcomingBookAdapter(upcomingList);
        upcomingRecycler.setAdapter(upcomingAdapter);

        // 「新刊表示」ボタン押下時の動作：検索結果を非表示にして新刊リストを表示
        findViewById(R.id.show_upcoming_button).setOnClickListener(v -> {
            findViewById(R.id.book_list).setVisibility(View.GONE);
            findViewById(R.id.upcoming_list).setVisibility(View.VISIBLE);
            displayUpcomingFromCache(); // キャッシュをもとに再描画
        });

        // --- 永続保存していた作家データや新刊情報の読み込み ---
        restoreAuthorsFromRealm(); // Realmからお気に入り作家を復元
        loadUpcomingFromFile();   // ファイルから新刊リストを復元
        displayUpcomingFromCache(); // 読み込んだ新刊をUIに表示

        // --- 検索ボタン押下時の検索処理 ---
        searchButton.setOnClickListener(v -> {
            String keyword = searchInput.getText().toString().trim();
            searchBooks(keyword); // 楽天APIで検索
        });

        // --- 毎日12時に通知が飛ぶようにアラームを設定 ---
        NotificationUtils.scheduleDailyNotification(this);
    }

    /**
     * 書籍のお気に入り登録/解除を処理する。
     *
     * - お気に入り登録された場合：
     *     - 作者のカウントを1増やし、初回登録なら新刊情報を取得
     *     - Realmにお気に入りデータを追加
     * - お気に入り解除された場合：
     *     - 作者のカウントを1減らし、0になったらマップから除去＋新刊情報も削除
     *     - Realmから該当のお気に入りを削除
     *
     * @param item 対象の書籍データ
     * @param isFav true：登録、false：解除
     */
    private void handleFavoriteToggle(BookItem item, boolean isFav) {
        String author = item.getAuthor();
        if (author == null || author.isEmpty()) return;

        Realm realm = Realm.getDefaultInstance();

        if (isFav) {
            // 登録処理
            int count = favAuthorsMap.getOrDefault(author, 0);
            favAuthorsMap.put(author, count + 1);
            if (count == 0) fetchUpcomingForAuthor(author); // 初回なら新刊取得

            // Realmに保存（IDが重複しないように itemCode を主キーに）
            realm.executeTransaction(r -> {
                Favorite fav = r.createObject(Favorite.class, item.getItemCode());
                fav.setAuthor(author);
            });

        } else {
            // 解除処理
            int count = favAuthorsMap.getOrDefault(author, 1);
            if (count <= 1) {
                favAuthorsMap.remove(author);
                upcomingBooksMap.remove(author);
                saveUpcomingToFile(); // JSONファイルも更新
            } else {
                favAuthorsMap.put(author, count - 1);
            }

            // Realmから削除
            realm.executeTransaction(r -> {
                Favorite fav = r.where(Favorite.class)
                        .equalTo("id", item.getItemCode())
                        .findFirst();
                if (fav != null) fav.deleteFromRealm();
            });
        }

        realm.close(); // Realmのインスタンスは忘れずにクローズ
    }

    /**
     * Realm に保存されているお気に入りデータ（Favorite）を読み込み、
     * 作者ごとのお気に入り登録数（favAuthorsMap）を復元する。
     *
     * - 主にアプリ起動時に呼び出される
     * - マップは作者名をキーとして、同一作者の書籍が複数お気に入り登録されている場合にカウントを持たせている
     */
    private void restoreAuthorsFromRealm() {
        // Realm のインスタンス取得
        Realm realm = Realm.getDefaultInstance();

        // 保存されている Favorite 全件を取得
        RealmResults<Favorite> favorites = realm.where(Favorite.class).findAll();

        // まずマップをクリア（初期化）
        favAuthorsMap.clear();

        // 各 Favorite エントリから author を取り出してカウントアップ
        for (Favorite fav : favorites) {
            String author = fav.getAuthor();
            if (author != null && !author.isEmpty()) {
                int count = favAuthorsMap.getOrDefault(author, 0);
                favAuthorsMap.put(author, count + 1);
            }
        }

        // Realm クローズ（忘れるとメモリリークになる可能性ある）
        realm.close();
    }

    /**
     * 指定された作家の新刊情報を楽天ブックスAPIから取得し、
     * today以降の発売日の書籍を `upcomingBooksMap` に保存する。
     *
     * - 通信は別スレッドで実行
     * - 書籍の発売日が「今日より後」のものだけフィルター
     * - JSONファイルにも保存し、UIに即時反映
     */
    private void fetchUpcomingForAuthor(String author) {
        // 新しいスレッドで通信処理を開始
        new Thread(() -> {
            try {
                // 楽天APIのURLを作成（著者検索）

                String query = "https://app.rakuten.co.jp/services/api/BooksBook/Search/20170404"
                        + "?applicationId=" + appID + "&author=" + author + "&format=json";

                // HTTPリクエストを実行
                Response response = client.newCall(new Request.Builder().url(query).build()).execute();

                if (response.isSuccessful()) {
                    // レスポンスのJSONを BookResponse にパース
                    String json = response.body().string();
                    BookResponse result = new Gson().fromJson(json, BookResponse.class);
                    List<BookItem> filtered = new ArrayList<>();
                    LocalDate today = LocalDate.now();

                    // 各アイテムの発売日が今日より後ならリストに追加
                    for (BookResponse.ItemWrapper wrapper : result.Items) {
                        BookItem item = wrapper.Item;
                        try {
                            String cleanDate = item.salesDate.replace("頃", ""); // "頃"を取り除く
                            LocalDate date = LocalDate.parse(cleanDate, formatter);
                            if (date.isAfter(today)) filtered.add(item);
                        } catch (Exception e) {
                            Log.w("date-parse", "失敗: " + item.salesDate); // パースできなかったとき
                        }
                    }

                    // 作家ごとの新刊リストとしてキャッシュに保存
                    upcomingBooksMap.put(author, filtered);

                    // JSONファイルにも保存して永続化
                    saveUpcomingToFile();

                    // UIスレッドでリストを更新表示
                    runOnUiThread(this::displayUpcomingFromCache);
                }
            } catch (Exception e) {
                Log.e("fetchUpcoming", "例外: ", e); // 通信エラーなど
            }
        }).start();
    }

    /**
     * `upcomingBooksMap` に格納された新刊情報を JSON に変換し、
     * アプリ内のローカルファイル (`upcoming_books.json`) に保存する。
     *
     * 永続化のために呼び出される（お気に入り削除時など）。
     */
    private void saveUpcomingToFile() {
        try {
            // Map<String, List<BookItem>> → JSON文字列に変換
            String json = new Gson().toJson(upcomingBooksMap);

            // アプリ内ファイルとして保存（MODE_PRIVATE: 自アプリのみ読み書き可能）
            FileOutputStream fos = openFileOutput(UPCOMING_FILE, MODE_PRIVATE);
            fos.write(json.getBytes());
            fos.close();
        } catch (Exception e) {
            // 例外が起きたらログに出す（保存失敗）
            Log.e("saveFile", "保存失敗", e);
        }
    }

    /**
     * アプリ起動時に、ローカルファイルから新刊情報を読み込む処理。
     * 発売日が今日より前の本はフィルタリングして除外し、
     * 未来の本のみ `upcomingBooksMap` に格納する。
     */
    private void loadUpcomingFromFile() {
        try {
            File file = new File(getFilesDir(), UPCOMING_FILE);
            if (!file.exists()) return;

            // ファイルから JSON 文字列を読み込む
            FileInputStream fis = new FileInputStream(file);
            String json = new BufferedReader(new InputStreamReader(fis))
                    .lines().collect(Collectors.joining());

            // author名 → BookItemリスト の構造でデシリアライズ
            Type type = new TypeToken<HashMap<String, List<BookItem>>>() {}.getType();
            HashMap<String, List<BookItem>> loaded = new Gson().fromJson(json, type);

            // 今日の日付（発売済みを除外するために使う）
            LocalDate today = LocalDate.now();

            // フィルタリング済みのデータをここに格納
            HashMap<String, List<BookItem>> cleaned = new HashMap<>();

            for (String author : loaded.keySet()) {
                // 各著者ごとの BookItemリストを日付でフィルタ
                List<BookItem> validBooks = loaded.get(author).stream()
                        .filter(item -> {
                            try {
                                String cleanDate = item.getSalesDate().replace("頃", "");
                                LocalDate date = LocalDate.parse(cleanDate, formatter);
                                return !date.isBefore(today); // 今日より前は除外
                            } catch (Exception e) {
                                // 日付パースに失敗したら残しておく（念のため）
                                return true;
                            }
                        })
                        .collect(Collectors.toList());

                // フィルタ後に1件でも残っていれば map に追加
                if (!validBooks.isEmpty()) {
                    cleaned.put(author, validBooks);
                }
            }

            // クリーンなデータで更新
            upcomingBooksMap.clear();
            upcomingBooksMap.putAll(cleaned);

        } catch (Exception e) {
            Log.e("loadFile", "読み込み失敗", e);
        }
    }

    /**
     * 楽天APIを使ってキーワードで書籍検索を行い、
     * RecyclerView に結果を表示する非同期処理。
     *
     * @param keyword 検索ワード（タイトルなど）
     */
    @SuppressLint("NotifyDataSetChanged")
    private void searchBooks(String keyword) {
        new Thread(() -> {
            try {
                // 楽天ブックスAPIのURLを組み立て（タイトルで検索）
                String query = "https://app.rakuten.co.jp/services/api/BooksBook/Search/20170404"
                        + "?applicationId="+ appID +"&title=" + keyword + "&format=json";

                // APIリクエストを送信
                Response response = client.newCall(new Request.Builder().url(query).build()).execute();
                if (response.isSuccessful()) {
                    // JSONレスポンスをパース
                    String json = response.body().string();
                    BookResponse result = new Gson().fromJson(json, BookResponse.class);

                    // Realmから現在のお気に入り状態をチェック
                    Realm realm = Realm.getDefaultInstance();
                    List<BookItem> books = new ArrayList<>();
                    for (BookResponse.ItemWrapper wrapper : result.Items) {
                        BookItem item = wrapper.Item;

                        // itemCode（ISBN）がRealmに存在すればお気に入り扱い
                        item.setFavorite(realm.where(Favorite.class)
                                .equalTo("id", item.getItemCode()).findFirst() != null);

                        books.add(item);
                    }
                    realm.close();

                    // UIスレッドで RecyclerView に表示を反映
                    runOnUiThread(() -> {
                        bookList.clear();
                        bookList.addAll(books);
                        adapter.notifyDataSetChanged();

                        // 通常の検索結果リストを表示、新刊リストは隠す
                        findViewById(R.id.book_list).setVisibility(View.VISIBLE);
                        findViewById(R.id.upcoming_list).setVisibility(View.GONE);
                    });
                }
            } catch (Exception e) {
                Log.e("search", "Exception: ", e);
            }
        }).start(); // 別スレッドで実行（ネットワーク処理だから）
    }

    /**
     * キャッシュから読み込んだ新刊情報（upcomingBooksMap）を
     * RecyclerView に表示するメソッド。
     * UIスレッドで更新処理を行う。
     */
    @SuppressLint("NotifyDataSetChanged")
    private void displayUpcomingFromCache() {
        runOnUiThread(() -> {
            // 表示用リストを一度クリアして再構築
            upcomingList.clear();
            for (List<BookItem> list : upcomingBooksMap.values()) {
                upcomingList.addAll(list); // 各作家の新刊リストを全部まとめる
            }

            // 発売日が早い順にソート（発売日が不明なものはそのまま）
            upcomingList.sort((a, b) -> {
                try {
                    LocalDate dateA = LocalDate.parse(a.getSalesDate().replace("頃", ""), formatter);
                    LocalDate dateB = LocalDate.parse(b.getSalesDate().replace("頃", ""), formatter);
                    return dateA.compareTo(dateB);
                } catch (Exception e) {
                    return 0; // パースできなければ順序はそのまま
                }
            });

            // アダプターに変更を通知して画面更新
            upcomingAdapter.notifyDataSetChanged();
        });
    }
}