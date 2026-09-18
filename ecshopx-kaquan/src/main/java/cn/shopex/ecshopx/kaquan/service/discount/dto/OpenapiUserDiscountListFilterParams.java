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

package cn.shopex.ecshopx.kaquan.service.discount.dto;

import lombok.Data;

@Data
public class OpenapiUserDiscountListFilterParams {

	private Long companyId;

	/** 对齐 filter.user_id = plat_account；会员预检通过后写入解析后的 userId */
	private Long userId;

	/** 仅 PHP truthy code 时非 null */
	private String code;
}
