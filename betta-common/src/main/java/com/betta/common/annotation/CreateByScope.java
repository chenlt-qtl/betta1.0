package com.betta.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限过滤注解
 *
 * @author ruoyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CreateByScope {
    public String value();

    /**
     * admin 是否可访问全部数据
     * @return
     */
    public boolean adminAccessAll() default false;
}
