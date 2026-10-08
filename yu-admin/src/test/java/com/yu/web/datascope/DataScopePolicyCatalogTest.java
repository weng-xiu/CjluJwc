package com.yu.web.datascope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.common.annotation.DataScope;
import com.yu.web.datascope.DataScopePolicyCatalog.PolicyEntry;

/**
 * 数据行级权限集中清单一致性校验（A4 数据权限体系化·守门测试）。
 *
 * <p>逐条反射校验 {@link DataScopePolicyCatalog} 登记的受保护资源，其 Service 列表方法上的
 * {@code @DataScope} 实际别名与清单期望值是否一致：漏挂注解或别名漂移都会让行级过滤失效或错位，
 * 本测试在离线（无需 DB/Spring）下即可拦截这类回归。</p>
 */
class DataScopePolicyCatalogTest
{
    @Test
    @DisplayName("清单非空且逐项 @DataScope 别名与登记一致")
    void catalog_matchesActualAnnotations() throws Exception
    {
        List<PolicyEntry> entries = DataScopePolicyCatalog.entries();
        assertTrue(!entries.isEmpty(), "集中清单不应为空");

        for (PolicyEntry e : entries)
        {
            Class<?> clazz = Class.forName(e.getServiceClass());
            Method method = findListMethod(clazz, e.getMethodName());
            assertNotNull(method, () -> e.getResourceLabel() + " 未找到列表方法 " + e.getMethodName());

            DataScope ds = method.getAnnotation(DataScope.class);
            assertNotNull(ds, () -> e.getResourceLabel() + " 的 " + e.getMethodName()
                    + " 漏挂 @DataScope，存在越权可见风险");

            assertEquals(e.getExpectedDeptAlias(), ds.deptAlias(),
                    () -> e.getResourceLabel() + " deptAlias 漂移，期望[" + e.getExpectedDeptAlias()
                            + "] 实际[" + ds.deptAlias() + "]");
            assertEquals(e.getExpectedUserAlias(), ds.userAlias(),
                    () -> e.getResourceLabel() + " userAlias 漂移，期望[" + e.getExpectedUserAlias()
                            + "] 实际[" + ds.userAlias() + "]");
        }
    }

    @Test
    @DisplayName("受保护资源的用户/部门字段名沿用默认口径 user_id/dept_id")
    void catalog_keepsDefaultFieldNames() throws Exception
    {
        for (PolicyEntry e : DataScopePolicyCatalog.entries())
        {
            Method method = findListMethod(Class.forName(e.getServiceClass()), e.getMethodName());
            DataScope ds = method.getAnnotation(DataScope.class);
            assertNotNull(ds);
            assertEquals("user_id", ds.userField(), e.getResourceLabel() + " userField 应沿用默认 user_id");
            assertEquals("dept_id", ds.deptField(), e.getResourceLabel() + " deptField 应沿用默认 dept_id");
        }
    }

    /** 按方法名查找唯一列表方法（形如 selectXxxList(Domain)），忽略参数类型差异 */
    private Method findListMethod(Class<?> clazz, String methodName)
    {
        for (Method m : clazz.getDeclaredMethods())
        {
            if (m.getName().equals(methodName) && m.getParameterCount() == 1)
            {
                return m;
            }
        }
        return null;
    }
}
