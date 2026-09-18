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

@Data
@MpTable(
		value = "goods_recommend_rule_recommend_item",
		comment = "商品推荐规则推荐商品",
		indexes = {
			@MpIndex(name = "uk_goods_recommend_recommend_rule_goods", columns = {"rule_id", "goods_id"}, unique = true),
			@MpIndex(name = "ix_goods_recommend_recommend_company", columns = {"company_id"})
		})
public class GoodsRecommendRuleRecommendItem {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "rule_id", columnType = "bigint")
	private Long ruleId;

	@MpField(value = "goods_id", columnType = "bigint")
	private Long goodsId;

	@MpField(value = "sort", columnType = "integer")
	private Integer sort;

	@MpField(value = "created", columnType = "integer")
	private Integer created;
}
