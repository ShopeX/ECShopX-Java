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
 * 仅 OpenAPI 文档；更新企业用户（已开户走 AdaPay CorpMember.update）请求字段
 * （{@code @FlexibleBody Map}，不改运行时签名）。
 * 证据：{@code CorpMemberController.update} 读取的 18 个 body 键 +
 * {@code AdapayCorpMemberUpdateService.validateUpdateActionRules / checkUpdateParams} 校验。
 * 附件走 multipart 独立 part（attach_file，可选），不在本 schema。
 */
@Schema(
		name = "AdapayCorpMemberUpdateBody",
		description =
			"更新企业用户字段（JSON 或 form 双通道；multipart 时字段放 form part）。"
					+ "所有值 toString 后 trim；area 在 Controller 侧支持数组（Collection 按逗号 join）")
public class AdapayCorpMemberUpdateBodyDoc {

	@Schema(
			description =
					"开户会员主键（adapay_member.id）；必填正整数，空/非数字/≤0 报「开户id不能为空」；"
							+ "查无报「开户信息不存在」（embedded 400）；app_id 空报「支付应用未配置」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1")
	public String member_id;

	@Schema(
			description = "企业名称；必填非空白，超 30 字符（码点）截断；bank_acct_type=1 时须与 card_name 一致",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "示例科技有限公司")
	public String name;

	@Schema(
			description =
					"地区，「省code,市code」两段（adapay_regions 四位码）；必填；字符串或数组形态"
							+ "（数组元素逗号 join）；格式错误报「地区数据格式错误」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "0011,1100")
	public String area;

	@Schema(
			description = "银行账户类型：1-对公；2-对私；必填，其余值报「银行账户类型错误」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			allowableValues = {"1", "2"},
			example = "1")
	public String bank_acct_type;

	@Schema(
			description = "营业执照号（统一社会信用代码）；必填非空白",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "91110000123456789X")
	public String social_credit_code;

	@Schema(
			description = "商户有效日期（营业执照有效期）；必填非空白；YYYYMMDD 形态（长期 20991231）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "20991231")
	public String social_credit_code_expires;

	@Schema(
			description = "经营范围；必填非空白；超 500 字符（码点）截断",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "软件开发与销售")
	public String business_scope;

	@Schema(
			description = "法人姓名；必填非空白；超 20 字符（码点）截断",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "李四")
	public String legal_person;

	@Schema(
			description =
					"法人身份证号；必须 18 位且匹配身份证正则，否则报「法人身份证号码必须是18位」/「法人身份证号码格式错误」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "110101199001011234")
	public String legal_cert_id;

	@Schema(
			description = "法人身份证有效期；必填非空白；YYYYMMDD 形态（长期 20991231）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "20991231")
	public String legal_cert_id_expires;

	@Schema(
			description = "法人手机号；长度必须 11（本接口仅校验长度，不校验纯数字），否则报「法人手机号必须是11位」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800138000")
	public String legal_mp;

	@Schema(
			description = "企业地址；必填非空白；超 60 字符（码点）截断",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "北京市东城区示例路1号")
	public String address;

	@Schema(description = "邮编；可选；超 6 字符（码点）截断，空值按空串", example = "100000")
	public String zip_code;

	@Schema(description = "企业电话；可选（拼写 telphone）", example = "010-12345678")
	public String telphone;

	@Schema(description = "企业邮箱；可选；本接口不做格式校验（透传出站与修改日志）", example = "a@b.com")
	public String email;

	@Schema(
			description = "结算银行代码（adapay_bank_codes.bank_code）；必填；查无报「请选择正确的结算银行卡所属银行」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "01020000")
	public String bank_code;

	@Schema(
			description = "银行卡号；必填非空白",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "622200")
	public String card_no;

	@Schema(
			description =
					"银行卡户名；必填非空白（本接口无 20 码点上限校验）；bank_acct_type=1（对公）时必须与 name 一致，"
							+ "否则报「银行卡对应的户名，必须与企业名称一致」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "示例科技有限公司")
	public String card_name;
}