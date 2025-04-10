package com.example.latestissueviewer.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.latestissueviewer.MainActivity;
import com.example.latestissueviewer.R;
import com.example.latestissueviewer.data.model.BookItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * アラームで発火される通知受け取り用のBroadcastReceiver
 * ・新刊リスト（キャッシュ）を読み込み
 * ・発売日が「指定日数前」と一致する本があれば通知を送信する
 */
public class NotificationReceiver extends BroadcastReceiver {

    private static final String UPCOMING_FILE = "upcoming_books.json";
    private static final String CHANNEL_ID = "book_release_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // 通知設定：何日前に通知するか取得（デフォルトは3日前）
            int daysBefore = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getInt("notify_days_before", 3);

            // キャッシュファイル（JSON）から新刊データを読み込む
            File file = new File(context.getFilesDir(), UPCOMING_FILE);
            if (!file.exists()) return;

            FileInputStream fis = new FileInputStream(file);
            String json = new BufferedReader(new InputStreamReader(fis))
                    .lines().collect(Collectors.joining());

            Type type = new TypeToken<HashMap<String, List<BookItem>>>() {}.getType();
            HashMap<String, List<BookItem>> upcomingBooksMap = new Gson().fromJson(json, type);

            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.JAPAN);

            // 各本の発売日が、通知日と一致するかチェック
            for (List<BookItem> list : upcomingBooksMap.values()) {
                for (BookItem item : list) {
                    try {
                        String cleanDate = item.salesDate.replace("頃", "");
                        LocalDate releaseDate = LocalDate.parse(cleanDate, formatter);
                        if (releaseDate.minusDays(daysBefore).isEqual(today)) {
                            sendNotification(context, item);
                        }
                    } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 通知を実際に作成・表示する処理
     */
    public void sendNotification(Context context, BookItem item) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8以上では通知チャンネルの作成が必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "新刊通知",
                    NotificationManager.IMPORTANCE_HIGH // 重要度：高
            );
            manager.createNotificationChannel(channel);
        }

        // 通知タップ時にMainActivityへ遷移させる
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 通知の内容を構築
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("まもなく発売！")
                .setContentText(item.getTitle() + "（" + item.getSalesDate() + " 発売）")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // タップで通知を消す

        // 通知を表示（itemCodeのハッシュでID一意化）
        manager.notify(item.getItemCode().hashCode(), builder.build());
    }

}
