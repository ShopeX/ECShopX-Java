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
 * 仅 OpenAPI 文档；data 数组元素（20 个被读取键），来源
 * {@code OpenapiThirdApiV2MemberCreateService#validateAndNormalize}（同步校验）
 * + {@code createDetailParamsFromPhpFormData}（异步 job 消费）。
 * 同步校验失败整批拒绝（先全部校验后入队）；占用/推荐人/导购/等级/卡号类校验在异步 job 内执行。
 */
@Schema(
		name = "EcxMemberBatchCreateItem",
		description =
				"批量创建会员单条参数（同步校验：手机号/来源/推荐人导购格式/unionid 类型/status/sex/"
						+ "birthday/habbit/枚举/email；异步校验：手机号占用 E5502、推荐人 E5511、导购 E5601/E5612、"
						+ "等级 E5531、卡号 E5522——异步失败不影响本接口同步响应）。"
						+ "tag_ids 不被读取（标签仅支持 tag_names）。")
public class EcxMemberBatchCreateItemDoc {

	@Schema(
			description =
					"会员手机号（明文）；必填非空白（E5001「手机号必填」）；trim 后须 ^1[3456789][0-9]{9}$"
							+ "（格式错同步报 E5502「会员手机号已存在」——PHP 旧契约文案；占用校验在异步 job）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800138000")
	public String mobile;

	@Schema(
			description = "来源渠道；仅 default/openapi（空白默认 openapi；其他值 E5001「来源渠道填写错误」）",
			example = "openapi")
	public String source_from;

	@Schema(
			description = "推荐人手机号；非空须手机号格式（E5001「推荐人手机号填写错误」）；查无校验在异步 job（E5511）",
			example = "13800000000")
	public String inviter_mobile;

	@Schema(
			description = "绑定导购手机号；非空须手机号格式（E5001「绑定导购手机号填写错误」）；导购查无/已绑定校验在异步 job（E5601/E5612）",
			example = "13800000000")
	public String salesperson_mobile;

	@Schema(
			description = "微信 unionid；须字符串或 null（数字/布尔/对象 E5001「微信unionid填写错误」）；异步写 members_associations")
	public String union_id;

	@Schema(
			description = "会员状态；0 禁用/1 启用（空/null 默认 1；其他值 E5001「会员状态填写错误」）",
			example = "1")
	public String status;

	@Schema(
			description = "标签名（逗号分隔字符串，逐段 trim）；仅字符串形态生效（数组/其他类型忽略）；异步按名建标签并关联")
	public String tag_names;

	@Schema(
			description = "会员卡号；空白时异步自动生成 12 位随机码；他人占用异步报 E5522（不影响同步响应）",
			example = "card")
	public String card_code;

	@Schema(
			description =
					"会员等级 ID；非负整数（空/null 默认 -1 → 异步取租户默认等级；负数/非数字 E5001「会员等级ID填写错误」）；"
							+ "指定等级查无异步报 E5531",
			example = "1")
	public String grade_id;

	@Schema(description = "姓名/昵称（异步加密落库；缺省空串）", example = "张三")
	public String username;

	@Schema(description = "头像 URL（缺省空串；示例为格式演示值）", example = "https://example.test/key.jpg")
	public String avatar;

	@Schema(
			description = "性别；仅 0 可成功（空/null 默认 0；非 0 同步报 E5001「性别填写错误」，1/2 不可传）",
			example = "0")
	public String sex;

	@Schema(
			description = "生日；可缺省；非空输入校验接受 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss 两种形态，"
					+ "但异步落库前按 ISO 日期重解析——datetime 形态会失败（job 内 E5001「生日格式填写错误」），"
					+ "实际仅 yyyy-MM-dd 可用；非法输入同步 E5001「生日填写错误」（示例为格式演示值）",
			example = "1990-01-01")
	public String birthday;

	@Schema(
			description =
					"爱好列表（数组或 JSON 数组字符串）；元素仅保留 name（非空字符串）/ischecked"
							+ "（布尔或 \"true\"/\"false\"），否则 E5001「爱好填写错误」；元素键值示例为格式演示",
			example = "[{\"name\":\"阅读\",\"ischecked\":true}]")
	public List<EcxMemberInfoUpdateBodyDoc.HabbitItemDoc> habbit;

	@Schema(description = "学历枚举 0-4（空/null 默认 4；越界/非数字 E5001「学历填写错误」）", example = "4")
	public String edu_background;

	@Schema(description = "年收入枚举 0-4（空/null 默认 4；越界/非数字 E5001「年收入填写错误」）", example = "4")
	public String income;

	@Schema(description = "行业枚举 0-12（空/null 默认 12；越界/非数字 E5001「行业填写错误」）", example = "12")
	public String industry;

	@Schema(
			description = "邮箱；非空须邮箱格式（E5001「email填写错误」）；示例为格式演示值",
			example = "zhangsan@example.test")
	public String email;

	@Schema(description = "地址（缺省空串，无格式校验）")
	public String address;

	@Schema(description = "备注（缺省空串，无格式校验）")
	public String remarks;
}