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
 * 商品分类表
 */
@Data
@MpTable(value = "items_category", comment = "商品分类表", indexes = {@MpIndex(name = "ix_company_main_show", columns = {"company_id", "is_main_category", "is_show_front"}), @MpIndex(name = "ix_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "ix_parent_id", columns = {"parent_id"})})
public class ItemsCategory {

	/** 商品分类id */
	@MpId(value = "category_id", type = IdType.AUTO, columnType = "bigint", comment = "商品分类id")
	private Long categoryId;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 地区ID */
	@MpField(value = "regionauth_id", columnType = "bigint", comment = "地区ID", defaultValue = "0")
	private Long regionauthId = 0L;

	/** 分类名称 */
	@MpField(value = "category_name", columnType = "string", length = 50, comment = "分类名称")
	private String categoryName;

	/** 分类编码 */
	@MpField(value = "category_code", columnType = "string", length = 50, nullable = true, comment = "分类编码")
	private String categoryCode;

	/** 父级id, 0为顶级 */
	@MpField(value = "parent_id", columnType = "bigint", comment = "父级id, 0为顶级", defaultValue = "0")
	private Long parentId = 0L;

	/** 商品分类等级 */
	@MpField(value = "category_level", columnType = "integer", nullable = true, comment = "商品分类等级", defaultValue = "1")
	private Integer categoryLevel = 1;

	/** 是否为商品主类目 */
	@MpField(value = "is_main_category", columnType = "boolean", nullable = true, comment = "是否为商品主类目", defaultValue = "False")
	private Boolean isMainCategory = false;

	/** 是否前台展示 */
	@MpField(value = "is_show_front", columnType = "smallint", comment = "是否前台展示", defaultValue = "1")
	private Integer isShowFront = 1;

	/** 路径 */
	@MpField(value = "path", columnType = "string", length = 255, nullable = true, comment = "路径", defaultValue = "0")
	private String path = "0";

	/** 店铺ID */
	@MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "店铺ID", defaultValue = "0")
	private Long distributorId = 0L;

	/** 佣金比例 */
	@MpField(value = "commission_ratio", columnType = "integer", comment = "佣金比例", defaultValue = "0")
	private Integer commissionRatio = 0;

	/** 排序 */
	@MpField(value = "sort", columnType = "bigint", nullable = true, comment = "排序", defaultValue = "0")
	private Long sort = 0L;

	/** 商品参数 */
	@MpField(value = "goods_params", columnType = "text", nullable = true, comment = "商品参数")
	private String goodsParams;

	/** 商品规格 */
	@MpField(value = "goods_spec", columnType = "text", nullable = true, comment = "商品规格")
	private String goodsSpec;

	/** 分类图片链接 */
	@MpField(value = "image_url", columnType = "text", nullable = true, comment = "分类图片链接")
	private String imageUrl;

	/** 跨境税率，百分比，小数点2位 */
	@MpField(value = "crossborder_tax_rate", columnType = "string", length = 10, nullable = true, comment = "跨境税率，百分比，小数点2位")
	private String crossborderTaxRate;

	/** 发票税率ID */
	@MpField(value = "invoice_tax_rate_id", columnType = "bigint", nullable = true, comment = "发票税率ID")
	private Long invoiceTaxRateId;

	/** 发票税率 */
	@MpField(value = "invoice_tax_rate", columnType = "string", length = 16, nullable = true, comment = "发票税率")
	private String invoiceTaxRate;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;

	/** 自定义模板id */
	@MpField(value = "customize_page_id", columnType = "bigint", nullable = true, comment = "自定义模板id", defaultValue = "0")
	private Long customizePageId = 0L;

	/** 淘宝分类id */
	@MpField(value = "category_id_taobao", columnType = "bigint", nullable = true, comment = "淘宝分类id", defaultValue = "0")
	private Long categoryIdTaobao = 0L;

	/** 淘宝父级分类ID */
	@MpField(value = "parent_id_taobao", columnType = "bigint", nullable = true, comment = "淘宝父级分类ID", defaultValue = "0")
	private Long parentIdTaobao = 0L;

	/** 淘宝分类信息行 */
	@MpField(value = "taobao_category_info", columnType = "json_array", nullable = true, comment = "淘宝分类信息行")
	private String taobaoCategoryInfo;
}
