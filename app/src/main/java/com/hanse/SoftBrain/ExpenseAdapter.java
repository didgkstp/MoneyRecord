package com.hanse.SoftBrain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * [화면 3 - 타임라인] 지출 내역 리스트 어댑터
 * - 언제(날짜/시간), 얼마를(금액), 어디에(장소 및 이름) 썼는지 명확히 나열합니다.
 * - 항목을 터치하면 아래에서 수정 바텀시트 탭이 올라오도록 리스너를 전달합니다.
 */
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnExpenseItemClickListener {
        void onItemClick(ExpenseItem item);
    }

    private final List<ExpenseItem> expenseList;
    private final OnExpenseItemClickListener listener;
    private final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.KOREA);

    public ExpenseAdapter(List<ExpenseItem> expenseList, OnExpenseItemClickListener listener) {
        this.expenseList = expenseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        ExpenseItem item = expenseList.get(position);

        // 1. 날짜 그룹 헤더 (첫 아이템이거나 바로 위 아이템과 날짜가 다를 때만 표시)
        boolean showDateHeader = (position == 0)
                || !item.getDate().equals(expenseList.get(position - 1).getDate());

        if (showDateHeader) {
            holder.tvDateHeader.setVisibility(View.VISIBLE);
            holder.tvDateHeader.setText(item.getDate());
        } else {
            holder.tvDateHeader.setVisibility(View.GONE);
        }

        // 2. 분류 아이콘, 이름(title), 분류 배지 표시
        holder.tvCategoryEmoji.setText(item.getCategoryEmoji());
        holder.tvStoreName.setText(item.getTitle());
        holder.tvCategoryBadge.setText(item.getCategory());

        // 3. 시간 + 장소(어디에) + 인식 앱 출처 표시
        String detailLine = item.getTime() + " · 📍" + item.getPlace() + " (" + item.getSource() + ")";
        holder.tvTimeAndSource.setText(detailLine);

        // 4. 금액 포맷팅
        String formattedAmount = "-" + currencyFormat.format(item.getAmount()) + "원";
        holder.tvExpenseAmount.setText(formattedAmount);

        // 5. 항목 클릭 시 하단 수정 바텀시트 호출
        holder.cardExpenseItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenseList != null ? expenseList.size() : 0;
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDateHeader;
        final MaterialCardView cardExpenseItem;
        final TextView tvCategoryEmoji;
        final TextView tvStoreName;
        final TextView tvCategoryBadge;
        final TextView tvTimeAndSource;
        final TextView tvExpenseAmount;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
            cardExpenseItem = itemView.findViewById(R.id.cardExpenseItem);
            tvCategoryEmoji = itemView.findViewById(R.id.tvCategoryEmoji);
            tvStoreName = itemView.findViewById(R.id.tvStoreName);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvTimeAndSource = itemView.findViewById(R.id.tvTimeAndSource);
            tvExpenseAmount = itemView.findViewById(R.id.tvExpenseAmount);
        }
    }
}
