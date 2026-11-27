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
 * 仅 OpenAPI 文档；企业用户对象详情 data，对应运行时
 * {@code AdapayCorpMemberReadService.get} 的 LinkedHashMap（键序即下列顺序）。
 * 敏感字段（legal_person/legal_cert_id/legal_mp/card_no）解密后明文返回。
 */
@Schema(name = "AdapayCorpMemberGetData", description = "企业用户对象详情（34 固定键 + 3 条件键）")
public class AdapayCorpMemberGetDataDoc {

	@Schema(description = "企业用户记录 id（adapay_corp_member.id）", example = "1")
	public Long id;

	@Schema(description = "AdaPay 应用 id（租户支付配置，未配置时为空串）", example = "app-x")
	public String app_id;

	@Schema(description = "请求订单号（创建时 yyyyMMddHHmmss+6位随机）", example = "20231127120000123456")
	public String order_no;

	@Schema(description = "开户会员主键（adapay_member.id）", example = "1")
	public Long member_id;

	@Schema(description = "租户公司 id", example = "1")
	public Long company_id;

	@Schema(description = "操作者 id；库值 null 或 0 时返回 null", nullable = true, example = "1")
	public Integer operator_id;

	@Schema(description = "企业名称", example = "示例科技有限公司")
	public String name;

	@Schema(description = "省份编码（四位码）；空白时返回 null", nullable = true, example = "0011")
	public String prov_code;

	@Schema(description = "地区编码（四位码）；null 回退空串", example = "1100")
	public String area_code;

	@Schema(description = "统一社会信用码", example = "91110000123456789X")
	public String social_credit_code;

	@Schema(description = "统一社会信用证有效期（YYYYMMDD，长期 20991231）", example = "20991231")
	public String social_credit_code_expires;

	@Schema(description = "经营范围", example = "软件开发与销售")
	public String business_scope;

	@Schema(description = "法人姓名（密文解密后明文）", example = "李四")
	public String legal_person;

	@Schema(description = "法人身份证号（密文解密后明文，18 位）", example = "110101199001011234")
	public String legal_cert_id;

	@Schema(description = "法人身份证有效期（YYYYMMDD，长期 20991231）", example = "20991231")
	public String legal_cert_id_expires;

	@Schema(description = "法人手机号（密文解密后明文，11 位）", example = "13800138000")
	public String legal_mp;

	@Schema(description = "企业地址", example = "北京市东城区示例路1号")
	public String address;

	@Schema(description = "邮编", example = "100000")
	public String zip_code;

	@Schema(description = "企业电话（拼写 telphone）", example = "010-12345678")
	public String telphone;

	@Schema(description = "企业邮箱", example = "a@b.com")
	public String email;

	@Schema(
			description = "附件存储路径（adapay/{company_id}/{ts}/{md5}.zip）",
			example = "adapay/1/1700000000/0123456789abcdef0123456789abcdef.zip")
	public String attach_file;

	@Schema(description = "附件原始文件名", example = "attach.zip")
	public String attach_file_name;

	@Schema(description = "经销商确认函附件路径（创建流程恒空串）", example = "")
	public String confirm_letter_file;

	@Schema(description = "确认函附件文件名（创建流程恒空串）", example = "")
	public String confirm_letter_file_name;

	@Schema(description = "银行代码（adapay_bank_codes.bank_code）", example = "01020000")
	public String bank_code;

	@Schema(description = "银行账户类型：1-对公；2-对私", example = "1")
	public String bank_acct_type;

	@Schema(description = "银行卡号（密文解密后明文）", example = "622200")
	public String card_no;

	@Schema(description = "银行卡户名（对公时与企业名称一致）", example = "示例科技有限公司")
	public String card_name;

	@Schema(
			description =
					"审核状态：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；"
							+ "E-开户和创建结算账户成功（本地创建初始为 0）",
			example = "E")
	public String audit_state;

	@Schema(description = "审核结果描述", example = "审核通过")
	public String audit_desc;

	@Schema(description = "创建时间（Unix 秒）", example = "1700000000")
	public Integer create_time;

	@Schema(description = "更新时间（Unix 秒）", nullable = true, example = "1700000000")
	public Integer update_time;

	@Schema(description = "固定空串（占位键）", example = "")
	public String disabled_type;

	@Schema(
			description = "银行名称（按 bank_code 反查；条件键：bank_code 非空白才出现，查无为空串）",
			example = "中国工商银行")
	public String bank_name;

	@Schema(
			description =
					"附件下载 URL（条件键：attach_file 非空白才出现；私有链接 3600 秒，local 驱动为直链）",
			example = "https://example.com/storage/adapay/1/1700000000/0123456789abcdef0123456789abcdef.zip")
	public String attach_file_url;

	@Schema(
			description = "确认函下载 URL（条件键：confirm_letter_file 非空白才出现；私有链接 3600 秒）",
			example = "https://example.com/storage/adapay/1/1700000000/confirm.zip")
	public String confirm_letter_file_url;

	@Schema(description = "分账手续费模式，固定「内扣」", example = "内扣")
	public String div_fee_mode;
}