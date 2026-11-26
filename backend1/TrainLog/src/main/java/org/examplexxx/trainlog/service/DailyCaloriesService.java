package org.examplexxx.trainlog.service;

import org.examplexxx.trainlog.dto.DailyCaloriesRequest;
import org.examplexxx.trainlog.vo.DailyCaloriesVO;

/**
 * 根据身高体重推算每天的基础代谢和总消耗。
 */
public interface DailyCaloriesService {

    DailyCaloriesVO estimate(DailyCaloriesRequest request);
}
