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

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 门店 POS 账号 */
@Data
@MpTable(
		value = "pos_store_account",
		comment = "门店POS账号",
		indexes = {
			@MpIndex(name = "idx_company_shop_status", columns = {"company_id", "distributor_id", "status"}),
			@MpIndex(name = "idx_company_role", columns = {"company_id", "role"})
		},
		uniqueIndexes = {@MpIndex(name = "uk_employee_no", columns = {"employee_no"})})
public class PosStoreAccount {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司id")
	private Long companyId;

	/** 全局唯一；库列 collation utf8mb4_unicode_ci（A01≡a01） */
	@MpField(value = "employee_no", columnType = "string", length = 30, comment = "员工号（全局唯一）")
	private String employeeNo;

	@MpField(value = "name", columnType = "string", length = 30, comment = "姓名", defaultValue = "")
	private String name = "";

	@MpField(value = "email", columnType = "string", length = 255, comment = "邮箱", defaultValue = "")
	private String email = "";

	@MpField(value = "mobile", columnType = "string", length = 20, comment = "手机号", defaultValue = "")
	private String mobile = "";

	@MpField(value = "role", columnType = "string", length = 32, comment = "store_manager/supervisor/clerk")
	private String role;

	@MpField(value = "distributor_id", columnType = "bigint", comment = "归属门店")
	private Long distributorId;

	@MpField(value = "status", columnType = "string", length = 16, comment = "enabled/disabled/resigned")
	private String status;

	@MpField(value = "password_cipher", columnType = "string", length = 512, comment = "可逆密文")
	private String passwordCipher;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
