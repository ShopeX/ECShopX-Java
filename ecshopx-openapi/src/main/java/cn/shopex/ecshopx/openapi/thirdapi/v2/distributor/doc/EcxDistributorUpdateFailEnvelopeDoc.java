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
 * <li>E5001：店铺号参数错误 / 联系方式参数错误 / 是否支持自提参数错误 / 是否支持快递参数错误 /
 *     是否自动同步总部商品参数错误 / 是否开启同城配参数错误 / 是否默认店铺参数错误 /
 *     店铺状态参数错误 / 经度参数错误 / 纬度参数错误；</li>
 * <li>E5401「店铺找不到」（shop_code 缺失/空/定位查无/更新命中 0 行）；</li>
 * <li>E5402：店铺号已存在 / 店铺名称已存在 / 店铺手机号已存在。</li>
 * </ul>
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxDistributorUpdateFailEnvelope")
public class EcxDistributorUpdateFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5001 参数错误；E5401 店铺找不到；E5402 重复；E4004 未授权", example = "E5402")
	public String code;

	@Schema(description = "错误说明", example = "店铺名称已存在")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}