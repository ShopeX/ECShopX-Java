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
 * 仅 OpenAPI 文档；data JSON（type=corp_member_update.succeeded / corp_member_update.failed）中
 * 代码实际读取的键。证据：{@code AdapayCallbackCorpMemberUpdateHandler.succeeded}。
 * 银行卡等修改明细取自本地 adapay_member_update_log.data（密文解密），不从回调 data 读取。
 */
@Schema(
		name = "AdapayCallbackCorpMemberUpdatePayload",
		description = "corp_member_update.* 事件 data JSON 字符串内的已读取键（示例值按代码常量/测试夹具构造）")
public class AdapayCallbackCorpMemberUpdatePayloadDoc {

	@Schema(
			description =
					"审核状态；入站映射：S→D（结算账户重建流程）、F→B（修改审核失败）、其余原样落库；"
							+ "写入 adapay_member_update_log.audit_state",
			example = "S")
	public String audit_state;

	@Schema(description = "AdaPay 应用 id（与修改记录 app_id 匹配）", example = "app-x")
	public String app_id;

	@Schema(
			description =
					"开户会员主键字符串（与修改记录 member_id 匹配，取最新一条）；查无记录报 422"
							+ "「企业用户修改记录不存在」；D 分支查无会员报 422「开户会员信息不存在」",
			example = "1")
	public String member_id;

	@Schema(description = "审核描述（写入修改记录 audit_desc）", example = "审核通过")
	public String audit_desc;
}