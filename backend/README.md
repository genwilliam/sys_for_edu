## 数据库配置（Database setup）

后端使用 Spring JDBC 和 Flyway 进行数据库迁移。生产环境默认使用本地 MySQL 数据库：

```
URL:      jdbc:mysql://localhost:3306/sys_for_edu?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
用户名:    root
密码:      （空）
```

启动后端之前，需要先创建 `sys_for_edu` 数据库；也可以通过以下环境变量覆盖数据库连接配置：

- `DB_URL`：数据库连接地址
- `DB_USERNAME`：数据库用户名
- `DB_PASSWORD`：数据库密码

应用启动时，Flyway 会自动执行位于 `src/main/resources/db/migration` 目录下的数据库迁移脚本。

测试环境使用 `test` 配置文件，采用启用了 MySQL 兼容模式的内存数据库 H2，并执行相同的 Flyway 数据库迁移脚本。

## 排课 API（Scheduling API）

所有时间均使用本地时间格式 `HH:mm`。

`weekday`（星期）只能取以下值：

- `周一` 至 `周日`

`type`（课程类型）只能取以下值：

- `常规课`
- `试听课`
- `补课`

`status`（课程状态）只能取以下值：

- `待上课`
- `已上课`
- `已取消`

创建单条排课记录或批量创建时，如果未指定 `status`，默认值为 `待上课`。

### API 接口列表

| 请求方法 | 接口路径                          | 功能说明                                                                                                                                           |
| -------- | --------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| `GET`    | `/api/schedule-items`             | 获取排课列表；支持以下精确匹配筛选条件：`campus`（校区）、`course`（课程）、`teacher`（教师）、`room`（教室）、`weekday`（星期）、`status`（状态） |
| `POST`   | `/api/schedule-items`             | 创建一条排课记录；成功时返回 HTTP `201`                                                                                                            |
| `PUT`    | `/api/schedule-items/{id}`        | 完整替换指定排课记录                                                                                                                               |
| `PATCH`  | `/api/schedule-items/{id}/cancel` | 取消指定排课记录                                                                                                                                   |
| `POST`   | `/api/schedule-items/batch`       | 原子性批量创建排课记录，请求体格式为 `{ "items": [...] }`；成功时返回 HTTP `201`                                                                   |
| `POST`   | `/api/schedule-items/auto-plan`   | 生成候选排课草案，但不会直接保存                                                                                                                   |

### 排课记录的数据结构

排课记录响应包含以下字段：

- `id`：记录 ID
- `course`：课程名称
- `className`：班级名称
- `teacher`：教师
- `room`：教室
- `campus`：校区
- `weekday`：星期
- `startTime`：开始时间
- `endTime`：结束时间
- `type`：课程类型
- `status`：课程状态

创建单条排课记录和批量创建时，请求体使用相同的字段，但不包含 `id`。其中 `status` 为可选字段。

### 自动排课（Auto-plan）

自动排课接口接收以下参数：

- `course`：课程名称
- `className`：班级名称
- `teacher`：教师
- `room`：教室
- `campus`：校区
- `weeklySessions`：每周上课次数
- `durationMinutes`：每节课时长（分钟）
- `weekdays`：允许安排上课的星期
- `startTime`：可安排的最早开始时间
- `endTime`：可安排的最晚结束时间
- `type`：课程类型

接口响应格式固定为：

```
{
  "items": [],
  "complete": true,
  "message": "...",
  "explanation": "..."
}
```

字段说明：

- `items`：生成的排课草案列表，不包含 `id`
- `complete`：是否成功生成完整排课方案
- `message`：结果摘要
- `explanation`：方案说明

完整排课方案的 `message` 格式为 `找到 N/M 节`，其中 N 表示实际找到的课程节数，M 表示目标课程节数。

如果无法生成完整方案：

- `complete` 设置为 `false`
- `items` 中仍可以包含当前已找到的排课草案
- 必须说明这些草案不能保存

只有当 `complete` 为 `true` 时，才能将完整方案提交到批量创建接口 `/api/schedule-items/batch` 进行保存。

批量创建操作会再次检查时间冲突，确保数据一致性。

### 排课冲突规则

当同一星期内的两个时间段发生重叠，并且满足以下任意一项条件时，即判定为排课冲突：

- 教师相同
- 教室相同
- 班级名称相同

补充规则：

1. 校区不同不会影响冲突判断。
2. 已取消的排课记录不再占用教师、教室或班级资源。
3. 时间段重叠时，采用上述规则进行冲突检测。

### HTTP 错误响应

| HTTP 状态码 | 含义             |
| ----------- | ---------------- |
| `409`       | 排课冲突         |
| `400`       | 请求参数验证失败 |

参数验证失败时，响应格式为：

```
{
  "status": 400,
  "error": "...",
  "message": "..."
}
```

补充可直接运行的请求示例明确 PUT 与 PATCH 的请求体
