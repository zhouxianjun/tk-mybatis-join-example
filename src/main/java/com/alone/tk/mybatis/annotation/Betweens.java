package com.alone.tk.mybatis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zhouxianjun(Alone)
 * @ClassName:
 * @Description:
 * @date 2019/2/13 12:46
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Betweens {
    Between[] value();
}
