package com.hanse.SoftBrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * 결제 알림 파서 테스트. 실행: ./gradlew :app:testDebugUnitTest
 * ⚠️ 아래 예시 문구는 "이런 형식일 것"이라고 가정한 것입니다.
 *    실기기 Logcat("PaymentListener")에서 진짜 문구를 모으면 여기에 추가하세요.
 */
public class PaymentParserTest {

    private static void assertPayment(String title, String text, int amount, String merchant) {
        PaymentParser.Result r = PaymentParser.parse(title, text);
        assertNotNull("결제로 인식돼야 함: " + text, r);
        assertEquals(amount, r.amount);
        assertEquals(merchant, r.merchant);
    }

    @Test
    public void tossStyle_merchantBeforeEseo() {
        assertPayment("토스", "스타벅스 역삼점에서 4,500원 결제했어요", 4500, "스타벅스 역삼점");
    }

    @Test
    public void tossStyle_withUserName() {
        assertPayment("토스", "홍길동님, 버거킹에서 12,900원 결제", 12900, "버거킹");
    }

    @Test
    public void cardSms_merchantAfterDateTime_ignoresCumulative() {
        assertPayment("15447200",
                "[Web발신]\n신한카드(1234)승인 홍*동 12,000원(일시불)11/19 13:20 버거킹 한세점 누적345,000원",
                12000, "버거킹 한세점");
    }

    @Test
    public void bankStyle_amountInTitle_merchantInText_ignoresBalance() {
        assertPayment("출금 4,500원", "스타벅스 | 잔액 120,000원", 4500, "스타벅스");
    }

    @Test
    public void merchantOnNextLine() {
        assertPayment("KB Pay", "4500원 결제 완료\nGS25 역삼점", 4500, "GS25 역삼점");
    }

    @Test
    public void unknownMerchant() {
        assertPayment("카드", "8,800원 승인", 8800, PaymentParser.UNKNOWN_MERCHANT);
    }

    @Test
    public void ignoresNonExpense() {
        assertNull(PaymentParser.parse("토스", "홍길동님이 10,000원을 보냈어요"));
        assertNull(PaymentParser.parse("카카오뱅크", "입금 50,000원 홍길동"));
        assertNull(PaymentParser.parse("토스", "토스뱅크 통장에 100,000원 충전 완료"));
        assertNull(PaymentParser.parse("메시지", "오늘 점심 뭐 먹을래?"));
        assertNull(PaymentParser.parse(null, null));
    }

    @Test
    public void cancellation() {
        PaymentParser.Result r = PaymentParser.parse("신한카드", "12,000원 승인 취소 버거킹");
        assertNotNull(r);
        assertEquals(true, r.cancelled);
        assertEquals("버거킹", r.merchant);
        r = PaymentParser.parse("토스", "스타벅스에서 4,500원 결제 취소");
        assertEquals(true, r.cancelled);
        assertEquals("스타벅스", r.merchant);
    }

    @Test
    public void ignoresVerificationBillsAndAds() {
        assertNull(PaymentParser.parse("카드", "결제 인증번호 [123456] 스타벅스 4,500원"));
        assertNull(PaymentParser.parse("카드", "이번 달 결제 예정 금액 300,000원"));
        assertNull(PaymentParser.parse("(광고) 쇼핑", "결제하면 5,000원 할인"));
    }

    @Test
    public void paymentWithPointsIsStillPayment() {
        assertPayment("토스", "스타벅스에서 4,500원 결제 45원 적립", 4500, "스타벅스");
    }

    // ───── 카드사 문자/푸시에서 흔한 여러 줄 형식 (실제 문구를 모으면 교체·추가) ─────

    @Test
    public void cardSms_multiLine_kbStyle() {
        assertPayment("KB국민카드",
                "[Web발신]\nKB국민카드1234승인\n홍*동\n12,000원 일시불\n11/26 10:15\n스타벅스역삼\n누적123,456원",
                12000, "스타벅스역삼");
    }

    @Test
    public void cardSms_installment() {
        assertPayment("현대카드",
                "[Web발신]\n현대카드 M 승인\n홍*동\n360,000원 3개월\n11/26 10:15\n하이마트 강남점\n누적1,234,560원",
                360000, "하이마트 강남점");
    }

    @Test
    public void cardSms_samsungStyle() {
        assertPayment("삼성카드",
                "[Web발신]\n삼성1234승인 홍*동\n8,800원 일시불\n11/26 10:15 카카오T 택시\n누적99,000원",
                8800, "카카오T 택시");
    }

    @Test
    public void cardSms_nameWithNim_singleLine() {
        assertPayment("하나카드",
                "[Web발신]\n하나(1*2*)홍*동님 5,500원 일시불 11/26 10:15 GS25 역삼점 누적55,000원",
                5500, "GS25 역삼점");
    }

    @Test
    public void checkCard_dateBeforeAmount() {
        assertPayment("KB국민체크",
                "[Web발신]\nKB국민체크(1234)\n홍*동님\n11/26 10:15\n12,000원\n스타벅스 사용",
                12000, "스타벅스");
    }

    @Test
    public void payApp_completedFirstLine() {
        assertPayment("카카오페이", "결제 완료\n스타벅스 역삼점\n4,500원", 4500, "스타벅스 역삼점");
    }

    @Test
    public void bankWithdrawal_balanceFirst() {
        assertPayment("KB스타뱅킹", "잔액 120,000원\n출금 4,500원 스타벅스", 4500, "스타벅스");
    }

    @Test
    public void ignoresStatementsAndUsageSummaries() {
        assertNull(PaymentParser.parse("KB국민카드", "11월 이용대금 명세서가 도착했어요. 결제금액 300,000원"));
        assertNull(PaymentParser.parse("신한카드", "이번 달 사용금액 합계 245,000원"));
        assertNull(PaymentParser.parse("토스", "결제일 알림: 12/14 결제금액 120,000원"));
    }

    @Test
    public void ignoresForeignCurrencyWithoutWon() {
        assertNull(PaymentParser.parse("KB국민카드", "[Web발신]\nKB국민카드1234승인\nUSD 12.50\n11/26 10:15\nAMAZON"));
    }

    @Test
    public void cardSms_cancelMultiLine() {
        PaymentParser.Result r = PaymentParser.parse("KB국민카드",
                "[Web발신]\nKB국민카드1234취소\n홍*동\n12,000원\n11/26 10:20\n스타벅스역삼");
        assertNotNull(r);
        assertEquals(true, r.cancelled);
        assertEquals(12000, r.amount);
        assertEquals("스타벅스역삼", r.merchant);
    }

    @Test
    public void classifier() {
        assertEquals("식비", CategoryClassifier.classify("스타벅스 역삼점"));
        assertEquals("식비", CategoryClassifier.classify("CU 한세대점"));
        assertEquals("교통", CategoryClassifier.classify("코레일 KTX"));
        assertEquals("통신", CategoryClassifier.classify("SKT 자동이체"));
        assertEquals("의료", CategoryClassifier.classify("온누리약국"));
        assertEquals("기타", CategoryClassifier.classify("알 수 없는 가맹점"));
    }
}
