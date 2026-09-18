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
 * 商品分组
 */
@Data
@MpTable(value = "items_group", comment = "商品分组", indexes = {@MpIndex(name = "idx_group_key", columns = {"group_key"})})
public class ItemsGroup {

	/** ID */
	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
	private Long id;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID", defaultValue = "0")
	private Long companyId = 0L;

	/** 地区ID */
	@MpField(value = "regionauth_id", columnType = "bigint", comment = "地区ID", defaultValue = "0")
	private Long regionauthId = 0L;

	/** 分组唯一码 */
	@MpField(value = "group_key", columnType = "string", length = 50, comment = "分组唯一码")
	private String groupKey = "";

	/** 备注 */
	@MpField(value = "remark", columnType = "string", length = 100, comment = "备注")
	private String remark = "";

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
