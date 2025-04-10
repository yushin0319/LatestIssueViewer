package com.example.latestissueviewer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

import com.google.gson.Gson;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    EditText searchInput;
    Button searchButton;

    private List<BookItem> bookList = new ArrayList<>();
    private BookAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        searchInput = findViewById(R.id.search_input);
        searchButton = findViewById(R.id.search_button);

        RecyclerView recyclerView = findViewById(R.id.book_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BookAdapter(bookList);
        recyclerView.setAdapter(adapter);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = searchInput.getText().toString().trim();
                Log.d("search","検索ワード: " + keyword);
                searchBooks(keyword);
            }
        });
    }

    private void searchBooks(String keyword){
        new Thread(() -> {
            try {
                String apiKey = "1006698996673204288";
                String apiUrl = "https://app.rakuten.co.jp/services/api/BooksBook/Search/20170404";
                String query = apiUrl + "?applicationId=" + apiKey + "&title=" + keyword + "&format=json";

                URL url = new URL(query);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                Log.d("search", "Response Code: " + responseCode);

                if (responseCode == 200){
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null){
                        response.append(line);
                    }
                    in.close();

                    Gson gson = new Gson();
                    BookResponse result = gson.fromJson(response.toString(), BookResponse.class);
                    List<BookItem> books = new ArrayList<>();
                    for (BookResponse.ItemWrapper wrapper : result.Items){
                        books.add(wrapper.Item);
                    }

                    runOnUiThread(() -> {
                        bookList.clear();
                        bookList.addAll(books);
                        adapter.notifyDataSetChanged();
                    });

                } else {
                    Log.e("search", "API Error: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("search", "Exception: " + e.toString());
            }
        }).start();
    }
}
