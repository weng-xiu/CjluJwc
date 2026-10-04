package com.yu.sam.service;

/**
 * 学生学籍身份证号「列加密灰度迁移」服务（V4.1 §6.2 K1 合规②）。
 *
 * <p>目标：对存量明文身份证列实施静态加密，但采用非破坏性的灰度路线——
 * <b>新增独立密文列 {@code sam_student.id_card_cipher}，保留原明文列不动</b>，
 * 通过「双读 + 批量回填」逐步切换，任何阶段都可回滚，不影响既有查询链路。</p>
 *
 * <p>灰度开关 {@code data.encrypt.enabled}（默认 {@code false}）：未开启时本服务的所有
 * 方法都不会触碰密文列（存量库可能尚未应用 DDL，避免 SQL 引用不存在列而报错），
 * 读路径直接返回明文。开启后 {@link #backfill(int)} 才会增量写入密文列。</p>
 *
 * @author yu
 */
public interface ISamStudentCipherMigrationService
{
    /** 灰度开关是否开启（{@code data.encrypt.enabled}）。 */
    boolean isEnabled();

    /**
     * 增量回填：将「有明文身份证、密文列尚为空」的学籍行加密写入密文列（只写密文列，保留明文）。
     *
     * @param limit 单批处理行数上限
     * @return 本次实际回填的行数；灰度未开启时返回 0 且不执行任何 SQL
     */
    int backfill(int limit);

    /**
     * 双读身份证号：密文列优先，缺失或解密失败则回退明文列（灰度迁移期的读取契约）。
     *
     * @param studentId 学生ID
     * @return 明文身份证号；学生不存在时返回 {@code null}
     */
    String resolveIdCard(Long studentId);
}
