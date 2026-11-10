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

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 商品搜索同义词组（公司维度）。 */
@Data
@MpTable(
		value = "goods_search_synonyms",
		comment = "商品搜索同义词",
		indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"})},
		uniqueIndexes = {@MpIndex(name = "uk_company_group_key", columns = {"company_id", "group_key"}, unique = true)})
public class GoodsSearchSynonym {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司id")
	private Long companyId;

	@MpField(value = "group_key", columnType = "string", length = 64, comment = "同义词组键")
	private String groupKey;

	/** JSON 数组字符串，如 ["手机","移动电话"] */
	@MpField(value = "words", columnType = "text", comment = "同义词 JSON 数组")
	private String words;

	@MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
	private Integer updated;
}
