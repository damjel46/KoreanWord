package com.nono.word;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrashActivity extends AppCompatActivity {

    private ListView listTrash;
    private TextView tvEmpty;
    private Button btnBack, btnRestoreAll, btnRestoreSelected;

    private SharedPreferences prefs;
    private Set<String> excludedSet;
    private List<String> trashList;
    private TrashAdapter adapter;

    // ★ 선택된 아이템들을 저장할 Set
    private Set<String> checkedItems = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        listTrash = findViewById(R.id.list_trash);
        tvEmpty = findViewById(R.id.tv_empty);
        btnBack = findViewById(R.id.btn_back);
        btnRestoreAll = findViewById(R.id.btn_restore_all); // 전체 복구 버튼
        btnRestoreSelected = findViewById(R.id.btn_restore_selected); // 선택 복구 버튼

        btnBack.setOnClickListener(v -> finish());

        //  전체 복구 클릭 시
        btnRestoreAll.setOnClickListener(v -> restoreAll());

        //  선택 복구 클릭 시
        btnRestoreSelected.setOnClickListener(v -> restoreSelected());

        prefs = getSharedPreferences("MyWordApp", MODE_PRIVATE);
        excludedSet = new HashSet<>(prefs.getStringSet("excluded", new HashSet<>()));
        trashList = new ArrayList<>(excludedSet);
        Collections.sort(trashList);

        adapter = new TrashAdapter(trashList);
        listTrash.setAdapter(adapter);

        updateUI();
    }

    private void updateUI() {
        if (trashList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listTrash.setVisibility(View.GONE);
            btnRestoreAll.setVisibility(View.GONE); // 목록 없으면 버튼도 숨김
        } else {
            tvEmpty.setVisibility(View.GONE);
            listTrash.setVisibility(View.VISIBLE);
            btnRestoreAll.setVisibility(View.VISIBLE);
        }

        // 선택된 게 있으면 하단 버튼 표시
        if (checkedItems.isEmpty()) {
            btnRestoreSelected.setVisibility(View.GONE);
        } else {
            btnRestoreSelected.setVisibility(View.VISIBLE);
            btnRestoreSelected.setText(checkedItems.size() + "개 복구하기");
        }
    }

    //  개별 복구
    private void restoreWord(String word) {
        excludedSet.remove(word);
        trashList.remove(word);
        checkedItems.remove(word); // 선택 목록에서도 제거

        prefs.edit().putStringSet("excluded", excludedSet).apply();
        adapter.notifyDataSetChanged();
        updateUI();

        Toast.makeText(this, "복구되었습니다.", Toast.LENGTH_SHORT).show();
    }

    //  전체 복구
    private void restoreAll() {
        if (trashList.isEmpty()) return;

        excludedSet.clear(); // 저장소 목록 비우기
        trashList.clear();   // 화면 목록 비우기
        checkedItems.clear();

        prefs.edit().putStringSet("excluded", excludedSet).apply();
        adapter.notifyDataSetChanged();
        updateUI();

        Toast.makeText(this, "휴지통을 모두 비웠습니다 (전체 복구).", Toast.LENGTH_SHORT).show();
    }

    //  선택 복구
    private void restoreSelected() {
        if (checkedItems.isEmpty()) return;

        // 선택된 것들 제거
        excludedSet.removeAll(checkedItems);
        trashList.removeAll(checkedItems);

        int count = checkedItems.size();
        checkedItems.clear(); // 선택 목록 초기화

        prefs.edit().putStringSet("excluded", excludedSet).apply();
        adapter.notifyDataSetChanged();
        updateUI();

        Toast.makeText(this, count + "개 단어를 복구했습니다.", Toast.LENGTH_SHORT).show();
    }

    class TrashAdapter extends ArrayAdapter<String> {
        public TrashAdapter(List<String> items) {
            super(TrashActivity.this, 0, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_trash, parent, false);
            }

            String word = getItem(position);

            TextView tvWord = convertView.findViewById(R.id.tv_word);
            Button btnRestore = convertView.findViewById(R.id.btn_restore);
            CheckBox chkSelect = convertView.findViewById(R.id.chk_select);

            tvWord.setText(word);

            // 체크박스 상태 초기화 (리스너 충돌 방지)
            chkSelect.setOnCheckedChangeListener(null);
            chkSelect.setChecked(checkedItems.contains(word));

            // 체크박스 클릭 리스너
            chkSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    checkedItems.add(word);
                } else {
                    checkedItems.remove(word);
                }
                updateUI(); // 하단 버튼 상태 갱신
            });

            // 개별 복구 버튼
            btnRestore.setOnClickListener(v -> restoreWord(word));

            return convertView;
        }
    }
}