# 智慧教培校园教务系统

面向教培机构和校外辅导课程的教务管理系统。项目采用 monorepo 管理前后端：前端使用 Vue 3，后端使用 Java 和 Spring Boot。

## 产品目标

系统计划提供 PC 管理后台，用于管理学校与校区、教师、学生、班级、课程及教室，并支持排课、调班调课、考勤、订单、回访、报表和用户权限管理。排课能力的目标包括按教师、教室和课程等条件安排课程，处理合班、多班联排及资源冲突，并提供多维度查询和数据导出。

以上为产品规划。当前仓库处于基础框架阶段，尚未实现完整的 82 页交互原型或教务业务功能。

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

在仓库根目录安装前端及仓库工具依赖：

```sh
pnpm install
```

启动前端开发服务器：

```sh
pnpm dev
```

前端其他常用命令：

```sh
pnpm build
pnpm lint
pnpm type-check
```

启动后端（在仓库根目录执行）：

```sh
cd backend
./mvnw spring-boot:run
```

后端默认监听 `http://localhost:8080`。当前提供的健康检查接口为 `GET http://localhost:8080/api/health`，成功时返回：

```json
{
  "status": "UP"
}
```

运行后端测试：

```sh
pnpm backend:test
```

也可以直接在 `backend/` 目录运行 `./mvnw test`。

## 当前实现范围

- 前端：Vue 3/Vite 初始应用框架，教务管理页面和业务流程尚未实现。
- 后端：Spring Boot 启动工程和 `/api/health` 健康检查接口。
- 测试：包含健康检查接口的 Spring Boot 集成测试。
- 尚未配置数据库、身份认证、角色权限、业务 API 和前后端代理联调。
