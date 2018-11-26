package com.alone.tk.mybatis;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ClassUtil;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhouxianjun(Alone)
 * @ClassName:
 * @Description:
 * @date 2018/11/21 9:47
 */
@tk.mybatis.mapper.annotation.RegisterMapper
public interface JoinExampleMapper<T> {
    /**
     * 联合查询
     * @param example 查询条件
     * @return 结果
     */
    @SelectProvider(type = JoinExampleProvider.class, method = "dynamicSQL")
    List<Map<String, Object>> selectByJoinExample(JoinExample example);

    /**
     * 联合查询
     * @param example 查询条件
     * @return 结果
     */
    @SelectProvider(type = JoinExampleProvider.class, method = "dynamicSQL")
    List<T> selectByJoinExampleEntity(JoinExample example);

    default <R> List<R> selectByJoinExampleTransform(JoinExample example, Function<Map, R> transform, Map<String, String> mapping, String ...ignore) {
        List<Map<String, Object>> list = selectByJoinExample(example);
        return transformList(list, transform, mapping, ignore);
    }
    default <R> List<R> selectByJoinExampleTransform(JoinExample example, Function<Map, R> transform, String ...ignore) {
        List<Map<String, Object>> list = selectByJoinExample(example);
        return transformList(list, transform, null, ignore);
    }
    default <R> List<R> selectByJoinExampleTransform(JoinExample example, Function<Map, R> transform) {
        List<Map<String, Object>> list = selectByJoinExample(example);
        return transformList(list, transform, null, new String[0]);
    }

    @SuppressWarnings("unchecked")
    default <R> R selectByJoinExampleTransform(JoinExample example, Class<R> rClass) {
        List<Map<String, Object>> list = selectByJoinExample(example);
        if (CollectionUtil.isEmpty(list) || list.get(0) == null) {
            return null;
        }
        if (ClassUtil.isBasicType(rClass) || rClass.isAssignableFrom(String.class)) {
            return Convert.convert(rClass, list.get(0).values().iterator().next(), (R) ClassUtil.getDefaultValue(rClass));
        }
        return Convert.convert(rClass, list.get(0));
    }
    default Map<String, Object> selectByJoinExampleOne(JoinExample example) {
        List<Map<String, Object>> list = selectByJoinExample(example);
        if (CollectionUtil.isEmpty(list) || list.get(0) == null) {
            return null;
        }
        return list.get(0);
    }
    default T selectByJoinExampleEntityOne(JoinExample example) {
        List<T> list = selectByJoinExampleEntity(example);
        if (CollectionUtil.isEmpty(list) || list.get(0) == null) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 转换集合实例
     * @param list 集合
     * @param transform 实例
     * @param mapping 字段mapping
     * @return
     */
    default <R> List<R> transformList(List<Map<String, Object>> list, Function<Map, R> transform, Map<String, String> mapping, String ...ignore) {
        if (list == null) {
            return null;
        }
        return list.stream().map(
                map -> transform(map, transform, mapping, ignore))
                .collect(Collectors.toList());
    }

    /**
     * 转换集合实例
     * @param list 集合
     * @param transform 实例
     * @return
     */
    default <R> List<R> transformList(List<Map<String, Object>> list, Function<Map, R> transform) {
        return transformList(list, transform, null, new String[0]);
    }

    default <R> List<R> transformList(List<Map<String, Object>> list, Function<Map, R> transform, String ...ignore) {
        return transformList(list, transform, null, ignore);
    }

    /**
     * 转换实例
     * @param map 集合
     * @param transform 实例
     * @param mapping 转换mapping
     * @param <R>
     * @return
     */
    default <R> R transform(Map<String, Object> map, Function<Map, R> transform, Map<String, String> mapping, String ...ignore) {
        return BeanUtil.fillBeanWithMap(
                map,
                transform.apply(map),
                true,
                CopyOptions.create()
                        .setIgnoreError(true)
                        .setIgnoreCase(true)
                        .setIgnoreNullValue(true)
                        .setFieldMapping(mapping)
                        .setIgnoreProperties(ignore)
        );
    }

    default <R> R transform(Map<String, Object> map, Function<Map, R> transform) {
        return transform(map, transform, null, new String[0]);
    }
    default <R> R transform(Map<String, Object> map, Function<Map, R> transform, String ...ignore) {
        return transform(map, transform, null, ignore);
    }
}
