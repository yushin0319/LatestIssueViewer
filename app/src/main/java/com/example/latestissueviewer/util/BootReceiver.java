package com.example.latestissueviewer.util;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 端末再起動時に呼び出されるBroadcastReceiver。
 * 再起動後はアラームがリセットされるため、通知スケジュールを再設定する。
 */
public class BootReceiver extends BroadcastReceiver {

    @SuppressLint("ScheduleExactAlarm")
    @Override
    public void onReceive(Context context, Intent intent) {
        // 再起動完了のインテントを受け取ったら、通知スケジュールを再設定する
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            NotificationUtils.scheduleDailyNotification(context);
        }
    }
}
