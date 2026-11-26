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
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，字段来源 {@code AftersalesController#getIncrAftersalesList}
 * 经 {@code OpenapiThirdApiV2AftersalesListParams#resolve} 与 {@code OpenapiRequestParams#originalString}
 * 读取的全部 key，过滤语义见 {@code OpenapiThirdApiV2AftersalesIncrListService}。
 */
@Schema(
		name = "EcxAftersalesIncrListBody",
		description =
				"增量售后单列表参数（body 与 query 同名 key 双通道：body 含键时取 body 原始值，否则用 query）。"
						+ " 与全量列表差异：时间过滤/排序作用于 update_time，且支持类型/状态/进度过滤。")
public class EcxAftersalesIncrListBodyDoc {

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
					"会员手机号；PHP-truthy（非空且非 \"0\"）才生效：须匹配 ^1[345789][0-9]{9}$，"
							+ "否则 E5320「请填写正确的手机号」；查无会员 E5331「会员找不到」；命中按 user_id 过滤",
			example = "13800000000")
	public String mobile;

	@Schema(
			description =
					"更新时间下界（update_time ≥，闭区间）；PHP-truthy 才生效；"
							+ "yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd（当日 00:00:00，系统时区）转 Unix 秒，解析失败按 0；"
							+ "与 end_modified 同时生效且下界大于上界报 E5320「开始时间不能大于结束时间」",
			example = "2023-11-15")
	public String start_modified;

	@Schema(description = "更新时间上界（update_time ≤，闭区间）；语义同 start_modified", example = "2023-11-16")
	public String end_modified;

	@Schema(
			description =
					"售后类型精确过滤；PHP-truthy 才生效；原样等值匹配（无枚举校验，非法值查空）："
							+ "ONLY_REFUND / REFUND_GOODS / EXCHANGING_GOODS",
			example = "REFUND_GOODS")
	public String aftersales_type;

	@Schema(
			description = "售后状态精确过滤；PHP-truthy 才生效；整数解析（trim，非法按 0）：0 待处理/1 处理中/2 已处理/3 已驳回/4 已撤销",
			example = "1")
	public String aftersales_status;

	@Schema(
			description =
					"处理进度精确过滤；PHP-truthy 才生效（\"0\" 视为未提供无法过滤进度 0）；整数解析（trim，非法按 0）；枚举同响应 progress",
			example = "2")
	public String progress;
}