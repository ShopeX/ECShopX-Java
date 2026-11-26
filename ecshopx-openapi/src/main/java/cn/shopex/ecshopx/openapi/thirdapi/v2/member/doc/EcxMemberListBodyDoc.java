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

/**
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，18 键与 query 同名双通道，来源
 * {@code OpenapiMemberV2ListHandlerParams#mergePresentKeys}（按键存在性合并，body 含键取原值否则 query 原样；
 * card_code 存在时优先，否则 user_card_code 重命名为 card_code）。
 * 过滤构建见 {@code OpenapiMemberV2ListFilterBuilder}，分页解析见
 * {@code OpenapiThirdApiV2MemberListParams#resolve}（E5002）。
 */
@Schema(
		name = "EcxMemberListBody",
		description =
				"会员批量查询参数（18 键，body 含键优先否则 query 同名键，均按键存在性生效）。"
						+ " 数值键宽松解析（非法/空→0，小数截断）；user_id 降序，total_count 为 DISTINCT 会员数。")
public class EcxMemberListBodyDoc {

	@Schema(
			description = "页码；默认 1。PHP 式解析后 max(…,1)",
			example = "1")
	public String page;

	@Schema(
			description =
					"每页数量；默认 20。原始解析值 ≥500 报 E5002「每页的大小最大为500！」（500 本身也报错）；"
							+ "钳制后生效 [20,499]",
			example = "20")
	public String page_size;

	@Schema(
			description = "会员手机号（明文）；键存在且非空白时 trim 加密后等值过滤；空串/空白忽略；查无返回空列表不报错",
			example = "13800138000")
	public String mobile;

	@Schema(
			description = "来源渠道等值过滤（原样字符串不 trim；空串忽略；无枚举校验）",
			example = "openapi")
	public String source_from;

	@Schema(
			description =
					"推荐人手机号；键存在且非空白时按加密手机号查推荐人（可多行）→ inviter_id IN 过滤；"
							+ "**查无推荐人时不过滤（返回全量）**",
			example = "13800000000")
	public String inviter_mobile;

	@Schema(
			description =
					"绑定导购手机号；键存在即按手机号查导购：命中 → 按绑定关系过滤，且行内 salesperson 统一显示该导购；"
							+ "**查无（id=0）时不过滤（返回全量）**。注意：过滤 SQL 引用 rel_salesperson 别名但 FROM 未 JOIN 该表，"
							+ "命中导购时触发 SQL 错误 → E0001「系统异常」（源码缺陷，如实记录）",
			example = "13800000000")
	public String salesperson_mobile;

	@Schema(
			description =
					"会员状态过滤；宽松整数（非法→0）：0→查禁用、1→查启用、其他值原样作为 disabled 等值"
							+ "（如 2 查 disabled=2，通常空结果）",
			example = "1")
	public String status;

	@Schema(
			description = "标签 ID 过滤；宽松整数；>0 时按 members_rel_tags 关联过滤；≤0 不过滤；优先于 tag_name",
			example = "1")
	public String tag_id;

	@Schema(
			description = "标签名过滤（tag_id 键缺席时生效）；trim 等值查标签：命中按其 id 过滤；查无（id=0）不过滤",
			example = "聚合测试类目")
	public String tag_name;

	@Schema(
			description = "是否消费过过滤（members_info.have_consume 等值）；宽松整数（非法→0 查未消费）",
			example = "0")
	public String have_consume;

	@Schema(
			description = "会员卡号等值过滤（members.user_card_code）；空串忽略；键存在时优先于 user_card_code",
			example = "card")
	public String card_code;

	@Schema(
			description = "会员卡号别名；card_code 键缺席时本键值重命名为 card_code 生效（同语义）",
			example = "card")
	public String user_card_code;

	@Schema(
			description = "等级 ID 等值过滤；宽松整数（非法→0 查 grade_id=0，通常空结果）；优先于 grade_name",
			example = "1")
	public String grade_id;

	@Schema(
			description =
					"等级名过滤（grade_id 键缺席时生效）；trim 等值查 membercard_grade：命中按其 id 过滤；"
							+ "查无（id=0）按 grade_id=0 过滤（通常空结果）",
			example = "普通会员")
	public String grade_name;

	@Schema(
			description =
					"付费等级 ID 过滤；宽松整数；>0 时按 vip_grade_rel_user 关联且 end_date > 当前时间（仅有效付费会员）；"
							+ "≤0 不过滤；优先于 vip_grade_name",
			example = "2")
	public String vip_grade_id;

	@Schema(
			description =
					"付费等级名过滤（vip_grade_id 键缺席时生效）；trim 等值查 kaquan_vip_grade"
							+ "**且 is_disabled=true（源码条件如此，仅匹配已停用等级，疑似缺陷）**；"
							+ "命中按其 id 过滤；查无（id=0）不过滤",
			example = "Gold")
	public String vip_grade_name;

	@Schema(
			description =
					"注册起始时间（created ≥）；键存在且非空白时须 yyyy-MM-dd HH:mm:ss（系统时区）；"
							+ "纯数字时间戳/其他格式报 E4002「时间格式有误」；空白忽略（示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String start_date;

	@Schema(
			description = "注册截止时间（created ≤）；解析同 start_date（示例为格式演示值）",
			example = "2026-12-31 23:59:59")
	public String end_date;
}