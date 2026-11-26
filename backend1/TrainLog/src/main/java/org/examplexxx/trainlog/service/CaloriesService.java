package org.examplexxx.trainlog.service;

import org.examplexxx.trainlog.dto.CaloriesRequest;
import org.examplexxx.trainlog.vo.CaloriesVO;

/**
 * 训练热量估算。
 */
public interface CaloriesService {

    CaloriesVO estimate(CaloriesRequest request);
}
