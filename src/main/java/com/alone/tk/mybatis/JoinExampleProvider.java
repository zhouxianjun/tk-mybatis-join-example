package com.alone.tk.mybatis;

import org.apache.ibatis.mapping.MappedStatement;
import tk.mybatis.mapper.mapperhelper.MapperHelper;
import tk.mybatis.mapper.mapperhelper.MapperTemplate;

/**
 * @author zhouxianjun(Alone)
 * @ClassName:
 * @Description:
 * @date 2018/6/13 17:21
 */
public class JoinExampleProvider extends MapperTemplate {
    public JoinExampleProvider(Class<?> mapperClass, MapperHelper mapperHelper) {
        super(mapperClass, mapperHelper);
    }

    public String selectByJoinExample(MappedStatement ms) {
        getEntityClass(ms);
        return selectByJoinExample("_parameter");
    }

    public String selectByJoinExampleEntity(MappedStatement ms) {
        Class<?> entityClass = this.getEntityClass(ms);
        this.setResultType(ms, entityClass);
        return selectByJoinExample(ms);
    }

    private String selectByJoinExample(String paramName) {
        return "SELECT " + exampleSelectColumns(paramName) +
                " FROM " +
                "${"+paramName+".tableName} " +
                "${"+paramName+".alias} " +
                joinTable(paramName) +
                exampleWhereClause(paramName) +
                group(paramName) +
                orderBy(paramName);
    }

    public static String joinTable(String paramName) {
        return "<if test=\""+ paramName +".tables != null and "+paramName+".tables.size() > 0\">" +
                "<foreach collection=\""+ paramName +".tables\" item=\"tab\" separator=\" \">" +
                "${tab}" +
                "</foreach>" +
                "</if>";
    }

    public static String group(String paramName) {
        return "<if test=\""+paramName+".groups != null and "+paramName+".groups.size() > 0\">" +
                "group by " +
                "<foreach collection=\""+paramName+".groups\" item=\"gby\" separator=\",\">" +
                "${gby}" +
                "</foreach>" +
                "</if>";
    }

    public static String orderBy(String paramName) {
        return "<if test=\""+paramName+".orderByMap != null and "+paramName+".orderByMap.size() > 0\">" +
                "order by " +
                "<foreach collection=\""+paramName+".orderByMap.keys\" item=\"byKey\" separator=\",\">" +
                "${byKey} ${"+paramName+".orderByMap[byKey]}" +
                "</foreach>" +
                "</if>";
    }

    public static String exampleSelectColumns(String paramName) {
        //不支持指定列的时候查询全部列
        return "<choose>" +
                "<when test=\""+ paramName +".selectColumns != null and "+ paramName +".selectColumns.size() > 0\">" +
                "<foreach collection=\""+ paramName +".selectColumns\" item=\"selectColumn\" separator=\",\">" +
                "${selectColumn}" +
                "</foreach>" +
                "</when>" +
                "<otherwise>*</otherwise>" +
                "</choose>";
    }

    public static String exampleWhereClause(String paramName) {
        return "<if test=\""+ paramName +" != null\">" +
                "<where>\n" +
                "  <foreach collection=\""+ paramName +".exampleCriterias\" item=\"criteria\">\n" +
                "    <if test=\"criteria.valid\">\n" +
                "      ${criteria.andOr}" +
                "      <trim prefix=\"(\" prefixOverrides=\"and |or \" suffix=\")\">\n" +
                "        <foreach collection=\"criteria.criterion\" item=\"criterion\">\n" +
                "          <choose>\n" +
                "            <when test=\"criterion.isNoValue()\">\n" +
                "              ${criterion.criterion.andOr} ${criterion.criterion.property} ${criterion.criterion.condition}\n" +
                "            </when>\n" +
                "            <when test=\"criterion.isSingleValue()\">\n" +
                "              ${criterion.criterion.andOr} ${criterion.criterion.property} ${criterion.criterion.condition} #{criterion.criterion.value}\n" +
                "            </when>\n" +
                "            <when test=\"criterion.isBetweenValue()\">\n" +
                "              ${criterion.criterion.andOr} ${criterion.criterion.property} ${criterion.criterion.condition} #{criterion.criterion.value} and #{criterion.criterion.secondValue}\n" +
                "            </when>\n" +
                "            <when test=\"criterion.isListValue()\">\n" +
                "              ${criterion.criterion.andOr} ${criterion.criterion.property} ${criterion.criterion.condition}\n" +
                "              <foreach close=\")\" collection=\"criterion.criterion.value\" item=\"listItem\" open=\"(\" separator=\",\">\n" +
                "                #{listItem}\n" +
                "              </foreach>\n" +
                "            </when>\n" +
                "          </choose>\n" +
                "        </foreach>\n" +
                "      </trim>\n" +
                "    </if>\n" +
                "  </foreach>\n" +
                "</where>" +
                "</if>";
    }
}
