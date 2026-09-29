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
RUN mvn -q -B -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 时区与运行目录
ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms512m -Xmx1024m"
RUN mkdir -p /app/logs /app/uploadPath
COPY --from=builder /build/build-temp/yu-admin/yu-admin.jar /app/app.jar

EXPOSE 8080

# 无 actuator：用 bash /dev/tcp 做 TCP 探活（temurin 镜像内含 bash）
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD bash -c '(echo > /dev/tcp/127.0.0.1/8080) 2>/dev/null' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
