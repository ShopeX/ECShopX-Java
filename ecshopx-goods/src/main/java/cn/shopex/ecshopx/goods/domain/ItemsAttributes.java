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
 * 商品属性表
 */
@Data
@MpTable(value = "items_attributes", comment = "商品属性表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_attribute_type", columns = {"attribute_type"}), @MpIndex(name = "ix_attribute_name", columns = {"attribute_name"})})
public class ItemsAttributes {

	/** 商品属性id */
	@MpId(value = "attribute_id", type = IdType.AUTO, columnType = "bigint", comment = "商品属性id")
	private Long attributeId;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 店铺ID，如果为0则表示总部 */
	@MpField(value = "shop_id", columnType = "bigint", comment = "店铺ID，如果为0则表示总部", defaultValue = "0")
	private Long shopId = 0L;

	/**
	 * 商品属性类型 unit单位，brand品牌，item_params商品参数, item_spec规格
	 */
	@MpField(value = "attribute_type", columnType = "string", length = 15, comment = "商品属性类型 unit单位，brand品牌，item_params商品参数, item_spec规格")
	private String attributeType;

	/** 属性展示方式 select 下拉 input 平铺 */
	@MpField(value = "attribute_show", columnType = "string", length = 15, nullable = true, comment = "属性展示方式 select 下拉 input 平铺")
	private String attributeShow = "select";

	/** 商品属性名称 */
	@MpField(value = "attribute_name", columnType = "string", comment = "商品属性名称")
	private String attributeName;

	/** 商品属性备注 */
	@MpField(value = "attribute_memo", columnType = "string", nullable = true, comment = "商品属性备注")
	private String attributeMemo;

	/** 商品属性排序，越大越在前 */
	@MpField(value = "attribute_sort", columnType = "string", length = 15, comment = "商品属性排序，越大越在前")
	private String attributeSort;

	/** 店铺ID */
	@MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "店铺ID", defaultValue = "0")
	private Long distributorId = 0L;

	/** 是否用于筛选 */
	@MpField(value = "is_show", columnType = "string", length = 15, comment = "是否用于筛选")
	private String isShow;

	/** 属性是否需要配置图片 */
	@MpField(value = "is_image", columnType = "string", comment = "属性是否需要配置图片")
	private String isImage;

	/** 图片 */
	@MpField(value = "image_url", columnType = "text", nullable = true, comment = "图片")
	private String imageUrl;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;

	/** oms 规格编码 */
	@MpField(value = "attribute_code", columnType = "string", length = 32, nullable = true, comment = "oms 规格编码")
	private String attributeCode;
}
