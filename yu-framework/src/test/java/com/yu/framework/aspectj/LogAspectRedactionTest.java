package com.yu.framework.aspectj;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * S-9 操作日志脱敏的切面侧验证：证明 {@code LogAspect} 落库前真的走了脱敏，
 * 而不只是 {@code OperLogRedactor} 单测通过。
 *
 * <p>没有走 MockMvc 全链路：{@code LogAspect} 把落库交给 {@code AsyncManager} 异步执行，
 * 断言异步结果要引入线程等待与 mock 队列，收益不如直接打这条私有拼装方法——
 * 它正是 oper_param 的唯一产出点（{@code setRequestValue} 的两个分支都汇到这里或同源的
 * {@code redactAndTrim}）。
 *
 * @author yu
 */
class LogAspectRedactionTest
{
    private static final String ID_PLAIN = "990101199001011234";

    private static Method privateMethod(String name, Class<?>... types) throws Exception
    {
        Method method = LogAspect.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }

    @Test
    void masksPiiInSerializedArgs() throws Exception
    {
        LogAspect aspect = new LogAspect();
        Map<String, Object> arg = new LinkedHashMap<>();
        arg.put("idCard", ID_PLAIN);
        arg.put("phonenumber", "13800138000");
        arg.put("studentNo", "2023001234");

        // 注意：必须显式构造参数数组。直接写 invoke(aspect, x, new String[] {}) 时，
        // 编译器会把末尾的 String[] 当成整个可变参数数组，实际传进去的实参个数为 0。
        Object[] invokeArgs = new Object[] { new Object[] { arg }, new String[] {} };
        String out = (String) privateMethod("argsArrayToString", Object[].class, String[].class)
                .invoke(aspect, invokeArgs);

        assertFalse(out.contains(ID_PLAIN), "身份证号不得出现在 oper_param：" + out);
        assertFalse(out.contains("13800138000"), "手机号不得出现在 oper_param：" + out);
        assertTrue(out.contains("2023001234"), "学号属 L2，必须保留：actual=[" + out + "]");
    }

    @Test
    void truncationHappensAfterMasking() throws Exception
    {
        // 长度 12 会切在脱敏结果内部；若顺序写反（先截断），半截身份证明文就会漏出去
        String out = (String) privateMethod("redactAndTrim", String.class, int.class)
                .invoke(new LogAspect(), "{\"idCard\":\"" + ID_PLAIN + "\"}", 12);

        assertFalse(out.contains("990101"), "截断后不得残留证件号前缀：" + out);
        assertTrue(out.startsWith("{\"idCard\":\"*"), "应先脱敏再截断：" + out);
    }
}
