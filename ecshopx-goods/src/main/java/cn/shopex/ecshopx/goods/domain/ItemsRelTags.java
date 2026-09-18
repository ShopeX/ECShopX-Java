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
 * 商品关联标签表
 */
@Data
@MpTable(value = "items_rel_tags", comment = "商品关联标签表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_item_id", columns = {"item_id"}), @MpIndex(name = "ix_tag_id", columns = {"tag_id"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"tag_id", "company_id", "item_id"})})
public class ItemsRelTags {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

	/** 标签id */
	@MpField(value = "tag_id", columnType = "bigint", comment = "标签id")
	private Long tagId;

	/** 公司id */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司id")
	private Long companyId;

	/** 商品id */
	@MpField(value = "item_id", columnType = "bigint", comment = "商品id")
	private Long itemId;
}
