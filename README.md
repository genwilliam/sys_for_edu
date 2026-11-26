# 智慧教培校园教务系统

面向教培机构和校外辅导课程的教务管理系统。项目采用 monorepo 管理前后端：前端使用 Vue 3，后端使用 Java 和 Spring Boot。

## 产品目标

系统计划提供 PC 管理后台，用于管理学校与校区、教师、学生、班级、课程及教室，并支持排课、调班调课、考勤、订单、回访、报表和用户权限管理。排课能力的目标包括按教师、教室和课程等条件安排课程，处理合班、多班联排及资源冲突，并提供多维度查询和数据导出。

目前前端管理界面已将核心业务记录的读取、新增、编辑和删除接入 Spring Boot API；业务数据由 MySQL 持久化。登录认证、角色权限校验和请假审批由后端执行，教师与学生只能访问其本人或相关班级的数据。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、pnpm
- 后端：Java 17、Spring Boot 3.5.3、Maven
- 仓库：pnpm workspace 管理前端包；后端通过 Maven Wrapper 独立构建

## 目录结构

```text
.
├── backend/                 # Spring Boot 后端
│   ├── .mvn/wrapper/        # Maven Wrapper 配置
│   ├── src/main/java/       # 后端源代码
│   ├── src/test/java/       # 后端测试
│   ├── mvnw                 # macOS/Linux Maven Wrapper
│   └── pom.xml
├── frontend/                # Vue 3 前端
├── package.json             # monorepo 常用命令
└── pnpm-workspace.yaml
```

## 环境要求

- Node.js `^22.18.0` 或 `>=24.12.0`
- pnpm
- JDK 17 或更高版本

后端使用 Maven Wrapper，无需单独安装 Maven。首次运行时 Wrapper 会下载 Maven 发行版，Maven 也会下载项目依赖，因此需要网络访问 Maven Central。

## 本地开发

### 1. 安装环境

- Node.js `^22.18.0` 或 `>=24.12.0` 和 pnpm
- JDK 17 或更高版本
- MySQL（本地开发默认连接 `localhost:3306`）

后端使用 Maven Wrapper，无需单独安装 Maven。首次运行时会下载 Maven 及项目依赖，因此需要网络访问 Maven Central。

### 2. 安装前端依赖

在仓库根目录执行：

```sh
pnpm install
```

### 3. 创建 MySQL 数据库

启动 MySQL 服务，然后创建项目数据库：

```sql
CREATE DATABASE sys_for_edu
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

后端默认使用 `root` 用户和空密码。若本机的 MySQL 用户名或密码不同，请在启动后端前设置连接环境变量：

```sh
export DB_URL='jdbc:mysql://localhost:3306/sys_for_edu?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export DB_USERNAME='你的 MySQL 用户名'
export DB_PASSWORD='你的 MySQL 密码'
```

Windows PowerShell 可使用 `$env:DB_USERNAME="..."` 和 `$env:DB_PASSWORD="..."` 设置环境变量。应用启动时 Flyway 会自动创建所需的数据表。

### 4. 启动后端

在仓库根目录新开一个终端执行：

```sh
./backend/mvnw -f backend/pom.xml spring-boot:run
```

后端默认监听 `http://localhost:8080`。健康检查接口为 `GET http://localhost:8080/api/health`。也可以在 `backend/` 目录执行 `./mvnw spring-boot:run`。

### 5. 启动前端

再新开一个终端，在仓库根目录执行：

```sh
pnpm dev
```

按 Vite 输出的地址打开前端页面（通常是 `http://localhost:5173`）。前端开发服务器会将 `/api` 请求代理到 `http://localhost:8080`，因此本地联调时需要保持前后端都在运行。

### 常用命令

在仓库根目录执行：

```sh
pnpm build
pnpm lint
pnpm type-check
pnpm backend:test
```

也可以直接在 `backend/` 目录运行后端测试：

```sh
./mvnw test
```

测试使用 H2 内存数据库，不需要连接本地 MySQL。

健康检查成功时返回：

```json
{
  "status": "UP"
}
```

## 当前实现范围

- 前端：Vue 3 页面从后端加载学校/校区、教师、学生、班级、课程、教室、考勤、订单、回访、用户、角色及班级成员数据；新增、编辑、删除均调用后端 API。排课页面另使用排课 API，支持筛选、冲突校验、批量与自动排课。
- 后端：Spring Boot 提供业务数据 CRUD API 与排课 API。一般业务记录存于 MySQL 的 `business_records` 表，按业务域保存 JSON 记录；排课及学生关联使用独立表。Flyway 管理 schema，示例数据包含约 10 名教师、185 名学生，6 个班级各有 30 名成员；已有数据库通过一次性升级保留并补齐示例成员、添加缺少的示例排课，示例班级容量调整为至少 32 人。
- 班级/学生：班级成员关系持久化在 `class-memberships` 业务域；班级详情读取后端成员关系和排课数据。成员变更会在后端校验班级容量与学生课表冲突，并同步班级排课的学生名单；移出学生前会要求二次确认。
- 自动排课：可按指定星期、时间范围、课时长度及每周课次数生成不冲突方案；不完整方案会明确标记，批量保存时再次校验冲突。
- 测试：后端集成测试使用 H2 的 MySQL 兼容模式执行 Flyway 迁移；前端支持 lint、类型检查和生产构建。
- 账号权限：管理员可访问管理页面和写入业务数据；教师和学生只能读取与本人关联的档案/课表，不能访问管理写接口。教师工作台展示本人课表、任教班级和相关学生；学生工作台展示本人课表及班级信息。学生可提交本人请假，任课教师可审批所教班级学生的请假，教师请假由管理员审批。教师注册需管理员审核，学生注册即时启用；注册以教师号/学号及档案手机号匹配。账号密码以 PBKDF2 哈希保存，会话令牌仅以 SHA-256 哈希存库。
- 本地演示账号：admin / `Admin@123`、teacher / `Teacher@123`、student / `Student@123`；演示账号登录后无需修改密码。演示账号仅用于本地开发，部署到公网前须配置真实账号并更换所有默认凭据。登录页明示的演示密码不适用于生产环境。
- 管理员还能在“系统管理 → 账号审核与请假”审批教师账号、管理账号启用状态及审批教师请假。一般业务域仍以 JSON 记录存储，尚未拆分为字段级关系表和外键。

数据库配置、API 请求格式和本地启动细节见 [backend/README.md](./backend/README.md)。本地 MySQL 启动前需创建 `sys_for_edu` 数据库，并可通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 设置连接信息。
