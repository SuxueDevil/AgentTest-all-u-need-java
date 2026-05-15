# 后端

Spring Boot 3 + MyBatis-Plus + MySQL 8.0

## 本地开发

```bash
# 1. 建库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS agent_aun"
mysql -u root -p agent_aun < aun-web/src/main/resources/init.sql

# 2. 启动
mvn clean package -DskipTests
cd aun-web
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端启动在 http://localhost:8080。

## 环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DB_URL` | 数据库地址 | `jdbc:mysql://localhost:3306/agent_aun?...` |
| `DB_USERNAME` | 用户名 | `root` |
| `DB_PASSWORD` | 密码 | 空（必填） |
