package com.hanse.SoftBrain;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Calendar;
import java.util.Locale;

/**
 * 지출 내역 1건을 담는 데이터 모델이자 Room DB 테이블(expenses)입니다.
 * - 홈 화면의 원 그래프 분류(식비, 주거, 교통, 통신, 의료, 여가, 기타)와 색상 매핑을 지원합니다.
 * - 날짜/시간은 timestamp(밀리초) 하나로 저장하고, 화면에 필요한 날짜 문자열·시간 문자열·
 *   며칠 전(daysAgo) 값은 getter에서 계산합니다.
 * - 하단 수정 바텀시트 탭의 필수 항목인 [이름, 시간, 장소, 분류, 금액]을 모두 포함합니다.
 */
@Entity(tableName = "expenses", indices = {@Index("timestamp")})
public class ExpenseItem {

    // 앱 전체에서 사용하는 표준 지출 분류 목록
    public static final String[] CATEGORIES = {
            "식비", "주거", "교통", "통신", "의료", "여가", "기타"
    };

    @PrimaryKey(autoGenerate = true)
    private long id;                // 고유 번호 (DB가 자동 발급)
    private String title;           // 이름 (지출 내용, 예: "점심 학식", "아메리카노")
    private String place;           // 장소 (상호명/위치, 예: "학생회관 1층", "스타벅스 한세점")
    private int amount;             // 지출 금액 (원)
    private String category;        // 분류 ("식비", "주거", "교통", "통신", "의료", "여가", "기타")
    private long timestamp;         // 결제 시각 (System.currentTimeMillis() 기준 밀리초)
    private String source;          // 인식 앱 출처 (예: "카카오뱅크 알림", "토스 알림", "문자 메시지", "직접 입력")
    @ColumnInfo(name = "auto_captured")
    private boolean autoCaptured;   // 알림 자동 인식 여부
    @ColumnInfo(name = "raw_text")
    private String rawText;         // 자동 인식된 원본 알림 문구 (파서 디버깅용, 직접 입력이면 null)

    /** Room 전용 기본 생성자 */
    public ExpenseItem() {
    }

    @Ignore
    public ExpenseItem(String title, String place, int amount, String category,
                       long timestamp, String source, boolean autoCaptured, String rawText) {
        this.title = title;
        this.place = place;
        this.amount = amount;
        this.category = category;
        this.timestamp = timestamp;
        this.source = source;
        this.autoCaptured = autoCaptured;
        this.rawText = rawText;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isAutoCaptured() {
        return autoCaptured;
    }

    public void setAutoCaptured(boolean autoCaptured) {
        this.autoCaptured = autoCaptured;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    // ───────────────────────── 화면 표시용 계산 값 ─────────────────────────

    /** 오늘 0, 어제 1, ... (달력 날짜 기준) */
    public int getDaysAgo() {
        return daysBetween(timestamp, System.currentTimeMillis());
    }

    /** 날짜 표시 문자열 (예: "11월 19일 (오늘)", "11월 17일 (2일 전)") */
    @NonNull
    public String getDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int daysAgo = getDaysAgo();
        String suffix;
        if (daysAgo == 0) {
            suffix = "오늘";
        } else if (daysAgo == 1) {
            suffix = "어제";
        } else {
            suffix = daysAgo + "일 전";
        }
        return String.format(Locale.KOREA, "%d월 %02d일 (%s)",
                cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), suffix);
    }

    /** 시간 표시 문자열 (예: "14:30") */
    @NonNull
    public String getTime() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return String.format(Locale.KOREA, "%02d:%02d",
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }

    /**
     * 날짜는 그대로 두고 시:분만 바꿉니다. (바텀시트에서 "14:30" 형식으로 입력받은 값)
     *
     * @return 형식이 올바르면 true
     */
    public boolean setTime(String hhmm) {
        int[] hm = parseHourMinute(hhmm);
        if (hm == null) return false;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, hm[0]);
        cal.set(Calendar.MINUTE, hm[1]);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        timestamp = cal.getTimeInMillis();
        return true;
    }

    /** "14:30" → {14, 30}, 잘못된 형식이면 null */
    public static int[] parseHourMinute(String hhmm) {
        if (hhmm == null) return null;
        String[] parts = hhmm.trim().split(":");
        if (parts.length != 2) return null;
        try {
            int h = Integer.parseInt(parts[0].trim());
            int m = Integer.parseInt(parts[1].trim());
            if (h < 0 || h > 23 || m < 0 || m > 59) return null;
            return new int[]{h, m};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 두 시각 사이의 달력 날짜 차이 */
    static int daysBetween(long from, long to) {
        return (int) ((startOfDay(to) - startOfDay(from)) / (24L * 60 * 60 * 1000));
    }

    /** 해당 시각이 속한 날의 00:00:00.000 */
    static long startOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
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
