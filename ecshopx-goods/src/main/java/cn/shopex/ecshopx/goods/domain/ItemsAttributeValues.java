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
 * 商品属性值表
 */
@Data
@MpTable(value = "items_attribute_values", comment = "商品属性值表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_attribute_id", columns = {"attribute_id"}), @MpIndex(name = "ix_attribuattribute_valuete_name", columns = {"attribuattribute_valuete_name"})})
public class ItemsAttributeValues {

	/** 商品属性值id */
	@MpId(value = "attribute_value_id", type = IdType.AUTO, columnType = "bigint", comment = "商品属性值id")
	private Long attributeValueId;

	/** 商品属性ID */
	@MpField(value = "attribute_id", columnType = "bigint", comment = "商品属性ID")
	private Long attributeId;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 店铺ID，如果为0则表示总部 */
	@MpField(value = "shop_id", columnType = "bigint", comment = "店铺ID，如果为0则表示总部")
	private Long shopId;

	/** 商品属性值（库表物理列名，非 attribute_value） */
	@MpField(value = "attribuattribute_valuete_name", columnType = "string", comment = "商品属性值")
	private String attributeValue;

	/** 商品属性排序，越大越在前 */
	@MpField(value = "sort", columnType = "string", length = 15, comment = "商品属性排序，越大越在前")
	private String sort;

	/** 图片 */
	@MpField(value = "image_url", columnType = "text", nullable = true, comment = "图片")
	private String imageUrl;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;

	/** oms商品属性值id */
	@MpField(value = "oms_value_id", columnType = "bigint", nullable = true, comment = "oms商品属性值id")
	private Long omsValueId;
}
