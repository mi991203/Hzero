package com.hand.api.controller.v1;

import com.hand.api.utils.PrincipalUtil;
import com.hand.config.SwaggerApiConfig;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.mybatis.annotation.VersionAudit;
import io.swagger.annotations.Api;
import org.hzero.boot.platform.code.builder.CodeRuleBuilder;
import org.hzero.core.util.Results;
import org.hzero.core.base.BaseController;
import com.hand.domain.entity.SoHeader;
import com.hand.domain.repository.SoHeaderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.hzero.mybatis.helper.SecurityTokenHelper;

import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

import javax.persistence.Version;
import java.security.Principal;

/**
 * 管理 API
 *
 * @author sh1101315853@163.com 2020-07-27 15:21:08
 */
@Api(tags = SwaggerApiConfig.SOHEADGER)
@RestController("soHeaderSiteController.v1")
@RequestMapping("/v1/so-headers")
public class SoHeaderController extends BaseController {

    @Autowired
    private SoHeaderRepository soHeaderRepository;

    @Autowired
    private CodeRuleBuilder codeRuleBuilder;

    private final String ORDERNUMBER = "HZERO.28263.ORDER.NUMBER";

    @ApiOperation(value = "列表")
    @Permission(level = ResourceLevel.SITE)
    @GetMapping
    public ResponseEntity<Page<SoHeader>> list(SoHeader soHeader, @ApiIgnore @SortDefault(value = SoHeader.FIELD_SO_HEADER_ID,
            direction = Sort.Direction.DESC) PageRequest pageRequest) {
        Page<SoHeader> list = soHeaderRepository.pageAndSort(pageRequest, soHeader);
        return Results.success(list);
    }

    @ApiOperation(value = "明细")
    @Permission(level = ResourceLevel.SITE)
    @GetMapping("/{soHeaderId}")
    public ResponseEntity<SoHeader> detail(@PathVariable Long soHeaderId) {
        SoHeader soHeader = soHeaderRepository.selectByPrimaryKey(soHeaderId);
        return Results.success(soHeader);
    }

    @ApiOperation(value = "创建")
    @Permission(level = ResourceLevel.SITE)
    @PostMapping
    public ResponseEntity<SoHeader> create(@RequestBody SoHeader soHeader) {
        validObject(soHeader);
        String code = codeRuleBuilder.generateCode(ORDERNUMBER, null);
        soHeader.setOrderNumber(code);
        soHeaderRepository.insertSelective(soHeader);
        return Results.success(soHeader);
    }

    @ApiOperation(value = "修改")
    @Permission(level = ResourceLevel.SITE)
    @PutMapping
    public ResponseEntity<String> update(@RequestBody SoHeader soHeader, Principal principal) {
        /*SecurityTokenHelper.validToken(soHeader);
        soHeaderRepository.updateByPrimaryKeySelective(soHeader);
        return Results.success(soHeader);*/

        //通过传过来要修改的ID查询原始的订单头数据
        SoHeader soHeader1 = soHeaderRepository.selectByPrimaryKey(soHeader.getSoHeaderId());
        if (soHeader1 == null) {
            throw new RuntimeException("当前订单不存在");
        }
        if (!soHeader1.getCreatedBy().equals(soHeader.getCreatedBy())) {
            throw new RuntimeException("当前用户与创建人不一致");
           /* PrincipalUtil principalUtil = new PrincipalUtil(principal);
            CustomUserDetails details = principalUtil.getDetails();
            details.getRoleId();
            details.getUsername();*/

        }
        if (!soHeader1.getOrderStatus().equals("NEW") && !soHeader1.getOrderStatus().equals("REJECTED")) {
            throw new RuntimeException("订单状态不能修改");
        }
        //不可以对订单编号进行更改
        soHeader.setOrderNumber(soHeader1.getOrderNumber());

        //不再输入ObjectVersionNumber
        soHeader.setObjectVersionNumber(soHeader1.getObjectVersionNumber());
        //不必再输入tokenID
        soHeader.set_token(soHeader1.get_token());
        //set相应的Status状态
        if (soHeader.getOrderStatus().equals("SUBMITED") || soHeader.getOrderStatus().equals("APPROVED")
                || soHeader.getOrderStatus().equals("REJECTED")) {
            //tokenId数据验证
            SecurityTokenHelper.validToken(soHeader);
            soHeaderRepository.updateByPrimaryKeySelective(soHeader);
            return Results.success("修改订单头成功！");
        } else {
            throw new RuntimeException("动作状态编码错误");
        }

    }

    /*@ApiOperation(value = "根据订单头ID进行更新订单的状态表")
    @Permission(level = ResourceLevel.SITE)
    @GetMapping("/updateStatus")
    public void updateByHeaderId(@RequestBody String soHeaderId, @RequestBody String createdBy, @RequestBody String orderStatus) {
        SoHeader soHeader1 = soHeaderRepository.selectByPrimaryKey(soHeaderId);
        if (soHeader1 != null) {
            if (soHeader1.getCreatedBy() == createdBy) {
                if (soHeader1.getOrderStatus().equals("NEW") || soHeader1.getOrderStatus().equals("REJECTED")) {
                    soHeader1.setOrderStatus(orderStatus);
                    soHeaderRepository.updateByPrimaryKeySelective(soHea der1);
                } else {
                    throw new RuntimeException("订单状态不能修改");
                }
            } else {
                throw new RuntimeException("当前用户与创建人不一致");
            }
        } else {
            throw new RuntimeException("当前订单不存在");
        }
    }*/


    @ApiOperation(value = "删除")
    @Permission(level = ResourceLevel.SITE)
    @DeleteMapping
    public ResponseEntity<?> remove(@RequestBody SoHeader soHeader) {
        SecurityTokenHelper.validToken(soHeader);
        soHeaderRepository.deleteByPrimaryKey(soHeader);
        return Results.success();
    }

}
