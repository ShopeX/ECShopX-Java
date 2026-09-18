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
 * 商品关联属性表
 */
@Data
@MpTable(value = "items_rel_attributes", comment = "商品关联属性表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_item_id", columns = {"item_id"}), @MpIndex(name = "ix_attribute_type", columns = {"attribute_type"})})
public class ItemRelAttributes {

	/** ID */
	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
	private Long id;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 商品ID */
	@MpField(value = "item_id", columnType = "bigint", comment = "商品ID")
	private Long itemId;

	/** 商品属性排序 */
	@MpField(value = "attribute_sort", columnType = "integer", nullable = true, comment = "商品属性排序", defaultValue = "0")
	private Integer attributeSort = 0;

	/** 商品属性id */
	@MpField(value = "attribute_id", columnType = "bigint", comment = "商品属性id")
	private Long attributeId;

	/**
	 * 商品属性类型 unit单位，brand品牌，item_params商品参数, item_spec规格
	 */
	@MpField(value = "attribute_type", columnType = "string", length = 15, comment = "商品属性类型 unit单位，brand品牌，item_params商品参数, item_spec规格")
	private String attributeType;

	/** 商品属性值id */
	@MpField(value = "attribute_value_id", columnType = "bigint", nullable = true, comment = "商品属性值id")
	private Long attributeValueId;

	/** 自定义属性名称 */
	@MpField(value = "custom_attribute_value", columnType = "string", nullable = true, comment = "自定义属性名称")
	private String customAttributeValue;

	/** 规格自定义图片 */
	@MpField(value = "image_url", columnType = "json_array", nullable = true, comment = "规格自定义图片")
	private String imageUrl;
}
