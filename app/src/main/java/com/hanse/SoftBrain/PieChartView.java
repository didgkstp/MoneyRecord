package com.hanse.SoftBrain;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * [화면 2 - 홈 화면 전용] 커스텀 원 그래프 (PieChartView)
 * - 외부 라이브러리 설치 없이 Android 기본 Canvas로 부드러운 도넛형 원 그래프를 그립니다.
 * - 식비, 주거, 교통, 통신, 의료, 여가 등 각 분류의 비율만큼 색상을 채워 보여줍니다.
 */
public class PieChartView extends View {

    public static class Slice {
        public final String category;
        public final float value;
        public final int color;

        public Slice(String category, float value, int color) {
            this.category = category;
            this.value = value;
            this.color = color;
        }
    }

    private final List<Slice> slices = new ArrayList<>();
    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint holePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerSubTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private String centerTopLabel = "최대 지출";
    private String centerMainLabel = "식비";

    public PieChartView(Context context) {
        super(context);
        init(context);
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        slicePaint.setStyle(Paint.Style.FILL);
        holePaint.setStyle(Paint.Style.FILL);

        centerTextPaint.setTextAlign(Paint.Align.CENTER);
        centerTextPaint.setFakeBoldText(true);

        centerSubTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * 원 그래프에 표시할 조각(Slice) 리스트와 중앙 텍스트를 설정하고 화면을 다시 그립니다.
     */
    public void setData(List<Slice> newSlices, String topCategoryName) {
        slices.clear();
        if (newSlices != null) {
            slices.addAll(newSlices);
        }
        this.centerMainLabel = (topCategoryName != null && !topCategoryName.isEmpty())
                ? topCategoryName : "없음";
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float size = Math.min(width, height);
        float padding = size * 0.04f;
        float left = (width - size) / 2f + padding;
        float top = (height - size) / 2f + padding;
        float right = left + size - padding * 2f;
        float bottom = top + size - padding * 2f;
        arcBounds.set(left, top, right, bottom);

        float total = 0f;
        for (Slice s : slices) {
            total += s.value;
        }

        // 데이터가 없을 때 기본 회색 원 표시
        if (total <= 0f) {
            slicePaint.setColor(ContextCompat.getColor(getContext(), R.color.divider_gray));
            canvas.drawOval(arcBounds, slicePaint);
        } else {
            // 12시 방향(-90도)부터 시계 방향으로 각 카테고리 색상 채우기
            float startAngle = -90f;
            for (Slice s : slices) {
                if (s.value <= 0) continue;
                float sweepAngle = (s.value / total) * 360f;
                slicePaint.setColor(s.color);
                // 조각 사이가 깔끔하게 보이도록 살짝 겹치거나 채움
                canvas.drawArc(arcBounds, startAngle, sweepAngle, true, slicePaint);
                startAngle += sweepAngle;
            }
        }

        // 가운데 도넛 홀(원)을 카드 배경색으로 채워 세련된 도넛형 원 그래프 연출
        float cx = width / 2f;
        float cy = height / 2f;
        float holeRadius = (size - padding * 2f) * 0.28f;
        holePaint.setColor(ContextCompat.getColor(getContext(), R.color.card_white));
        canvas.drawCircle(cx, cy, holeRadius, holePaint);

        // 중앙 텍스트 ("최대 지출" / 카테고리명)
        centerSubTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        centerSubTextPaint.setTextSize(size * 0.055f);
        canvas.drawText(centerTopLabel, cx, cy - (size * 0.025f), centerSubTextPaint);

        centerTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        centerTextPaint.setTextSize(size * 0.085f);
        canvas.drawText(centerMainLabel, cx, cy + (size * 0.065f), centerTextPaint);
    }
}
