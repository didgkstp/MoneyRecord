package com.hanse.SoftBrain;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * [화면 2, 3, 3-1, 4 통합 컨트롤러] MainActivity
 *
 * - 화면 2 (홈 화면): 상단에 "오늘 ~의 지출이 커요!" 헤드라인 표시 + 원 그래프(PieChartView) +
 *                    하단에 분류별 색상, 퍼센트(%), 금액 상세 리스트 표시
 * - 화면 3 (타임라인): 하루 / 일주일 / 한달 필터링 + 언제, 얼마를, 어디에 썼는지 나열 +
 *                    오른쪽 상단 [＋ 추가] 및 [✎ 수정] 버튼 제공
 * - 화면 3-1 (하단 수정 탭): 아래에서 올라오는 BottomSheetDialog에
 *                    [이름, 시간, 장소, 분류, 올리기, 취소] 버튼 배치
 * - 화면 4 (설정): 다크모드 즉시 전환 + 인식 앱(카카오뱅크, 토스, 메시지, 카드사) 스위치 설정
 */
public class MainActivity extends AppCompatActivity {

    // 3개 메인 화면 컨테이너
    private View viewHome;
    private View viewTimeline;
    private View viewSettings;
    private BottomNavigationView bottomNavigation;

    // [화면 2: 홈 화면] 뷰
    private TextView tvHomeHeadline;
    private TextView tvHomeSubHeadline;
    private PieChartView pieChartView;
    private TextView tvHomeTotalExpense;
    private LinearLayout layoutCategoryLegendContainer;

    // [화면 3: 타임라인] 뷰
    private TextView btnTopAdd;
    private TextView btnTopEdit;
    private ChipGroup chipGroupPeriod;
    private TextView tvTimelinePeriodSum;
    private TextView tvEditModeBanner;
    private RecyclerView rvExpenseTimeline;
    private TextView tvEmptyView;

    // [화면 4: 설정] 뷰
    private MaterialSwitch switchDarkMode;
    private MaterialSwitch switchAppKakaoBank;
    private MaterialSwitch switchAppToss;
    private MaterialSwitch switchAppSms;
    private MaterialSwitch switchAppCard;
    private MaterialCardView cardSystemNotificationPermission;

    // 데이터 리스트 (전체 원본 리스트 & 기간 필터링된 표시용 리스트)
    private static final List<ExpenseItem> masterExpenseList = new ArrayList<>();
    private final List<ExpenseItem> filteredExpenseList = new ArrayList<>();
    private ExpenseAdapter expenseAdapter;

    // 현재 선택된 타임라인 기간 필터 (0: 하루, 7: 일주일, 30: 한달)
    private int currentPeriodDays = 30;
    private boolean isEditGuideMode = false;

    private final NumberFormat currencyFormat = NumberFormat.getInstance(Locale.KOREA);
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("SoftBrainPrefs", MODE_PRIVATE);

        // 시스템 바(상단 상태바/하단 제스처바) 여백 적용
        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();
        setupRecyclerView();

        // 최초 실행 시에만 샘플 더미 데이터 세팅 (다크모드 전환 시에도 수정 내역 유지)
        if (masterExpenseList.isEmpty()) {
            loadInitialDummyData();
        }

        setupBottomNavigation();
        setupTimelineControls();
        setupSettingsControls();

        // 홈 화면(원 그래프 + 분류별 상세) 및 타임라인 리스트 최신화
        refreshAllScreens();
    }

    private void initViews() {
        viewHome = findViewById(R.id.viewHome);
        viewTimeline = findViewById(R.id.viewTimeline);
        viewSettings = findViewById(R.id.viewSettings);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // 홈 화면
        tvHomeHeadline = findViewById(R.id.tvHomeHeadline);
        tvHomeSubHeadline = findViewById(R.id.tvHomeSubHeadline);
        pieChartView = findViewById(R.id.pieChartView);
        tvHomeTotalExpense = findViewById(R.id.tvHomeTotalExpense);
        layoutCategoryLegendContainer = findViewById(R.id.layoutCategoryLegendContainer);

        // 타임라인 화면
        btnTopAdd = findViewById(R.id.btnTopAdd);
        btnTopEdit = findViewById(R.id.btnTopEdit);
        chipGroupPeriod = findViewById(R.id.chipGroupPeriod);
        tvTimelinePeriodSum = findViewById(R.id.tvTimelinePeriodSum);
        tvEditModeBanner = findViewById(R.id.tvEditModeBanner);
        rvExpenseTimeline = findViewById(R.id.rvExpenseTimeline);
        tvEmptyView = findViewById(R.id.tvEmptyView);

        // 설정 화면
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchAppKakaoBank = findViewById(R.id.switchAppKakaoBank);
        switchAppToss = findViewById(R.id.switchAppToss);
        switchAppSms = findViewById(R.id.switchAppSms);
        switchAppCard = findViewById(R.id.switchAppCard);
        cardSystemNotificationPermission = findViewById(R.id.cardSystemNotificationPermission);
    }

    private void setupRecyclerView() {
        rvExpenseTimeline.setLayoutManager(new LinearLayoutManager(this));
        // 타임라인 항목을 누르면 아래에서 수정 탭(BottomSheet)이 올라옵니다.
        expenseAdapter = new ExpenseAdapter(filteredExpenseList, this::showExpenseBottomSheet);
        rvExpenseTimeline.setAdapter(expenseAdapter);
    }

    /**
     * 하단 내비게이션 탭(홈 / 타임라인 / 설정) 전환
     */
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                viewHome.setVisibility(View.VISIBLE);
                viewTimeline.setVisibility(View.GONE);
                viewSettings.setVisibility(View.GONE);
                updateHomeDashboard();
                return true;
            } else if (id == R.id.nav_timeline) {
                viewHome.setVisibility(View.GONE);
                viewTimeline.setVisibility(View.VISIBLE);
                viewSettings.setVisibility(View.GONE);
                applyTimelineFilter(currentPeriodDays);
                return true;
            } else if (id == R.id.nav_settings) {
                viewHome.setVisibility(View.GONE);
                viewTimeline.setVisibility(View.GONE);
                viewSettings.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });
    }

    /**
     * [화면 3: 타임라인] 하루/일주일/한달 기간 필터 & 우측 상단 [추가]/[수정] 버튼 설정
     */
    private void setupTimelineControls() {
        // 기간 필터 (하루 / 일주일 / 한달)
        chipGroupPeriod.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipDay) {
                currentPeriodDays = 0; // 오늘 하루
            } else if (checkedId == R.id.chipWeek) {
                currentPeriodDays = 7; // 최근 일주일
            } else {
                currentPeriodDays = 30; // 최근 한달
            }
            applyTimelineFilter(currentPeriodDays);
        });

        // 오른쪽 상단 [＋ 추가] 버튼 -> 빈 입력 바텀시트 탭을 아래에서 올림
        btnTopAdd.setOnClickListener(v -> showExpenseBottomSheet(null));

        // 오른쪽 상단 [✎ 수정] 버튼 -> 수정 안내 배너 토글 또는 가장 최근 항목 수정 탭 바로 열기
        btnTopEdit.setOnClickListener(v -> {
            if (filteredExpenseList.isEmpty()) {
                Toast.makeText(this, "수정할 지출 내역이 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            isEditGuideMode = !isEditGuideMode;
            tvEditModeBanner.setVisibility(isEditGuideMode ? View.VISIBLE : View.GONE);
            // 사용자가 바로 수정 탭을 확인할 수 있도록 첫 번째(가장 최근) 항목의 하단 수정 탭도 즉시 띄워줍니다.
            showExpenseBottomSheet(filteredExpenseList.get(0));
        });
    }

    /**
     * [화면 3-1: 아래에서 올라오는 수정/추가 탭 (BottomSheetDialog)]
     * 요구사항: 이름, 시간, 장소, 분류, 올리기, 취소 버튼을 배치
     *
     * @param targetItem null이면 '새 지출 추가', 객체가 전달되면 해당 지출 내역 '수정'
     */
    private void showExpenseBottomSheet(ExpenseItem targetItem) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_expense_edit, null, false);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvSheetTitle = sheetView.findViewById(R.id.tvSheetTitle);
        EditText etExpenseTitle = sheetView.findViewById(R.id.etExpenseTitle);
        EditText etExpenseTime = sheetView.findViewById(R.id.etExpenseTime);
        EditText etExpenseAmount = sheetView.findViewById(R.id.etExpenseAmount);
        EditText etExpensePlace = sheetView.findViewById(R.id.etExpensePlace);
        Spinner spinnerCategory = sheetView.findViewById(R.id.spinnerCategory);
        MaterialButton btnSheetCancel = sheetView.findViewById(R.id.btnSheetCancel);
        MaterialButton btnSheetSubmit = sheetView.findViewById(R.id.btnSheetSubmit);

        // 분류 Spinner 어댑터 연결 (식비, 주거, 교통, 통신, 의료, 여가, 기타)
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                ExpenseItem.CATEGORIES
        );
        spinnerCategory.setAdapter(categoryAdapter);

        final boolean isEdit = (targetItem != null);
        if (isEdit) {
            tvSheetTitle.setText("지출 내역 수정");
            etExpenseTitle.setText(targetItem.getTitle());
            etExpenseTime.setText(targetItem.getTime());
            etExpenseAmount.setText(String.valueOf(targetItem.getAmount()));
            etExpensePlace.setText(targetItem.getPlace());

            // 기존 분류 선택
            for (int i = 0; i < ExpenseItem.CATEGORIES.length; i++) {
                if (ExpenseItem.CATEGORIES[i].equals(targetItem.getCategory())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        } else {
            tvSheetTitle.setText("새 지출 내역 추가");
            etExpenseTime.setText("15:00");
        }

        // [취소] 버튼
        btnSheetCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // [올리기] 버튼
        btnSheetSubmit.setOnClickListener(v -> {
            String title = etExpenseTitle.getText().toString().trim();
            String time = etExpenseTime.getText().toString().trim();
            String amountStr = etExpenseAmount.getText().toString().trim();
            String place = etExpensePlace.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();

            if (title.isEmpty()) {
                etExpenseTitle.setError("이름(내역)을 입력해 주세요");
                return;
            }
            if (time.isEmpty()) {
                time = "12:00";
            }
            if (place.isEmpty()) {
                place = "장소 미입력";
            }

            int amount = 0;
            try {
                amount = Integer.parseInt(amountStr);
            } catch (NumberFormatException e) {
                etExpenseAmount.setError("올바른 숫자를 입력해 주세요");
                return;
            }

            if (isEdit) {
                // 기존 항목 수정 반영
                targetItem.setTitle(title);
                targetItem.setTime(time);
                targetItem.setAmount(amount);
                targetItem.setPlace(place);
                targetItem.setCategory(category);
                Toast.makeText(this, "지출 내역이 수정되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                // 새 지출 내역을 최상단(오늘)에 추가
                long newId = System.currentTimeMillis();
                ExpenseItem newItem = new ExpenseItem(
                        newId,
                        title,
                        place,
                        amount,
                        category,
                        "11월 19일 (오늘)",
                        time,
                        "직접 입력",
                        false,
                        0
                );
                masterExpenseList.add(0, newItem);
                Toast.makeText(this, "새 지출 내역이 등록되었습니다.", Toast.LENGTH_SHORT).show();
            }

            // 홈 화면 원 그래프 & 타임라인 리스트 즉시 갱신
            refreshAllScreens();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    /**
     * [화면 2: 홈 화면]
     * 1) "오늘 ~의 지출이 커요!" 상단 헤드라인 자동 계산
     * 2) 원 그래프(PieChartView) 색상 및 비율 업데이트
     * 3) 원 그래프 하단에 무엇이 무슨 색이고 몇 퍼센트(%)이고 얼마인지 상세 목록 생성
     */
    private void updateHomeDashboard() {
        // 1. 오늘(daysAgo == 0) 지출 중 가장 큰 분류 찾기 (오늘 내역이 없으면 전체 기간 기준)
        Map<String, Integer> todayCategorySum = new LinkedHashMap<>();
        for (ExpenseItem item : masterExpenseList) {
            if (item.getDaysAgo() == 0) {
                int prev = todayCategorySum.containsKey(item.getCategory())
                        ? todayCategorySum.get(item.getCategory()) : 0;
                todayCategorySum.put(item.getCategory(), prev + item.getAmount());
            }
        }

        // 2. 전체 분류별 합계 계산 (식비, 주거, 교통, 통신, 의료, 여가, 기타 순서 유지)
        Map<String, Integer> totalCategorySum = new LinkedHashMap<>();
        for (String cat : ExpenseItem.CATEGORIES) {
            totalCategorySum.put(cat, 0);
        }

        int grandTotal = 0;
        for (ExpenseItem item : masterExpenseList) {
            String cat = item.getCategory();
            int prev = totalCategorySum.containsKey(cat) ? totalCategorySum.get(cat) : 0;
            totalCategorySum.put(cat, prev + item.getAmount());
            grandTotal += item.getAmount();
        }

        // 오늘 가장 지출이 큰 분류 선정
        String biggestTodayCategory = "식비";
        int biggestTodayAmount = -1;
        for (Map.Entry<String, Integer> entry : todayCategorySum.entrySet()) {
            if (entry.getValue() > biggestTodayAmount) {
                biggestTodayAmount = entry.getValue();
                biggestTodayCategory = entry.getKey();
            }
        }

        // 전체에서 가장 지출이 큰 분류 선정
        String biggestOverallCategory = "식비";
        int biggestOverallAmount = -1;
        for (Map.Entry<String, Integer> entry : totalCategorySum.entrySet()) {
            if (entry.getValue() > biggestOverallAmount) {
                biggestOverallAmount = entry.getValue();
                biggestOverallCategory = entry.getKey();
            }
        }

        if (biggestTodayAmount <= 0) {
            biggestTodayCategory = biggestOverallCategory;
        }

        // 상단 "오늘 ~의 지출이 커요!" 텍스트 업데이트
        tvHomeHeadline.setText("오늘 " + biggestTodayCategory + "의 지출이 커요!");

        if (grandTotal > 0) {
            int targetCatTotal = totalCategorySum.containsKey(biggestTodayCategory)
                    ? totalCategorySum.get(biggestTodayCategory) : 0;
            float ratio = (targetCatTotal * 100f) / grandTotal;
            tvHomeSubHeadline.setText(String.format(
                    Locale.KOREA,
                    "이번 달 %s 지출은 %s원 (전체의 %.1f%%)",
                    biggestTodayCategory,
                    currencyFormat.format(targetCatTotal),
                    ratio
            ));
        } else {
            tvHomeSubHeadline.setText("아직 기록된 지출이 없어요");
        }

        tvHomeTotalExpense.setText("이번 달 누적 지출: " + currencyFormat.format(grandTotal) + "원");

        // 3. 원 그래프 조각(Slice) 데이터 생성 및 상세 범례 리스트(색상 + 이름 + % + 금액) 동적 생성
        List<PieChartView.Slice> chartSlices = new ArrayList<>();
        layoutCategoryLegendContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String category : ExpenseItem.CATEGORIES) {
            int catAmount = totalCategorySum.containsKey(category) ? totalCategorySum.get(category) : 0;
            int catColor = ExpenseItem.getColorForCategory(this, category);
            float percent = (grandTotal > 0) ? (catAmount * 100f / grandTotal) : 0f;

            if (catAmount > 0) {
                chartSlices.add(new PieChartView.Slice(category, catAmount, catColor));
            }

            // 하단 상세 정보 행 (무엇이 무슨 색이고 몇 퍼센트이고 얼마인지 표시)
            View legendRow = inflater.inflate(
                    R.layout.item_category_legend,
                    layoutCategoryLegendContainer,
                    false
            );

            View viewColorDot = legendRow.findViewById(R.id.viewColorDot);
            TextView tvLegendCategoryName = legendRow.findViewById(R.id.tvLegendCategoryName);
            TextView tvLegendPercent = legendRow.findViewById(R.id.tvLegendPercent);
            TextView tvLegendAmount = legendRow.findViewById(R.id.tvLegendAmount);

            // 원형 색상 점에 해당 카테고리 컬러 입히기
            GradientDrawable dotDrawable = new GradientDrawable();
            dotDrawable.setShape(GradientDrawable.OVAL);
            dotDrawable.setColor(catColor);
            viewColorDot.setBackground(dotDrawable);

            String emoji = ExpenseItem.getEmojiForCategory(category);
            tvLegendCategoryName.setText(emoji + "  " + category);
            tvLegendPercent.setText(String.format(Locale.KOREA, "%.1f%%", percent));
            tvLegendAmount.setText(currencyFormat.format(catAmount) + "원");

            layoutCategoryLegendContainer.addView(legendRow);
        }

        pieChartView.setData(chartSlices, biggestOverallCategory);
    }

    /**
     * [화면 3: 타임라인] 하루(0일) / 일주일(7일 이내) / 한달(30일 이내) 필터링 적용
     */
    private void applyTimelineFilter(int maxDaysAgo) {
        filteredExpenseList.clear();
        int periodSum = 0;

        for (ExpenseItem item : masterExpenseList) {
            if (item.getDaysAgo() <= maxDaysAgo) {
                filteredExpenseList.add(item);
                periodSum += item.getAmount();
            }
        }

        tvTimelinePeriodSum.setText("총 " + currencyFormat.format(periodSum) + "원");
        expenseAdapter.notifyDataSetChanged();

        if (filteredExpenseList.isEmpty()) {
            rvExpenseTimeline.setVisibility(View.GONE);
            tvEmptyView.setVisibility(View.VISIBLE);
        } else {
            rvExpenseTimeline.setVisibility(View.VISIBLE);
            tvEmptyView.setVisibility(View.GONE);
        }
    }

    /**
     * [화면 4: 설정] 다크모드 & 인식 앱(카카오뱅크, 토스, 메시지, 카드사) 스위치 이벤트
     */
    private void setupSettingsControls() {
        // 저장된 스위치 상태 불러오기
        boolean isDark = prefs.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDark);
        switchAppKakaoBank.setChecked(prefs.getBoolean("app_kakaobank", true));
        switchAppToss.setChecked(prefs.getBoolean("app_toss", true));
        switchAppSms.setChecked(prefs.getBoolean("app_sms", true));
        switchAppCard.setChecked(prefs.getBoolean("app_card", true));

        // 1. 다크모드 스위치
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // 2. 인식 앱 스위치 (카카오뱅크, 토스, 문자 메시지, 카드사 앱)
        switchAppKakaoBank.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("app_kakaobank", isChecked).apply();
            showAppToggleToast("카카오뱅크", isChecked);
        });

        switchAppToss.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("app_toss", isChecked).apply();
            showAppToggleToast("토스(Toss)", isChecked);
        });

        switchAppSms.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("app_sms", isChecked).apply();
            showAppToggleToast("기본 문자 메시지(SMS)", isChecked);
        });

        switchAppCard.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("app_card", isChecked).apply();
            showAppToggleToast("카드사 앱 푸시", isChecked);
        });

        // 3. 안드로이드 알림 접근 허용 시스템 설정 화면으로 이동
        cardSystemNotificationPermission.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "알림 접근 설정 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAppToggleToast(String appName, boolean enabled) {
        String status = enabled ? "알림 인식이 켜졌습니다." : "알림 인식이 꺼졌습니다.";
        Toast.makeText(this, appName + " " + status, Toast.LENGTH_SHORT).show();
    }

    private void refreshAllScreens() {
        updateHomeDashboard();
        applyTimelineFilter(currentPeriodDays);
    }

    /**
     * 원 그래프의 모든 분류(식비, 주거, 교통, 통신, 의료, 여가)와
     * 타임라인의 기간 필터(하루, 일주일, 한달)를 즉시 테스트할 수 있는 샘플 데이터입니다.
     */
    private void loadInitialDummyData() {
        masterExpenseList.clear();

        // [하루 (오늘, daysAgo = 0)] -> 오늘 식비 지출이 가장 크게 잡히도록 구성
        masterExpenseList.add(new ExpenseItem(
                1L, "수제버거 세트 & 음료", "버거킹 한세점", 14500, "식비",
                "11월 19일 (오늘)", "13:20", "카카오뱅크 알림", true, 0
        ));
        masterExpenseList.add(new ExpenseItem(
                2L, "아이스 아메리카노", "스타벅스 역삼점", 4500, "식비",
                "11월 19일 (오늘)", "09:40", "토스 알림", true, 0
        ));
        masterExpenseList.add(new ExpenseItem(
                3L, "택시 이동", "카카오T 택시", 8800, "교통",
                "11월 19일 (오늘)", "08:50", "카카오뱅크 알림", true, 0
        ));

        // [일주일 이내 (daysAgo = 1 ~ 6)]
        masterExpenseList.add(new ExpenseItem(
                4L, "CGV 영화 & 팝콘", "CGV 강남", 22000, "여가",
                "11월 17일 (2일 전)", "19:30", "토스 알림", true, 2
        ));
        masterExpenseList.add(new ExpenseItem(
                5L, "감기약 처방 및 진료", "한세내과의원·온누리약국", 16500, "의료",
                "11월 15일 (4일 전)", "11:15", "문자 메시지(SMS)", true, 4
        ));
        masterExpenseList.add(new ExpenseItem(
                6L, "삼겹살 저녁 모임", "하남돼지집", 38000, "식비",
                "11월 14일 (5일 전)", "20:10", "카카오뱅크 알림", true, 5
        ));

        // [한달 이내 (daysAgo = 7 ~ 30)]
        masterExpenseList.add(new ExpenseItem(
                7L, "월세 관리비 및 수도세", "자취방 관리사무소", 65000, "주거",
                "11월 05일 (2주 전)", "10:00", "카카오뱅크 알림", true, 14
        ));
        masterExpenseList.add(new ExpenseItem(
                8L, "휴대폰 5G 요금제 납부", "SKT 자동이체", 49000, "통신",
                "11월 01일 (이번 달 초)", "09:00", "문자 메시지(SMS)", true, 18
        ));
        masterExpenseList.add(new ExpenseItem(
                9L, "지하철·버스 정기 교통비", "티머니 후불교통", 42500, "교통",
                "10월 28일 (3주 전)", "08:30", "KB국민카드 알림", true, 22
        ));
    }
}
