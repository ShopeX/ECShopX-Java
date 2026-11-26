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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code JurisdictionController#sysuser} 经
 * {@code OpenapiRequestParams#mergeString} 读取的全部 key（8 个）。
 */
@Schema(
		name = "EcxJurisdictionSysuserBody",
		description =
				"系统账号同步参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）。"
						+ " 无必填硬校验：缺省按空串参与查询/落库")
public class EcxJurisdictionSysuserBodyDoc {

	@Schema(
			description = "管理员手机号：用于定位 operator_type=admin 的账号并校验其 company_id 与鉴权租户一致；不匹配返回「配置错误」",
			example = "13800000000")
	public String shopexid;

	@Schema(description = "目标账号手机号（operators.mobile 定位/新建）", example = "13900000000")
	public String mobile;

	@Schema(
			description =
					"目标账号类型：admin 走开通流程（新建 admin + 公司 + demo license + 异步事件）；"
							+ "其他值（如 staff）走轻量新建/更新",
			example = "staff")
	public String operator_type;

	@Schema(description = "登录名（仅轻量新建时落库）")
	public String login_name;

	@Schema(description = "姓名（新建落库；更新时非空白才覆盖）", example = "张三")
	public String username;

	@Schema(
			description =
					"密码：更新时非空白才覆盖（库原样写入）；新建时忽略入参、落库随机密码"
							+ "（admin 开通为随机明文 UUID 的 BCrypt；轻量新建为随机 BCrypt）")
	public String password;

	@Schema(description = "企业 ID（admin 开通流程写入 operators.eid 与 companys.eid）")
	public String eid;

	@Schema(description = "Passport UID（admin 开通流程写入 operators 与 companys）")
	public String passport_uid;
}