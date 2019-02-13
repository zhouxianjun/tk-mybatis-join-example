package com.alone.tk.mybatis.annotation;

import com.alone.tk.mybatis.OperationTypes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zhouxianjun(Alone)
 * @ClassName:
 * @Description:
 * @date 2019/2/13 12:15
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Operation {
    String value() default OperationTypes.EQUAL;
    boolean and() default true;
    String column() default "";
    String likeFormat() default "%{0}%";
}
