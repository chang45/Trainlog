package org.examplexxx.trainlog.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.examplexxx.trainlog.dto.TrainingRecordQueryDTO;
import org.examplexxx.trainlog.entity.TrainingRecord;
import org.examplexxx.trainlog.vo.TrainingStatsVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 训练记录数据访问。动态查询（分页筛选）写在 mapper/TrainingRecordMapper.xml 里。
 */
public interface TrainingRecordMapper {

    @Insert("INSERT INTO training_record "
            + "(user_id, title, category, type, training_date, duration_minutes, "
            + " distance_km, sets, reps, weight_kg, calories, intensity, notes, created_at, updated_at) "
            + "VALUES "
            + "(#{userId}, #{title}, #{category}, #{type}, #{trainingDate}, #{durationMinutes}, "
            + " #{distanceKm}, #{sets}, #{reps}, #{weightKg}, #{calories}, #{intensity}, #{notes}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TrainingRecord record);

    @Select("SELECT * FROM training_record WHERE id = #{id} AND user_id = #{userId}")
    TrainingRecord findById(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE training_record SET "
            + "title = #{title}, category = #{category}, type = #{type}, training_date = #{trainingDate}, "
            + "duration_minutes = #{durationMinutes}, distance_km = #{distanceKm}, sets = #{sets}, "
            + "reps = #{reps}, weight_kg = #{weightKg}, calories = #{calories}, intensity = #{intensity}, "
            + "notes = #{notes}, updated_at = #{updatedAt} "
            + "WHERE id = #{id} AND user_id = #{userId}")
    int update(TrainingRecord record);

    @Delete("DELETE FROM training_record WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") Long id, @Param("userId") Long userId);

    /** 分页查询，SQL 在 TrainingRecordMapper.xml */
    List<TrainingRecord> findPage(@Param("userId") Long userId, @Param("q") TrainingRecordQueryDTO query);

    /** 分页总数，SQL 在 TrainingRecordMapper.xml */
    long countPage(@Param("userId") Long userId, @Param("q") TrainingRecordQueryDTO query);

    /** 时间段汇总，SQL 在 TrainingRecordMapper.xml */
    TrainingStatsVO totals(@Param("userId") Long userId,
                           @Param("from") LocalDate from,
                           @Param("to") LocalDate to);

    /** 每日训练量，SQL 在 TrainingRecordMapper.xml */
    List<TrainingStatsVO.DailyStat> daily(@Param("userId") Long userId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    /** 按项目分布，SQL 在 TrainingRecordMapper.xml */
    List<TrainingStatsVO.TypeStat> byType(@Param("userId") Long userId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    /** 最近的训练日期（降序），用于算连续打卡 */
    @Select("SELECT DISTINCT training_date FROM training_record "
            + "WHERE user_id = #{userId} ORDER BY training_date DESC LIMIT 400")
    List<LocalDate> recentDates(@Param("userId") Long userId);
}
