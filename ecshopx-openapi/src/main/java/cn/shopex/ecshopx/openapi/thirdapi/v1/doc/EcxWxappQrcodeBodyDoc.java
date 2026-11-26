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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code OpenapiWxappQrcodeParams#parse}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key（原始值不 trim）。
 */
@Schema(
		name = "EcxWxappQrcodeBody",
		description =
				"导购任务小程序码参数（body 与 query 同名 key 双通道：body 含键时取 body 原始值——不 trim，"
						+ "body 无该键时用 query）。任何校验失败统一报「参数错误」。")
public class EcxWxappQrcodeBodyDoc {

	@Schema(
			description =
					"落地页类型；必填，仅 index / goods_list / goods_detail / recommend_list / recommend_detail / share_land，"
							+ "分别映射小程序页面 pages/index、pages/item/list、pages/item/espier-detail、"
							+ "pages/recommend/list、pages/recommend/detail、pages/share-land",
			requiredMode = Schema.RequiredMode.REQUIRED,
			allowableValues = {"index", "goods_list", "goods_detail", "recommend_list", "recommend_detail", "share_land"},
			example = "goods_detail")
	public String path_type;

	@Schema(
			description =
					"场景参数 JSON 对象字符串；必填（缺失/空白/\"0\"/非 JSON 对象均报「参数错误」）。"
							+ "path_type=goods_detail 或 recommend_detail 时必须含 id（整数或整数字符串且 ≥1，"
							+ "小数/\"0\"/非数字拒绝），id 进入小程序码 scene；"
							+ "其余键原样存入 Redis 分享参数（key=companyId_shareId，TTL 30 天），"
							+ "小程序码 scene 恒为 [id=...&]share_id=...（share_id 服务端生成）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "{\"id\":1001}")
	public String scene;

	@Schema(
			description = "小程序码宽度 px；可选，正整数生效；缺失/非整数/≤0 一律按微信默认 430",
			example = "430")
	public String width;
}