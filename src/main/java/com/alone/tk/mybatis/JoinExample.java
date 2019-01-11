package com.alone.tk.mybatis;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import lombok.*;
import lombok.experimental.Accessors;
import tk.mybatis.mapper.annotation.ColumnType;
import tk.mybatis.mapper.code.Style;
import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.entity.EntityField;
import tk.mybatis.mapper.entity.EntityTable;
import tk.mybatis.mapper.entity.SqlsCriteria;
import tk.mybatis.mapper.mapperhelper.EntityHelper;
import tk.mybatis.mapper.mapperhelper.FieldHelper;
import tk.mybatis.mapper.util.SimpleTypeUtil;
import tk.mybatis.mapper.util.SqlReservedWords;
import tk.mybatis.mapper.util.Sqls;
import tk.mybatis.mapper.util.StringUtil;
import tk.mybatis.mapper.weekend.Fn;
import tk.mybatis.mapper.weekend.reflection.ReflectionOperationException;
import tk.mybatis.mapper.weekend.reflection.Reflections;

import java.beans.Introspector;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author zhouxianjun(Alone)
 * @ClassName:
 * @Description:
 * @date 2018/10/15 9:05
 */
@Data
@Accessors(chain = true)
public class JoinExample {
    private static final Pattern GET_PATTERN = Pattern.compile("^get[A-Z].*");
    private static final Pattern IS_PATTERN  = Pattern.compile("^is[A-Z].*");
    public static final Map<Class, List<Column>> CLASS_COLUMN_MAP = new ConcurrentHashMap<>(10);
    public enum JoinType {
        /**
         * 关联类型
         */
        JOIN("JOIN"),
        LEFT("LEFT JOIN"),
        RIGHT("RIGHT JOIN"),
        INNER("INNER JOIN");

        @Getter
        private String value;

        JoinType(String value) {
            this.value = value;
        }
    }

    private String tableName;
    private String alias;
    private List<Table> tables = new ArrayList<>();
    private Set<String> selectColumns = new HashSet<>();
    private List<Criteria> exampleCriterias = new ArrayList<>();
    private Map<String, String> orderByMap = new HashMap<>();
    private Set<String> groups = new HashSet<>(0);

    @Data
    @Accessors(chain = true)
    public static class Table {
        private Class tableClass;
        private String alias;
        private JoinType type;
        private LinkedHashMap<String, String> on = new LinkedHashMap<>(1);

        public Table(Class tableClass, JoinType type, String col1, String col2) {
            this.tableClass = tableClass;
            this.alias = tableAlias(tableClass);
            this.type = type;
            this.and(col1, col2);
        }
        public Table(Class tableClass, String col1, String col2) {
            this(tableClass, JoinType.JOIN, col1, col2);
        }
        public <T,E> Table(Class tableClass, JoinType type, Fn<E, Object> fn1, Fn<T, Object> fn2) {
            this(tableClass, type, column(fn1), column(fn2));
        }
        public <T,E> Table(Class tableClass, Fn<E, Object> fn1, Fn<T, Object> fn2) {
            this(tableClass, JoinType.JOIN, column(fn1), column(fn2));
        }
        public <T> Table(Class tableClass, JoinType type, String col1, Fn<T, Object> fn2) {
            this(tableClass, type, col1, column(fn2));
        }
        public <T> Table(Class tableClass, String col1, Fn<T, Object> fn2) {
            this(tableClass, JoinType.JOIN, col1, column(fn2));
        }
        public <T> Table(Class tableClass, JoinType type, Fn<T, Object> fn, String col2) {
            this(tableClass, type, column(fn), col2);
        }
        public <T> Table(Class tableClass, Fn<T, Object> fn, String col2) {
            this(tableClass, JoinType.JOIN, column(fn), col2);
        }

        public Table and(String col1, Object col2) {
            this.on.put(col1, StrUtil.utf8Str(col2));
            return this;
        }
        public <T> Table and(String col1, Fn<T, Object> fn) {
            return and(col1, column(fn));
        }
        public <T> Table and(Fn<T, Object> fn, Object col2) {
            return and(column(fn), col2);
        }
        public <T,E> Table and(Fn<E, Object> fn1, Fn<T, Object> fn2) {
            return and(column(fn1), column(fn2));
        }

        @Override
        public String toString() {
            EntityTable entityTable = EntityHelper.getEntityTable(tableClass);
            StringBuilder sb = new StringBuilder(" ");
            sb.append(type.value)
                    .append(" ")
                    .append(entityTable.getName())
                    .append(" ")
                    .append(alias);
            sb.append(" ON ");
            Set<Map.Entry<String, String>> entries = on.entrySet();
            int i = 0;
            for (Map.Entry<String, String> entry : entries) {
                if (i > 0) {
                    sb.append(" AND ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                i++;
            }
            return sb.toString();
        }
    }

    public static class Builder extends JustWhere<Builder> {
        private JoinExample example;

        public Builder(Class tableClass) {
            EntityTable entityTable = EntityHelper.getEntityTable(tableClass);
            example = new JoinExample().setTableName(entityTable.getName()).setAlias(tableAlias(tableClass));
        }

        public Builder noAlias() {
            return setAlias("");
        }

        public Builder setAlias(String alias) {
            example.setAlias(alias);
            return this;
        }

        public Builder addTable(Table table) {
            example.getTables().add(table);
            return this;
        }

        public Builder addCol(String col) {
            example.getSelectColumns().add(col);
            return this;
        }

        public Builder count(String col) {
            return addCol("count(" + col + ")");
        }

        public <T> Builder count(Fn<T, Object> fn) {
            return addCol("count(" + column(fn) + ")");
        }

        public Builder count() {
            return addCol("count(*)");
        }

        public Builder addCol(String col, String alias) {
            example.getSelectColumns().add(col + " AS " + alias);
            return this;
        }

        public <T> Builder addCol(Fn<T, Object> fn) {
            return addCol(column(fn));
        }
        public <T> Builder addGroupCol(Fn<T, Object> fn, boolean isDistinct, String alias) {
            return addCol("GROUP_CONCAT(" + (isDistinct ? "distinct " : "") + column(fn) + ")", alias);
        }
        public <T> Builder addGroupCol(Fn<T, Object> fn, String alias) {
            return addGroupCol(fn, false, alias);
        }

        public <T> Builder addCol(Fn<T, Object> fn, String alias) {
            return addCol(column(fn) + " AS " + alias);
        }

        public <T,A> Builder addCol(Fn<T, Object> fn, Fn<A, Object> alias) {
            return addCol(fn, Reflections.fnToFieldName(alias));
        }

        public <A> Builder addCol(String col, Fn<A, Object> alias) {
            return addCol(col, Reflections.fnToFieldName(alias));
        }

        public Builder addCol(Class entityClass, String tableAlias, String childName) {
            Set<EntityColumn> columnSet = EntityHelper.getColumns(entityClass);
            String cName = tableAlias == null ? tableAlias(entityClass) : tableAlias;
            columnSet.forEach(col -> {
                if (!col.getColumn().replace("`", "").equals(col.getProperty()) || StrUtil.isNotBlank(childName)) {
                    addCol(cName + "." + col.getColumn() + " AS " + (StrUtil.isNotBlank(childName) ? childName + "$" + col.getProperty() : col.getProperty()));
                } else {
                    addCol(cName + "." + col.getColumn());
                }
            });
            return this;
        }
        public Builder addCol(Class entityClass, String childName) {
            return addCol(entityClass, null, childName);
        }
        public Builder addCol(Class entityClass) {
            return addCol(entityClass, null);
        }

        public Builder asc(String col) {
            example.getOrderByMap().put(col, "asc");
            return this;
        }
        public <T> Builder asc(Fn<T, Object> fn) {
            return asc(column(fn));
        }

        public Builder desc(String col) {
            example.getOrderByMap().put(col, "desc");
            return this;
        }
        public <T> Builder desc(Fn<T, Object> fn) {
            return desc(column(fn));
        }

        public <T> Builder orderBy(Fn<T, Object> fn, boolean isDesc) {
            return isDesc ? desc(fn) : asc(fn);
        }

        public <T> Builder orderBy(String col, boolean isDesc) {
            return isDesc ? desc(col) : asc(col);
        }

        public Builder groupBy(String col) {
            example.getGroups().add(col);
            return this;
        }
        public <T> Builder groupBy(Fn<T, Object> fn) {
            return groupBy(column(fn));
        }


        public JoinExample build() {
            Set<String> sets = new HashSet<>(1);
            example.getSelectColumns().removeIf(name -> {
                if (name.contains(" ")) {
                    name = StrUtil.subAfter(name, " ", true);
                }
                if (name.contains(".")) {
                    name = StrUtil.subAfter(name, ".", true);
                }
                return !sets.add(name);
            });
            sets.clear();
            example.getExampleCriterias().addAll(buildCriteria());
            return example;
        }
    }

    public static JoinExample.Builder builder(Class tableClass) {
        return new JoinExample.Builder(tableClass);
    }

    @Data
    public static class Criteria {
        private List<Criterion> criterion = new ArrayList<>();
        @NonNull
        private String andOr;

        void addCriterion(Sqls.Criterion criterion) {
            this.criterion.add(new Criterion(criterion));
        }

        public boolean isValid() {
            return criterion.size() > 0;
        }
    }

    @Data
    public static class Criterion {
        @NonNull
        private Sqls.Criterion criterion;

        public boolean isListValue() {
            return criterion.getValue() instanceof Collection;
        }
        public boolean isSingleValue() {
            return !isListValue();
        }
        public boolean isBetweenValue() {
            return criterion.getValue() != null && criterion.getSecondValue() != null && criterion.getCondition().contains("between");
        }
        public boolean isNoValue() {
            return criterion.getValue() == null && criterion.getSecondValue() == null;
        }
    }

    public static class Where implements SqlsCriteria {
        private Sqls.Criteria criteria;
        private boolean isNullValue = false;
        @Setter
        private boolean isAlias = true;

        private Where() {
            this.criteria = new Sqls.Criteria();
        }

        public static Where custom(boolean alias) {
            return new Where().setAlias(alias);
        }
        public static Where custom() {
            return new Where();
        }

        public Where allowNullValue() {
            this.isNullValue = true;
            return this;
        }

        public Where andIsNull(String property) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, "is null", "and"));
            return this;
        }

        public <T> Where andIsNull(Fn<T, Object> fn) {
            return this.andIsNull(column(fn, isAlias));
        }

        public Where andIsNotNull(String property) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, "is not null", "and"));
            return this;
        }

        public <T> Where andIsNotNull(Fn<T, Object> fn) {
            return this.andIsNotNull(column(fn, isAlias));
        }

        public Where andEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "=", "and"));
            return this;
        }

        public <T> Where andEqualTo(Fn<T, Object> fn, Object value) {
            return this.andEqualTo(column(fn, isAlias), value);
        }

        public Where andNotEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "<>", "and"));
            return this;
        }

        public <T> Where andNotEqualTo(Fn<T, Object> fn, Object value) {
            return this.andNotEqualTo(column(fn, isAlias), value);
        }

        public Where andGreaterThan(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, ">", "and"));
            return this;
        }

        public <T> Where andGreaterThan(Fn<T, Object> fn, Object value) {
            return this.andGreaterThan(column(fn, isAlias), value);
        }

        public <T> Where andIncrement(Fn<T, Object> fn, Number increment, String condition, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(column(fn, isAlias) + " + " + increment, value, condition, "and"));
            return this;
        }

        public <T> Where andIncrement(Fn<T, Object> fn, Number increment, String condition, Fn<T, Object> col) {
            this.criteria.getCriterions().add(new Sqls.Criterion(column(fn, isAlias) + " + " + increment, condition + " " + column(col, isAlias), "and"));
            return this;
        }

        public Where andIncrement(String property, Number increment, String condition, Number value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property + " + " + increment, value, condition, "and"));
            return this;
        }

        public Where andIncrement(String property, Number increment, String condition, String col) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property + " + " + increment, condition + " " + col, "and"));
            return this;
        }

        public Where andGreaterThanOrEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, ">=", "and"));
            return this;
        }

        public <T> Where andGreaterThanOrEqualTo(Fn<T, Object> fn, Object value) {
            return this.andGreaterThanOrEqualTo(column(fn, isAlias), value);
        }

        public Where andLessThan(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "<", "and"));
            return this;
        }

        public <T> Where andLessThan(Fn<T, Object> fn, Object value) {
            return this.andLessThan(column(fn, isAlias), value);
        }

        public Where andLessThanOrEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "<=", "and"));
            return this;
        }

        public <T> Where andLessThanOrEqualTo(Fn<T, Object> fn, Object value) {
            return this.andLessThanOrEqualTo(column(fn, isAlias), value);
        }

        public Where andIn(String property, Iterable values) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, values, "in", "and"));
            return this;
        }

        public <T> Where andIn(Fn<T, Object> fn, Iterable values) {
            return this.andIn(column(fn, isAlias), values);
        }

        public Where andNotIn(String property, Iterable values) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, values, "not in", "and"));
            return this;
        }

        public <T> Where andNotIn(Fn<T, Object> fn, Iterable values) {
            return this.andNotIn(column(fn, isAlias), values);
        }

        public Where andBetween(String property, Object value1, Object value2) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value1, value2, "between", "and"));
            return this;
        }

        public <T> Where andBetween(Fn<T, Object> fn, Object value1, Object value2) {
            return this.andBetween(column(fn, isAlias), value1, value2);
        }

        public Where andNotBetween(String property, Object value1, Object value2) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value1, value2, "not between", "and"));
            return this;
        }

        public <T> Where andNotBetween(Fn<T, Object> fn, Object value1, Object value2) {
            return this.andNotBetween(column(fn, isAlias), value1, value2);
        }

        public Where andLike(String property, String value, String format) {
            if (StrUtil.isNotBlank(value)) {
                value = MessageFormat.format(format, value);
                this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "like", "and"));
            }
            return this;
        }
        public Where andLike(String property, String value) {
            return andLike(property, value, "%{0}%");
        }

        public <T> Where andLike(Fn<T, Object> fn, String value) {
            return this.andLike(column(fn, isAlias), value);
        }
        public <T> Where andLike(Fn<T, Object> fn, String value, String format) {
            return this.andLike(column(fn, isAlias), value, format);
        }

        public Where andNotLike(String property, String value, String format) {
            if (StrUtil.isNotBlank(value)) {
                value = MessageFormat.format(format, value);
                this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "not like", "and"));
            }
            return this;
        }
        public Where andNotLike(String property, String value) {
            return andNotLike(property, value, "%{0}%");
        }

        public <T> Where andNotLike(Fn<T, Object> fn, String value) {
            return this.andNotLike(column(fn, isAlias), value);
        }
        public <T> Where andNotLike(Fn<T, Object> fn, String value, String format) {
            return this.andNotLike(column(fn, isAlias), value, format);
        }

        public Where orIsNull(String property) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, "is null", "or"));
            return this;
        }

        public <T> Where orIsNull(Fn<T, Object> fn) {
            return this.orIsNull(column(fn, isAlias));
        }

        public Where orIsNotNull(String property) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, "is not null", "or"));
            return this;
        }

        public <T> Where orIsNotNull(Fn<T, Object> fn) {
            return this.orIsNotNull(column(fn, isAlias));
        }

        public Where orEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "=", "or"));
            return this;
        }

        public <T> Where orEqualTo(Fn<T, Object> fn, String value) {
            return this.orEqualTo(column(fn, isAlias), value);
        }

        public Where orNotEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "<>", "or"));
            return this;
        }

        public <T> Where orNotEqualTo(Fn<T, Object> fn, String value) {
            return this.orNotEqualTo(column(fn, isAlias), value);
        }

        public Where orGreaterThan(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, ">", "or"));
            return this;
        }

        public <T> Where orGreaterThan(Fn<T, Object> fn, String value) {
            return this.orGreaterThan(column(fn, isAlias), value);
        }

        public Where orGreaterThanOrEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, ">=", "or"));
            return this;
        }

        public <T> Where orGreaterThanOrEqualTo(Fn<T, Object> fn, String value) {
            return this.orGreaterThanOrEqualTo(column(fn, isAlias), value);
        }

        public Where orLessThan(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "<", "or"));
            return this;
        }

        public <T> Where orLessThan(Fn<T, Object> fn, String value) {
            return this.orLessThan(column(fn, isAlias), value);
        }

        public Where orLessThanOrEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "<=", "or"));
            return this;
        }

        public <T> Where orLessThanOrEqualTo(Fn<T, Object> fn, String value) {
            return this.orLessThanOrEqualTo(column(fn, isAlias), value);
        }

        public Where orIn(String property, Iterable values) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, values, "in", "or"));
            return this;
        }

        public <T> Where orIn(Fn<T, Object> fn, Iterable values) {
            return this.orIn(column(fn, isAlias), values);
        }

        public Where orNotIn(String property, Iterable values) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, values, "not in", "or"));
            return this;
        }

        public <T> Where orNotIn(Fn<T, Object> fn, Iterable values) {
            return this.orNotIn(column(fn, isAlias), values);
        }

        public Where orBetween(String property, Object value1, Object value2) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value1, value2, "between", "or"));
            return this;
        }

        public <T> Where orBetween(Fn<T, Object> fn, Object value1, Object value2) {
            return this.orBetween(column(fn, isAlias), value1, value2);
        }

        public Where orNotBetween(String property, Object value1, Object value2) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value1, value2, "not between", "or"));
            return this;
        }

        public <T> Where orNotBetween(Fn<T, Object> fn, Object value1, Object value2) {
            return this.orNotBetween(column(fn, isAlias), value1, value2);
        }

        public Where orLike(String property, String value) {
            return this.orLike(property, value, "%{0}%");
        }

        public <T> Where orLike(Fn<T, Object> fn, String value) {
            return this.orLike(column(fn, isAlias), value);
        }

        public Where orLike(String property, String value, String format) {
            if (StrUtil.isNotBlank(value)) {
                value = MessageFormat.format(format, value);
                this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "like", "or"));
            }
            return this;
        }

        public <T> Where orLike(Fn<T, Object> fn, String value, String format) {
            return this.orLike(column(fn, isAlias), value, format);
        }

        public Where orNotLike(String property, String value) {
            return this.orNotLike(property, value, "%{0}%");
        }

        public Where orNotLike(String property, String value, String format) {
            if (StrUtil.isNotBlank(value)) {
                value = MessageFormat.format(format, value);
                this.criteria.getCriterions().add(new Sqls.Criterion(property, value, "not like", "or"));
            }
            return this;
        }

        public <T> Where orNotLike(Fn<T, Object> fn, String value) {
            return this.orNotLike(column(fn, isAlias), value);
        }

        public <T> Where orNotLike(Fn<T, Object> fn, String value, String format) {
            return this.orNotLike(column(fn, isAlias), value, format);
        }

        @Override
        public Sqls.Criteria getCriteria() {
            if (!isNullValue) {
                this.criteria.getCriterions().removeIf(criterion ->
                        (!criterion.getCondition().contains("null") && criterion.getValue() == null)
                        || (criterion.getValue() instanceof Collection && CollectionUtil.isEmpty((Collection<?>) criterion.getValue()))
                );
            }
            return criteria;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class Column {
        private String name;
        private EntityField field;
        private String tableName;
        private Object value;
        @SneakyThrows
        public boolean isNullValueAbsent(Object object) {
            Object value = field.getValue(object);
            boolean is = StrUtil.isBlankIfStr(value);
            if (!is) {
                this.value = value;
            }
            return is;
        }
    }

    public static class JustWhere<T extends JustWhere> {
        private List<Sqls.Criteria> sqlsCriteria = new ArrayList<>();
        public T where(Sqls sqls) {
            Sqls.Criteria criteria = sqls.getCriteria();
            criteria.setAndOr("and");
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T where(SqlsCriteria sqls) {
            Sqls.Criteria criteria = sqls.getCriteria();
            criteria.setAndOr("and");
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T where(Object object, String alias) {
            Sqls.Criteria criteria = new Sqls.Criteria();
            String tableName = StrUtil.blankToDefault(alias, tableAlias(object.getClass()));
            criteria.getCriterions().addAll(getColumns(object.getClass()).stream()
                    .filter(column -> !column.isNullValueAbsent(object))
                    .map(column -> new Sqls.Criterion(tableName + "." + column.getName(), column.getValue(), " = ", "and"))
                    .collect(Collectors.toList())
            );

            criteria.setAndOr("and");
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T where(Object object) {
            return where(object, null);
        }

        public T andWhere(Sqls sqls) {
            Sqls.Criteria criteria = sqls.getCriteria();
            criteria.setAndOr("and");
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T andWhere(SqlsCriteria sqls) {
            Sqls.Criteria criteria = sqls.getCriteria();
            criteria.setAndOr("and");
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T orWhere(Sqls sqls) {
            Sqls.Criteria criteria = sqls.getCriteria();
            criteria.setAndOr("or");
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T orWhere(SqlsCriteria sqls) {
            Sqls.Criteria criteria = sqls.getCriteria();
            criteria.setAndOr("or");
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public List<Criteria> buildCriteria() {
            List<Criteria> list = new ArrayList<>(sqlsCriteria.size());
            for (Sqls.Criteria criteria : sqlsCriteria) {
                Criteria exampleCriteria = new Criteria(criteria.getAndOr());
                for (Sqls.Criterion criterion : criteria.getCriterions()) {
                    exampleCriteria.addCriterion(criterion);
                }
                list.add(exampleCriteria);
            }
            return list;
        }

        public JoinExample buildExampleWhere() {
            return new JoinExample().setExampleCriterias(buildCriteria());
        }
    }

    private static String column(Fn fn, boolean alias) {
        try {
            Method method = fn.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            SerializedLambda serializedLambda = (SerializedLambda) method.invoke(fn);
            String getter = serializedLambda.getImplMethodName();
            if (GET_PATTERN.matcher(getter).matches()) {
                getter = getter.substring(3);
            } else if (IS_PATTERN.matcher(getter).matches()) {
                getter = getter.substring(2);
            }
            String fieldName = Introspector.decapitalize(getter);
            Class c = fn.getClass().getClassLoader().loadClass(serializedLambda.getImplClass().replace("/", "."));
            Optional<Column> optional = getColumns(c).stream().filter(col -> col.getField().getName().equals(fieldName)).findFirst();
            Column column = optional.orElseThrow(ReflectionOperationException::new);
            return alias ? tableAlias(c) + "." + column.getName() : column.getName();
        } catch (ReflectiveOperationException e) {
            throw new ReflectionOperationException(e);
        }
    }
    private static String column(Fn fn) {
        return column(fn, true);
    }

    /**
     * 获取class列
     * @param c
     * @return
     */
    public static List<Column> getColumns(Class c) {
        return CLASS_COLUMN_MAP.computeIfAbsent(c, key -> {
            //获取全部列
            List<EntityField> fields = FieldHelper.getFields(c);
            return fields.stream().filter(JoinExample::checkField).map(field -> {
                String columnName = null;
                if (field.isAnnotationPresent(javax.persistence.Column.class)) {
                    javax.persistence.Column column = field.getAnnotation(javax.persistence.Column.class);
                    columnName = column.name();
                }
                //ColumnType
                if (field.isAnnotationPresent(ColumnType.class)) {
                    ColumnType columnType = field.getAnnotation(ColumnType.class);
                    //column可以起到别名的作用
                    if (StringUtil.isEmpty(columnName) && StringUtil.isNotEmpty(columnType.column())) {
                        columnName = columnType.column();
                    }
                }
                //列名
                if (StringUtil.isEmpty(columnName)) {
                    columnName = StringUtil.convertByStyle(field.getName(), Style.camelhump);
                }
                //自动处理关键字
                if (SqlReservedWords.containsWord(columnName)) {
                    columnName = MessageFormat.format("`{0}`", columnName);
                }
                return new Column().setName(columnName).setField(field);
            }).collect(Collectors.toList());
        });
    }

    /**
     * 如果不是简单类型，直接跳过
     * 如果标记了 Column 或 ColumnType 注解，也不忽略
     * @param field
     * @return
     */
    private static boolean checkField(EntityField field) {
        return !(!field.isAnnotationPresent(javax.persistence.Column.class)
                && !field.isAnnotationPresent(ColumnType.class)
                && !SimpleTypeUtil.isSimpleType(field.getJavaType()));
    }

    private static String tableAlias(Class c) {
        String alias = StrUtil.toUnderlineCase(c.getSimpleName());
        //自动处理关键字
        if (SqlReservedWords.containsWord(alias)) {
            alias = MessageFormat.format("`{0}`", alias);
        }
        return alias;
    }
}
