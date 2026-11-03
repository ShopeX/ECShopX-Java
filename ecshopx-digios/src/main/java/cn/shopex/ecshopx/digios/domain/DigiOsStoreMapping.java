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

package cn.shopex.ecshopx.digios.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@MpTable(
		value = "digios_store_mapping",
		comment = "DigiOS门店档案ID映射",
		uniqueIndexes = @MpIndex(name = "uk_digios_store_company_distributor", columns = {"company_id", "distributor_id"}),
		indexes = @MpIndex(name = "idx_digios_store_shop_code", columns = {"company_id", "shop_code"}))
public class DigiOsStoreMapping {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint")
	private Long companyId;

	@MpField(value = "distributor_id", columnType = "bigint")
	private Long distributorId;

	@MpField(value = "shop_code", columnType = "string", length = 128)
	private String shopCode;

	@MpField(value = "digios_store_id", columnType = "bigint")
	private Long digiosStoreId;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
