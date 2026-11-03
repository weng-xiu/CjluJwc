package com.yu.common.utils.file;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yu.common.constant.CacheConstants;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.spring.SpringUtils;

/**
 * 导出件归属登记（S-3）。
 *
 * <p>背景：{@code /common/download} 读取的是全局共享的下载目录（{@code ruoyi.profile}/download），
 * 文件名由服务器生成后就脱离了用户身份，任何已登录用户只要知道或猜到文件名即可下载他人导出件。
 * 原先只有「文件名扩展名白名单 + 禁止目录上跳」，拦得住路径穿越，拦不住横向越权。</p>
 *
 * <p>做法：导出落盘时登记 {文件名 → 归属用户}（Redis，带 TTL），下载时校验归属；
 * 查不到登记、登记已过期、或归属人不匹配时一律<b>拒绝</b>（fail-closed），仅超管放行。
 * 之所以不落库：文件本身是临时产物，登记只需活到「导出→下载」这一段，用 Redis TTL 自动清理，
 * 避免为临时文件引入一张需要定期归档的表。</p>
 *
 * <p>注意：这里选择的是「归属登记」而非「一次性下载 token」。导出件被浏览器重试、被多标签页
 * 重复打开都属正常，一次性 token 会把这些正常场景误伤成失败；归属 + 2 小时 TTL 已足够收窄猜名窗口。</p>
 *
 * @author yu
 */
public class DownloadOwnershipRegistry
{
    private static final Logger log = LoggerFactory.getLogger(DownloadOwnershipRegistry.class);

    /**
     * 无法识别登录用户时（定时任务、异步线程等无请求上下文的导出）登记的归属标识。
     * 这类文件没有任何普通账号可下载，只有超管能取，避免异步产物成为公共资源。
     */
    public static final String SYSTEM_OWNER = "system";

    private DownloadOwnershipRegistry()
    {
    }

    /**
     * 登记文件归属。由 ExcelUtil 的落盘漏斗调用，不依赖调用方显式传用户。
     *
     * @param fileName 下载目录内的文件名（不含路径）
     */
    public static void register(String fileName)
    {
        if (StringUtils.isEmpty(fileName))
        {
            return;
        }
        Long userId = currentUserId();
        String owner = userId == null ? SYSTEM_OWNER : String.valueOf(userId);
        try
        {
            redisCache().setCacheObject(CacheConstants.DOWNLOAD_OWNER_KEY + fileName, owner,
                    CacheConstants.DOWNLOAD_OWNER_EXPIRATION, TimeUnit.MINUTES);
        }
        catch (Exception e)
        {
            // 登记失败不阻断导出（导出已经发生，在这里抛异常只会让用户拿不到文件），
            // 但必须留痕：登记缺失意味着该文件之后只有超管可下载
            log.warn("导出件归属登记失败，该文件将只有超管可下载：fileName={}, owner={}, 原因={}",
                    fileName, owner, e.getMessage());
        }
    }

    /**
     * 判断当前用户是否为该文件的归属人。
     *
     * <p>任何异常（Redis 不可用、无 Spring 上下文）都返回 false——下载鉴权出错时宁可拒绝，
     * 不能因为基础设施抖动把越权下载放行。
     *
     * @param fileName 下载目录内的文件名（不含路径）
     * @param userId   当前用户ID；超管由调用方先行放行，不必传 1
     * @return true 表示可下载
     */
    public static boolean isOwner(String fileName, Long userId)
    {
        if (StringUtils.isEmpty(fileName) || userId == null)
        {
            return false;
        }
        String owner;
        try
        {
            owner = redisCache().getCacheObject(CacheConstants.DOWNLOAD_OWNER_KEY + fileName);
        }
        catch (Exception e)
        {
            log.warn("导出件归属查询失败，按无权下载处理：fileName={}, 原因={}", fileName, e.getMessage());
            return false;
        }
        if (StringUtils.isEmpty(owner))
        {
            // 未登记（存量文件 / 登记已过期）：按无归属处理
            return false;
        }
        return owner.equals(String.valueOf(userId));
    }

    /** 取当前登录用户ID；无请求上下文时返回 null，由调用方按系统件/拒绝处理。 */
    private static Long currentUserId()
    {
        try
        {
            return SecurityUtils.getUserId();
        }
        catch (Exception e)
        {
            log.debug("导出发生在无登录上下文的线程，按系统件登记：{}", e.getMessage());
            return null;
        }
    }

    private static RedisCache redisCache()
    {
        return SpringUtils.getBean(RedisCache.class);
    }
}
