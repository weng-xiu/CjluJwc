package com.yu.web.datascope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据行级权限（@DataScope）集中策略清单（A4 数据权限体系化·轻量集中）。
 *
 * <p>本工程行级过滤沿用若依声明式 {@code @DataScope} 注解 + {@code DataScopeAspect} 统一拼接 SQL 的机制，
 * 注解分散在各业务 Service 的列表方法上。分散写法的主要风险是「别名漂移」与「列表方法漏挂注解导致越权可见」。
 * 本清单作为<b>单一事实来源</b>集中登记受保护资源与其期望别名，配合 {@code DataScopePolicyCatalogTest}
 * 在编译期后可离线反射校验实际注解与清单一致，充当行级策略的集中守门。</p>
 *
 * <p>刻意保持为纯静态登记（非 Spring Bean、不改变运行期行为），只做治理与校验用途。</p>
 *
 * @author yu
 */
public final class DataScopePolicyCatalog
{
    /** 受保护资源的一条策略登记项 */
    public static final class PolicyEntry
    {
        /** 资源中文名（如 系统用户） */
        private final String resourceLabel;

        /** Service 实现类全限定名 */
        private final String serviceClass;

        /** 承载 @DataScope 的列表方法名 */
        private final String methodName;

        /** 期望的部门表别名（deptAlias） */
        private final String expectedDeptAlias;

        /** 期望的用户表别名（userAlias），无则为空串 */
        private final String expectedUserAlias;

        PolicyEntry(String resourceLabel, String serviceClass, String methodName,
                String expectedDeptAlias, String expectedUserAlias)
        {
            this.resourceLabel = resourceLabel;
            this.serviceClass = serviceClass;
            this.methodName = methodName;
            this.expectedDeptAlias = expectedDeptAlias;
            this.expectedUserAlias = expectedUserAlias;
        }

        public String getResourceLabel()
        {
            return resourceLabel;
        }

        public String getServiceClass()
        {
            return serviceClass;
        }

        public String getMethodName()
        {
            return methodName;
        }

        public String getExpectedDeptAlias()
        {
            return expectedDeptAlias;
        }

        public String getExpectedUserAlias()
        {
            return expectedUserAlias;
        }
    }

    /** 集中登记：跨模块受保护资源的期望别名（与各 *ServiceImpl 上的 @DataScope 对齐） */
    private static final List<PolicyEntry> ENTRIES;

    static
    {
        List<PolicyEntry> list = new ArrayList<>();
        // 系统域（yu-system）
        list.add(new PolicyEntry("系统用户", "com.yu.system.service.impl.SysUserServiceImpl",
                "selectUserList", "d", "u"));
        list.add(new PolicyEntry("部门", "com.yu.system.service.impl.SysDeptServiceImpl",
                "selectDeptList", "d", ""));
        list.add(new PolicyEntry("学生(系统)", "com.yu.system.service.impl.SysStudentServiceImpl",
                "selectStudentList", "d", "u"));
        // 基础资源域（yu-brm）
        list.add(new PolicyEntry("教师", "com.yu.brm.service.impl.BrmTeacherServiceImpl",
                "selectBrmTeacherList", "d", "t"));
        list.add(new PolicyEntry("班级", "com.yu.brm.service.impl.BrmClassServiceImpl",
                "selectBrmClassList", "d", ""));
        // 学籍学位域（yu-sam）
        list.add(new PolicyEntry("学籍学生", "com.yu.sam.service.impl.SamStudentServiceImpl",
                "selectSamStudentList", "d", "s"));
        // 考核成绩域（yu-aem）
        list.add(new PolicyEntry("成绩记录", "com.yu.aem.service.impl.AemGradeRecordServiceImpl",
                "selectAemGradeRecordList", "d", "ss"));
        list.add(new PolicyEntry("督导记录", "com.yu.aem.service.impl.AemSupervisionRecordServiceImpl",
                "selectAemSupervisionRecordList", "d", "t"));
        // 培养过程域（yu-tpm）
        list.add(new PolicyEntry("开课", "com.yu.tpm.service.impl.TpmCourseOfferingServiceImpl",
                "selectTpmCourseOfferingList", "d", "t"));
        ENTRIES = Collections.unmodifiableList(list);
    }

    private DataScopePolicyCatalog()
    {
    }

    /** 返回全部登记项（只读） */
    public static List<PolicyEntry> entries()
    {
        return ENTRIES;
    }
}

