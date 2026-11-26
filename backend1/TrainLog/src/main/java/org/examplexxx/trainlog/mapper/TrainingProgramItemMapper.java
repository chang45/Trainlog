package org.examplexxx.trainlog.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.examplexxx.trainlog.entity.TrainingProgramItem;

import java.util.List;

public interface TrainingProgramItemMapper {

    @Insert("INSERT INTO training_program_item (program_id, category, type, goal, note, sort) "
            + "VALUES (#{programId}, #{category}, #{type}, #{goal}, #{note}, #{sort})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TrainingProgramItem item);

    @Select("SELECT * FROM training_program_item WHERE program_id = #{programId} "
            + "ORDER BY sort, id")
    List<TrainingProgramItem> findByProgram(@Param("programId") Long programId);

    @Select("SELECT * FROM training_program_item WHERE id = #{id} AND program_id = #{programId}")
    TrainingProgramItem findById(@Param("id") Long id, @Param("programId") Long programId);

    @Update("UPDATE training_program_item SET category = #{category}, "
            + "type = #{type}, goal = #{goal}, note = #{note}, sort = #{sort} "
            + "WHERE id = #{id} AND program_id = #{programId}")
    int update(TrainingProgramItem item);

    @Delete("DELETE FROM training_program_item WHERE id = #{id} AND program_id = #{programId}")
    int delete(@Param("id") Long id, @Param("programId") Long programId);
}
