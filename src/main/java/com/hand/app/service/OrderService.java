package com.hand.app.service;

import com.hand.api.controller.dto.OrderReturnDTO;
import com.hand.domain.entity.Conditions;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

import java.util.List;

public interface OrderService {
    /**
     * 条件分页查询订单汇总的数据
     * @param conditions 条件实体类
     * @return 分页以后的数据
     **/
    Page<OrderReturnDTO> list(PageRequest pageRequest, Conditions conditions);

}
