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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code CouponController#getCouponList} 经
 * {@code OpenapiRequestParams#mergeString} 与 {@code OpenapiCouponListParams#isParamPresent} 读取的全部 key。
 */
@Schema(
		name = "EcxCouponListBody",
		description =
				"优惠券列表查询参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）")
public class EcxCouponListBodyDoc {

	@Schema(
			description =
					"店铺 ID 筛选，支持逗号分隔多个；非空且非 \"0\" 时生效，"
							+ "命中「全部店铺（distributor_id=\",\"）」或包含任一指定店铺的券模板；缺省/\"0\" 不过滤",
			example = "5")
	public String distributor_id;

	@Schema(
			description = "页码，从 1 起；仅当 page_no 与 page_size 同时提供（键存在即算提供）才分页，否则返回全部；非整数报错",
			example = "1")
	public String page_no;

	@Schema(description = "每页条数；与 page_no 成对提供才分页；非整数报错", example = "20")
	public String page_size;
}