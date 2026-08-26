package com.allinone.generator.util;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.allinone.common.exception.UtilException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * CREATE TABLE SQL 安全校验器
 * 使用AST白名单方式校验SQL，防止SQL注入
 */
public class CreateTableSqlValidator {

    /**
     * 表名和列名允许的字符模式
     */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    /**
     * 允许的列类型
     */
    private static final java.util.Set<String> ALLOWED_COLUMN_TYPES = java.util.Set.of(
        "bigint", "int", "integer", "smallint", "tinyint",
        "decimal", "numeric", "float", "double",
        "char", "varchar", "text", "longtext", "mediumtext",
        "date", "datetime", "timestamp", "time",
        "blob", "longblob", "mediumblob",
        "json", "enum", "set"
    );

    /**
     * 校验CREATE TABLE SQL
     * 
     * @param sql SQL语句
     * @return 校验后的规范化SQL
     * @throws UtilException 如果SQL不合法
     */
    public static String validate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new UtilException("SQL语句不能为空");
        }

        // 解析SQL
        List<SQLStatement> sqlStatements;
        try {
            sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        } catch (Exception e) {
            throw new UtilException("SQL解析失败: " + e.getMessage());
        }

        // 只允许一条CREATE TABLE语句
        if (sqlStatements.size() != 1) {
            throw new UtilException("只允许一条CREATE TABLE语句");
        }

        SQLStatement sqlStatement = sqlStatements.get(0);
        
        // 必须是CREATE TABLE语句
        if (!(sqlStatement instanceof MySqlCreateTableStatement)) {
            throw new UtilException("只允许CREATE TABLE语句");
        }

        MySqlCreateTableStatement createTable = (MySqlCreateTableStatement) sqlStatement;

        // 检查是否有AS SELECT（防止数据外带）
        if (createTable.getSelect() != null) {
            throw new UtilException("不允许CREATE TABLE ... AS SELECT语句");
        }

        // 检查是否有LIKE
        if (createTable.getLike() != null) {
            throw new UtilException("不允许CREATE TABLE ... LIKE语句");
        }

        // 检查表名
        String tableName = createTable.getTableName();
        if (tableName != null) {
            tableName = tableName.replace("`", "");
            if (!IDENTIFIER_PATTERN.matcher(tableName).matches()) {
                throw new UtilException("表名不符合规范: " + tableName);
            }
        }

        // 检查列定义
        if (createTable.getTableElementList() != null) {
            for (var tableElement : createTable.getTableElementList()) {
                if (tableElement instanceof com.alibaba.druid.sql.ast.statement.SQLColumnDefinition) {
                    com.alibaba.druid.sql.ast.statement.SQLColumnDefinition column = 
                        (com.alibaba.druid.sql.ast.statement.SQLColumnDefinition) tableElement;
                    
                    // 检查列名
                    String columnName = column.getColumnName();
                    if (columnName != null && !IDENTIFIER_PATTERN.matcher(columnName).matches()) {
                        throw new UtilException("列名不符合规范: " + columnName);
                    }

                    // 检查列类型
                    if (column.getDataType() != null) {
                        String dataType = column.getDataType().getName();
                        if (dataType != null) {
                            dataType = dataType.toLowerCase();
                            boolean allowed = false;
                            for (String allowedType : ALLOWED_COLUMN_TYPES) {
                                if (dataType.startsWith(allowedType)) {
                                    allowed = true;
                                    break;
                                }
                            }
                            if (!allowed) {
                                throw new UtilException("不允许的列类型: " + dataType);
                            }
                        }
                    }
                }
            }
        }

        // 返回规范化后的SQL
        return createTable.toString();
    }
}