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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷（员工账号单对象，25 键）。
 *
 * <p>字段来源：{@code OperatorsMapper.xml operatorColumns}（25 列）经
 * {@code OpenapiThirdApiV1JurisdictionGetuserService#toPhpOperatorData}：camelCase 别名归一为 snake_case、
 * shop_ids/distributor_ids JSON 列解码、缺失键补 null 占位。
 */
@Schema(name = "EcxJurisdictionGetuserData", description = "员工账号信息（operators 表 25 字段）")
public class EcxJurisdictionGetuserDataDoc {

	@Schema(description = "操作员 ID", example = "5")
	public Long operator_id;

	@Schema(description = "手机号", example = "13800000000")
	public String mobile;

	@Schema(description = "登录名；缺失补 null", nullable = true)
	public String login_name;

	@Schema(description = "账号类型（本接口恒 staff）", example = "staff")
	public String operator_type;

	@Schema(description = "密码（库原值）；缺失补 null", nullable = true)
	public String password;

	@Schema(description = "租户/公司 ID", example = "100")
	public Long company_id;

	@Schema(description = "是否禁用", example = "false")
	public Boolean is_disable;

	@Schema(description = "经销商 ID；缺失补 null", nullable = true)
	public Long merchant_id;

	@ArraySchema(
			arraySchema =
					@Schema(
							description =
									"关联店铺列表：JSON 列解码为 [{distributor_id, name}]；非法 JSON 为 []；列缺失为 null",
							nullable = true))
	public List<DistributorRefDoc> distributor_ids;

	@Schema(
			description =
					"关联门店 ID 集合：JSON 列解码（数组或对象原样，元素通常为门店 ID）；非法/非集合为 []；列缺失为 null",
			nullable = true,
			example = "[]")
	public Object shop_ids;

	@Schema(description = "员工姓名；缺失补 null", nullable = true)
	public String username;

	@Schema(description = "头像；缺失补 null", nullable = true)
	public String head_portrait;

	@Schema(description = "区域权限 ID", example = "0")
	public Long regionauth_id;

	@Schema(description = "联系人；缺失补 null", nullable = true)
	public String contact;

	@Schema(description = "分账信息（库原样字符串）；缺失补 null", nullable = true)
	public String split_ledger_info;

	@Schema(description = "Adapay 开户时间（库原样字符串）；缺失补 null", nullable = true)
	public String adapay_open_account_time;

	@Schema(description = "上级经销商 ID（字符串）；缺失补 null", nullable = true)
	public String dealer_parent_id;

	@Schema(description = "是否经销商主账号；缺失补 null", nullable = true)
	public Boolean is_dealer_main;

	@Schema(description = "是否经销商主账号（distributor）；缺失补 null", nullable = true)
	public Boolean is_distributor_main;

	@Schema(description = "Shopex 绑定账号", nullable = true)
	public String shopex_bind_account;

	@Schema(description = "企业 ID；缺失补 null", nullable = true)
	public String eid;

	@Schema(description = "Passport UID；缺失补 null", nullable = true)
	public String passport_uid;

	@Schema(description = "创建时间（Unix 秒）", nullable = true)
	public Integer created;

	@Schema(description = "更新时间（Unix 秒）；缺失补 null", nullable = true)
	public Integer updated;

	@Schema(description = "是否经销商（merchant）主账号；缺失补 null", nullable = true)
	public Boolean is_merchant_main;

	@Schema(name = "EcxJurisdictionGetuserDistributorRef", description = "关联店铺引用")
	public static class DistributorRefDoc {

		@Schema(description = "店铺 ID", example = "5")
		public Object distributor_id;

		@Schema(description = "店铺名称", nullable = true)
		public String name;
	}
}