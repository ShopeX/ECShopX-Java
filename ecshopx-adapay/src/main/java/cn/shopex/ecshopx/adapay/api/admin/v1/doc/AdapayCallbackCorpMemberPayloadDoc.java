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

package cn.shopex.ecshopx.adapay.api.admin.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；data JSON（type=corp_member.succeeded / corp_member.failed）中代码实际读取的键。
 * 证据：{@code AdapayCallbackCorpMemberHandler.succeeded}。
 */
@Schema(
		name = "AdapayCallbackCorpMemberPayload",
		description = "corp_member.* 事件 data JSON 字符串内的已读取键（示例值按代码生成规则/表结构构造）")
public class AdapayCallbackCorpMemberPayloadDoc {

	@Schema(
			description =
					"开户会员主键（adapay_member.id，长整型解析）；≤0 或不可解析报 400「member_id 无效」；"
							+ "按 id+app_id 查无报 422「开户会员信息不存在」",
			example = "1")
	public String member_id;

	@Schema(description = "AdaPay 应用 id（与 adapay_member.app_id 匹配）", example = "app-x")
	public String app_id;

	@Schema(
			description =
					"审核状态（原样写入 adapay_member/adapay_corp_member.audit_state）；D/E 时置"
							+ " adapay_member.is_created=true；E 时另读取 settle_account_id 并回写结算账户、"
							+ "经销商运营者开户时间",
			example = "E")
	public String audit_state;

	@Schema(description = "审核描述（原样写入 audit_desc）", example = "审核通过")
	public String audit_desc;

	@Schema(
			description = "AdaPay 结算账户 id；仅 audit_state=E 时读取，写入 adapay_settle_account.settle_account_id",
			example = "local_settle_account")
	public String settle_account_id;
}