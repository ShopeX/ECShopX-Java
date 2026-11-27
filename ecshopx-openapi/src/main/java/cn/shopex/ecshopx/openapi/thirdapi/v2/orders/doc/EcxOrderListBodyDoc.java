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
 * 仅 OpenAPI 文档；POST 请求 JSON body，4 键与 query 同名双通道（均 {@code mergeString}：
 * body 含键优先——null/空白串归一为未传；否则 query trim 后非空才生效）。
 * 校验见 {@code OpenapiThirdApiV2OrderListService#validateParams}，
 * 分页见 {@code OpenapiOrderListParams#resolve}（page 未传时固定 LIMIT 10 OFFSET 0）。
 */
@Schema(
		name = "EcxOrderListBody",
		description =
				"会员订单列表参数（4 键，body 含键优先否则 query 同名键）。"
						+ " mobile 与 unionid 至少一个非空（PHP empty：null/空串/\"0\"/0/false 视为空）；"
						+ " mobile 优先按手机号找会员，否则按 unionid（user_type=wechat）找会员。")
public class EcxOrderListBodyDoc {

	@Schema(
			description =
					"会员手机号；键一旦出现（query 存在或 body 含键，即使值为 null/空串）即须匹配"
							+ " ^1[345789][0-9]{9}$，否则 E0001「请填写正确的手机号」。"
							+ "非空（PHP truthy）时优先按手机号查会员（查无 → E0001「会员信息获取失败」）；"
							+ "与 unionid 均空 → E0001「unionid或者手机号必填」",
			example = "13800138000")
	public String mobile;

	@Schema(
			description =
					"微信 unionid；mobile 未生效时按 members_associations（user_type=wechat）查会员，"
							+ "查无 → E0001「会员信息获取失败」。body 值须为字符串或 null——"
							+ "传数字/布尔/对象等非字符串 → E0001「请填写unionid」（示例为格式演示值）",
			example = "oX1vU0abcDEF123456")
	public String unionid;

	@Schema(
			description =
					"页码；默认 1。PHP truthy（非 null/空串/\"0\"）才启用自定义分页；"
							+ "整数解析失败回退 1；无下限校验（page≤0 时 OFFSET 为负 → SQL 错误 → HTTP 500）",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；page 未传时忽略（固定每页 10）。page 已传且本键缺失/PHP empty（null/空串/\"0\"/0/false）"
							+ "或解析失败 → 不分页返回全部订单；>0 时 LIMIT page_size OFFSET (page-1)*page_size；无上限校验",
			example = "10")
	public String page_size;
}