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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null：
 * <ul>
 * <li>E5001：管理分类必填 / 商品名称必填 / 请上传商品图片 / 运费模板必填 / 品牌必填 / 商品分类ID必填；</li>
 * <li>E5100（GOODS_ERROR）：商品图片格式错误 / 商品分类格式错误 / 请填写正确的规格数据 /
 *     请选择销售分类 / 拼团名称【…】的活动价格大于商品价格，请检查后再提交！ /
 *     落库链路其他异常 message 透传（含数字解析失败等运行时异常信息）。</li>
 * </ul>
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxItemEntityAddFailEnvelope")
public class EcxItemEntityAddFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 必填缺失；E5100 格式/规格/分类/落库失败；E4004 未授权", example = "E5100")
	public String code;

	@Schema(description = "错误说明", example = "请填写正确的规格数据")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}