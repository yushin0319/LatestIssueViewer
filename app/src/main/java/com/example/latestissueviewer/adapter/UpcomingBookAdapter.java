package com.example.latestissueviewer.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.latestissueviewer.R;
import com.example.latestissueviewer.data.model.BookItem;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerViewで新刊情報を表示するためのアダプター。
 *
 * 1行目に通知設定用のSpinner（ヘッダー）を表示し、それ以降にBookItemのリストを表示する。
 * ヘッダー行ではSharedPreferencesと連携して、通知の何日前にリマインドを送るか設定可能。
 */
public class UpcomingBookAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ヘッダー行と通常行の識別用定数
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    // 新刊本リスト（通常の BookItem 一覧）
    private final List<BookItem> upcomingBooks;

    public UpcomingBookAdapter(List<BookItem> books) {
        this.upcomingBooks = books;
    }

    // 位置に応じてビュータイプ（ヘッダーかアイテムか）を返す
    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    // ヘッダー分を含めたアイテム数を返す
    @Override
    public int getItemCount() {
        return upcomingBooks.size() + 1; // 1行分はヘッダー用
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ビュータイプに応じて適切な ViewHolder を返す
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.notification_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_upcoming, parent, false);
            return new UpcomingBookViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // 通常アイテムの場合のみデータバインドする
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            // ヘッダー分ずらして position - 1
            BookItem book = upcomingBooks.get(position - 1);
            UpcomingBookViewHolder vh = (UpcomingBookViewHolder) holder;
            vh.titleView.setText(book.getTitle());
            vh.authorView.setText(book.getAuthor());
            vh.dateView.setText(book.salesDate);
        }
    }

    // 通常の新刊表示用ViewHolder
    public static class UpcomingBookViewHolder extends RecyclerView.ViewHolder {
        TextView titleView, authorView, dateView;

        public UpcomingBookViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.book_title);
            authorView = itemView.findViewById(R.id.book_author);
            dateView = itemView.findViewById(R.id.book_sales_date);
        }
    }

    // ヘッダーに表示する Spinner（通知何日前かを選ぶ）
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        Spinner spinner;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            spinner = itemView.findViewById(R.id.spinner_days_before);

            // 1〜7日を選べるスピナーリスト作成
            List<Integer> days = new ArrayList<>();
            for (int i = 1; i <= 7; i++) days.add(i);

            // スピナーにデータをセット
            ArrayAdapter<Integer> adapter = new ArrayAdapter<>(itemView.getContext(),
                    android.R.layout.simple_spinner_item, days);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            // SharedPreferences から保存済みの選択を復元
            SharedPreferences prefs = itemView.getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            int saved = prefs.getInt("notify_days_before", 3); // デフォルトは3日前
            int index = days.indexOf(saved);
            if (index != -1) spinner.setSelection(index);

            // スピナーで選択されたら保存する
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    prefs.edit().putInt("notify_days_before", days.get(pos)).apply();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }
}
