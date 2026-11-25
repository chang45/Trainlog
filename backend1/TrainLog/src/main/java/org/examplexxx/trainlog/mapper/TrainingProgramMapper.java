package org.examplexxx.trainlog.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.examplexxx.trainlog.entity.TrainingProgram;

import java.time.LocalDateTime;
import java.util.List;

public interface TrainingProgramMapper {

    @Insert("INSERT INTO training_program (user_id, name, description, active, created_at, updated_at) "
            + "VALUES (#{userId}, #{name}, #{description}, #{active}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TrainingProgram program);

    @Select("SELECT * FROM training_program WHERE id = #{id} AND user_id = #{userId}")
    TrainingProgram findById(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM training_program WHERE user_id = #{userId} ORDER BY active DESC, id")
    List<TrainingProgram> findByUser(@Param("userId") Long userId);

    @Update("UPDATE training_program SET name = #{name}, description = #{description}, "
            + "updated_at = #{updatedAt} WHERE id = #{id} AND user_id = #{userId}")
    int update(TrainingProgram program);

    @Update("UPDATE training_program SET active = #{active}, updated_at = #{updatedAt} "
            + "WHERE id = #{id} AND user_id = #{userId}")
    int updateActive(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("active") boolean active,
                     @Param("updatedAt") LocalDateTime updatedAt);

    /** 把该用户其它计划都设为非当前，配合 setActive 使用 */
    @Update("UPDATE training_program SET active = FALSE WHERE user_id = #{userId}")
    int clearActive(@Param("userId") Long userId);

    @Delete("DELETE FROM training_program WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM training_program WHERE user_id = #{userId}")
    int countByUser(@Param("userId") Long userId);
}
