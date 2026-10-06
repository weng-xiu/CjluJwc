package com.yu.tpm.schedule.dsl;

import java.util.Collections;
import java.util.List;

/**
 * N4 约束 DSL 解析异常：聚合全部校验错误，供控制器回传给前端逐条修正。
 *
 * @author N4
 */
public class ConstraintDslException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final List<String> errors;

    public ConstraintDslException(List<String> errors)
    {
        super("约束 DSL 校验失败: " + String.join("; ", errors));
        this.errors = errors == null ? Collections.emptyList() : Collections.unmodifiableList(errors);
    }

    public List<String> getErrors()
    {
        return errors;
    }
}
