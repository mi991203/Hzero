package com.hand.api.controller.v1;

import com.hand.api.controller.dto.OrderReturnDTO;
import com.hand.app.service.OrderService;
import com.hand.app.service.SoLineService;
import com.hand.config.SwaggerApiConfig;
import com.hand.domain.entity.Conditions;
import com.hand.domain.entity.Order;
import com.hand.domain.entity.SoHeader;
import com.hand.domain.entity.SoLine;
import com.hand.domain.repository.SoHeaderRepository;
import com.hand.domain.repository.SoLineRepository;
import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.SwaggerConfig;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Tag;
import org.apache.ibatis.annotations.Delete;
import org.hzero.boot.platform.code.builder.CodeRuleBuilder;
import org.hzero.boot.platform.lov.annotation.ProcessLovValue;
import org.hzero.core.base.BaseController;
import org.hzero.core.util.Results;
import org.hzero.mybatis.helper.SecurityTokenHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@Api(tags = SwaggerApiConfig.ORDER)
@RestController("orderController.v1")
@RequestMapping("/v1/{organizationId}/order")
public class OrderController extends BaseController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private SoLineService soLineService;
    @Autowired
    private SoLineRepository soLineRepository;
    @Autowired
    private SoHeaderRepository soHeaderRepository;

    @Autowired
    private CodeRuleBuilder codeRuleBuilder;

    private final String ORDERNUMBER = "HZERO.28263.ORDER.NUMBER";

    @ApiOperation(value = "根据条件查询订单汇总")
    @Permission(level = ResourceLevel.SITE)
    @GetMapping("/list")
    @ProcessLovValue()
    public Page<OrderReturnDTO> list(@PathVariable Long organizationId,
                                     PageRequest pageRequest,
                                     Conditions conditions) {
        Page<OrderReturnDTO> list = orderService.list(pageRequest, conditions);
        return list;
    }


    @ApiOperation(value = "根据订单头ID查询对应的订单行信息")
    @Permission(level = ResourceLevel.SITE)
    @GetMapping("/listLine")
    public Page<SoLine> listLine(@PathVariable Long organizationId,
                                 PageRequest pageRequest,
                                 @RequestParam Long headerId) {
        Page<SoLine> list = PageHelper.doPageAndSort(pageRequest, () -> soLineService.selectById(headerId));
        return list;
    }

    @ApiOperation(value = "根据订单头ID删除订单行信息")
    @Permission(level = ResourceLevel.SITE)
    @DeleteMapping("/deleteLine")
    public void deleteLine(@PathVariable Long organizationId,
                           Long soheaderid) {
        List<SoLine> list = soLineService.selectById(soheaderid);
        if (list == null) {
            throw new RuntimeException("输入的订单头没有相应的订单");
        }
        SoHeader soHeader1 = soHeaderRepository.selectByPrimaryKey(soheaderid);
        if (!soHeader1.getOrderStatus().equals("NEW") && !soHeader1.getOrderStatus().equals("REJECTED")) {
            throw new RuntimeException("订单状态不能修改");
        }
        if (list.size() != 0) {
            //遍历
            for (SoLine soLine : list) {
                SoLine soLine1 = soLineRepository.selectByPrimaryKey(soLine.getSoLineId());
                soLine.set_token(soLine1.get_token());
                soLine.setSoLineId(soLine.getSoLineId());
                SecurityTokenHelper.validToken(soLine);
                soLineRepository.deleteByPrimaryKey(soLine);
            }
        }
    }


    @ApiOperation(value = "根据订单头ID删除订单")
    @Permission(level = ResourceLevel.SITE)
    @DeleteMapping("/deleteId")
    public ResponseEntity<String> deleteId(@PathVariable Long organizationId,
                                           Long soHeaderId) {
        List<SoLine> list = soLineService.selectById(soHeaderId);
        if (list == null) {
            throw new RuntimeException("输入的订单头没有相应的订单");
        }
        if (list.size() != 0) {
            //先删除订单行数据再删除订单头数据，否则只需要删除订单头数据
            for (SoLine soLine : list) {
                SoLine soLine1 = soLineRepository.selectByPrimaryKey(soLine.getSoLineId());
                soLine.set_token(soLine1.get_token());
                soLine.setSoLineId(soLine.getSoLineId());
                SecurityTokenHelper.validToken(soLine);
                soLineRepository.deleteByPrimaryKey(soLine);
            }
        }
        SoHeader soHeader1 = soHeaderRepository.selectByPrimaryKey(soHeaderId);
        SoHeader soHeader = new SoHeader();
        soHeader.set_token(soHeader1.get_token());
        soHeader.setObjectVersionNumber(soHeader1.getObjectVersionNumber());
        soHeader.setSoHeaderId(soHeaderId);
        SecurityTokenHelper.validToken(soHeader);
        soHeaderRepository.deleteByPrimaryKey(soHeader);
        return Results.success("删除订单成功");
    }


    /**
     * 已完成
     *
     * @param organizationId
     * @param order
     * @return
     */
    @ApiOperation(value = "添加相应的订单头和订单行")
    @Permission(level = ResourceLevel.SITE)
    @PostMapping("/addOrder")
    public Order addOrder(@PathVariable Long organizationId,
                          @RequestBody Order order) {
        //如果order的SoHeaderId不为空
        if (order.getSoHeaderId() != null) {
            //当订单头不为空，表示更新订单头
            SoHeader soHeader = soHeaderRepository.selectByPrimaryKey(order.getSoHeaderId());
            if (order.getObjectVersionNumber() != null) {
                throw new RuntimeException("object_version_number为空");
            } else if (order.getObjectVersionNumber() != soHeader.getObjectVersionNumber()) {
                throw new RuntimeException("object_version_number不一致");
            } else {
                if ("NEW".equals(soHeader.getOrderStatus()) ||  "REJECTED".equals(soHeader.getOrderStatus())) {
                    soHeaderRepository.updateByPrimaryKeySelective(soHeader);
                }else {
                    throw new RuntimeException("当钱包订单状态无法更改");
                }
            }
        } else {
            //因为当soHeaderId为空的时候
            //将原order中对象中的关于SoHeader信息取出
            SoHeader soHeader = new SoHeader();
            //订单号按照编码规则生成并设置到soHeader
            String code = codeRuleBuilder.generateCode(ORDERNUMBER, null);
            soHeader.setOrderNumber(code);
            soHeader.setCompanyId(order.getCompanyId());
            soHeader.setOrderDate(order.getOrderDate());
            //使orderStatus默认是NEW
            if (order.getOrderStatus() != null) {
                soHeader.setOrderStatus(order.getOrderStatus());
            } else {
                soHeader.setOrderStatus("NEW");
            }
            soHeader.setCustomerId(order.getCustomerId());
            soHeader.set_token(order.get_token());
            //校验soHeader对象
            validObject(soHeader);
            //这个地方soHeaderId自动填充
            soHeaderRepository.insertSelective(soHeader);
            //将这个处理过得soHeader传递进orderResult
            order.setSoHeaderId(soHeader.getSoHeaderId());
            //insert有关SoLine
            List<SoLine> list = order.getList();
            int index = 0;
            for (SoLine soLine :
                    list) {
                //判断当前输入的soLine对象的soLineId是否为空，为空是添加，不为空是更新
                if(soLine.getSoLineId() != null){
                    SoLine soLine1 = soLineRepository.selectByPrimaryKey(soLine.getSoLineId());
                    //更新
                    if (soLine.getObjectVersionNumber() != null) {
                        if (soLine1.getObjectVersionNumber() == soLine.getObjectVersionNumber()){
                            soLineRepository.updateByPrimaryKey(soLine);
                            index++;
                        }else {
                            throw new RuntimeException("当前obejct_version_number不匹配");
                        }
                    }else {
                        throw new RuntimeException("当前obejct_version_number为空");
                    }
                }else {
                    //添加
                    soLine.setSoHeaderId(soHeader.getSoHeaderId());
                    validObject(soLine);
                    soLineRepository.insertSelective(soLine);
                    order.getList().get(index).setSoLineId(soLine.getSoLineId());
                    index++;
                }
            }
        }
        return order;
    }

}
