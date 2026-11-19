package com.hanse.SoftBrain;

import android.content.Context;

import androidx.core.content.ContextCompat;

/**
 * 지출 내역 1건을 담는 데이터 모델 클래스입니다.
 * - 홈 화면의 원 그래프 분류(식비, 주거, 교통, 통신, 의료, 여가, 기타)와 색상 매핑을 지원합니다.
 * - 타임라인의 기간 필터(하루 / 일주일 / 한달)를 위해 몇 일 전 내역인지(daysAgo)를 함께 저장합니다.
 * - 하단 수정 바텀시트 탭의 필수 항목인 [이름, 시간, 장소, 분류, 금액]을 모두 포함합니다.
 */
public class ExpenseItem {

    // 앱 전체에서 사용하는 표준 지출 분류 목록
    public static final String[] CATEGORIES = {
            "식비", "주거", "교통", "통신", "의료", "여가", "기타"
    };

    private long id;                // 고유 번호
    private String title;           // 이름 (지출 내용, 예: "점심 학식", "아메리카노")
    private String place;           // 장소 (상호명/위치, 예: "학생회관 1층", "스타벅스 한세점")
    private int amount;             // 지출 금액 (원)
    private String category;        // 분류 ("식비", "주거", "교통", "통신", "의료", "여가", "기타")
    private String date;            // 날짜 표시 문자열 (예: "11월 19일 (오늘)")
    private String time;            // 시간 표시 문자열 (예: "14:30")
    private String source;          // 인식 앱 출처 (예: "카카오뱅크 알림", "토스 알림", "문자 메시지", "직접 입력")
    private boolean isAutoCaptured; // 알림 자동 인식 여부
    private int daysAgo;            // 기간 필터용 (0: 오늘/하루, 1~6: 일주일 이내, 7~30: 한달 이내)

    public ExpenseItem(long id, String title, String place, int amount, String category,
                       String date, String time, String source, boolean isAutoCaptured, int daysAgo) {
        this.id = id;
        this.title = title;
        this.place = place;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.time = time;
        this.source = source;
        this.isAutoCaptured = isAutoCaptured;
        this.daysAgo = daysAgo;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isAutoCaptured() {
        return isAutoCaptured;
    }

    public void setAutoCaptured(boolean autoCaptured) {
        isAutoCaptured = autoCaptured;
    }

    public int getDaysAgo() {
        return daysAgo;
    }

    public void setDaysAgo(int daysAgo) {
        this.daysAgo = daysAgo;
    }

    /**
     * 분류(식비, 주거, 교통, 통신, 의료, 여가, 기타)에 맞는 이모지를 반환합니다.
     */
    public String getCategoryEmoji() {
        return getEmojiForCategory(category);
    }

    public static String getEmojiForCategory(String categoryName) {
        if (categoryName == null) return "💸";
        switch (categoryName) {
            case "식비":
                return "🍚";
            case "주거":
                return "🏠";
            case "교통":
                return "🚌";
            case "통신":
                return "📱";
            case "의료":
                return "💊";
            case "여가":
                return "🎬";
            default:
                return "💸";
        }
    }

    /**
     * 원 그래프(Pie Chart)와 상세 목록의 색상 점에서 사용할 카테고리별 고유 컬러를 반환합니다.
     */
    public static int getColorForCategory(Context context, String categoryName) {
        if (categoryName == null) {
            return ContextCompat.getColor(context, R.color.cat_etc);
        }
        switch (categoryName) {
            case "식비":
                return ContextCompat.getColor(context, R.color.cat_food);
            case "주거":
                return ContextCompat.getColor(context, R.color.cat_housing);
            case "교통":
                return ContextCompat.getColor(context, R.color.cat_transport);
            case "통신":
                return ContextCompat.getColor(context, R.color.cat_telecom);
            case "의료":
                return ContextCompat.getColor(context, R.color.cat_medical);
            case "여가":
                return ContextCompat.getColor(context, R.color.cat_leisure);
            default:
                return ContextCompat.getColor(context, R.color.cat_etc);
        }
    }
}
