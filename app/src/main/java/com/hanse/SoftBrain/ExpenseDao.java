package com.hanse.SoftBrain;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * 지출 내역 테이블(expenses) 접근 객체.
 * - observe* 메서드는 LiveData를 돌려주므로, 알림 리스너가 DB에 저장하면 화면이 자동으로 갱신됩니다.
 * - 나머지 메서드는 메인 스레드에서 호출하면 안 됩니다. (AppDatabase.IO 실행기에서 호출)
 */
@Dao
public interface ExpenseDao {

    /** 최신순 전체 내역 (홈/타임라인 화면에서 관찰) */
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    LiveData<List<ExpenseItem>> observeAll();

    @Insert
    long insert(ExpenseItem item);

    @Insert
    void insertAll(List<ExpenseItem> items);

    @Update
    void update(ExpenseItem item);

    @Delete
    void delete(ExpenseItem item);

    /**
     * 중복 알림 판별용: 시간 범위 안의 같은 금액 중
     * 다른 출처에서 왔거나(앱 푸시 + 문자) 원문까지 똑같은(알림 재게시) 자동 인식 내역 수.
     * 같은 앱에서 온 문구가 다른 결제는 별개의 결제로 봅니다.
     */
    @Query("SELECT COUNT(*) FROM expenses WHERE auto_captured = 1 AND raw_text IS NOT NULL "
            + "AND amount = :amount AND timestamp BETWEEN :from AND :to "
            + "AND (source != :source OR raw_text = :rawText)")
    int countDuplicates(int amount, long from, long to, String source, String rawText);

    /** 결제 취소 대상: 금액이 같고 원본 알림에 가맹점 이름이 들어 있는 가장 최근 내역 (없으면 null) */
    @Query("SELECT * FROM expenses WHERE auto_captured = 1 AND amount = :amount "
            + "AND instr(raw_text, :merchant) > 0 AND timestamp BETWEEN :from AND :to "
            + "ORDER BY timestamp DESC LIMIT 1")
    ExpenseItem findCancelTarget(int amount, String merchant, long from, long to);
}
