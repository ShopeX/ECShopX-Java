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

package cn.shopex.ecshopx.orders.repository;

import java.util.List;
import lombok.Data;

/**
 * 疫情登记列表查询条件（company_id 必填；店铺条件 eq 与 IN 互斥）。
 */
@Data
public class OrderEpidemicRegisterListFilter {

	private long companyId;
	private Long distributorIdEq;
	private List<Long> distributorIdIn;
	private Integer orderTimeGte;
	private Integer orderTimeLte;

	/** 非 null 时 WHERE user_id = ? */
	private Long userIdEq;

	/** 非 null 时 WHERE is_use = ? */
	private Integer isUseEq;
}
