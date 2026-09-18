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

/** Web端商城-菜单项表 */
@Data
@MpTable(value = "web_menu_items", comment = "Web端商城-菜单项表", indexes = {
		@MpIndex(name = "idx_menu_id", columns = {"menu_id"}),
		@MpIndex(name = "idx_company_id", columns = {"company_id"}),
		@MpIndex(name = "idx_parent_id", columns = {"parent_id"})
})
public class WebMenuItem {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "menu_id", columnType = "bigint", comment = "所属菜单id")
	private Long menuId;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司id")
	private Long companyId;

	@MpField(value = "parent_id", columnType = "bigint", comment = "父菜单项id，0=顶级", defaultValue = "0")
	private Long parentId = 0L;

	@MpField(value = "name", columnType = "string", length = 100, comment = "菜单项显示名称")
	private String name;

	@MpField(value = "image_url", columnType = "string", length = 500, nullable = true, comment = "菜单项图片")
	private String imageUrl;

	@MpField(value = "link_type", columnType = "string", length = 50, comment = "链接类型", defaultValue = "url")
	private String linkType = "url";

	@MpField(value = "link_value", columnType = "string", length = 500, nullable = true, comment = "关联目标值")
	private String linkValue;

	@MpField(value = "link_extra", columnType = "text", nullable = true, comment = "链接扩展信息")
	private String linkExtra;

	@MpField(value = "sort", columnType = "integer", comment = "排序", defaultValue = "0")
	private Integer sort = 0;

	@MpField(value = "status", columnType = "integer", comment = "1=启用 0=禁用", defaultValue = "1")
	private Integer status = 1;

	@MpField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@MpField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;
}
