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

package cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；分页回显（{@code OpenapiDiscountCardV2ListFormatSupport#formatListStruct}）。
 */
@Schema(name = "EcxUserdiscountListPager")
public class EcxUserdiscountListPagerDoc {

	@Schema(description = "页码回显（归一化后）", example = "1")
	public Integer page;

	@Schema(description = "每页条数回显（归一化后，最小 20）", example = "20")
	public Integer page_size;
}