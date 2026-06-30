package com.veriweb.veriweb_backend.entity.analysis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum ContentCategory {

    NEWS("뉴스/언론", w(
            ScoreCategory.DOMAIN, 25, ScoreCategory.AUTHOR, 20,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.MANIPULATION, 20,
            ScoreCategory.REFERENCE, 10), 100),

    TECH_BLOG("개발/기술 블로그", w(
            ScoreCategory.AUTHOR, 25, ScoreCategory.REFERENCE, 45,
            ScoreCategory.CONSISTENCY, 20, ScoreCategory.MANIPULATION, 10), 100),

    ACADEMIC_PAPER("학술 논문/리포트", w(
            ScoreCategory.ACADEMIC, 30, ScoreCategory.AUTHOR, 25,
            ScoreCategory.REFERENCE, 25, ScoreCategory.DOMAIN, 10,
            ScoreCategory.CONSISTENCY, 10), 100),

    GOVERNMENT("정부/공공기관", w(
            ScoreCategory.DOMAIN, 35, ScoreCategory.AUTHOR, 25,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.MANIPULATION, 15), 100),

    COMMUNITY("커뮤니티/포럼", w(
            ScoreCategory.CONSISTENCY, 30, ScoreCategory.REFERENCE, 25,
            ScoreCategory.AUTHOR, 20, ScoreCategory.MANIPULATION, 25), 100),

    SNS("SNS/개인 미디어", w(
            ScoreCategory.AUTHOR, 30, ScoreCategory.MANIPULATION, 30,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.REFERENCE, 15), 100),

    HEALTH("의료/건강", w(
            ScoreCategory.ACADEMIC, 30, ScoreCategory.GOV, 25,
            ScoreCategory.AUTHOR, 20, ScoreCategory.MANIPULATION, 15,
            ScoreCategory.DOMAIN, 10), 100),

    LEGAL("법률/법령", w(
            ScoreCategory.GOV, 35, ScoreCategory.AUTHOR, 25,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.MANIPULATION, 15), 100),

    FINANCE("금융/경제", w(
            ScoreCategory.AUTHOR, 25, ScoreCategory.DOMAIN, 20,
            ScoreCategory.CONSISTENCY, 20, ScoreCategory.REFERENCE, 20,
            ScoreCategory.MANIPULATION, 15), 100),

    EDUCATION("교육/학습", w(
            ScoreCategory.AUTHOR, 25, ScoreCategory.REFERENCE, 25,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.DOMAIN, 15,
            ScoreCategory.MANIPULATION, 10), 100),

    WIKI("위키/백과사전", w(
            ScoreCategory.REFERENCE, 30, ScoreCategory.CONSISTENCY, 25,
            ScoreCategory.AUTHOR, 20, ScoreCategory.MANIPULATION, 15,
            ScoreCategory.DOMAIN, 10), 100),

    FACTCHECK("팩트체크 전문", w(
            ScoreCategory.DOMAIN, 30, ScoreCategory.REFERENCE, 30,
            ScoreCategory.AUTHOR, 20, ScoreCategory.CONSISTENCY, 20), 100),

    STATISTICS("통계/데이터 리포트", w(
            ScoreCategory.REFERENCE, 30, ScoreCategory.AUTHOR, 25,
            ScoreCategory.DOMAIN, 20, ScoreCategory.CONSISTENCY, 15,
            ScoreCategory.MANIPULATION, 10), 100),

    NEWSLETTER("뉴스레터", w(
            ScoreCategory.AUTHOR, 30, ScoreCategory.REFERENCE, 25,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.MANIPULATION, 20), 100),

    SCIENCE("과학/환경", w(
            ScoreCategory.ACADEMIC, 30, ScoreCategory.AUTHOR, 25,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.MANIPULATION, 10,
            ScoreCategory.DOMAIN, 10), 100),

    REAL_ESTATE("부동산/세금", w(
            ScoreCategory.GOV, 30, ScoreCategory.AUTHOR, 25,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.MANIPULATION, 20), 100),

    SPORTS("스포츠/엔터테인먼트", w(
            ScoreCategory.CONSISTENCY, 30, ScoreCategory.MANIPULATION, 25,
            ScoreCategory.AUTHOR, 25, ScoreCategory.REFERENCE, 20), 100),

    TRAVEL("여행/지역", w(
            ScoreCategory.MANIPULATION, 30, ScoreCategory.AUTHOR, 25,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.REFERENCE, 20), 100),

    FOOD("음식/레시피", w(
            ScoreCategory.AUTHOR, 25, ScoreCategory.REFERENCE, 25,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.MANIPULATION, 25), 100),

    RELIGION("종교/철학", w(
            ScoreCategory.AUTHOR, 30, ScoreCategory.REFERENCE, 25,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.MANIPULATION, 20), 100),

    CORPORATE("기업 보도자료/IR", w(
            ScoreCategory.DOMAIN, 25, ScoreCategory.AUTHOR, 25,
            ScoreCategory.REFERENCE, 25, ScoreCategory.MANIPULATION, 25), 100),

    REVIEW("제품 리뷰/비교", w(
            ScoreCategory.REFERENCE, 30, ScoreCategory.CONSISTENCY, 25,
            ScoreCategory.AUTHOR, 25, ScoreCategory.MANIPULATION, 20), 100),

    MARKETING("마케팅 콘텐츠", w(
            ScoreCategory.MANIPULATION, 35, ScoreCategory.REFERENCE, 25,
            ScoreCategory.CONSISTENCY, 20, ScoreCategory.AUTHOR, 20), 70),

    PETITION("청원/캠페인", w(
            ScoreCategory.REFERENCE, 30, ScoreCategory.CONSISTENCY, 25,
            ScoreCategory.AUTHOR, 25, ScoreCategory.MANIPULATION, 20), 100),

    POLITICS("정치/선거", w(
            ScoreCategory.MANIPULATION, 30, ScoreCategory.REFERENCE, 25,
            ScoreCategory.CONSISTENCY, 25, ScoreCategory.AUTHOR, 20), 75);

    private final String displayName;
    private final Map<ScoreCategory, Integer> weights;
    private final int scoreCap;

    public int calculateTotalScore(Map<ScoreCategory, Integer> rawScores) {
        // 감점제: 각 항목이 100점에서 시작, 문제 발견 시만 감점
        // penalty = (100 - rawScore) * weight / 100
        int totalDeduction = 0;
        for (Map.Entry<ScoreCategory, Integer> entry : weights.entrySet()) {
            int raw = rawScores.getOrDefault(entry.getKey(), 100); // 누락 항목은 감점 없음
            int penalty = 100 - raw;
            totalDeduction += penalty * entry.getValue() / 100;
        }
        return Math.min(Math.max(100 - totalDeduction, 0), scoreCap);
    }

    public static ContentCategory fromString(String name) {
        if (name == null || name.isBlank()) return NEWS;
        try {
            return ContentCategory.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEWS;
        }
    }

    private static Map<ScoreCategory, Integer> w(Object... pairs) {
        Map<ScoreCategory, Integer> map = new EnumMap<>(ScoreCategory.class);
        for (int i = 0; i < pairs.length - 1; i += 2) {
            map.put((ScoreCategory) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }
}
