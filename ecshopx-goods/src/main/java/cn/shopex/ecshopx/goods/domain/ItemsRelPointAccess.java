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
 * 商品和积分获取设置关联表
 */
@Data
@MpTable(value = "items_rel_point_access", comment = "商品和积分获取设置关联表", indexes = {@MpIndex(name = "ix_itemid_companyid", columns = {"company_id", "item_id"})})
public class ItemsRelPointAccess {

	/** 商品ID */
	@MpId(value = "item_id", type = IdType.INPUT, columnType = "bigint", comment = "商品ID")
	private Long itemId;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 积分 */
	@MpField(value = "point", columnType = "bigint", comment = "积分")
	private Long point;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
