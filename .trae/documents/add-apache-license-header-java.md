# 为 Java 源码批量添加 Apache License 2.0 头 — 实施规划

## 摘要

为 `ecshopx-java` 多模块项目中 **`src/main/java` 下的全部 `.java` 源文件**（共 **7609** 个，覆盖 50 个模块）在文件顶部统一插入与 PHP `ecshopx-api-open` 一致的 Apache License 2.0 版权头（`/** ... */` Javadoc 风格）。`src/test` 下的 918 个测试文件**不处理**，与 PHP 脚本（`license.sh` 仅遍历 `./src`、不遍历 `./tests`）对齐。

采用**一次性批量添加**（不引入 Maven 插件、不改动构建配置）。

## 现状分析

- 项目为 Maven 多模块（父 `pom.xml` 共 50 个 module），Java 源码位于各模块 `src/main/java/cn/shopex/ecshopx/{module}/...`。
- 统计结果（`find` 实测）：
  - `src/main/java/**/*.java`：**7609** 个
  - `src/test/**/*.java`：**918** 个
  - 其它位置 `.java`：**0** 个
  - `src/main/java/**/package-info.java`：**48** 个
- 当前 Java 源码**没有任何版权头**：全仓仅 `docker/tables_xxl_job.sql` 一个文件含 `Copyright` 字样（与源码无关）；`Copyright 2019-2026 ShopeX` 在 Java 项目中 **0 命中**。
- 父 `pom.xml` 未配置 license / spotless / checkstyle 等任何头校验插件（见 [pom.xml](file:///Library/WebServer/Documents/work/git/xecshop/platform/ecshopx-java/pom.xml#L425-L435)）。
- PHP 侧参考（`../ecshopx-api-open`）：
  - `src/` 3499 个 `.php` 中 3244 个已带新头；少数 legacy/第三方 bundle（ShuyunOpenPlatformBundle、KujialeBundle、ThirdPartyBundle 等）未加。
  - `tests/` 187 个 `.php` 中仅 11 个带新头（均为新建/新迁移的测试）。
  - 根目录脚本 `license.sh` 的 PHP 处理段仅遍历 `./src`（**不遍历 `./tests`**），且其 `str_replace($newCopyright, $oldCopyright, ...)` 实为把 Apache 头**回退**为旧版权头（Apache→`Copyright © ShopeX ... See LICENSE`），即该脚本是"商用 license 分支回退"脚本，**不能直接复用于"添加 Apache 头"这一方向**。此处仅借用其"只处理 `src`、不处理 `tests`"的范围约定与头部文本格式。

## 变更方案

### 1. 头部内容（精确文本，URL 不带反引号）

插入到每个目标文件**最顶部**的版权头如下（与 PHP 文件逐字一致）：

```java
/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

### 2. 目标文件范围

- **包含**：`ecshopx-*/src/main/java/**/*.java`，共 7609 个（所有 50 个模块）。
- **排除**：
  - `src/test/**/*.java`（918 个，与 PHP 脚本不处理 `tests` 对齐）。
  - `pom.xml`、`.yml`、`.properties`、`.xml`（mapper）等资源/配置文件。
- **幂等**：已包含 `Copyright 2019-2026 ShopeX` 的文件跳过，不重复插入。

### 3. 插入位置与排版

- 版权头置于文件**第 1 行**。
- 版权头 `*/` 之后保留**一个空行**，再接原文件内容。
- 普通 `.java` 文件结果：

```java
/**
 * Copyright 2019-2026 ShopeX
 * ...
 */

package cn.shopex.ecshopx.salesperson.api.admin.v1;

import ...
```

- `package-info.java`（48 个）保持原有包文档注释，版权头置于其上方（中间空一行）：

```java
/**
 * Copyright 2019-2026 ShopeX
 * ...
 */

/**
 * 业务员/导购域模块。
 */
package cn.shopex.ecshopx.salesperson;
```

### 4. 执行方式（一次性批量）

执行阶段用一个一次性脚本完成（**不落地为常驻文件、不改 `pom.xml`**），逻辑为：

1. 递归遍历 `**/src/main/java/**/*.java`（按模块 `find` 汇总）。
2. 若文件内容已含 `Copyright 2019-2026 ShopeX` 则跳过。
3. 否则把上述版权头 + 一个空行**前置**到原文件内容之前。
4. 保持 UTF-8 编码、不引入 BOM，不修改原文件其它内容。
5. 记录处理/跳过数量，处理完输出统计。

> 实现将采用 `bash`（`find` + 临时头部文件 + `cat`/`mv` 或等价的 `sed` 前置），确保 7609 个文件的处理幂等、可重跑、可 review（配合 `--dry-run` 预览）。

## 假设与决策

- **测试文件不加头**：与 PHP `license.sh` 只处理 `src`、不处理 `tests` 对齐（用户确认对齐 PHP）。
- **注释风格用 `/** ... */`**：与 PHP 逐字一致（用户确认）。`package-info.java` 因头部为 Javadoc 风格，会出现"版权头 Javadoc + 包文档 Javadoc"两个连续 `/** */`，编译器不受影响（注释不参与编译）；javadoc 工具只会把紧邻 `package` 声明的最后一个注释视为包文档，版权头视为游离注释，属可接受范围。
- **URL 不带反引号**：用户消息中的 `` `http://...` `` 反引号为 Markdown 转义，实际以 PHP 源文件与 `license.sh` 中无反引号的 `http://www.apache.org/licenses/LICENSE-2.0` 为准。
- **不新建 LICENSE / NOTICE 文件**、不引入构建插件：本次仅做文件头插入。
- **范围仅限 Java 源码**：不含配置文件、资源文件、SQL、pom。

## 验证步骤

1. **数量核对**：`grep -rl 'Copyright 2019-2026 ShopeX' --include='*.java' . | wc -l` 应等于 **7609**（全部命中 `src/main/java`，且 `src/test` 内为 0）。
2. **范围核对**：确认 `src/test` 下无新增头：`grep -rl 'Copyright 2019-2026 ShopeX' --include='*.java' ecshopx-*/src/test | wc -l` 应为 **0**。
3. **抽样检查**：抽查一个 Controller（如 `ecshopx-salesperson/.../SalespersonTaskController.java`）、一个 Service、一个 domain、一个 Mapper、一个 `package-info.java`，确认头部位于第 1 行、空行排版正确、原内容未变。
4. **编译自检（可选）**：`./mvnw -q -DskipTests compile`（或至少对受影响模块 `-pl` 编译）确认注释插入未破坏语法（注释不影响编译，主要验证 `package-info.java` 未产生异常）。
5. **变更审视**：`git diff --stat` 与 `git diff <抽样文件>` 确认仅文件头新增、无其它改动。
