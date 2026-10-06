package com.yu.tpm.schedule.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * N4 约束 DSL 解析器：把声明式约束文本解析为 {@link ScheduleConstraintPolicy}。
 *
 * <p>DSL 语法（大小写不敏感、以 ';' 或换行分隔）：</p>
 * <pre>
 *   hard.forbidden=true; hard.maxSessionsPerDay=2;
 *   soft.labMatch=120; soft.sameCampus=50; soft.capacityFitDivisor=10;
 *   pref.dailyBalance=true
 * </pre>
 * <p>空串/仅空白解析为默认策略；未知键、非法布尔/数字、越界数字均收集为校验错误，
 * {@link #parse(String)} 遇错抛 {@link ConstraintDslException}，{@link #validate(String)}
 * 返回错误列表（空表示合法）。设计为纯静态、无副作用，便于单元测试。</p>
 *
 * @author N4
 */
public final class ConstraintDslParser
{
    private ConstraintDslParser() { }

    /** 解析 DSL 文本为策略；非法输入抛出异常（携带全部校验错误）。 */
    public static ScheduleConstraintPolicy parse(String dsl)
    {
        List<String> errors = new ArrayList<>();
        ScheduleConstraintPolicy policy = doParse(dsl, errors);
        if (!errors.isEmpty())
        {
            throw new ConstraintDslException(errors);
        }
        return policy;
    }

    /** 仅校验 DSL，返回错误列表（空即合法），不抛异常。 */
    public static List<String> validate(String dsl)
    {
        List<String> errors = new ArrayList<>();
        doParse(dsl, errors);
        return errors;
    }

    private static ScheduleConstraintPolicy doParse(String dsl, List<String> errors)
    {
        ScheduleConstraintPolicy p = ScheduleConstraintPolicy.defaults();
        if (dsl == null || dsl.trim().isEmpty())
        {
            return p;
        }
        String[] tokens = dsl.split("[;\\r\\n]+");
        for (String raw : tokens)
        {
            String token = raw.trim();
            if (token.isEmpty()) { continue; }
            int eq = token.indexOf('=');
            if (eq < 0)
            {
                errors.add("缺少 '=' 的分段: " + token);
                continue;
            }
            String key = token.substring(0, eq).trim().toLowerCase();
            String val = token.substring(eq + 1).trim();
            apply(p, key, val, errors);
        }
        return p;
    }

    private static void apply(ScheduleConstraintPolicy p, String key, String val, List<String> errors)
    {
        switch (key)
        {
            case "hard.forbidden":
                p.setHardForbidden(bool(val, key, errors));
                break;
            case "hard.samecampus":
                p.setHardSameCampus(bool(val, key, errors));
                break;
            case "hard.maxsessionsperday":
            {
                int v = intVal(val, key, errors);
                if (v < 0) { errors.add(key + " 不能为负: " + val); }
                else { p.setHardMaxSessionsPerDay(v); }
                break;
            }
            case "soft.labmatch":
                p.setSoftLabMatch(intVal(val, key, errors));
                break;
            case "soft.labmismatch":
                p.setSoftLabMismatch(intVal(val, key, errors));
                break;
            case "soft.regularroom":
                p.setSoftRegularRoom(intVal(val, key, errors));
                break;
            case "soft.regularinlab":
                p.setSoftRegularInLab(intVal(val, key, errors));
                break;
            case "soft.samecampus":
                p.setSoftSameCampus(intVal(val, key, errors));
                break;
            case "soft.samebuilding":
                p.setSoftSameBuilding(intVal(val, key, errors));
                break;
            case "soft.capacityfitdivisor":
            {
                int v = intVal(val, key, errors);
                if (v < 1) { errors.add(key + " 必须 >= 1: " + val); }
                else { p.setCapacityFitDivisor(v); }
                break;
            }
            case "pref.dailybalance":
                p.setDailyBalance(bool(val, key, errors));
                break;
            default:
                errors.add("未知约束键: " + key);
        }
    }

    private static boolean bool(String val, String key, List<String> errors)
    {
        String v = val.toLowerCase();
        if ("true".equals(v) || "1".equals(v) || "yes".equals(v) || "on".equals(v)) { return true; }
        if ("false".equals(v) || "0".equals(v) || "no".equals(v) || "off".equals(v)) { return false; }
        errors.add(key + " 需要布尔值(true/false): " + val);
        return false;
    }

    private static int intVal(String val, String key, List<String> errors)
    {
        try
        {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e)
        {
            errors.add(key + " 需要整数: " + val);
            return 0;
        }
    }
}
