# syntax=docker/dockerfile:1
# CjluJwc 后端镜像：多阶段构建（Maven 编译 → JRE 运行）
# 注意：父 pom 将 <directory> 定制为 ${project.basedir}/../build-temp/${artifactId}，
#       故 fat jar 产物在 build-temp/yu-admin/yu-admin.jar（非 yu-admin/target）。

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先拷贝全部 pom 利用层缓存解析依赖（13 个模块）
COPY pom.xml ./
COPY yu-admin/pom.xml          yu-admin/
COPY yu-aem/pom.xml            yu-aem/
COPY yu-brm/pom.xml            yu-brm/
COPY yu-common/pom.xml         yu-common/
COPY yu-dis/pom.xml            yu-dis/
COPY yu-framework/pom.xml      yu-framework/
COPY yu-generator/pom.xml      yu-generator/
COPY yu-oa/pom.xml             yu-oa/
COPY yu-portal/pom.xml         yu-portal/
COPY yu-quartz/pom.xml         yu-quartz/
COPY yu-sam/pom.xml            yu-sam/
COPY yu-system/pom.xml         yu-system/
COPY yu-tpm/pom.xml            yu-tpm/
RUN mvn -q -B dependency:go-offline || true

# 再拷贝源码并打包（finalName=yu-admin，输出 build-temp/yu-admin/yu-admin.jar）
COPY . .
# 构建阶段仅用于容器镜像：跳过测试（测试由 CI 门禁执行）
RUN mvn -q -B -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 安全基线（V4.0 §7.3）：以非 root 运行；JVM 按压测结果调参时仅需覆盖 JAVA_OPTS
ENV TZ=Asia/Shanghai \
    # JDK17 默认感知 cgroup 内存限制，配合 compose 的 mem_limit 自动收敛堆大小，不再写死 -Xmx
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
RUN groupadd --system --gid 1001 spring && useradd --system --uid 1001 --gid spring --home /app --shell /usr/sbin/nologin spring \
    && mkdir -p /app/logs /app/uploadPath \
    && chown -R spring:spring /app
COPY --from=builder --chown=spring:spring /build/build-temp/yu-admin/yu-admin.jar /app/app.jar

USER spring:spring
EXPOSE 8080

# actuator 健康检查（V4.0 A5）：/actuator/health 已在 SecurityConfig 匿名放行，无需再靠 TCP 探活
# 镜像不含 curl，沿用 JDK 自带 bash /dev/tcp 做端口级探活，业务存活由 compose 的 healthcheck 覆盖
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD bash -c '(echo > /dev/tcp/127.0.0.1/8080) 2>/dev/null' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
