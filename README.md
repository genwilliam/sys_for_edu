# 智慧教培校园教务系统

面向教培机构和校外辅导课程的教务管理系统。项目采用 monorepo 管理前后端：前端使用 Vue 3，后端使用 Java 和 Spring Boot。

## 产品目标

系统计划提供 PC 管理后台，用于管理学校与校区、教师、学生、班级、课程及教室，并支持排课、调班调课、考勤、订单、回访、报表和用户权限管理。排课能力的目标包括按教师、教室和课程等条件安排课程，处理合班、多班联排及资源冲突，并提供多维度查询和数据导出。

目前已实现管理界面原型和首期排课功能；产品目标中的其他教务模块仍在规划中，尚未全部实现。

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

- 前端：Vue 3 管理界面原型。排课页面已接入后端 API，支持周视图/列表、筛选、新增、编辑、取消和自动排课候选预览；其他业务页面目前仍使用 mock 数据。
- 后端：Spring Boot 提供健康检查及排课 API。排课数据通过 JDBC 持久化到 MySQL，Flyway 负责 schema 迁移；排课冲突按星期和时间区间检查教师、教室、班级资源。
- 自动排课：可按指定星期、时间范围、课时长度及每周课次数生成不冲突方案；不完整方案会明确标记，批量保存时再次校验冲突。
- 测试：后端集成测试使用 H2 的 MySQL 兼容模式执行 Flyway 迁移；前端支持 lint、类型检查和生产构建。
- 当前仍未实现：学校/校区、教师、学生、班级、课程和教室的持久化管理，考勤/订单/回访/报表业务，以及真实登录和角色/数据权限。排课资源暂以名称关联，不是外键。

数据库配置、API 请求格式和本地启动细节见 [backend/README.md](./backend/README.md)。本地 MySQL 启动前需创建 `sys_for_edu` 数据库，并可通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 设置连接信息。
