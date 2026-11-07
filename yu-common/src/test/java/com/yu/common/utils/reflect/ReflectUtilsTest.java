package com.yu.common.utils.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * G5：{@link ReflectUtils} 的契约测试。
 *
 * <p>全类都是纯反射，没有任何容器依赖。真正需要钉住的是<b>失败姿态</b>：
 * 这个工具被改控制器、通用 Mapper、导出处理器到处调用，找不到字段/方法时它选择「返回 null 静默跳过」
 * 而不是抛异常，所以调用方拿到 null 后还得自己判 —— 测试要把这个约定写死，避免以后被「顺手改成抛异常」。</p>
 *
 * <p>{@code invokeMethodByName} 还有一段隐式类型转换：只有包装类型（Integer/Long/Double/Float）和
 * String/Date/boolean 会被转，原始类型 int/long 不会被转 —— 这点很容易踩，单独测。</p>
 */
class ReflectUtilsTest
{
    @Nested
    @DisplayName("invokeGetter / invokeSetter")
    class GetterSetter
    {
        @Test
        @DisplayName("单级 getter / setter 正常读写")
        void single()
        {
            Child child = new Child();
            assertEquals("c", ReflectUtils.<String> invokeGetter(child, "name"));
            ReflectUtils.invokeSetter(child, "name", "changed");
            assertEquals("changed", child.getName());
        }

        @Test
        @DisplayName("多级路径（对象名.对象名）逐级下钻")
        void nested()
        {
            Outer outer = new Outer();
            assertEquals("c", ReflectUtils.<String> invokeGetter(outer, "child.name"), "Outer.child.name 的初值");
            ReflectUtils.invokeSetter(outer, "child.name", "deep");
            assertEquals("deep", outer.getChild().getName());
        }

        @Test
        @DisplayName("getter 定义在父类上也能调用")
        void inherited()
        {
            Child child = new Child();
            assertEquals("p", ReflectUtils.<String> invokeGetter(child, "parentField"));
        }

        @Test
        @DisplayName("找不到 getter / setter 时返回 null 且不抛异常")
        void missing()
        {
            Child child = new Child();
            assertNull(ReflectUtils.invokeGetter(child, "notExist"), "getter 缺失返回 null");
            ReflectUtils.invokeSetter(child, "notExist", "x");
            assertEquals("c", child.getName(), "setter 缺失时对象状态不能被破坏");
        }
    }

    @Nested
    @DisplayName("字段直读直写")
    class FieldAccess
    {
        @Test
        @DisplayName("读取本类 private 字段与父类 private 字段")
        void read()
        {
            Child child = new Child();
            assertEquals("c", ReflectUtils.<String> getFieldValue(child, "name"));
            assertEquals(3, ReflectUtils.<Integer> getFieldValue(child, "age"));
            assertEquals("p", ReflectUtils.<String> getFieldValue(child, "parentField"), "要向上转型找父类字段");
        }

        @Test
        @DisplayName("字段不存在返回 null，不抛异常")
        void readMissing()
        {
            assertNull(ReflectUtils.getFieldValue(new Child(), "nope"));
        }

        @Test
        @DisplayName("写字段：本类与父类 private 都能写")
        void write()
        {
            Child child = new Child();
            ReflectUtils.setFieldValue(child, "name", "viaField");
            assertEquals("viaField", child.getName());
            ReflectUtils.setFieldValue(child, "parentField", "viaParent");
            assertEquals("viaParent", child.getParentField());
        }

        @Test
        @DisplayName("写不存在的字段静默返回（不打日志到 stderr、不抛异常）")
        void writeMissing()
        {
            Child child = new Child();
            ReflectUtils.setFieldValue(child, "nope", "x");
            assertEquals("c", child.getName(), "其它字段不能被连带改动");
        }

        @Test
        @DisplayName("obj 为 null 时读字段返回 null，不能抛 NPE")
        void readNullTarget()
        {
            assertNull(ReflectUtils.getFieldValue(null, "name"),
                    "getAccessibleField 已经判空返回 null，日志行不能再对 obj 取 getClass");
        }

        @Test
        @DisplayName("obj 为 null 时写字段静默返回")
        void writeNullTarget()
        {
            ReflectUtils.setFieldValue(null, "name", "x");
        }

        @Test
        @DisplayName("字段名空白直接拒绝（Validate 契约）")
        void blankFieldName()
        {
            assertThrows(IllegalArgumentException.class, () -> ReflectUtils.getAccessibleField(new Child(), " "));
            assertThrows(IllegalArgumentException.class, () -> ReflectUtils.getAccessibleField(new Child(), ""));
        }
    }

    @Nested
    @DisplayName("方法调用")
    class InvokeMethod
    {
        @Test
        @DisplayName("按方法名 + 参数类型精确匹配")
        void bySignature()
        {
            Child child = new Child();
            String result = ReflectUtils.invokeMethod(child, "greet", new Class[] { String.class },
                    new Object[] { "world" });
            assertEquals("hello world", result);
        }

        @Test
        @DisplayName("私有方法也能调（无视修饰符）")
        void privateMethod()
        {
            assertEquals("secret", ReflectUtils.<String> invokeMethod(new Child(), "secret", new Class[] {},
                    new Object[] {}));
        }

        @Test
        @DisplayName("obj / methodName 为 null 时返回 null")
        void nullArgs()
        {
            assertNull(ReflectUtils.invokeMethod(null, "greet", new Class[] {}, new Object[] {}));
            assertNull(ReflectUtils.invokeMethod(new Child(), null, new Class[] {}, new Object[] {}));
        }

        @Test
        @DisplayName("签名对不上时返回 null")
        void noSuchMethod()
        {
            assertNull(ReflectUtils.invokeMethod(new Child(), "greet", new Class[] { Integer.class },
                    new Object[] { 1 }));
        }

        @Test
        @DisplayName("方法内部抛受检异常时包成 RuntimeException 上抛")
        void wrappedException()
        {
            RuntimeException e = assertThrows(RuntimeException.class,
                    () -> ReflectUtils.invokeMethod(new Child(), "boom", new Class[] {}, new Object[] {}));
            assertInstanceOf(IllegalStateException.class, e.getCause(), "原始异常必须留在 cause 里");
        }

        @Test
        @DisplayName("invokeMethodByName：同名方法按参数个数选")
        void byName()
        {
            Child child = new Child();
            assertEquals("one", ReflectUtils.<String> invokeMethodByName(child, "overload", new Object[] { "a" }));
            assertEquals("two", ReflectUtils.<String> invokeMethodByName(child, "overload", new Object[] { "a", "b" }));
        }

        @Test
        @DisplayName("invokeMethodByName：args 为 null 时按 0 个参数找（当前实现直接 NPE，属内部约定）")
        void byNameNullArgs()
        {
            assertThrows(NullPointerException.class,
                    () -> ReflectUtils.invokeMethodByName(new Child(), "overload", null),
                    "内部调用点都传数组，这里只记录现状");
        }
    }

    @Nested
    @DisplayName("invokeMethodByName 的隐式类型转换")
    class Coercion
    {
        @Test
        @DisplayName("Integer → String，且去掉 .0 尾巴")
        void toStr()
        {
            Child child = new Child();
            ReflectUtils.invokeMethodByName(child, "setText", new Object[] { 5 });
            assertEquals("5", child.getText());
            ReflectUtils.invokeMethodByName(child, "setText", new Object[] { 5.0D });
            assertEquals("5", child.getText(), "Convert.toStr(5.0) 得到 \"5.0\"，必须再截掉 .0");
        }

        @Test
        @DisplayName("String → Integer / Long / Double / Float 包装类型")
        void toWrapper()
        {
            Child child = new Child();
            ReflectUtils.invokeMethodByName(child, "setBoxed", new Object[] { "7" });
            assertEquals(7, child.getBoxed());
            ReflectUtils.invokeMethodByName(child, "setLongBoxed", new Object[] { "8" });
            assertEquals(8L, child.getLongBoxed());
            ReflectUtils.invokeMethodByName(child, "setDoubleBoxed", new Object[] { "1.5" });
            assertEquals(1.5D, child.getDoubleBoxed());
            ReflectUtils.invokeMethodByName(child, "setFloatBoxed", new Object[] { "2.5" });
            assertEquals(2.5F, child.getFloatBoxed());
        }

        @Test
        @DisplayName("String / Double → Date")
        void toDate()
        {
            Child child = new Child();
            ReflectUtils.invokeMethodByName(child, "setDate", new Object[] { "2026-11-07" });
            assertNotNull(child.getDate(), "字符串走 DateUtils.parseDate");
            assertEquals(2026, Integer.parseInt(new java.text.SimpleDateFormat("yyyy").format(child.getDate())));

            ReflectUtils.invokeMethodByName(child, "setDate", new Object[] { 46000.0D });
            assertNotNull(child.getDate(), "Double 走 Excel 的序列号换算");
        }

        @Test
        @DisplayName("Boolean / boolean 都走 Convert.toBool")
        void toBool()
        {
            Child child = new Child();
            ReflectUtils.invokeMethodByName(child, "setFlag", new Object[] { "true" });
            assertTrue(child.getFlag());
            ReflectUtils.invokeMethodByName(child, "setPrimitiveFlag", new Object[] { "1" });
            assertTrue(child.isPrimitiveFlag());
        }

        @Test
        @DisplayName("原始类型 int 不在转换白名单里，传 String 会失败（调用方必须自己转）")
        void primitiveIntNotCoerced()
        {
            // 转换分支只写了 Integer.class / Long.class / ... 这些包装类型，int.class 落到最后直接 invoke。
            assertThrows(RuntimeException.class,
                    () -> ReflectUtils.invokeMethodByName(new Child(), "setAge", new Object[] { "9" }),
                    "int 形参不会自动转 String，别指望它像 Integer 一样兜住");
        }
    }

    @Nested
    @DisplayName("Method / Field 查找")
    class Lookup
    {
        @Test
        @DisplayName("getAccessibleMethod 能拿到私有方法并已设为可访问")
        void accessibleMethod()
        {
            Method method = ReflectUtils.getAccessibleMethod(new Child(), "secret");
            assertNotNull(method);
            assertTrue(method.canAccess(new Child()) || method.isAccessible(), "必须已放开访问限制");
        }

        @Test
        @DisplayName("getAccessibleMethod 找不到 / 目标为 null 时返回 null")
        void accessibleMethodMissing()
        {
            assertNull(ReflectUtils.getAccessibleMethod(new Child(), "nope"));
            assertNull(ReflectUtils.getAccessibleMethod(null, "nope"));
            assertThrows(IllegalArgumentException.class, () -> ReflectUtils.getAccessibleMethod(new Child(), ""));
        }

        @Test
        @DisplayName("getAccessibleMethodByName 按名字 + 参数个数找")
        void accessibleMethodByName()
        {
            assertNotNull(ReflectUtils.getAccessibleMethodByName(new Child(), "overload", 1));
            assertNotNull(ReflectUtils.getAccessibleMethodByName(new Child(), "overload", 2));
            assertNull(ReflectUtils.getAccessibleMethodByName(new Child(), "overload", 3));
            assertNull(ReflectUtils.getAccessibleMethodByName(null, "overload", 1));
        }

        @Test
        @DisplayName("getAccessibleField 找到父类字段，找不到返回 null")
        void accessibleField()
        {
            Field field = ReflectUtils.getAccessibleField(new Child(), "parentField");
            assertNotNull(field);
            assertSame(Parent.class, field.getDeclaringClass());
            assertNull(ReflectUtils.getAccessibleField(new Child(), "nope"));
            assertNull(ReflectUtils.getAccessibleField(null, "name"));
        }

        @Test
        @DisplayName("makeAccessible 对 public 成员是幂等的，不抛异常")
        void makeAccessibleIdempotent()
        {
            Field field = ReflectUtils.getAccessibleField(new Child(), "name");
            ReflectUtils.makeAccessible(field);
            ReflectUtils.makeAccessible(field);
            Method method = ReflectUtils.getAccessibleMethod(new Child(), "getName");
            ReflectUtils.makeAccessible(method);
            ReflectUtils.makeAccessible(method);
        }
    }

    @Nested
    @DisplayName("泛型与真实类")
    class Generics
    {
        @Test
        @DisplayName("getClassGenricType：取父类声明的第一个泛型实参")
        void genericSuperclass()
        {
            assertEquals(Integer.class, ReflectUtils.getClassGenricType(IntHolder.class));
            assertEquals(String.class, ReflectUtils.getClassGenricType(StringHolder.class));
        }

        @Test
        @DisplayName("指定下标取第二 / 第三个泛型实参；越界返回 Object")
        void genericIndex()
        {
            assertEquals(Integer.class, ReflectUtils.getClassGenricType(TripleHolder.class, 0));
            assertEquals(String.class, ReflectUtils.getClassGenricType(TripleHolder.class, 1));
            assertEquals(Long.class, ReflectUtils.getClassGenricType(TripleHolder.class, 2));
            assertEquals(Object.class, ReflectUtils.getClassGenricType(TripleHolder.class, 3));
            assertEquals(Object.class, ReflectUtils.getClassGenricType(TripleHolder.class, -1));
        }

        @Test
        @DisplayName("父类不是泛型类型时返回 Object")
        void nonGenericSuperclass()
        {
            assertEquals(Object.class, ReflectUtils.getClassGenricType(Child.class), "Child 的父类没有泛型");
        }

        @Test
        @DisplayName("泛型实参仍是类型变量时返回 Object")
        void typeVariable()
        {
            assertEquals(Object.class, ReflectUtils.getClassGenricType(RawHolder.class),
                    "<T> 未被具体化时拿不到 Class");
        }

        @Test
        @DisplayName("getUserClass：普通对象返回自身，null 直接拒绝")
        void userClass()
        {
            Child child = new Child();
            assertSame(Child.class, ReflectUtils.getUserClass(child));
            assertThrows(RuntimeException.class, () -> ReflectUtils.getUserClass(null));
        }

        @Test
        @DisplayName("getUserClass：CGLIB 代理类要剥掉 $$ 后缀返回父类")
        void userClassOfProxy()
        {
            assertSame(Target.class, ReflectUtils.getUserClass(new Target$$ByCglib()),
                    "类名含 $$ 的代理必须还原成被代理类，否则 AOP 后取注解/泛型全部落空");
        }
    }

    @Nested
    @DisplayName("异常转换")
    class ExceptionConversion
    {
        @Test
        @DisplayName("IllegalAccess / IllegalArgument / NoSuchMethod 转 IllegalArgumentException")
        void toIllegalArgument()
        {
            String msg = "boom";
            assertInstanceOf(IllegalArgumentException.class,
                    ReflectUtils.convertReflectionExceptionToUnchecked(msg, new IllegalAccessException()));
            assertInstanceOf(IllegalArgumentException.class,
                    ReflectUtils.convertReflectionExceptionToUnchecked(msg, new IllegalArgumentException()));
            assertInstanceOf(IllegalArgumentException.class,
                    ReflectUtils.convertReflectionExceptionToUnchecked(msg, new NoSuchMethodException()));
        }

        @Test
        @DisplayName("InvocationTargetException 取出被调用方法抛的原始异常")
        void toRuntimeWithTarget()
        {
            IllegalStateException target = new IllegalStateException("inner");
            RuntimeException e = ReflectUtils.convertReflectionExceptionToUnchecked("outer",
                    new InvocationTargetException(target));
            assertSame(target, e.getCause());
            assertEquals("outer", e.getMessage());
        }

        @Test
        @DisplayName("其它异常统一包成 RuntimeException")
        void toRuntime()
        {
            RuntimeException e = ReflectUtils.convertReflectionExceptionToUnchecked("msg", new java.io.IOException("io"));
            assertInstanceOf(RuntimeException.class, e);
            assertInstanceOf(java.io.IOException.class, e.getCause());
            assertEquals("msg", e.getMessage());
        }
    }

    /** 父类：验证「向上转型找字段」。 */
    static class Parent
    {
        private String parentField = "p";

        public String getParentField()
        {
            return parentField;
        }
    }

    static class Child extends Parent
    {
        private String name = "c";

        private int age = 3;

        private String text;

        private Integer boxed;

        private Long longBoxed;

        private Double doubleBoxed;

        private Float floatBoxed;

        private Date date;

        private Boolean flag;

        private boolean primitiveFlag;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public int getAge()
        {
            return age;
        }

        public void setAge(int age)
        {
            this.age = age;
        }

        public String getText()
        {
            return text;
        }

        public void setText(String text)
        {
            this.text = text;
        }

        public Integer getBoxed()
        {
            return boxed;
        }

        public void setBoxed(Integer boxed)
        {
            this.boxed = boxed;
        }

        public Long getLongBoxed()
        {
            return longBoxed;
        }

        public void setLongBoxed(Long longBoxed)
        {
            this.longBoxed = longBoxed;
        }

        public Double getDoubleBoxed()
        {
            return doubleBoxed;
        }

        public void setDoubleBoxed(Double doubleBoxed)
        {
            this.doubleBoxed = doubleBoxed;
        }

        public Float getFloatBoxed()
        {
            return floatBoxed;
        }

        public void setFloatBoxed(Float floatBoxed)
        {
            this.floatBoxed = floatBoxed;
        }

        public Date getDate()
        {
            return date;
        }

        public void setDate(Date date)
        {
            this.date = date;
        }

        public Boolean getFlag()
        {
            return flag;
        }

        public void setFlag(Boolean flag)
        {
            this.flag = flag;
        }

        public boolean isPrimitiveFlag()
        {
            return primitiveFlag;
        }

        public void setPrimitiveFlag(boolean primitiveFlag)
        {
            this.primitiveFlag = primitiveFlag;
        }

        public String greet(String who)
        {
            return "hello " + who;
        }

        public String overload(String a)
        {
            return "one";
        }

        public String overload(String a, String b)
        {
            return "two";
        }

        public void boom()
        {
            throw new IllegalStateException("boom");
        }

        private String secret()
        {
            return "secret";
        }
    }

    static class Outer
    {
        private Child child = new Child();

        public Child getChild()
        {
            return child;
        }
    }

    static class Base<T>
    {
    }

    static class Base3<A, B, C>
    {
    }

    static class IntHolder extends Base<Integer>
    {
    }

    static class StringHolder extends Base<String>
    {
    }

    static class TripleHolder extends Base3<Integer, String, Long>
    {
    }

    static class RawHolder<T> extends Base<T>
    {
    }

    static class Target
    {
    }

    /** 类名故意带 $$，模拟 CGLIB 生成的子类。 */
    static class Target$$ByCglib extends Target
    {
    }
}
