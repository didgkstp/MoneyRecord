package com.hanse.SoftBrain;

/**
 * 알림을 보낸 앱의 패키지명 → 인식 출처(설정 화면 스위치) 매핑.
 *
 * ⚠️ 패키지명은 실기기에서 꼭 확인하세요. 확인 방법:
 *    adb shell pm list packages | grep -i toss
 *    또는 리스너 Logcat("PaymentListener")에 찍히는 pkg 값
 * 앱을 추가하려면 해당 *_PACKAGES 배열에 패키지명을 넣으면 됩니다.
 *
 * 일부러 넣지 않은 앱:
 * - 뱅크샐러드(com.rainist.banksalad2): 다른 앱 결제를 모아 다시 알려줘서 중복됨
 * - 삼성 월렛(com.samsung.android.spay): 카드 앱 알림과 겹칠 수 있어 보류
 * - 카카오톡(com.kakao.talk): 카드사 알림톡이 오지만 개인 대화까지 읽게 되므로,
 *   넣는다면 발신자(제목) 이름으로 걸러야 함
 */
public enum PaymentSource {
    KAKAOBANK("app_kakaobank", "카카오뱅크 알림"),
    TOSS("app_toss", "토스 알림"),
    SMS("app_sms", "문자 메시지(SMS)"),
    CARD("app_card", "카드사 앱 알림"),
    BANK("app_bank", "은행 앱 알림"),
    PAY("app_pay", "간편결제 알림"),
    /** 디버그 빌드에서 adb로 띄운 테스트 알림 (com.android.shell) */
    ADB_TEST(null, "테스트 알림");

    /** 설정 화면 스위치의 SharedPreferences 키 (null이면 항상 켜짐) */
    public final String prefKey;
    /** 타임라인에 표시할 출처 문구 */
    public final String label;

    PaymentSource(String prefKey, String label) {
        this.prefKey = prefKey;
        this.label = label;
    }

    private static final String[] SMS_PACKAGES = {
            "com.samsung.android.messaging",       // 삼성 메시지
            "com.google.android.apps.messaging",   // 구글 메시지
            "com.android.mms",                     // 기본 문자 (일부 기기)
    };

    private static final String[] CARD_PACKAGES = {
            "com.shcard.smartpay",                 // 신한 SOL페이
            "com.kbcard.cxh.appcard",              // KB Pay
            "kr.co.samsungcard.mpocket",           // 삼성카드
            "com.hyundaicard.appcard",             // 현대카드
    };

    private static final String[] BANK_PACKAGES = {
            "com.kbstar.kbbank",                   // KB스타뱅킹
            "com.shinhan.sbanking",                // 신한 SOL뱅크
            "com.kbankwith.smartbank",             // 케이뱅크
    };

    private static final String[] PAY_PACKAGES = {
            "com.kakaopay.app",                    // 카카오페이
            "com.naverfin.payapp",                 // 네이버페이
            "com.nhnent.payapp",                   // PAYCO
    };

    /**
     * @param isDebugBuild true면 adb 테스트 알림(com.android.shell)도 받아들임
     * @return 인식 대상 앱이 아니면 null
     */
    public static PaymentSource fromPackage(String pkg, boolean isDebugBuild) {
        if (pkg == null) return null;
        switch (pkg) {
            case "com.kakaobank.channel":
                return KAKAOBANK;
            case "viva.republica.toss":
                return TOSS;
            case "com.android.shell":
                return isDebugBuild ? ADB_TEST : null;
        }
        for (String p : SMS_PACKAGES) if (p.equals(pkg)) return SMS;
        for (String p : CARD_PACKAGES) if (p.equals(pkg)) return CARD;
        for (String p : BANK_PACKAGES) if (p.equals(pkg)) return BANK;
        for (String p : PAY_PACKAGES) if (p.equals(pkg)) return PAY;
        return null;
    }
}
