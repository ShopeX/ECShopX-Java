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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 商城物流启用表 */
@Data
@MpTable(value = "company_rel_logistics", comment = "商城物流启用表")
public class CompanyRelLogistics {

	@MpId(value = "id", type = IdType.AUTO, columnType = "integer")
	private Integer id;

	@MpField(value = "company_id", columnType = "smallint", comment = "公司ID")
	private Integer companyId;

	@MpField(value = "corp_id", columnType = "smallint", comment = "物流公司ID")
	private Integer corpId;

	@MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
	private Long distributorId = 0L;

	@MpField(value = "supplier_id", columnType = "bigint", comment = "供应商id", defaultValue = "0")
	private Long supplierId = 0L;

	@MpField(value = "corp_code", columnType = "string", comment = "物流公司代码")
	private String corpCode;

	@MpField(value = "corp_name", columnType = "string", comment = "物流公司简称")
	private String corpName;
}
