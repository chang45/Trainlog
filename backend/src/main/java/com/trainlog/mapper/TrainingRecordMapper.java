package com.trainlog.mapper;

import com.trainlog.entity.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TrainingRecordMapper
        extends JpaRepository<TrainingRecord, Long>, JpaSpecificationExecutor<TrainingRecord> {

    @Query("select distinct r.trainingDate from TrainingRecord r "
            + "where r.userId = :userId order by r.trainingDate desc")
    List<LocalDate> findTrainingDates(@Param("userId") Long userId);

    long countByUserId(Long userId);
}
