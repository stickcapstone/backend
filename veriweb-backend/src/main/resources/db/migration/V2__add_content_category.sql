ALTER TABLE analysis
    ADD COLUMN content_category VARCHAR(50) NULL COMMENT 'ContentCategory enum (NEWS, TECH_BLOG, ...)' AFTER grade;
