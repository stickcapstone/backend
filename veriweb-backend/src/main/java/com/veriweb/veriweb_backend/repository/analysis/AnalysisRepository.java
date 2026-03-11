package com.veriweb.veriweb_backend.repository.analysis;

import com.veriweb.veriweb_backend.entity.analysis.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    Optional<Analysis> findByUrl(String url);
}
