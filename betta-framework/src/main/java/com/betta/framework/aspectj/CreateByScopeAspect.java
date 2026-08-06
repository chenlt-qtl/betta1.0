package com.betta.framework.aspectj;

import com.betta.common.annotation.CreateByScope;
import com.betta.common.core.domain.BaseEntity;
import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.common.utils.SecurityUtils;
import com.betta.common.utils.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 数据所有人过滤处理
 *
 * @author ruoyi
 */
@Aspect
@Component
public class CreateByScopeAspect {

    /**
     * 数据权限过滤关键字
     */
    public static final String CREATE_BY_SCOPE = "createByScope";

    @Before("@annotation(controllerCreateByScope)")
    public void doBefore(JoinPoint point, CreateByScope controllerCreateByScope) throws Throwable {
        clearCreateBy(point);
        handleCreateByScope(point, controllerCreateByScope);
    }

    protected void handleCreateByScope(final JoinPoint joinPoint, CreateByScope controllerDataScope) {
        // 获取当前的用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNotNull(loginUser)) {
            SysUser currentUser = loginUser.getUser();
            // 如果adminAccessAll是true 且 是超级管理员，则不过滤数据
            if (StringUtils.isNotNull(currentUser) && !(controllerDataScope.adminAccessAll() && currentUser.isAdmin())) {
                createByFilter(joinPoint, currentUser, controllerDataScope.value());
            }
        }
    }

    /**
     * 数据范围过滤
     *
     * @param joinPoint 切点
     * @param user      用户
     */
    public static void createByFilter(JoinPoint joinPoint, SysUser user, String tableAlias) {
        StringBuilder sqlString = new StringBuilder();
        sqlString.append(StringUtils.format(
                " and {}create_by = '{}' ",
                StringUtils.hasText(tableAlias) ? (tableAlias + ".") : "", user.getUserName()));

        Object params = joinPoint.getArgs()[0];
        if (StringUtils.isNotNull(params) && params instanceof BaseEntity) {
            BaseEntity baseEntity = (BaseEntity) params;
            baseEntity.getParams().put(CREATE_BY_SCOPE, sqlString);
        }
    }


    /**
     * 拼接权限sql前先清空params.createBy参数防止注入
     */
    private void clearCreateBy(final JoinPoint joinPoint) {
        Object params = joinPoint.getArgs()[0];
        if (StringUtils.isNotNull(params) && params instanceof BaseEntity) {
            BaseEntity baseEntity = (BaseEntity) params;
            baseEntity.getParams().put(CREATE_BY_SCOPE, "");
        }
    }
}
