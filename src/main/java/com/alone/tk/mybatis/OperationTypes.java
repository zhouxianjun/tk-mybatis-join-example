package com.alone.tk.mybatis;

/**
 * @author zhouxianjun(Alone)
 * @ClassName:
 * @Description:
 * @date 2019/2/13 12:34
 */
public interface OperationTypes {
    String EQUAL = "=";
    String NOT_EQUAL = "<>";
    String GREATER_THAN = ">";
    String GREATER_THAN_OR_EQUAL = ">=";
    String LESS_THAN = "<";
    String LESS_THAN_OR_EQUAL = "<=";
    String IN = "in";
    String NOT_IN = "not in";
    String LIKE = "like";
    String NOT_LIKE = "not like";
    String BETWEEN = "between";
    String NOT_BETWEEN = "not between";
    String IS_NULL = "is null";
    String IS_NOT_NULL = "is not null";
}
