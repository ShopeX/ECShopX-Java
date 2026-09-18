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

package cn.shopex.ecshopx.community.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 社区拼团商品池
 */
@Data
@MpTable(value = "community_items", comment = "社区拼团商品池", indexes = {@MpIndex(name = "ix_goods_id", columns = {"goods_id"})})
public class CommunityItems {

	@MpId(value = "goods_id", type = IdType.INPUT, columnType = "bigint", comment = "商品ID")
	private Long goodsId;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司id")
	private Long companyId;

	/**
	 * 店铺id，为 0 时表示该商品为商城商品，否则为店铺自有商品。
	 */
	@MpField(value = "distributor_id", columnType = "integer", comment = "店铺id,为0时表示该商品为商城商品，否则为店铺自有商品", defaultValue = "0")
	private Integer distributorId = 0;

	@MpField(value = "min_delivery_num", columnType = "integer", comment = "起送量", defaultValue = "0")
	private Integer minDeliveryNum = 0;

	@MpField(value = "sort", columnType = "integer", comment = "商品排序", defaultValue = "0")
	private Integer sort = 0;

	@MpField(value = "created_at", columnType = "integer")
	private Integer createdAt;

	@MpField(value = "updated_at", columnType = "integer", nullable = true)
	private Integer updatedAt;
}
