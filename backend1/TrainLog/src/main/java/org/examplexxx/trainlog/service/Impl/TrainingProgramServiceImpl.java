package org.examplexxx.trainlog.service.Impl;

import org.examplexxx.trainlog.constant.TrainingType;
import org.examplexxx.trainlog.context.BaseContext;
import org.examplexxx.trainlog.dto.TrainingProgramDTO;
import org.examplexxx.trainlog.dto.TrainingProgramItemDTO;
import org.examplexxx.trainlog.entity.TrainingProgram;
import org.examplexxx.trainlog.entity.TrainingProgramItem;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.mapper.TrainingProgramItemMapper;
import org.examplexxx.trainlog.mapper.TrainingProgramMapper;
import org.examplexxx.trainlog.service.TrainingProgramService;
import org.examplexxx.trainlog.vo.TrainingProgramItemVO;
import org.examplexxx.trainlog.vo.TrainingProgramVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrainingProgramServiceImpl implements TrainingProgramService {

    private static final int MAX_PROGRAMS = 10;

    private final TrainingProgramMapper trainingProgramMapper;
    private final TrainingProgramItemMapper trainingProgramItemMapper;

    public TrainingProgramServiceImpl(TrainingProgramMapper trainingProgramMapper,
                                      TrainingProgramItemMapper trainingProgramItemMapper) {
        this.trainingProgramMapper = trainingProgramMapper;
        this.trainingProgramItemMapper = trainingProgramItemMapper;
    }

    @Override
    public List<TrainingProgramVO> list() {
        Long userId = currentUserId();
        List<TrainingProgramVO> result = new ArrayList<>();
        for (TrainingProgram program : trainingProgramMapper.findByUser(userId)) {
            result.add(toVO(program));
        }
        return result;
    }

    @Override
    @Transactional
    public TrainingProgramVO create(TrainingProgramDTO dto) {
        Long userId = currentUserId();
        String name = requireName(dto);
        int count = trainingProgramMapper.countByUser(userId);
        if (count >= MAX_PROGRAMS) {
            throw new BizException(400, "最多创建 " + MAX_PROGRAMS + " 个计划");
        }

        LocalDateTime now = LocalDateTime.now();
        TrainingProgram program = new TrainingProgram();
        program.setUserId(userId);
        program.setName(name);
        program.setDescription(trimToNull(dto.getDescription(), 200, "描述最多 200 个字符"));
        // 第一个计划默认设为当前计划
        program.setActive(count == 0);
        program.setCreatedAt(now);
        program.setUpdatedAt(now);
        trainingProgramMapper.insert(program);

        if (dto.getItems() != null) {
            for (TrainingProgramItemDTO itemDTO : dto.getItems()) {
                insertItem(program.getId(), itemDTO);
            }
        }
        return toVO(program);
    }

    @Override
    public TrainingProgramVO update(Long id, TrainingProgramDTO dto) {
        Long userId = currentUserId();
        TrainingProgram program = requireOwned(id, userId);
        program.setName(requireName(dto));
        program.setDescription(trimToNull(dto.getDescription(), 200, "描述最多 200 个字符"));
        program.setUpdatedAt(LocalDateTime.now());
        trainingProgramMapper.update(program);
        return toVO(program);
    }

    @Override
    public void delete(Long id) {
        Long userId = currentUserId();
        if (id == null || trainingProgramMapper.delete(id, userId) == 0) {
            throw new BizException(404, "计划不存在或不属于你");
        }
    }

    @Override
    @Transactional
    public TrainingProgramVO setActive(Long id) {
        Long userId = currentUserId();
        TrainingProgram program = requireOwned(id, userId);
        trainingProgramMapper.clearActive(userId);
        trainingProgramMapper.updateActive(id, userId, true, LocalDateTime.now());
        program.setActive(true);
        return toVO(program);
    }

    @Override
    public TrainingProgramItemVO addItem(Long programId, TrainingProgramItemDTO dto) {
        Long userId = currentUserId();
        requireOwned(programId, userId);
        return TrainingProgramItemVO.from(insertItem(programId, dto));
    }

    @Override
    public TrainingProgramItemVO updateItem(Long programId, Long itemId, TrainingProgramItemDTO dto) {
        Long userId = currentUserId();
        requireOwned(programId, userId);
        TrainingProgramItem item = trainingProgramItemMapper.findById(itemId, programId);
        if (item == null) {
            throw new BizException(404, "计划项不存在");
        }
        applyItem(dto, item);
        item.setProgramId(programId);
        trainingProgramItemMapper.update(item);
        return TrainingProgramItemVO.from(item);
    }

    @Override
    public void deleteItem(Long programId, Long itemId) {
        Long userId = currentUserId();
        requireOwned(programId, userId);
        if (itemId == null || trainingProgramItemMapper.delete(itemId, programId) == 0) {
            throw new BizException(404, "计划项不存在");
        }
    }

    private TrainingProgramItem insertItem(Long programId, TrainingProgramItemDTO dto) {
        TrainingProgramItem item = new TrainingProgramItem();
        applyItem(dto, item);
        item.setProgramId(programId);
        trainingProgramItemMapper.insert(item);
        return item;
    }

    private void applyItem(TrainingProgramItemDTO dto, TrainingProgramItem item) {
        if (dto == null) {
            throw new BizException(400, "请求体不能为空");
        }
        if (dto.getNote() != null && dto.getNote().length() > 60) {
            throw new BizException(400, "备注最多 60 个字符");
        }

        TrainingType type = TrainingType.find(dto.getType()).orElse(null);
        String category;
        String typeCode = null;
        if (type != null) {
            category = type.getCategory();
            typeCode = type.name();
            if (dto.getCategory() != null && !dto.getCategory().isBlank()
                    && !category.equalsIgnoreCase(dto.getCategory().trim())) {
                throw new BizException(400, "训练大类和具体项目不匹配");
            }
        } else if (dto.getType() != null && !dto.getType().isBlank()) {
            throw new BizException(400, "未知的训练类型");
        } else {
            category = normalizeCategory(dto.getCategory());
        }

        item.setCategory(category);
        item.setType(typeCode);
        item.setNote(dto.getNote() == null ? null : dto.getNote().trim());
        item.setSort(dto.getSort() == null ? 0 : Math.max(dto.getSort(), 0));
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new BizException(400, "请选择训练大类");
        }
        String value = category.trim().toUpperCase();
        if (!"STRENGTH".equals(value) && !"CARDIO".equals(value)) {
            throw new BizException(400, "未知的训练大类");
        }
        return value;
    }

    private TrainingProgramVO toVO(TrainingProgram program) {
        TrainingProgramVO vo = TrainingProgramVO.from(program);
        List<TrainingProgramItemVO> items = new ArrayList<>();
        for (TrainingProgramItem item : trainingProgramItemMapper.findByProgram(program.getId())) {
            items.add(TrainingProgramItemVO.from(item));
        }
        vo.setItems(items);
        return vo;
    }

    private TrainingProgram requireOwned(Long id, Long userId) {
        if (id == null) {
            throw new BizException(400, "缺少计划 id");
        }
        TrainingProgram program = trainingProgramMapper.findById(id, userId);
        if (program == null) {
            throw new BizException(404, "计划不存在或不属于你");
        }
        return program;
    }

    private String requireName(TrainingProgramDTO dto) {
        if (dto == null) {
            throw new BizException(400, "请求体不能为空");
        }
        String name = dto.getName() == null ? "" : dto.getName().trim();
        if (name.isEmpty()) {
            throw new BizException(400, "请填写计划名");
        }
        if (name.length() > 50) {
            throw new BizException(400, "计划名最多 50 个字符");
        }
        return name;
    }

    private String trimToNull(String value, int max, String message) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > max) {
            throw new BizException(400, message);
        }
        return trimmed;
    }

    private Long currentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        return userId;
    }
}
