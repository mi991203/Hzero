package com.hand.app.service.impl;

import com.hand.api.controller.dto.OrderReturnDTO;
import com.hand.app.service.OrderService;
import com.hand.domain.entity.Conditions;
import com.hand.infra.mapper.OrderMapper;
import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderMapper orderMapper;

    /**
     * 多条件分页查询
     * @param conditions 条件实体类
     * @return
     */
    @Override
    public Page<OrderReturnDTO> list(PageRequest pageRequest,Conditions conditions) {
        Page<OrderReturnDTO> page = PageHelper.doPageAndSort(pageRequest,() -> orderMapper.list(conditions));
        List<OrderReturnDTO> list = orderMapper.list(conditions);
        Map<String, BigDecimal> map = new HashMap<String, BigDecimal>();
        for (OrderReturnDTO orderReturnDTO:list) {
            if(map.containsKey(orderReturnDTO.getOrderNumber())){
                //通过Key值查询value值，value值不为空，代表为同一个订单，就把行金额相加
                map.put(orderReturnDTO.getOrderNumber(),map.get(orderReturnDTO.getOrderNumber()).add(orderReturnDTO.getLineAmount()));
            }else {
                //如果value为空代表还没有Map中的key值没有该Key值，就新增一个key值为订单编号,value值为行金额的的map
                map.put(orderReturnDTO.getOrderNumber(),orderReturnDTO.getLineAmount());//
            }
        }
        for (OrderReturnDTO orderReturnDTO:page.getContent()){
            //循环把订单的总金额放进对应每一个订单行数据中
            if(map.containsKey(orderReturnDTO.getOrderNumber())){
                orderReturnDTO.setOrderAmount(map.get(orderReturnDTO.getOrderNumber()));
            }
        }
        return page;
    }

}
