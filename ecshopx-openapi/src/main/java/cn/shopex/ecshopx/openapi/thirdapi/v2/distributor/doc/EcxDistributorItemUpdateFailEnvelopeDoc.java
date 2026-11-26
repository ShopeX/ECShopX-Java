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

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null：
 * <ul>
 * <li>E5001：店铺号参数有误 / 商品货号参数错误 / 店铺商品是否上架参数错误 / 店铺商品是否总部发货参数错误；</li>
 * <li>E5401「店铺找不到」/ E5101「商品找不到」；</li>
 * <li>E5410：是否总部发货, 当前只支持更新spu级商品 / 商品总部发货无法更新库存和价格；</li>
 * <li>E0001：拼团名称【{活动名}】的活动价格大于商品价格，请检查后再提交！（价格守卫 ResourceException）。</li>
 * </ul>
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxDistributorItemUpdateFailEnvelope")
public class EcxDistributorItemUpdateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数错误；E5401 店铺查无；E5101 商品查无；E5410 总部发货限制；E0001 拼团价守卫；E4004 未授权",
			example = "E5410")
	public String code;

	@Schema(description = "错误说明", example = "商品总部发货无法更新库存和价格")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}