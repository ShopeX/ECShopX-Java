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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 商品积分获取设置（订单模块只读，表与 goods 域共用） */
@Data
@MpTable("items_rel_point_access")
public class OrderItemsRelPointAccess {

	@MpId(value = "item_id", type = IdType.INPUT)
	private Long itemId;

	@MpField("company_id")
	private Long companyId;

	@MpField("point")
	private Long point;

	@MpField("created")
	private Integer created;

	@MpField("updated")
	private Integer updated;
}
