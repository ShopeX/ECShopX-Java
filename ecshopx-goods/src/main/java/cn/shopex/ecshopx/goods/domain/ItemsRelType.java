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
 * 商品数值属性关联表
 */
@Data
@MpTable(value = "items_rel_type", comment = "商品数值属性关联表", uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"item_id", "label_id"})})
public class ItemsRelType {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

	/** 商品ID */
	@MpField(value = "item_id", columnType = "bigint", comment = "商品ID")
	private Long itemId;

	/** 数值属性ID */
	@MpField(value = "label_id", columnType = "bigint", comment = "数值属性ID")
	private Long labelId;

	/** 数值属性名称 */
	@MpField(value = "label_name", columnType = "string", length = 255, comment = "数值属性名称")
	private String labelName;

	/** 价格,单位为‘分’ */
	@MpField(value = "label_price", columnType = "integer", comment = "价格,单位为‘分’")
	private Integer labelPrice;

	/**
	 * 会员数值属性类型，plus：加，minux：减，multiple：乘
	 */
	@MpField(value = "num_type", columnType = "string", length = 30, comment = "会员数值属性类型，plus：加，minux：减，multiple：乘")
	private String numType;

	/** 数值属性值，例如买了50个次卡，就是填50；赠送了10个经验，就是填10 */
	@MpField(value = "num", columnType = "bigint", comment = "数值属性值，例如买了50个次卡，就是填50；赠送了10个经验，就是填10")
	private Long num;

	/** 限制核销次数,1:不限制；2:限制 */
	@MpField(value = "is_not_limit_num", columnType = "integer", comment = "限制核销次数,1:不限制；2:限制", defaultValue = "2")
	private Integer isNotLimitNum = 2;

	/** 有效期，例如30天 */
	@MpField(value = "limit_time", columnType = "bigint", comment = "有效期，例如30天", defaultValue = "0")
	private Long limitTime = 0L;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
