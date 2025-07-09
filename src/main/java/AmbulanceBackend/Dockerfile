# 选择 OpenJDK 基础镜像
FROM openjdk:21-jdk-slim

# 设置工作目录
WORKDIR /app

# 拷贝 Maven 构建好的 jar 包（或者复制 pom.xml 和 src 后构建）
COPY target/JavaWebLearning-0.0.1-SNAPSHOT.jar app.jar

# 开放端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
