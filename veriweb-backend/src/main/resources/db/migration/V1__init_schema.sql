CREATE TABLE analysis
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    url          VARCHAR(2048) NOT NULL,
    total_score  INT          NOT NULL,
    grade        VARCHAR(10)  NOT NULL COMMENT 'SAFE / CAUTION / DANGER',
    summary      TEXT         NOT NULL,
    published_at DATETIME     NULL,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE analysis_score
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    analysis_id BIGINT      NOT NULL,
    category    VARCHAR(50) NOT NULL COMMENT 'DOMAIN / AUTHOR / REFERENCE / CONSISTENCY / MANIPULATION / ACADEMIC / GOV',
    score       INT         NOT NULL,
    max_score   INT         NOT NULL,
    reason      TEXT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_analysis_score_analysis FOREIGN KEY (analysis_id) REFERENCES analysis (id)
);

CREATE TABLE recommended_article
(
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    analysis_id  BIGINT        NOT NULL,
    title        VARCHAR(512)  NOT NULL,
    url          VARCHAR(2048) NOT NULL,
    source       VARCHAR(255)  NOT NULL,
    published_at DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recommended_article_analysis FOREIGN KEY (analysis_id) REFERENCES analysis (id),
    CONSTRAINT uq_recommended_article UNIQUE (analysis_id, url(766))
);

CREATE TABLE feed_article
(
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    title         VARCHAR(512)  NOT NULL,
    url           VARCHAR(2048) NOT NULL,
    source        VARCHAR(255)  NOT NULL,
    thumbnail_url VARCHAR(2048) NULL,
    category      VARCHAR(50)   NOT NULL COMMENT 'POLITICS / ECONOMY / IT / HEALTH',
    trust_score   INT           NOT NULL,
    grade         VARCHAR(10)   NOT NULL COMMENT 'SAFE / CAUTION / DANGER',
    published_at  DATETIME      NOT NULL,
    created_at    DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_feed_article_url UNIQUE (url(768))
);
