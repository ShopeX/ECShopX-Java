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
 * 商品药品数据表
 */
@Data
@MpTable(value = "items_medicine", comment = "商品药品数据表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_medicine_type", columns = {"medicine_type"}), @MpIndex(name = "ix_common_name", columns = {"common_name"}), @MpIndex(name = "ix_approval_number", columns = {"approval_number"}), @MpIndex(name = "ix_is_prescription", columns = {"is_prescription"})})
public class ItemsMedicine {

	/** 商品id */
	@MpId(value = "item_id", type = IdType.INPUT, columnType = "bigint", comment = "商品id")
	private Long itemId;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 药品分类:0为西药，1为中成药，3为其他 */
	@MpField(value = "medicine_type", columnType = "smallint", comment = "药品分类:0为西药，1为中成药，3为其他")
	private Integer medicineType;

	/** 通用名（重要，具体请查看《同步药品信息规范指引》文档） */
	@MpField(value = "common_name", columnType = "string", comment = "通用名（重要，具体请查看《同步药品信息规范指引》文档）")
	private String commonName;

	/** 剂型 */
	@MpField(value = "dosage", columnType = "string", comment = "剂型")
	private String dosage = "";

	/** 规格（重要，具体请查看《同步药品信息规范指引》文档） */
	@MpField(value = "spec", columnType = "string", comment = "规格（重要，具体请查看《同步药品信息规范指引》文档）")
	private String spec;

	/** 包装规格 */
	@MpField(value = "packing_spec", columnType = "string", comment = "包装规格")
	private String packingSpec = "";

	/** 生产厂家 */
	@MpField(value = "manufacturer", columnType = "string", comment = "生产厂家")
	private String manufacturer;

	/** 批准文号 */
	@MpField(value = "approval_number", columnType = "string", comment = "批准文号")
	private String approvalNumber;

	/** 最小售卖单位 */
	@MpField(value = "unit", columnType = "string", comment = "最小售卖单位")
	private String unit;

	/** 是否处方药（1为是，0为否） */
	@MpField(value = "is_prescription", columnType = "smallint", comment = "是否处方药（1为是，0为否）")
	private Integer isPrescription;

	/** 特殊通用名 */
	@MpField(value = "special_common_name", columnType = "string", comment = "特殊通用名")
	private String specialCommonName = "";

	/** 特殊规格 */
	@MpField(value = "special_spec", columnType = "string", comment = "特殊规格")
	private String specialSpec = "";

	/**
	 * 审核状态，0不需要审核（非处方药），1未审核，2审核通过，3审核不通过
	 */
	@MpField(value = "audit_status", columnType = "smallint", comment = "审核状态，0不需要审核（非处方药），1未审核，2审核通过，3审核不通过", defaultValue = "1")
	private Integer auditStatus = 1;

	/** 审核不通过原因 */
	@MpField(value = "audit_reason", columnType = "string", comment = "审核不通过原因")
	private String auditReason = "";

	/** 商品类型 normal普通商品 point积分商品 */
	@MpField(value = "item_type", columnType = "string", comment = "商品类型 normal普通商品 point积分商品", defaultValue = "normal")
	private String itemType = "normal";

	/** 处方药用药提示 */
	@MpField(value = "use_tip", columnType = "string", comment = "处方药用药提示")
	private String useTip = "";

	/** 处方药适用症状 */
	@MpField(value = "symptom", columnType = "string", comment = "处方药适用症状")
	private String symptom = "";

	/** 处方药单次下单最大购买数量 */
	@MpField(value = "max_num", columnType = "integer", comment = "处方药单次下单最大购买数量", defaultValue = "0")
	private Integer maxNum = 0;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
