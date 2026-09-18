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

package cn.shopex.ecshopx.salesperson.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@MpTable("items")
public class SalespersonItemCartRow {

	@MpId(value = "item_id", type = IdType.AUTO)
	private Long itemId;

	@MpField("company_id")
	private Long companyId;

	@MpField("store")
	private Integer store;

	@MpField("special_type")
	private String specialType;

	@MpField("distributor_id")
	private Integer distributorId;

	@MpField("approve_status")
	private String approveStatus;
}
