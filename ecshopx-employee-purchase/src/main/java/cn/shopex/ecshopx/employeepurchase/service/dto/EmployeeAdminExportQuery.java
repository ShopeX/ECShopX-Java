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

package cn.shopex.ecshopx.employeepurchase.service.dto;

/**
 * 管理端员工导出 / 列表查询的 SQL 条件（不含 datapass 等非表字段）。
 */
public record EmployeeAdminExportQuery(
		long companyId,
		Integer distributorId,
		String mobile,
		String account,
		String email,
		String memberMobile,
		Long enterpriseId) {}
