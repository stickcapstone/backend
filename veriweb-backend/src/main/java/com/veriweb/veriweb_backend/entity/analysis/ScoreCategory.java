package com.veriweb.veriweb_backend.entity.analysis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScoreCategory {
    DOMAIN(15),
    AUTHOR(10),
    REFERENCE(15),
    CONSISTENCY(20),
    MANIPULATION(15),
    ACADEMIC(15),
    GOV(10);

    private final int maxScore;
}
