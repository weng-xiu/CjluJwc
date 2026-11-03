package com.yu.framework.cache;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.types.Expiration;

/**
 * T-5：分布式锁的字节级契约。
 *
 * <p>这一层不是“加锁能成功”的普通功能测试，而是把 T-5 的两个缺陷钉在断言里：</p>
 * <ol>
 *   <li><b>隐式序列化</b>：锁值与 Lua 的 ARGV 必须是裸 UTF-8 字节。旧实现走
 *       {@code RedisTemplate<Object,Object>} 的 valueSerializer（工程内是 FastJson2），
 *       token 会被写成带引号的 JSON 字面量 {@code "\"uuid\""}，而脚本内
 *       {@code redis.call('get')} 拿到的是原始字符串，比较必然失配 —— 表现为“锁永远解不掉，
 *       只能等 TTL 过期”且不报任何错。这里直接断言写进 Redis 的字节不含引号。</li>
 *   <li><b>租约短于任务时长</b>：句柄必须在租约的 1/3 处自动 {@code PEXPIRE} 续期，
 *       关闭后立刻停止续期并（按 token）解锁。</li>
 * </ol>
 *
 * @author cjlu
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DistributedLockTest
{
    private static final String BIZ_KEY = "selection:lottery:1";

    private static final String EXPECTED_KEY = "lock:" + BIZ_KEY;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection connection;

    @InjectMocks
    private DistributedLock distributedLock;

    @BeforeEach
    void setUp()
    {
        when(connectionFactory.getConnection()).thenReturn(connection);
    }

    @AfterEach
    void tearDown()
    {
        // 看门狗是守护线程，但显式关掉才不会跨用例互相干扰
        distributedLock.shutdown();
    }

    private void setReturns(boolean acquired)
    {
        when(connection.set(any(byte[].class), any(byte[].class), any(Expiration.class), any(SetOption.class)))
                .thenReturn(acquired);
    }

    @Test
    @DisplayName("加锁：键与值都是裸 UTF-8 字节（不经 valueSerializer，值不带 JSON 引号）")
    void tryLock_writesRawUtf8Bytes()
    {
        setReturns(true);

        String token = distributedLock.tryLock(BIZ_KEY, 0L, 30000L);

        assertNotNull(token);
        ArgumentCaptor<byte[]> key = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<byte[]> value = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<Expiration> expiration = ArgumentCaptor.forClass(Expiration.class);
        verify(connection).set(key.capture(), value.capture(), expiration.capture(), any(SetOption.class));

        assertArrayEquals(EXPECTED_KEY.getBytes(UTF_8), key.getValue(), "锁键必须带 lock: 前缀且为原始字节");
        assertArrayEquals(token.getBytes(UTF_8), value.getValue(), "锁值必须就是裸 token 本身");
        assertFalse(new String(value.getValue(), UTF_8).startsWith("\""),
                "值被 FastJson 序列化成带引号字面量时，Lua 的 get 比较会静默失配");
        assertEquals(30000L, expiration.getValue().getExpirationTimeInMilliseconds(), "租约必须显式下发，不能依赖默认值");
    }

    @Test
    @DisplayName("加锁：已被他人持有且不等待时返回 null（不做 read-then-write）")
    void tryLock_returnsNullWhenHeldByOther()
    {
        setReturns(false);

        assertNull(distributedLock.tryLock(BIZ_KEY, 0L, 30000L));
    }

    @Test
    @DisplayName("解锁：Lua 比对 token 后删除，脚本与 ARGV 均为原始字节")
    void unlock_comparesTokenThenDeletes()
    {
        when(connection.eval(any(byte[].class), eq(ReturnType.INTEGER), eq(1), any(byte[].class), any(byte[].class)))
                .thenReturn(1L);

        assertTrue(distributedLock.unlock(BIZ_KEY, "token-abc"));

        ArgumentCaptor<byte[]> script = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<byte[]> key = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<byte[]> arg = ArgumentCaptor.forClass(byte[].class);
        verify(connection).eval(script.capture(), eq(ReturnType.INTEGER), eq(1), key.capture(), arg.capture());

        String lua = new String(script.getValue(), UTF_8);
        assertTrue(lua.contains("redis.call('get', KEYS[1]) == ARGV[1]"), lua);
        assertTrue(lua.contains("del"), lua);
        assertArrayEquals(EXPECTED_KEY.getBytes(UTF_8), key.getValue());
        assertArrayEquals("token-abc".getBytes(UTF_8), arg.getValue());
    }

    @Test
    @DisplayName("解锁：token 不匹配（脚本返回 0）时报告失败，便于调用方留痕")
    void unlock_reportsFailureWhenTokenMismatch()
    {
        when(connection.eval(any(byte[].class), eq(ReturnType.INTEGER), eq(1), any(byte[].class), any(byte[].class)))
                .thenReturn(0L);

        assertFalse(distributedLock.unlock(BIZ_KEY, "stale-token"));
    }

    @Test
    @DisplayName("续期：租约以 ARGV[2] 传入，脚本只对匹配 token 的锁 PEXPIRE")
    void renew_passesLeaseAsSecondArg()
    {
        when(connection.eval(any(byte[].class), eq(ReturnType.INTEGER), eq(1),
                any(byte[].class), any(byte[].class), any(byte[].class))).thenReturn(1L);

        assertTrue(distributedLock.renew(BIZ_KEY, "token-abc", 20000L));

        ArgumentCaptor<byte[]> script = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<byte[]> lease = ArgumentCaptor.forClass(byte[].class);
        verify(connection).eval(script.capture(), eq(ReturnType.INTEGER), eq(1),
                any(byte[].class), any(byte[].class), lease.capture());

        String lua = new String(script.getValue(), UTF_8);
        assertTrue(lua.contains("pexpire"), lua);
        assertTrue(lua.contains("ARGV[2]"), lua);
        assertArrayEquals("20000".getBytes(UTF_8), lease.getValue());
    }

    @Test
    @DisplayName("句柄：按租约 1/3 自动续期，关闭后停止续期并按 token 解锁")
    void lockHandle_renewsUntilClosed() throws Exception
    {
        setReturns(true);
        when(connection.eval(any(byte[].class), eq(ReturnType.INTEGER), eq(1),
                any(byte[].class), any(byte[].class), any(byte[].class))).thenReturn(1L);
        when(connection.eval(any(byte[].class), eq(ReturnType.INTEGER), eq(1), any(byte[].class), any(byte[].class)))
                .thenReturn(1L);

        // 租约 3s → 首次续期在 1s 后（间隔 = max(1000, lease/3)）
        DistributedLock.LockHandle handle = distributedLock.tryAcquire(BIZ_KEY, 0L, 3000L);
        assertNotNull(handle);
        Thread.sleep(1600L);

        assertTrue(countEvalCalls("pexpire") > 0, "长任务必须在租约到期前续期，否则任务跑到一半就丢锁");

        handle.close();
        assertEquals(1, countEvalCalls("redis.call('del'"), "关闭时必须按 token 解锁，且只解一次");

        int renewalsAfterClose = countEvalCalls("pexpire");
        // 关闭后不得再续期：再等一个续期周期，续期调用数不应增加
        Thread.sleep(1400L);
        assertEquals(renewalsAfterClose, countEvalCalls("pexpire"), "close() 必须先停续期再解锁，否则会在 unlock 之后把锁重新续命");
    }

    /** 统计所有 EVAL 调用中包含指定片段的次数（续期 3 个 ARGV、解锁 2 个，不能用同一组参数匹配器） */
    private int countEvalCalls(String fragment)
    {
        return (int) Mockito.mockingDetails(connection).getInvocations().stream()
                .filter(invocation -> "eval".equals(invocation.getMethod().getName()))
                .filter(invocation -> new String((byte[]) invocation.getArgument(0), UTF_8).contains(fragment))
                .count();
    }

    @Test
    @DisplayName("句柄：拿不到锁时不返回句柄，避免调用方误以为持有锁")
    void tryAcquire_returnsNullWhenLockMissing()
    {
        setReturns(false);

        assertNull(distributedLock.tryAcquire(BIZ_KEY, 0L, 3000L));
    }
}
