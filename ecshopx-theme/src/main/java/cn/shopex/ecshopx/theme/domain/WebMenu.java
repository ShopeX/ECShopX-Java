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

package cn.shopex.ecshopx.theme.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/** Web端商城-菜单主表 */
@Data
@MpTable(value = "web_menus", comment = "Web端商城-菜单主表", uniqueIndexes = {
		@MpIndex(name = "uk_company_key", columns = {"company_id", "key"})
})
public class WebMenu {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司id")
	private Long companyId;

	@MpField(value = "name", columnType = "string", length = 100, comment = "菜单名称")
	private String name;

	@MpField(value = "`key`", columnType = "string", length = 100, comment = "菜单标识符")
	private String key;

	@MpField(value = "status", columnType = "integer", comment = "1=启用 0=禁用", defaultValue = "1")
	private Integer status = 1;

	@MpField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@MpField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;
}
