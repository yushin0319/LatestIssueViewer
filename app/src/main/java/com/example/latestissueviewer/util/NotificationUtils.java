package com.example.latestissueviewer.util;

// Androidで通知やアラーム、設定を扱うためのクラスたち
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.RequiresPermission;

import java.util.Calendar;

/**
 * 通知関連のユーティリティクラス
 * ・アラームで毎日決まった時間に通知処理を呼び出す
 * ・通知何日前かの設定値の管理
 */
public class NotificationUtils {

    /**
     * 毎日決まった時間（12:00）に通知処理をスケジュールする
     * AlarmManager + PendingIntent + BroadcastReceiver を組み合わせて実現
     */
    @SuppressLint("ScheduleExactAlarm")
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    public static void scheduleDailyNotification(Context context) {
        // アラームサービスを取得
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // 通知用のBroadcastを送るIntentを作成
        Intent intent = new Intent(context, NotificationReceiver.class);

        // それをOSに預けるPendingIntentとして生成
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 現在時刻から今日の12:00を計算
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long triggerTime = calendar.getTimeInMillis();

        // すでに今日の12:00を過ぎてたら、明日にずらす
        if (System.currentTimeMillis() > triggerTime) {
            triggerTime += AlarmManager.INTERVAL_DAY;
        }

        // 毎日同じ時間に通知処理を起動（BroadcastReceiver発火）
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,            // スリープ状態でも起動
                triggerTime,                         // 初回の起動時間
                AlarmManager.INTERVAL_DAY,           // 毎日の繰り返し
                pendingIntent                        // 実行内容
        );
    }

    /**
     * SharedPreferencesから「何日前に通知するか」の設定値を取得
     * デフォルトは3日前
     */
    public static int getNotificationDaysBefore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getInt("notify_days_before", 3); // デフォルトは3日前
    }
}
