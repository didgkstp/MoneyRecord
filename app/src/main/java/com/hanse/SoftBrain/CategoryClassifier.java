package com.hanse.SoftBrain;

import java.util.Locale;

/**
 * 가맹점 이름 키워드로 지출 분류를 추측합니다. (순수 Java, 서버/AI 없음)
 * 못 맞히면 "기타" → 사용자가 타임라인에서 직접 수정.
 * 키워드는 자유롭게 추가하세요. 위에 있는 분류가 먼저 검사됩니다.
 */
public final class CategoryClassifier {

    // {분류, 키워드...} — "KTX"가 통신의 "KT"에 걸리지 않도록 교통을 통신보다 먼저 둡니다.
    private static final String[][] RULES = {
            {"교통", "택시", "카카오t", "티머니", "버스", "지하철", "코레일", "ktx", "srt", "주유", "오일", "쏘카", "그린카", "따릉이", "고속도로", "하이패스"},
            {"식비", "스타벅스", "커피", "카페", "이디야", "투썸", "메가mgc", "메가커피", "빽다방", "컴포즈", "맥도날드", "버거", "롯데리아", "kfc",
                    "배달의민족", "배민", "요기요", "쿠팡이츠", "편의점", "gs25", "세븐일레븐", "이마트24", "cu ", "씨유",
                    "식당", "치킨", "피자", "베이커리", "파리바게뜨", "뚜레쥬르", "김밥", "분식", "학식", "국밥", "마라", "고기", "돼지"},
            {"의료", "약국", "병원", "의원", "치과", "한의원", "내과", "안과", "피부과"},
            {"여가", "cgv", "메가박스", "롯데시네마", "넷플릭스", "netflix", "유튜브", "youtube", "스포티파이", "멜론", "노래방", "pc방",
                    "게임", "steam", "스팀", "인터파크", "티켓", "야놀자", "여기어때"},
            {"통신", "skt", "sk텔레콤", "kt ", "lg u+", "lgu+", "유플러스", "알뜰폰", "통신요금"},
            {"주거", "관리비", "월세", "전기요금", "한국전력", "도시가스", "가스요금", "수도"},
    };

    private CategoryClassifier() {
    }

    public static String classify(String merchant) {
        if (merchant == null) return "기타";
        // 끝에 공백을 붙여 "CU", "KT"처럼 짧은 키워드가 단어 끝에서도 맞도록 함
        String m = merchant.toLowerCase(Locale.ROOT) + " ";
        for (String[] rule : RULES) {
            for (int i = 1; i < rule.length; i++) {
                if (m.contains(rule[i])) return rule[0];
            }
        }
        return "기타";
    }
}
