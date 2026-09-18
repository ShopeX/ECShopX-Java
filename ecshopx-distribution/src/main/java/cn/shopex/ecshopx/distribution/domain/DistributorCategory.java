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

package cn.shopex.ecshopx.distribution.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 店铺分类表 {@code distribution_distributor_category} */
@Data
@MpTable(value = "distribution_distributor_category", comment = "店铺分类表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_category_name", columns = {"category_name"}), @MpIndex(name = "idx_category_code", columns = {"category_code"})})
public class DistributorCategory {

	@MpId(value = "category_id", type = IdType.AUTO, columnType = "bigint", comment = "分类ID")
	private Long categoryId;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "category_name", columnType = "string", comment = "店铺分类名称")
	private String categoryName;

	@MpField(value = "category_code", columnType = "string", comment = "分类编号")
	private String categoryCode;

	@MpField(value = "created", columnType = "bigint")
	private Long created;

	@MpField(value = "updated", columnType = "bigint")
	private Long updated;
}
