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

package cn.shopex.ecshopx.chinaumspay.service.divisiondetail;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分账单明细导出与列表同源筛选条件。
 */
@Getter
@AllArgsConstructor
public class DivisionDetailExportFilter {

	private final long companyId;
	private final Long orderId;
	private final Long divisionId;
	private final Long distributorId;
	private final String createTimeBegin;
	private final String createTimeEnd;
}
