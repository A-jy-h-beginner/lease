package com.atguigu.lease.web.admin.controller.system;


import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.model.entity.SystemUser;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.web.admin.service.SystemUserService;
import com.atguigu.lease.web.admin.vo.system.user.SystemUserItemVo;
import com.atguigu.lease.web.admin.vo.system.user.SystemUserQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Tag(name = "后台用户信息管理")
@RestController
@RequestMapping("/admin/system/user")
public class SystemUserController {
    @Autowired
    private SystemUserService systemUserService;
    @Operation(summary = "根据条件分页查询后台用户列表")
    @GetMapping("page")
    public Result<IPage<SystemUserItemVo>> page(@RequestParam long current, @RequestParam long size, SystemUserQueryVo queryVo) {
        Page<SystemUserItemVo> page = new Page<>(current, size);
        IPage<SystemUserItemVo> result = systemUserService.pageSystemUserItem(page, queryVo);
        return Result.ok(result);
    }

    @Operation(summary = "根据ID查询后台用户信息")
    @GetMapping("getById")
    public Result<SystemUserItemVo> getById(@RequestParam Long id) {
        SystemUserItemVo systemUserItemVo = systemUserService.getBySystemUserId(id);
        return Result.ok(systemUserItemVo);
    }

    @Operation(summary = "保存或更新后台用户信息")
    @PostMapping("saveOrUpdate")
    public Result saveOrUpdate(@RequestBody SystemUser systemUser) {
        // 加密
        if(systemUser.getPassword()!=null){
            systemUser.setPassword(DigestUtils.md5Hex(systemUser.getPassword())); // md5Hex必须为不为空
        }
        systemUserService.saveOrUpdate(systemUser);
        return Result.ok();
    }

    @Operation(summary = "判断后台用户名是否可用")
    @GetMapping("isUserNameAvailable")
    public Result<Boolean> isUsernameExists(@RequestParam String username) {
        Long count = systemUserService.lambdaQuery()
                .eq(SystemUser::getUsername, username)
                .count();
        if(count != 0){
            return Result.fail(ResultCodeEnum.ADMIN_ACCOUNT_EXIST_ERROR.getCode(),
                    ResultCodeEnum.ADMIN_ACCOUNT_EXIST_ERROR.getMessage());
        }
        return Result.ok();
    }

    @DeleteMapping("deleteById")
    @Operation(summary = "根据ID删除后台用户信息")
    public Result removeById(@RequestParam Long id) {
        systemUserService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "根据ID修改后台用户状态")
    @PostMapping("updateStatusByUserId")
    public Result updateStatusByUserId(@RequestParam Long id, @RequestParam BaseStatus status) {
        systemUserService.lambdaUpdate()
                .eq(SystemUser::getId, id)
                .set(SystemUser::getStatus, status)
                .update();
        return Result.ok();
    }
}
