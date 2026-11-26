package com.hanse.SoftBrain;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 폰에 뜨는 모든 알림을 받아서, 결제 알림이면 로컬 DB(Room)에 지출로 저장하는 서비스.
 * - 사용자가 [설정 > 알림 접근 허용]에서 이 앱을 켜야 동작합니다.
 * - 저장하면 MainActivity가 LiveData로 관찰 중이라 화면이 자동 갱신됩니다.
 *
 * [디버그 빌드 전용] 인식 대상 앱의 알림 원문과 처리 결과를 두 군데에 남깁니다. (파서 규칙 만들 때 참고)
 * - Logcat 태그 "PaymentListener" (케이블 연결 중일 때)
 * - 앱 내부 파일 files/capture_log.txt (케이블 없이도 계속 쌓임). 꺼내는 방법:
 *     adb exec-out run-as com.hanse.SoftBrain cat files/capture_log.txt
 */
public class PaymentNotificationListener extends NotificationListenerService {

    private static final String TAG = "PaymentListener";
    private static final String CAPTURE_LOG_FILE = "capture_log.txt";
    /** 수집 파일이 이 크기를 넘으면 비우고 새로 시작 */
    private static final long CAPTURE_LOG_MAX_BYTES = 1024 * 1024;
    /** 중복 알림 판별 시간 (±2분). 기준은 ExpenseDao.countDuplicates 참고 */
    private static final long DUPLICATE_WINDOW_MS = 2 * 60 * 1000L;
    /** 취소 알림이 오면 이 기간 안의 결제만 취소 대상으로 찾음 */
    private static final long CANCEL_LOOKBACK_MS = 30L * 24 * 60 * 60 * 1000;

    // 직전에 처리한 취소 (푸시+문자로 같은 취소가 두 번 올 때 한 건만 지우기 위함). IO 스레드에서만 사용
    private int lastCancelAmount;
    private long lastCancelTime;

    private boolean isDebug;

    /** 알림 접근 권한이 켜져 있는지 (설정 화면 표시용) */
    public static boolean isPermissionGranted(Context context) {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.getPackageName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isDebug = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        String pkg = sbn.getPackageName();
        if (getPackageName().equals(pkg)) return;

        PaymentSource source = PaymentSource.fromPackage(pkg, isDebug);
        if (source == null) return;

        Notification n = sbn.getNotification();
        // 묶음 알림의 요약본은 개별 알림과 내용이 겹치므로 무시
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;

        SharedPreferences prefs = getSharedPreferences("SoftBrainPrefs", MODE_PRIVATE);
        if (source.prefKey != null && !prefs.getBoolean(source.prefKey, true)) return;

        Bundle extras = n.extras;
        String title = str(extras.getCharSequence(Notification.EXTRA_TITLE));
        String text = str(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if (text.isEmpty()) text = str(extras.getCharSequence(Notification.EXTRA_TEXT));

        trace("pkg=" + pkg + " | title=" + title + " | text=" + text);

        PaymentParser.Result result = PaymentParser.parse(title, text);
        if (result == null) {
            trace("→ 결제 알림 아님, 무시");
            return;
        }

        long postTime = sbn.getPostTime();
        String merchant = result.merchant;
        if (result.cancelled) {
            AppDatabase.IO.execute(() -> handleCancel(result, postTime));
            return;
        }
        String rawText = (title + "\n" + text).trim();
        ExpenseItem item = new ExpenseItem(
                merchant,                               // 이름 (사용자가 나중에 수정 가능)
                merchant,                               // 장소
                result.amount,
                CategoryClassifier.classify(merchant),
                postTime,
                source.label,
                true,
                rawText
        );

        AppDatabase.IO.execute(() -> {
            ExpenseDao dao = AppDatabase.get(this).expenseDao();
            int dup = dao.countDuplicates(result.amount,
                    postTime - DUPLICATE_WINDOW_MS, postTime + DUPLICATE_WINDOW_MS,
                    source.label, rawText);
            if (dup > 0) {
                trace("→ 중복 알림, 무시 (" + result.amount + "원)");
                return;
            }
            dao.insert(item);
            trace("→ 저장: " + merchant + " " + result.amount + "원 [" + item.getCategory() + "]");
        });
    }

    /** 취소 알림: 가맹점과 금액이 모두 맞는 내역 한 건만 삭제. 가맹점을 모르면 건드리지 않음 (IO 스레드) */
    private void handleCancel(PaymentParser.Result result, long postTime) {
        if (PaymentParser.UNKNOWN_MERCHANT.equals(result.merchant)) {
            trace("→ 취소: 가맹점을 몰라서 삭제 안 함 (" + result.amount + "원)");
            return;
        }
        if (result.amount == lastCancelAmount
                && Math.abs(postTime - lastCancelTime) <= DUPLICATE_WINDOW_MS) {
            trace("→ 취소: 방금 처리한 취소와 중복, 무시 (" + result.amount + "원)");
            return;
        }
        ExpenseDao dao = AppDatabase.get(this).expenseDao();
        ExpenseItem target = dao.findCancelTarget(result.amount, result.merchant,
                postTime - CANCEL_LOOKBACK_MS, postTime);
        if (target == null) {
            trace("→ 취소: 맞는 결제 내역 없음 (" + result.merchant + " " + result.amount + "원)");
            return;
        }
        dao.delete(target);
        lastCancelAmount = result.amount;
        lastCancelTime = postTime;
        trace("→ 취소: 삭제함 (" + target.getTitle() + " " + result.amount + "원)");
    }

    /**
     * 디버그 빌드에서만 Logcat과 수집 파일에 기록합니다.
     * (원문에 결제 정보가 들어 있으므로 릴리스 빌드에서는 아무것도 남기지 않음)
     */
    private void trace(String message) {
        if (!isDebug) return;
        Log.d(TAG, message);
        String stamp = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA).format(new Date());
        String line = stamp + " " + message.replace("\n", "⏎") + "\n";
        File file = new File(getFilesDir(), CAPTURE_LOG_FILE);
        AppDatabase.IO.execute(() -> {
            boolean append = file.length() < CAPTURE_LOG_MAX_BYTES;
            try (FileWriter w = new FileWriter(file, append)) {
                w.write(line);
            } catch (IOException e) {
                Log.w(TAG, "수집 파일 기록 실패", e);
            }
        });
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        // 배터리 최적화 등으로 연결이 끊기면 다시 연결 요청
        requestRebind(new ComponentName(this, PaymentNotificationListener.class));
    }

    private static String str(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
