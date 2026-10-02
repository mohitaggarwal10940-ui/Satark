package com.satark.backend.repository;

import com.satark.backend.model.Analysis;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AnalysisRepository extends MongoRepository<Analysis, String> {
}