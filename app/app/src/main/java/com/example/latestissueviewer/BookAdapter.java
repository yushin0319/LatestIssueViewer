package com.example.latestissueviewer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import com.bumptech.glide.Glide;
import com.example.latestissueviewer.model.Favorite;

import io.realm.Realm;
import io.realm.RealmResults;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private List<BookItem> bookList;

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView authorView;
        ImageView imageView;

        ImageView overlayView;

        public BookViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.book_title);
            authorView = itemView.findViewById(R.id.book_author);
            imageView = itemView.findViewById(R.id.book_image);
            overlayView = itemView.findViewById(R.id.favorite_overlay);
        }
    }

    // コンストラクタでデータ受け取る
    public BookAdapter(List<BookItem> books) {
        this.bookList = books;
    }

    // レイアウトを膨らませて ViewHolder 作る
    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    // データをViewHolderにバインド
    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        BookItem book = bookList.get(position);
        holder.titleView.setText(book.getTitle());
        holder.authorView.setText(book.getAuthor());

        Glide.with(holder.itemView.getContext())
                .load(book.getLargeImageUrl())
                .into(holder.imageView);

        holder.overlayView.setVisibility(book.isFavorite() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            book.toggleFavorite();
            notifyItemChanged(position);

            Realm realm = Realm.getDefaultInstance();

            realm.executeTransaction(r -> {
                if (book.isFavorite()) {
                    // 追加
                    Favorite fav = r.createObject(Favorite.class, book.getItemCode());
                } else {
                    // 削除（存在してたら）
                    Favorite fav = r.where(Favorite.class).equalTo("id", book.getItemCode()).findFirst();
                    if (fav != null) fav.deleteFromRealm();
                }
            });

            realm.close();
        });
    }

    // 表示するアイテム数
    @Override
    public int getItemCount() {
        return bookList.size();
    }
}
