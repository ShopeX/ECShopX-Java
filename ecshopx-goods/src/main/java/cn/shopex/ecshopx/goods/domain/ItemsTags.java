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

package cn.shopex.ecshopx.goods.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 商品标签库表
 */
@Data
@MpTable(value = "items_tags", comment = "商品标签库表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_tag_id", columns = {"tag_id"})})
public class ItemsTags {

	/** 标签id */
	@MpId(value = "tag_id", type = IdType.AUTO, columnType = "bigint", comment = "标签id")
	private Long tagId;

	/** 公司id */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司id")
	private Long companyId;

	/** 标签名称 */
	@MpField(value = "tag_name", columnType = "string", length = 50, comment = "标签名称")
	private String tagName;

	/** 标签颜色 */
	@MpField(value = "tag_color", columnType = "string", length = 50, comment = "标签颜色")
	private String tagColor = "#ff1939";

	/** 店铺ID */
	@MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "店铺ID", defaultValue = "0")
	private Long distributorId = 0L;

	/** 字体颜色 */
	@MpField(value = "font_color", columnType = "string", length = 50, comment = "字体颜色")
	private String fontColor = "#ffffff";

	/** 标签描述 */
	@MpField(value = "description", columnType = "string", length = 255, nullable = true, comment = "标签描述")
	private String description;

	/** 标签icon */
	@MpField(value = "tag_icon", columnType = "text", nullable = true, comment = "标签icon")
	private String tagIcon;

	/** 前台是否显示 0 否 1 是 */
	@MpField(value = "front_show", columnType = "smallint", nullable = true, comment = "前台是否显示 0 否 1 是", defaultValue = "0")
	private Integer frontShow = 0;

	@MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
	private Integer updated;
}
