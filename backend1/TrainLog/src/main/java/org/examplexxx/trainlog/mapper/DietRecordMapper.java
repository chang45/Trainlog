package org.examplexxx.trainlog.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.examplexxx.trainlog.entity.DietRecord;

import java.time.LocalDate;
import java.util.List;

public interface DietRecordMapper {

    @Insert("INSERT INTO diet_record "
            + "(user_id, meal_type, food_name, calories, protein, carbs, fat, eaten_date, created_at) "
            + "VALUES (#{userId}, #{mealType}, #{foodName}, #{calories}, #{protein}, #{carbs}, "
            + "#{fat}, #{eatenDate}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DietRecord record);

    @Select("SELECT * FROM diet_record WHERE user_id = #{userId} AND eaten_date = #{date} "
            + "ORDER BY FIELD(meal_type, 'BREAKFAST', 'LUNCH', 'DINNER'), id")
    List<DietRecord> findByUserAndDate(@Param("userId") Long userId,
                                       @Param("date") LocalDate date);

    @Delete("DELETE FROM diet_record WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
