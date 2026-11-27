package org.examplexxx.trainlog.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.examplexxx.trainlog.entity.UserNutritionTarget;

public interface UserNutritionTargetMapper {

    @Select("SELECT * FROM user_nutrition_target WHERE user_id = #{userId}")
    UserNutritionTarget findByUserId(Long userId);

    /**
     * 一个用户一行：存在就更新，不存在就插入。
     * created_at 只在第一次插入时写入，更新时不动。
     */
    @Insert("INSERT INTO user_nutrition_target "
            + "(user_id, daily_calories, protein_normal, protein_bulk, protein_cut, created_at, updated_at) "
            + "VALUES (#{userId}, #{dailyCalories}, #{proteinNormal}, #{proteinBulk}, #{proteinCut}, "
            + "#{createdAt}, #{updatedAt}) "
            + "ON DUPLICATE KEY UPDATE "
            + "daily_calories = VALUES(daily_calories), "
            + "protein_normal = VALUES(protein_normal), "
            + "protein_bulk = VALUES(protein_bulk), "
            + "protein_cut = VALUES(protein_cut), "
            + "updated_at = VALUES(updated_at)")
    int upsert(UserNutritionTarget target);
}
