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

package cn.shopex.ecshopx.goods.domain.recommend;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 商品推荐四页展示设置（一行一公司，SSOT §3.4） */
@Data
@MpTable(
		value = "goods_recommend_display_setting",
		comment = "商品推荐四页展示设置",
		indexes = {@MpIndex(name = "uk_goods_recommend_display_company", columns = {"company_id"}, unique = true)})
public class GoodsRecommendDisplaySetting {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "detail_enabled", columnType = "integer", comment = "商品详情页开关")
	private Integer detailEnabled;

	@MpField(value = "cart_enabled", columnType = "integer", comment = "购物车页开关")
	private Integer cartEnabled;

	@MpField(value = "checkout_enabled", columnType = "integer", comment = "结算页开关")
	private Integer checkoutEnabled;

	@MpField(value = "order_detail_enabled", columnType = "integer", comment = "订单详情页开关")
	private Integer orderDetailEnabled;

	@MpField(value = "detail_limit", columnType = "integer", comment = "详情页展示数量")
	private Integer detailLimit;

	@MpField(value = "cart_limit", columnType = "integer", comment = "购物车页展示数量")
	private Integer cartLimit;

	@MpField(value = "checkout_limit", columnType = "integer", comment = "结算页展示数量")
	private Integer checkoutLimit;

	@MpField(value = "order_detail_limit", columnType = "integer", comment = "订单详情页展示数量")
	private Integer orderDetailLimit;

	@MpField(value = "detail_sort", columnType = "string", length = 16, comment = "详情页排序")
	private String detailSort;

	@MpField(value = "cart_sort", columnType = "string", length = 16, comment = "购物车页排序")
	private String cartSort;

	@MpField(value = "checkout_sort", columnType = "string", length = 16, comment = "结算页排序")
	private String checkoutSort;

	@MpField(value = "order_detail_sort", columnType = "string", length = 16, comment = "订单详情页排序")
	private String orderDetailSort;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
