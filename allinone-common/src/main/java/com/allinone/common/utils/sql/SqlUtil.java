package com.allinone.common.utils.sql;

import com.allinone.common.exception.UtilException;
import com.allinone.common.utils.StringUtils;

/**
 * sql操作工具类
 * 
 * @author ruoyi
 */
public class SqlUtil
{
    /**
     * 定义常用的 sql关键字
     */
    public static String SQL_REGEX = "\u000B|%0A|and |extractvalue|updatexml|sleep|information_schema|exec |insert |select |delete |update |drop |count |chr |mid |master |truncate |char |declare |or |union |like |+|/*|user()";

    /**
     * 仅支持字母、数字、下划线、空格、逗号、小数点（支持多个字段排序）
     */
    public static String SQL_PATTERN = "[a-zA-Z0-9_\\ \\,\\.]+";

    /**
     * 限制orderBy最大长度
     */
    private static final int ORDER_BY_MAX_LENGTH = 500;

    /**
     * 检查字符，防止注入绕过
     */
    public static String escapeOrderBySql(String value)
    {
        if (StringUtils.isNotEmpty(value) && !isValidOrderBySql(value))
        {
            throw new UtilException("参数不符合规范，不能进行查询");
        }
        if (StringUtils.length(value) > ORDER_BY_MAX_LENGTH)
        {
            throw new UtilException("参数已超过最大限制，不能进行查询");
        }
        return value;
    }

    /**
     * 验证 order by 语法是否符合规范
     */
    public static boolean isValidOrderBySql(String value)
    {
        return value.matches(SQL_PATTERN);
    }

    /**
     * SQL关键字检查
     * 使用大小写不敏感的正则表达式匹配，保留原始分词边界
     */
    public static void filterKeyword(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return;
        }
        // 使用正则表达式匹配关键字，保留原始分词边界
        // 使用\b确保匹配完整的单词，避免误匹配
        String[] sqlKeywords = StringUtils.split(SQL_REGEX, "\\|");
        for (String sqlKeyword : sqlKeywords)
        {
            // 跳过特殊字符
            if (sqlKeyword.equals("+") || sqlKeyword.equals("/*") || sqlKeyword.equals("user()"))
            {
                // 对于特殊字符，直接检查是否包含
                if (StringUtils.indexOfIgnoreCase(value, sqlKeyword) > -1)
                {
                    throw new UtilException("请求参数包含敏感关键词'" + sqlKeyword + "'，可能存在安全风险");
                }
                continue;
            }

            // 空白关键字（如首项垂直制表符 \u000B）trim 后为空串，若进入正则会编译成 \b\b
            // 匹配任意含字母数字的字符串导致全拒绝；改为直接包含性判断，保留其拦截控制字符的本意
            if (sqlKeyword.isBlank())
            {
                if (!sqlKeyword.isEmpty() && StringUtils.contains(value, sqlKeyword))
                {
                    throw new UtilException("请求参数包含敏感关键词'" + sqlKeyword + "'，可能存在安全风险");
                }
                continue;
            }

            // 对于普通关键字，使用正则表达式匹配
            // 添加可选的尾随空格或行尾
            String pattern = "(?i)\\b" + sqlKeyword.trim() + "\\b";
            if (java.util.regex.Pattern.compile(pattern).matcher(value).find())
            {
                throw new UtilException("请求参数包含敏感关键词'" + sqlKeyword.trim() + "'，可能存在安全风险");
            }
        }
    }
}
