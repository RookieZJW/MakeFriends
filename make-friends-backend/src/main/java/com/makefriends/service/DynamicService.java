package com.makefriends.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.makefriends.dto.DynamicPublishDTO;
import com.makefriends.vo.DynamicVO;

public interface DynamicService {

    DynamicVO publish(DynamicPublishDTO dto);

    boolean delete(Long id);

    IPage<DynamicVO> getList(int page, int size);

    DynamicVO getDetail(Long id);

    IPage<DynamicVO> getMyDynamics(int page, int size);

    IPage<DynamicVO> getUserDynamics(Long userId, int page, int size);
}
