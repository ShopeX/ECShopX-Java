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

package cn.shopex.ecshopx.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 审计字段自动填充。
 * <p>
 * 同时兼容两种命名模式：
 * <ul>
 *   <li>{@code createTime / updateTime}（Integer，Unix 时间戳）</li>
 *   <li>{@code createdAt / updatedAt}（LocalDateTime）</li>
 * </ul>
 * 只有当实体字段标注了 {@code @TableField(fill = FieldFill.INSERT)} 等才会触发填充。
 */
@Component
public class MybatisPlusMetaHandler implements MetaObjectHandler {

	@Override
	public void insertFill(MetaObject metaObject) {
		int now = (int) (System.currentTimeMillis() / 1000);
		this.strictInsertFill(metaObject, "createTime", Integer.class, now);
		this.strictInsertFill(metaObject, "updateTime", Integer.class, now);
		this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
		this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
	}

	@Override
	public void updateFill(MetaObject metaObject) {
		int now = (int) (System.currentTimeMillis() / 1000);
		this.strictUpdateFill(metaObject, "updateTime", Integer.class, now);
		this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
	}
}
