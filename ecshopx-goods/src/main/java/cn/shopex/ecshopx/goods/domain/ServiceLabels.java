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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 数值属性表
 */
@Data
@MpTable(value = "servicelabels", comment = "数值属性表")
public class ServiceLabels {

	/** 数值属性ID */
	@MpId(value = "label_id", type = IdType.AUTO, columnType = "bigint", comment = "数值属性ID")
	private Long labelId;

	/** 数值属性名称 */
	@MpField(value = "label_name", columnType = "string", length = 255, comment = "数值属性名称")
	private String labelName;

	/** 价格,单位为‘分’ */
	@MpField(value = "label_price", columnType = "integer", comment = "价格,单位为‘分’")
	private Integer labelPrice;

	/**
	 * 会员数值属性类型，point：积分类型，deposit：预存类型，timescard：次卡类型
	 */
	@MpField(value = "service_type", columnType = "string", length = 30, comment = "会员数值属性类型，point：积分类型，deposit：预存类型，timescard：次卡类型")
	private String serviceType;

	/** 数值属性描述 */
	@MpField(value = "label_desc", columnType = "string", length = 255, comment = "数值属性描述")
	private String labelDesc;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
