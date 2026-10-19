package com.yu.integration;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.yu.common.core.redis.RedisCache;

/**
 * G4 集成测试基类：真实容器上下文（Spring Security + MyBatis/Druid + Redis 全装配）。
 *
 * <p>与既有分层测试策略的分工：纯单测（Service mock）与 MockMvc standalone（控制器契约）
 * 继续跑默认套件；本基类下的用例走 @SpringBootTest 全量上下文 + 本地栈（MySQL 3306/yu-cjlujwc、
 * Redis 6379），为装配回归提供防护网（V5.0 §7.2 G4 / 债务 D11）。
 *
 * <p>隔离机制：@Tag("integration") + yu-admin surefire excludedGroups（默认 {@code mvn test} 不跑，
 * 未起本地栈也不会红）；显式执行：
 * <pre>mvn -o -pl yu-admin test -Dexcluded.groups= -Dgroups=integration</pre>
 *
 * <p>数据前置：testdata/apply.ps1 的 97xx 段可复位 fixture（轮次 9711 / 开课 9721-9723 /
 * 草稿成绩 9731 / 账号 admin、wxl）。各用例自带 @AfterEach 清理运行期增量。
 *
 * <p>MockMvc 装配：Spring Boot 4 把 @AutoConfigureMockMvc 拆到了新构件（本地离线仓库无此包），
 * 改用 spring-test 原生 webAppContextSetup + 手动挂接真实 springSecurityFilterChain，
 * 安全过滤器链（JWT 鉴权/登录端点 permitAll）仍是生产同一条，装配回归价值不打折。
 */
@SpringBootTest
@ActiveProfiles({ "dev", "it" })
@Tag("integration")
public abstract class AbstractIntegrationTest
{
    /** fixture 账号统一口令（bin/seed_p5_test_accounts.sql 口径） */
    protected static final String FIXTURE_PASSWORD = "admin123";

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected RedisCache redisCache;

    @Autowired
    protected DataSource dataSource;

    /** 带完整 Security 过滤器链的 MockMvc（每个用例重建，隔离请求状态） */
    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc()
    {
        FilterChainProxy securityFilterChain = context.getBean("springSecurityFilterChain", FilterChainProxy.class);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
    }

    /** 直连断言/清场用 JDBC（不走业务 Mapper，避免与被测装配耦合） */
    protected JdbcTemplate jdbc()
    {
        return new JdbcTemplate(dataSource);
    }
}
