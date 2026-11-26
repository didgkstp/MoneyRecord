package com.hanse.SoftBrain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 결제 알림 문구 → (금액, 가맹점) 추출기.
 * 안드로이드 API를 쓰지 않는 순수 Java 클래스라서 JUnit(PaymentParserTest)으로 바로 테스트할 수 있습니다.
 *
 * ⚠️ 앱마다 알림 문구 형식이 다르고 업데이트로 바뀔 수 있습니다.
 *    실기기에서 Logcat 태그 "PaymentListener"로 원본 문구를 모은 뒤,
 *    PaymentParserTest에 예시를 추가하고 이 규칙을 보완하세요.
 */
public final class PaymentParser {

    /** 파싱 결과. 결제 알림이 아니면 parse()가 null을 돌려줍니다. */
    public static final class Result {
        public final int amount;
        public final String merchant;
        /** true면 결제가 아니라 "결제 취소" 알림 */
        public final boolean cancelled;

        Result(int amount, String merchant, boolean cancelled) {
            this.amount = amount;
            this.merchant = merchant;
            this.cancelled = cancelled;
        }
    }

    public static final String UNKNOWN_MERCHANT = "알 수 없는 가맹점";

    // 결제(지출)로 볼 키워드
    // ("일시불/할부/N개월"은 승인 단어 없이 오는 카드 문자에서 결제 표시로 쓰임)
    private static final Pattern PAY_KEYWORD = Pattern.compile("결제|승인|출금|사용|지불|일시불|할부|\\d+\\s*개월");
    // 지출이 아닌 알림 (입금/환불/인증번호/청구 예정/광고 등) → 무시
    private static final Pattern NOT_EXPENSE = Pattern.compile(
            "입금|환불|충전|거절|실패|받았|인증\\s*번호|인증\\s*코드|예정|청구|\\(광고\\)"
                    + "|명세서|이용대금|사용\\s*금액|이용\\s*금액|결제일|합계");
    private static final Pattern CANCEL = Pattern.compile("취소");

    // 금액이 아닌 숫자 (잔액·누적·한도) 제거용
    private static final Pattern NON_PAYMENT_AMOUNT =
            Pattern.compile("(잔액|누적|한도|남은\\s*금액|사용가능)\\s*[:：]?\\s*[0-9,]+\\s*원");
    private static final Pattern AMOUNT =
            Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*원");

    // 예) "스타벅스 역삼점에서 4,500원 결제"
    private static final Pattern MERCHANT_BEFORE_EOSEO =
            Pattern.compile("([^\\n]+?)\\s*에서\\s*[0-9,]+\\s*원");
    // 예) 카드 문자 "... 11/19 13:20 스타벅스 누적 ..."
    private static final Pattern MERCHANT_AFTER_DATETIME =
            Pattern.compile("\\d{1,2}/\\d{1,2}\\s+\\d{1,2}:\\d{2}\\s+([^\\n]+)");
    // 예) "4,500원 결제 | 스타벅스", "출금 4,500원 스타벅스"
    private static final Pattern MERCHANT_AFTER_AMOUNT =
            Pattern.compile("[0-9,]+\\s*원\\s*(?:\\([^)]*\\))?\\s*(?:결제|승인|출금|사용)?\\s*(?:취소)?\\s*[|:·\\-]?\\s*([^\\n]*)");

    // 가맹점 문자열 끝에 붙는 군더더기
    private static final Pattern MERCHANT_NOISE = Pattern.compile(
            "(누적|잔액).*$|(일시불|할부\\S*|체크|취소|결제|승인|출금|사용|완료|되었습니다|했어요|됐어요)|[\\[\\]()|]");

    private PaymentParser() {
    }

    /**
     * @param title 알림 제목 (Notification.EXTRA_TITLE)
     * @param text  알림 본문 (EXTRA_BIG_TEXT가 있으면 그것, 없으면 EXTRA_TEXT)
     * @return 결제 알림이면 Result, 아니면 null
     */
    public static Result parse(String title, String text) {
        String t = title == null ? "" : title.trim();
        String b = text == null ? "" : text.trim();
        String full = (t + "\n" + b).trim();
        if (full.isEmpty()) return null;

        if (NOT_EXPENSE.matcher(full).find()) return null;
        boolean cancelled = CANCEL.matcher(full).find();
        if (!cancelled && !PAY_KEYWORD.matcher(full).find()) return null;

        String cleaned = NON_PAYMENT_AMOUNT.matcher(full).replaceAll("");
        Matcher am = AMOUNT.matcher(cleaned);
        if (!am.find()) return null;
        int amount;
        try {
            amount = Integer.parseInt(am.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0) return null;

        String merchant = findMerchant(cleaned, t);
        return new Result(amount, merchant == null ? UNKNOWN_MERCHANT : merchant,
                cancelled);
    }

    private static String findMerchant(String s, String title) {
        Matcher m = MERCHANT_BEFORE_EOSEO.matcher(s);
        if (m.find()) {
            String candidate = m.group(1);
            // "홍길동님, 스타벅스" → "스타벅스"
            int cut = Math.max(candidate.lastIndexOf(','), candidate.lastIndexOf("님"));
            if (cut >= 0) candidate = candidate.substring(cut + 1);
            String c = clean(candidate);
            if (c != null) return c;
        }

        m = MERCHANT_AFTER_DATETIME.matcher(s);
        if (m.find()) {
            String c = clean(m.group(1));
            if (c != null) return c;
        }

        m = MERCHANT_AFTER_AMOUNT.matcher(s);
        if (m.find()) {
            String c = clean(m.group(1));
            if (c != null) return c;
            // 금액 줄 뒤가 비어 있으면 다음 줄을 가맹점으로 사용
            int next = s.indexOf('\n', m.end());
            if (next >= 0) {
                int end = s.indexOf('\n', next + 1);
                c = clean(s.substring(next + 1, end < 0 ? s.length() : end));
                if (c != null) return c;
            }
            // 예) "결제 완료\n스타벅스 역삼점\n4,500원": 금액 줄 바로 위 줄을 가맹점으로 사용
            String before = s.substring(0, m.start());
            int lineStart = before.lastIndexOf('\n');
            if (lineStart >= 0) {
                String prevLines = before.substring(0, lineStart);
                String prev = prevLines.substring(prevLines.lastIndexOf('\n') + 1);
                // 알림 제목(앱·발신자 이름)은 가맹점 후보에서 제외
                if (!prev.trim().equals(title) && !looksLikeHeaderOrName(prev)) {
                    c = clean(prev);
                    if (c != null) return c;
                }
            }
        }
        return null;
    }

    /** "[Web발신]", 가려진 이름("홍*동"), "~님", 카드번호 줄은 가맹점이 아님 */
    private static boolean looksLikeHeaderOrName(String line) {
        String l = line.trim();
        return l.contains("Web발신") || l.contains("*") || l.endsWith("님")
                || l.matches(".*\\d{4}.*(승인|취소).*");
    }

    private static String clean(String raw) {
        if (raw == null) return null;
        String c = MERCHANT_NOISE.matcher(raw).replaceAll(" ");
        c = c.replaceAll("\\s+", " ").trim();
        c = c.replaceAll("^[,.:·\\-]+|[,.:·\\-]+$", "").trim();
        // 남은 게 숫자/금액뿐이면 가맹점이 아님
        if (c.isEmpty() || c.matches("[0-9,/: 원]+")) return null;
        if (c.length() > 30) c = c.substring(0, 30);
        return c;
    }
}
