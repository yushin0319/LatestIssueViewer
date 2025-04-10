package com.example.latestissueviewer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // 画像読み込みライブラリ
import com.example.latestissueviewer.R;
import com.example.latestissueviewer.data.model.BookItem;

import java.util.List;

/**
 * 検索結果の本一覧を表示する RecyclerView.Adapter。
 * - 本のタイトル・著者・画像を表示
 * - クリックで「お気に入り」状態をトグル
 */
public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private List<BookItem> bookList;
    private OnFavoriteToggleListener listener;

    // お気に入りトグル時のコールバック用インターフェース
    public interface OnFavoriteToggleListener {
        void onToggle(BookItem item, boolean isFav);
    }

    // コンストラクタで本リストとリスナーを受け取る
    public BookAdapter(List<BookItem> books, OnFavoriteToggleListener listener) {
        this.bookList = books;
        this.listener = listener;
    }

    // ビューホルダー：1アイテムのレイアウトを保持
    public static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView titleView, authorView;
        ImageView imageView, overlayView;

        public BookViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.book_title);
            authorView = itemView.findViewById(R.id.book_author);
            imageView = itemView.findViewById(R.id.book_image);
            overlayView = itemView.findViewById(R.id.favorite_overlay); // ☆マーク用
        }
    }

    // アイテムレイアウトを膨らませてビューホルダーを作る
    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    // 各アイテムにデータをバインド
    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        BookItem book = bookList.get(position);
        holder.titleView.setText(book.getTitle());
        holder.authorView.setText(book.getAuthor());

        // 画像は Glide で非同期読み込み
        Glide.with(holder.itemView.getContext())
                .load(book.getLargeImageUrl())
                .into(holder.imageView);

        // お気に入り状態でオーバーレイを表示/非表示
        holder.overlayView.setVisibility(book.isFavorite() ? View.VISIBLE : View.GONE);

        // クリックでトグル＋コールバック実行
        holder.itemView.setOnClickListener(v -> {
            book.toggleFavorite();
            notifyItemChanged(position);
            if (listener != null) {
                listener.onToggle(book, book.isFavorite());
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }
}
