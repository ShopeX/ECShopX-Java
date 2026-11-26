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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/** 仅 OpenAPI 文档；GET 请求可选 JSON body，与 query 同名参数等价。 */
@Schema(name = "EcxDistributorListBody", description = "店铺列表查询参数（body 与 query 同名 key 等价，body 优先）")
public class EcxDistributorListBodyDoc {

	@Schema(description = "页码，默认 1，最小 1", example = "1")
	public String page;

	@Schema(description = "每页条数，默认 20，范围 1–500", example = "20")
	public String page_size;

	@Schema(description = "店铺号，精确匹配", example = "S001")
	public String shop_code;

	@Schema(description = "店铺名称，模糊匹配")
	public String distributor_name;

	@Schema(description = "状态筛选：0 已删除，1 启用，2 禁用", example = "1")
	public String status;

	@Schema(description = "省，模糊匹配")
	public String province;

	@Schema(description = "市，模糊匹配")
	public String city;

	@Schema(description = "区，模糊匹配")
	public String area;

	@Schema(description = "联系人姓名，模糊匹配")
	public String contact_username;

	@Schema(description = "联系人手机，精确匹配")
	public String contact_mobile;

	@Schema(description = "店铺 ID，精确匹配")
	public String distributor_id;
}