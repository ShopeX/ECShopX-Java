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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；data 单键 {list}，来源
 * {@code OpenapiThirdApiV2MemberRechargeRuleListService}：
 * 固定 page=1/page_size=20 查 deposit_recharge_rule（create_time 降序），
 * 行复用创建接口 5 键格式；无规则时服务返回 null → data=null。
 */
@Schema(name = "EcxRechargeRuleGetData")
public class EcxRechargeRuleGetDataDoc {

	@Schema(
			description = "面额规则数组（create_time 降序，固定取前 20 条——面额上限 14 故实际全量；行 5 键同创建接口）")
	public List<EcxRechargeRuleAddDataDoc> list;
}