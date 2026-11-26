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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，字段来源 {@code AftersalesController#getAftersalesList}
 * 经 {@code OpenapiThirdApiV2AftersalesListParams#resolve} 与 {@code OpenapiRequestParams#originalString}
 * 读取的全部 key。
 */
@Schema(
		name = "EcxAftersalesListBody",
		description =
				"售后单列表参数（body 与 query 同名 key 双通道：body 含键时取 body 原始值，否则用 query；"
						+ "各字段内部再按需 trim）")
public class EcxAftersalesListBodyDoc {

	@Schema(description = "页码；PHP 整型解析（含小数取整、非法按 0），缺省 1，小于 1 归一为 1", example = "1")
	public String page;

	@Schema(
			description =
					"每页条数；缺省 20；解析后 ≥500 直接报「每页的大小最大为500！」（E0001，500 本身也报错）；"
							+ "小于 20 一律归一为 20（PHP 旧契约）",
			example = "20")
	public String page_size;

	@Schema(
			description =
					"会员手机号；PHP-truthy（非空且非 \"0\"）才生效：须匹配 ^1[345789][0-9]{9}$"
							+ "（不含 14x/16x），否则 E5320「请填写正确的手机号」；查无会员 E5331「会员找不到」；"
							+ "命中则按该会员 user_id 过滤",
			example = "13800000000")
	public String mobile;

	@Schema(
			description =
					"创建时间下界（create_time ≥，闭区间）；PHP-truthy 才生效；"
							+ "yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd（当日 00:00:00，系统时区）转 Unix 秒，解析失败按 0；"
							+ "与 time_end 同时生效且下界大于上界报 E5320「开始时间不能大于结束时间」",
			example = "2023-11-15")
	public String time_begin;

	@Schema(description = "创建时间上界（create_time ≤，闭区间）；语义同 time_begin", example = "2023-11-16")
	public String time_end;
}