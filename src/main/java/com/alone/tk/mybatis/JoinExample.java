package com.alone.tk.mybatis;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanDesc;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alone.tk.mybatis.annotation.Between;
import com.alone.tk.mybatis.annotation.Betweens;
import com.alone.tk.mybatis.annotation.Operation;
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

import javax.persistence.Transient;
import java.beans.Introspector;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
    public static final String LIKE_FORMAT = "%{0}%";
    public static final String AND = "and";
    public static final String OR = "or";
    public static final String PARENTHESIS_START = "(";
    public static final String PARENTHESIS_END = ")";
    public static final String ON = "on";
    public static final String COUNT = "count";
    public static final String ASTERISK = "*";
    public static final String AS = "as";
    public static final String GROUP_CONCAT = "GROUP_CONCAT";
    public static final String DISTINCT = "distinct";
    public static final String GRAVE_ACCENT = "`";
    public static final String DOLLER = "$";
    public static final String ASC = "asc";
    public static final String DESC = "desc";
    public static final String IS = "is";
    public static final String NULL = "null";
    public static final String ADD = "+";
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
    private List<Table> tables = new ArrayList<>(0);
    private Set<String> selectColumns = new HashSet<>(0);
    private List<Criteria> exampleCriterias = new ArrayList<>(1);
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
            StringBuilder sb = new StringBuilder(StrUtil.SPACE);
            sb.append(type.value)
                    .append(StrUtil.SPACE)
                    .append(entityTable.getName())
                    .append(StrUtil.SPACE)
                    .append(alias);
            sb.append(StrUtil.SPACE).append(ON).append(StrUtil.SPACE);
            Set<Map.Entry<String, String>> entries = on.entrySet();
            int i = 0;
            for (Map.Entry<String, String> entry : entries) {
                if (i > 0) {
                    sb.append(StrUtil.SPACE).append(AND).append(StrUtil.SPACE);
                }
                sb.append(entry.getKey()).append(OperationTypes.EQUAL).append(entry.getValue());
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
            return setAlias(StrUtil.EMPTY);
        }

        public Builder setAlias(String alias) {
            example.setAlias(alias);
            return this;
        }

        public Builder optional(boolean is, Consumer<Builder> builder) {
            if (is) {
                builder.accept(this);
            }
            return this;
        }

        public Builder optional(Supplier<Boolean> is, Consumer<Builder> builder) {
            return optional(is.get(), builder);
        }

        public Builder addTable(Table table) {
            boolean exists = example.getTables().contains(table);
            if (exists) {
                exists = example.getTables().stream().anyMatch(t -> t.toString().equals(table.toString()));
            }
            if (!exists) {
                example.getTables().add(table);
            }
            return this;
        }

        public Builder addCol(String col) {
            example.getSelectColumns().add(col);
            return this;
        }

        public Builder count(String col) {
            return addCol(COUNT + PARENTHESIS_START + col + PARENTHESIS_END);
        }

        public <T> Builder count(Fn<T, Object> fn) {
            return addCol(COUNT + PARENTHESIS_START + column(fn) + PARENTHESIS_END);
        }

        public Builder count() {
            return addCol(COUNT + PARENTHESIS_START + ASTERISK + PARENTHESIS_END);
        }

        public Builder addCol(String col, String alias) {
            example.getSelectColumns().add(col + StrUtil.SPACE + AS + StrUtil.SPACE + alias);
            return this;
        }

        public <T> Builder addCol(Fn<T, Object> fn) {
            return addCol(column(fn));
        }
        public <T> Builder addGroupCol(Fn<T, Object> fn, boolean isDistinct, String alias) {
            return addCol(GROUP_CONCAT + PARENTHESIS_START + (isDistinct ? (DISTINCT + StrUtil.SPACE) : StrUtil.EMPTY) + column(fn) + PARENTHESIS_END, alias);
        }
        public <T> Builder addGroupCol(Fn<T, Object> fn, String alias) {
            return addGroupCol(fn, false, alias);
        }

        public <T> Builder addCol(Fn<T, Object> fn, String alias) {
            return addCol(column(fn) + StrUtil.SPACE + AS + StrUtil.SPACE + alias);
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
                if (!col.getColumn().replace(GRAVE_ACCENT, StrUtil.EMPTY).equals(col.getProperty()) || StrUtil.isNotBlank(childName)) {
                    addCol(cName + StrUtil.DOT + col.getColumn() + StrUtil.SPACE + AS + StrUtil.SPACE + (StrUtil.isNotBlank(childName) ? childName + DOLLER + col.getProperty() : col.getProperty()));
                } else {
                    addCol(cName + StrUtil.DOT + col.getColumn());
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
            example.getOrderByMap().put(col, ASC);
            return this;
        }
        public <T> Builder asc(Fn<T, Object> fn) {
            return asc(column(fn));
        }

        public Builder desc(String col) {
            example.getOrderByMap().put(col, DESC);
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
            if (CollUtil.isEmpty(example.getSelectColumns()) && CollUtil.isNotEmpty(example.getTables())) {
                addCol(example.alias + StrUtil.DOT + ASTERISK);
            }
            example.getSelectColumns().removeIf(name -> {
                if (name.contains(StrUtil.SPACE)) {
                    name = StrUtil.subAfter(name, StrUtil.SPACE, true);
                }
                if (name.contains(StrUtil.DOT)) {
                    name = StrUtil.subAfter(name, StrUtil.DOT, true);
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
            return criterion.getValue() instanceof Iterable;
        }
        public boolean isSingleValue() {
            return !isListValue();
        }
        public boolean isBetweenValue() {
            return criterion.getValue() != null && criterion.getSecondValue() != null && criterion.getCondition().contains(OperationTypes.BETWEEN);
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

        public Where optional(boolean is, Consumer<Where> where) {
            if (is) {
                where.accept(this);
            }
            return this;
        }

        public Where optional(Supplier<Boolean> is, Consumer<Where> where) {
            return optional(is.get(), where);
        }

        public Where allowNullValue() {
            this.isNullValue = true;
            return this;
        }

        public Where andIsNull(String property) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, OperationTypes.IS_NULL, AND));
            return this;
        }

        public <T> Where andIsNull(Fn<T, Object> fn) {
            return this.andIsNull(column(fn, isAlias));
        }

        public Where andIsNotNull(String property) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, OperationTypes.IS_NOT_NULL, AND));
            return this;
        }

        public <T> Where andIsNotNull(Fn<T, Object> fn) {
            return this.andIsNotNull(column(fn, isAlias));
        }

        public Where andEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.EQUAL, AND));
            return this;
        }

        public <T> Where andEqualTo(Fn<T, Object> fn, Object value) {
            return this.andEqualTo(column(fn, isAlias), value);
        }

        public Where andNotEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.NOT_EQUAL, AND));
            return this;
        }

        public <T> Where andNotEqualTo(Fn<T, Object> fn, Object value) {
            return this.andNotEqualTo(column(fn, isAlias), value);
        }

        public Where andGreaterThan(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.GREATER_THAN, AND));
            return this;
        }

        public <T> Where andGreaterThan(Fn<T, Object> fn, Object value) {
            return this.andGreaterThan(column(fn, isAlias), value);
        }

        public <T> Where andIncrement(Fn<T, Object> fn, Number increment, String condition, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(column(fn, isAlias) + StrUtil.SPACE + ADD + StrUtil.SPACE + increment, value, condition, AND));
            return this;
        }

        public <T> Where andIncrement(Fn<T, Object> fn, Number increment, String condition, Fn<T, Object> col) {
            this.criteria.getCriterions().add(new Sqls.Criterion(column(fn, isAlias) + StrUtil.SPACE + ADD + StrUtil.SPACE + increment, condition + StrUtil.SPACE + column(col, isAlias), AND));
            return this;
        }

        public Where andIncrement(String property, Number increment, String condition, Number value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property + StrUtil.SPACE + ADD + StrUtil.SPACE + increment, value, condition, AND));
            return this;
        }

        public Where andIncrement(String property, Number increment, String condition, String col) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property + StrUtil.SPACE + ADD + StrUtil.SPACE + increment, condition + StrUtil.SPACE + col, AND));
            return this;
        }

        public Where andGreaterThanOrEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.GREATER_THAN_OR_EQUAL, AND));
            return this;
        }

        public <T> Where andGreaterThanOrEqualTo(Fn<T, Object> fn, Object value) {
            return this.andGreaterThanOrEqualTo(column(fn, isAlias), value);
        }

        public Where andLessThan(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.LESS_THAN, AND));
            return this;
        }

        public <T> Where andLessThan(Fn<T, Object> fn, Object value) {
            return this.andLessThan(column(fn, isAlias), value);
        }

        public Where andLessThanOrEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.LESS_THAN_OR_EQUAL, AND));
            return this;
        }

        public <T> Where andLessThanOrEqualTo(Fn<T, Object> fn, Object value) {
            return this.andLessThanOrEqualTo(column(fn, isAlias), value);
        }

        public Where andIn(String property, Iterable values) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, values, OperationTypes.IN, AND));
            return this;
        }

        public <T> Where andIn(Fn<T, Object> fn, Iterable values) {
            return this.andIn(column(fn, isAlias), values);
        }

        public Where andNotIn(String property, Iterable values) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, values, OperationTypes.NOT_IN, AND));
            return this;
        }

        public <T> Where andNotIn(Fn<T, Object> fn, Iterable values) {
            return this.andNotIn(column(fn, isAlias), values);
        }

        public Where andBetween(String property, Object value1, Object value2) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value1, value2, OperationTypes.BETWEEN, AND));
            return this;
        }

        public <T> Where andBetween(Fn<T, Object> fn, Object value1, Object value2) {
            return this.andBetween(column(fn, isAlias), value1, value2);
        }

        public Where andNotBetween(String property, Object value1, Object value2) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value1, value2, OperationTypes.NOT_BETWEEN, AND));
            return this;
        }

        public <T> Where andNotBetween(Fn<T, Object> fn, Object value1, Object value2) {
            return this.andNotBetween(column(fn, isAlias), value1, value2);
        }

        public Where andLike(String property, String value, String format) {
            return like(property, value, format, OperationTypes.LIKE, AND);
        }

        private Where like(String property, String value, String format, String condition, String andOr) {
            if (StrUtil.isNotBlank(value)) {
                value = MessageFormat.format(format, value);
                this.criteria.getCriterions().add(new Sqls.Criterion(property, value, condition, andOr));
            }
            return this;
        }

        public Where andLike(String property, String value) {
            return andLike(property, value, LIKE_FORMAT);
        }

        public <T> Where andLike(Fn<T, Object> fn, String value) {
            return this.andLike(column(fn, isAlias), value);
        }
        public <T> Where andLike(Fn<T, Object> fn, String value, String format) {
            return this.andLike(column(fn, isAlias), value, format);
        }

        public Where andNotLike(String property, String value, String format) {
            return like(property, value, format, OperationTypes.NOT_LIKE, AND);
        }
        public Where andNotLike(String property, String value) {
            return andNotLike(property, value, LIKE_FORMAT);
        }

        public <T> Where andNotLike(Fn<T, Object> fn, String value) {
            return this.andNotLike(column(fn, isAlias), value);
        }
        public <T> Where andNotLike(Fn<T, Object> fn, String value, String format) {
            return this.andNotLike(column(fn, isAlias), value, format);
        }

        public Where orIsNull(String property) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, OperationTypes.IS_NULL, OR));
            return this;
        }

        public <T> Where orIsNull(Fn<T, Object> fn) {
            return this.orIsNull(column(fn, isAlias));
        }

        public Where orIsNotNull(String property) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, OperationTypes.IS_NOT_NULL, OR));
            return this;
        }

        public <T> Where orIsNotNull(Fn<T, Object> fn) {
            return this.orIsNotNull(column(fn, isAlias));
        }

        public Where orEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.EQUAL, OR));
            return this;
        }

        public <T> Where orEqualTo(Fn<T, Object> fn, Object value) {
            return this.orEqualTo(column(fn, isAlias), value);
        }

        public Where orNotEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.NOT_EQUAL, OR));
            return this;
        }

        public <T> Where orNotEqualTo(Fn<T, Object> fn, String value) {
            return this.orNotEqualTo(column(fn, isAlias), value);
        }

        public Where orGreaterThan(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.GREATER_THAN, OR));
            return this;
        }

        public <T> Where orGreaterThan(Fn<T, Object> fn, String value) {
            return this.orGreaterThan(column(fn, isAlias), value);
        }

        public Where orGreaterThanOrEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.GREATER_THAN_OR_EQUAL, OR));
            return this;
        }

        public <T> Where orGreaterThanOrEqualTo(Fn<T, Object> fn, String value) {
            return this.orGreaterThanOrEqualTo(column(fn, isAlias), value);
        }

        public Where orLessThan(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.LESS_THAN, OR));
            return this;
        }

        public <T> Where orLessThan(Fn<T, Object> fn, String value) {
            return this.orLessThan(column(fn, isAlias), value);
        }

        public Where orLessThanOrEqualTo(String property, Object value) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value, OperationTypes.LESS_THAN_OR_EQUAL, OR));
            return this;
        }

        public <T> Where orLessThanOrEqualTo(Fn<T, Object> fn, String value) {
            return this.orLessThanOrEqualTo(column(fn, isAlias), value);
        }

        public Where orIn(String property, Iterable values) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, values, OperationTypes.IN, OR));
            return this;
        }

        public <T> Where orIn(Fn<T, Object> fn, Iterable values) {
            return this.orIn(column(fn, isAlias), values);
        }

        public Where orNotIn(String property, Iterable values) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, values, OperationTypes.NOT_IN, OR));
            return this;
        }

        public <T> Where orNotIn(Fn<T, Object> fn, Iterable values) {
            return this.orNotIn(column(fn, isAlias), values);
        }

        public Where orBetween(String property, Object value1, Object value2) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value1, value2, OperationTypes.BETWEEN, OR));
            return this;
        }

        public <T> Where orBetween(Fn<T, Object> fn, Object value1, Object value2) {
            return this.orBetween(column(fn, isAlias), value1, value2);
        }

        public Where orNotBetween(String property, Object value1, Object value2) {
            this.criteria.getCriterions().add(new Sqls.Criterion(property, value1, value2, OperationTypes.NOT_BETWEEN, OR));
            return this;
        }

        public <T> Where orNotBetween(Fn<T, Object> fn, Object value1, Object value2) {
            return this.orNotBetween(column(fn, isAlias), value1, value2);
        }

        public Where orLike(String property, String value) {
            return this.orLike(property, value, LIKE_FORMAT);
        }

        public <T> Where orLike(Fn<T, Object> fn, String value) {
            return this.orLike(column(fn, isAlias), value);
        }

        public Where orLike(String property, String value, String format) {
            return like(property, value, format, OperationTypes.LIKE, OR);
        }

        public <T> Where orLike(Fn<T, Object> fn, String value, String format) {
            return this.orLike(column(fn, isAlias), value, format);
        }

        public Where orNotLike(String property, String value) {
            return this.orNotLike(property, value, LIKE_FORMAT);
        }

        public Where orNotLike(String property, String value, String format) {
            return like(property, value, format, OperationTypes.NOT_LIKE, OR);
        }

        public <T> Where orNotLike(Fn<T, Object> fn, String value) {
            return this.orNotLike(column(fn, isAlias), value);
        }

        public <T> Where orNotLike(Fn<T, Object> fn, String value, String format) {
            return this.orNotLike(column(fn, isAlias), value, format);
        }

        public <T> Where bean(Object bean, String alias, Fn<T, Object>... ignores) {
            return bean(bean, alias, Arrays.stream(ignores).map(f -> column(f, isAlias)).toArray(String[]::new));
        }

        public <T> Where bean(Object bean, Fn<T, Object>... ignores) {
            return bean(bean, tableAlias(bean.getClass()), Arrays.stream(ignores).map(f -> column(f, isAlias)).toArray(String[]::new));
        }
        public <T> Where bean(Object bean, String... ignores) {
            return bean(bean, tableAlias(bean.getClass()), ignores);
        }

        public Where bean(Object bean, String alias, String... ignores) {
            BeanDesc desc = BeanUtil.getBeanDesc(bean.getClass());
            Betweens bs = AnnotationUtil.getAnnotation(bean.getClass(), Betweens.class);
            Between[] betweenArray = bs == null ? null : bs.value();
            List<Between> betweenList;
            if (betweenArray == null) {
                betweenList = new ArrayList<>(0);
            } else {
                betweenList = Arrays.stream(betweenArray).collect(Collectors.toList());
            }
            Between between = AnnotationUtil.getAnnotation(bean.getClass(), Between.class);
            if (between != null) {
                betweenList.add(between);
            }
            Map<String, Object> betweenMap = new HashMap<>(betweenList.size() * 2);
            for (Between b : betweenList) {
                betweenMap.put(b.start(), null);
                betweenMap.put(b.end(), null);
            }
            String tableName = StrUtil.blankToDefault(alias, tableAlias(bean.getClass()));
            for (BeanDesc.PropDesc prop : desc.getProps()) {
                if (ArrayUtil.contains(ignores, prop.getFieldName())) {
                    continue;
                }
                if (prop.getField().isAnnotationPresent(Transient.class)) {
                    continue;
                }
                Object value = prop.getValue(bean);
                if (value != null) {
                    if (betweenMap.containsKey(prop.getFieldName())) {
                        betweenMap.put(prop.getFieldName(), value);
                        continue;
                    }
                    Operation operation = AnnotationUtil.getAnnotation(prop.getField(), Operation.class);
                    String condition = OperationTypes.EQUAL;
                    String andOr = AND;
                    String column = prop.getFieldName();
                    String tName = tableName;
                    if (operation != null) {
                        condition = StrUtil.blankToDefault(operation.value(), condition);
                        andOr = operation.and() ? andOr : OR;
                        column = StrUtil.blankToDefault(operation.column(), column);
                        tName = StrUtil.blankToDefault(operation.alias(), tName);
                    }
                    //自动处理关键字
                    if (SqlReservedWords.containsWord(column)) {
                        column = MessageFormat.format("`{0}`", column);
                    }
                    if (value instanceof Iterable) {
                        if (!OperationTypes.IN.equals(condition) && !OperationTypes.NOT_IN.equals(condition)) {
                            throw new RuntimeException(StrUtil.format("属性:{} 值为 Iterable 但 条件为 {}", prop.getFieldName(), condition));
                        }
                    }
                    if (condition.equals(OperationTypes.NOT_LIKE) || condition.equals(OperationTypes.LIKE)) {
                        String format = Optional.ofNullable(operation).map(Operation::likeFormat).orElse(LIKE_FORMAT);
                        value = value instanceof CharSequence && !StrUtil.isBlankIfStr(value) ? MessageFormat.format(format, value) : null;
                    }
                    this.criteria.getCriterions().add(new Sqls.Criterion(tName + StrUtil.DOT + column, value, condition, andOr));
                }
            }
            for (Between b : betweenList) {
                this.criteria.getCriterions().add(new Sqls.Criterion(
                        b.value(),
                        betweenMap.get(b.start()),
                        betweenMap.get(b.end()),
                        b.not() ? OperationTypes.NOT_BETWEEN : OperationTypes.BETWEEN,
                        b.and() ? AND : OR
                ));
            }
            return this;
        }

        @Override
        public Sqls.Criteria getCriteria() {
            if (!isNullValue) {
                this.criteria.getCriterions().removeIf(criterion ->
                        (!criterion.getCondition().contains(NULL) && criterion.getValue() == null)
                        || (criterion.getValue() instanceof Iterable && CollUtil.isEmpty((Iterable<?>) criterion.getValue()))
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
            criteria.setAndOr(AND);
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T where(SqlsCriteria sqls) {
            Sqls.Criteria criteria = sqls.getCriteria();
            criteria.setAndOr(AND);
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T where(Object object, String alias) {
            Sqls.Criteria criteria = new Sqls.Criteria();
            String tableName = StrUtil.blankToDefault(alias, tableAlias(object.getClass()));
            criteria.getCriterions().addAll(getColumns(object.getClass()).stream()
                    .filter(column -> !column.isNullValueAbsent(object))
                    .map(column -> new Sqls.Criterion(tableName + StrUtil.DOT + column.getName(), column.getValue(), StrUtil.SPACE + OperationTypes.EQUAL + StrUtil.SPACE, AND))
                    .collect(Collectors.toList())
            );

            criteria.setAndOr(AND);
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T where(Object object) {
            return where(object, null);
        }

        public T andWhere(Sqls sqls) {
            Sqls.Criteria criteria = sqls.getCriteria();
            criteria.setAndOr(AND);
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T andWhere(SqlsCriteria sqls) {
            Sqls.Criteria criteria = sqls.getCriteria();
            criteria.setAndOr(AND);
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T orWhere(Sqls sqls) {
            Sqls.Criteria criteria = sqls.getCriteria();
            criteria.setAndOr(OR);
            this.sqlsCriteria.add(criteria);
            return (T) this;
        }

        public T orWhere(SqlsCriteria sqls) {
            Sqls.Criteria criteria = sqls.getCriteria();
            criteria.setAndOr(OR);
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
            Class c = fn.getClass().getClassLoader().loadClass(serializedLambda.getImplClass().replace(StrUtil.SLASH, StrUtil.DOT));
            Optional<Column> optional = getColumns(c).stream().filter(col -> col.getField().getName().equals(fieldName)).findFirst();
            Column column = optional.orElseThrow(ReflectionOperationException::new);
            return alias ? tableAlias(c) + StrUtil.DOT + column.getName() : column.getName();
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

    public static String tableAlias(Class c) {
        String alias = StrUtil.toUnderlineCase(c.getSimpleName());
        //自动处理关键字
        if (SqlReservedWords.containsWord(alias)) {
            alias = MessageFormat.format("`{0}`", alias);
        }
        return alias;
    }
}
