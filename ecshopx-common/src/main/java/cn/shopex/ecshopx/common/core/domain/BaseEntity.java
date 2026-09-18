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

package cn.shopex.ecshopx.common.core.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.io.Serializable;

/**
 * 实体基类，提供审计字段自动填充。
 * <p>
 * 适用于使用 {@code create_time / update_time}（Unix 时间戳 Integer）的表。
 * 使用 {@code created_at / updated_at}（LocalDateTime）的表可不继承此类，
 * MetaObjectHandler 仍会尝试自动填充。
 */
@Data
public abstract class BaseEntity implements Serializable {

	@MpField(value = "create_time", fill = FieldFill.INSERT)
	private Integer createTime;

	@MpField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
	private Integer updateTime;
}
