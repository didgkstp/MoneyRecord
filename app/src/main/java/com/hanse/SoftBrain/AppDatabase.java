package com.hanse.SoftBrain;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 앱 전체에서 하나만 쓰는 로컬 Room DB (서버 없음, 모든 데이터는 폰 안에만 저장).
 * - 사용법: AppDatabase.get(context).expenseDao()
 * - DB 쓰기/읽기는 메인 스레드에서 하면 안 되므로 AppDatabase.IO.execute(() -> ...) 안에서 호출하세요.
 */
@Database(entities = {ExpenseItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * true면 DB가 처음 만들어질 때 UI 확인용 샘플 지출 9건을 넣습니다.
     * 실제 사용/발표 때는 false로 바꾸고 앱을 삭제 후 재설치하세요.
     */
    private static final boolean SEED_DUMMY_DATA = true;

    /** DB 작업 전용 백그라운드 실행기 */
    public static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private static volatile AppDatabase instance;

    public abstract ExpenseDao expenseDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "money_record.db")
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    if (SEED_DUMMY_DATA) {
                                        IO.execute(() -> instance.expenseDao().insertAll(buildDummyData()));
                                    }
                                }
                            })
                            .build();
                }
            }
        }
        return instance;
    }

    /**
     * 원 그래프의 모든 분류와 타임라인 기간 필터(하루/일주일/한달)를 확인할 수 있는 샘플 데이터
     */
    private static List<ExpenseItem> buildDummyData() {
        List<ExpenseItem> list = new ArrayList<>();
        list.add(new ExpenseItem("수제버거 세트 & 음료", "버거킹 한세점", 14500, "식비", at(0, 13, 20), "카카오뱅크 알림", true, null));
        list.add(new ExpenseItem("아이스 아메리카노", "스타벅스 역삼점", 4500, "식비", at(0, 9, 40), "토스 알림", true, null));
        list.add(new ExpenseItem("택시 이동", "카카오T 택시", 8800, "교통", at(0, 8, 50), "카카오뱅크 알림", true, null));
        list.add(new ExpenseItem("CGV 영화 & 팝콘", "CGV 강남", 22000, "여가", at(2, 19, 30), "토스 알림", true, null));
        list.add(new ExpenseItem("감기약 처방 및 진료", "한세내과의원·온누리약국", 16500, "의료", at(4, 11, 15), "문자 메시지(SMS)", true, null));
        list.add(new ExpenseItem("삼겹살 저녁 모임", "하남돼지집", 38000, "식비", at(5, 20, 10), "카카오뱅크 알림", true, null));
        list.add(new ExpenseItem("월세 관리비 및 수도세", "자취방 관리사무소", 65000, "주거", at(14, 10, 0), "카카오뱅크 알림", true, null));
        list.add(new ExpenseItem("휴대폰 5G 요금제 납부", "SKT 자동이체", 49000, "통신", at(18, 9, 0), "문자 메시지(SMS)", true, null));
        list.add(new ExpenseItem("지하철·버스 정기 교통비", "티머니 후불교통", 42500, "교통", at(22, 8, 30), "KB국민카드 알림", true, null));
        return list;
    }

    /** daysAgo일 전 hour:minute 시각 */
    private static long at(int daysAgo, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
