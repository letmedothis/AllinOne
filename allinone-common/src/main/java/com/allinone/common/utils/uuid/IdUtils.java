package com.allinone.common.utils.uuid;

/**
 * ID生成器工具类
 * 
 * @author ruoyi
 */
public class IdUtils
{
    /** 2020-01-01 UTC；保留 41 位毫秒时间、6 位节点、6 位序列，结果始终在 JS 安全整数范围内。 */
    private static final long LONG_ID_EPOCH = 1577836800000L;
    private static final long MAX_JS_SAFE_INTEGER = 9007199254740991L;
    private static final int NODE_BITS = 6;
    private static final int SEQUENCE_BITS = 6;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private static final long NODE_ID = resolveNodeId();
    private static long lastTimestamp = -1L;
    private static long sequence = 0L;

    /**
     * 获取随机UUID
     * 
     * @return 随机UUID
     */
    public static String randomUUID()
    {
        return UUID.randomUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线
     * 
     * @return 简化的UUID，去掉了横线
     */
    public static String simpleUUID()
    {
        return UUID.randomUUID().toString(true);
    }

    /**
     * 获取随机UUID，使用性能更好的ThreadLocalRandom生成UUID
     * 
     * @return 随机UUID
     */
    public static String fastUUID()
    {
        return UUID.fastUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线，使用性能更好的ThreadLocalRandom生成UUID
     * 
     * @return 简化的UUID，去掉了横线
     */
    public static String fastSimpleUUID()
    {
        return UUID.fastUUID().toString(true);
    }

    /**
     * 生成可安全传输给 JavaScript number 的分布式长整型 ID。
     * 多实例部署时应通过 -Dallinone.node-id=0..63 为每个实例配置不同节点号。
     */
    public static synchronized long nextLongId()
    {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp)
        {
            throw new IllegalStateException("系统时钟回拨，无法生成ID");
        }
        if (timestamp == lastTimestamp)
        {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0)
            {
                timestamp = waitNextMillis(lastTimestamp);
            }
        }
        else
        {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        long id = ((timestamp - LONG_ID_EPOCH) << (NODE_BITS + SEQUENCE_BITS))
                | (NODE_ID << SEQUENCE_BITS)
                | sequence;
        if (id <= 0 || id > MAX_JS_SAFE_INTEGER)
        {
            throw new IllegalStateException("生成的ID超出JavaScript安全整数范围");
        }
        return id;
    }

    private static long waitNextMillis(long previousTimestamp)
    {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= previousTimestamp)
        {
            Thread.onSpinWait();
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    private static long resolveNodeId()
    {
        String configured = System.getProperty("allinone.node-id");
        if (configured != null && !configured.isBlank())
        {
            long nodeId = Long.parseLong(configured);
            if (nodeId < 0 || nodeId >= (1L << NODE_BITS))
            {
                throw new IllegalArgumentException("allinone.node-id 必须在 0..63 范围内");
            }
            return nodeId;
        }
        String runtimeName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        return Integer.toUnsignedLong(runtimeName.hashCode()) & ((1L << NODE_BITS) - 1);
    }
}
