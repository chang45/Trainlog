package org.examplexxx.trainlog.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.examplexxx.trainlog.entity.DietRecognitionTask;

public interface DietRecognitionTaskMapper {

    @Insert("INSERT INTO diet_recognition_task "
            + "(task_id, user_id, meal_type, status, created_at, updated_at) "
            + "VALUES (#{taskId}, #{userId}, #{mealType}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DietRecognitionTask task);

    @Select("SELECT * FROM diet_recognition_task WHERE task_id = #{taskId}")
    DietRecognitionTask findByTaskId(String taskId);

    @Update("UPDATE diet_recognition_task SET status = #{status}, record_id = #{recordId}, "
            + "error_message = #{errorMessage}, updated_at = #{updatedAt} "
            + "WHERE task_id = #{taskId}")
    int update(DietRecognitionTask task);
}
