package com.veriweb.veriweb_backend.entity.analysis;

public enum Grade {
    SAFE, CAUTION, DANGER;

    public static Grade from(int score) {
        if (score >= 80) return SAFE;
        if (score >= 50) return CAUTION;
        return DANGER;
    }
}
