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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 权益日志列表中联查的门店人员行（表 {@code shop_salesperson}） */
@Data
@MpTable(value = "shop_salesperson", comment = "门店人员")
public class RightsLogsShopSalesperson {

	@MpId(value = "salesperson_id", type = IdType.AUTO, columnType = "bigint", comment = "门店人员ID")
	private Long salespersonId;

	@MpField(value = "name", columnType = "string", length = 500, comment = "姓名")
	private String name;

	@MpField(value = "mobile", columnType = "string", comment = "手机号")
	private String mobile;

	@MpField(value = "salesperson_type", columnType = "string", comment = "人员类型 admin: 管理员; verification_clerk:核销员; shopping_guide:导购员", defaultValue = "admin")
	private String salespersonType;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
	private Long companyId;
}
