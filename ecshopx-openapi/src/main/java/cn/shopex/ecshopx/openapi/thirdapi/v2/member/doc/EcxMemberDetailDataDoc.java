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
 * 仅 OpenAPI 文档；data 会员详情行（27 固定键），来源
 * {@code selectMemberV2ListForOpenapi}（22 SQL 列，LIMIT 1）+
 * {@code OpenapiMemberV2ListEnrichmentService#enrichDetailRaw}（inviter/salesperson/tags/vip_grades，
 * inviter_id 移除）+ {@code appendPointAndDeposit}（point/deposit）。
 * 注意：detail 路径**不执行**列表路径的 handleDataToList——created/updated 保持 Unix 秒整型、
 * disabled 原样透出（无 status 反演键）、habbit 保持库内 JSON 字符串（不解析为数组）。
 */
@Schema(name = "EcxMemberDetailData")
public class EcxMemberDetailDataDoc {

	@Schema(description = "会员 user_id（整型）", example = "50")
	public Long user_id;

	@Schema(description = "手机号（密文解密明文）", example = "13800138000")
	public String mobile;

	@Schema(description = "来源渠道（default/openapi 等库值）", example = "openapi")
	public String source_from;

	@Schema(
			description = "是否禁用（库内原样布尔/0|1；**detail 不做 status 反演**，区别于会员列表）",
			example = "false")
	public Boolean disabled;

	@Schema(description = "创建时间（**Unix 秒整型原样**，detail 不格式化；示例为格式演示值）", example = "1735689600")
	public Integer created;

	@Schema(description = "更新时间（Unix 秒整型原样；示例为格式演示值）", example = "1735689600")
	public Integer updated;

	@Schema(description = "备注（库值，可为 null）", nullable = true)
	public String remarks;

	@Schema(description = "等级 ID（整型，可为 null）", nullable = true, example = "1")
	public Long grade_id;

	@Schema(description = "会员卡号（库值，可为 null）", nullable = true, example = "card")
	public String user_card_code;

	@Schema(description = "姓名/昵称（密文解密明文；无 info 行为 null）", nullable = true, example = "张三")
	public String username;

	@Schema(description = "头像 URL（库值，可为 null）", nullable = true)
	public String avatar;

	@Schema(description = "性别（整型 0 未知/1 男/2 女；无 info 行为 null）", nullable = true, example = "0")
	public Integer sex;

	@Schema(description = "生日（yyyy-MM-dd 字符串；可为 null/空串；示例为格式演示值）", nullable = true, example = "1990-01-01")
	public String birthday;

	@Schema(
			description = "爱好（**库内 JSON 字符串原样**，detail 不解析为数组；元素 name/ischecked；可为 null）",
			nullable = true,
			example = "[{\"name\":\"阅读\",\"ischecked\":true}]")
	public String habbit;

	@Schema(description = "学历（库内字符串 \"0\"-\"4\"；可为 null）", nullable = true, example = "4")
	public String edu_background;

	@Schema(description = "年收入（库内字符串 \"0\"-\"4\"；可为 null）", nullable = true, example = "4")
	public String income;

	@Schema(description = "行业（库内字符串 \"0\"-\"12\"；可为 null）", nullable = true, example = "12")
	public String industry;

	@Schema(description = "邮箱（库值，可为 null）", nullable = true)
	public String email;

	@Schema(description = "地址（库值，可为 null）", nullable = true)
	public String address;

	@Schema(description = "微信 unionid（members_associations 关联；无关联为 null）", nullable = true)
	public String unionid;

	@Schema(description = "等级名称（membercard_grade JOIN；查无为 null）", nullable = true, example = "普通会员")
	public String grade_name;

	@Schema(description = "推荐人（id=inviter_id 原值；name/mobile 解密回查，无推荐人 id=0 其余空串；inviter_id 顶层键已移除）")
	public InviterDoc inviter;

	@Schema(description = "绑定导购（work_wechat_rel is_bind=true 回查；无绑定 id=0 其余空串）")
	public SalespersonDoc salesperson;

	@Schema(description = "标签数组（members_rel_tags 关联；无标签空数组）")
	public List<TagDoc> tags;

	@Schema(
			description = "有效付费等级数组（vip_grade_rel_user 关联且 end_date ≥ 当前时间；过期剔除；无则空数组）")
	public List<VipGradeDoc> vip_grades;

	@Schema(description = "可用积分（整型；point_member.point，无账户 0）", example = "10")
	public Integer point;

	@Schema(description = "储值汇总（have 当前余额/total 累计成功充值，均元 2 位小数字符串）")
	public DepositDoc deposit;

	/** 推荐人 brief（3 键）。 */
	@Schema(name = "EcxMemberDetailInviter")
	public static class InviterDoc {

		@Schema(description = "推荐人 user_id（整型；无推荐人 0）", example = "0")
		public Long id;

		@Schema(description = "推荐人姓名（解密；查无空串）", example = "")
		public String name;

		@Schema(description = "推荐人手机号（解密；查无空串）", example = "")
		public String mobile;
	}

	/** 绑定导购 brief（3 键）。 */
	@Schema(name = "EcxMemberDetailSalesperson")
	public static class SalespersonDoc {

		@Schema(description = "导购 salesperson_id（整型；无绑定 0）", example = "0")
		public Long id;

		@Schema(description = "导购姓名（查无空串）", example = "")
		public String name;

		@Schema(description = "导购手机号（查无空串）", example = "")
		public String mobile;
	}

	/** 标签（2 键）。 */
	@Schema(name = "EcxMemberDetailTag")
	public static class TagDoc {

		@Schema(description = "标签 tag_id（整型）", example = "1")
		public Long id;

		@Schema(description = "标签名", example = "聚合测试类目")
		public String name;
	}

	/** 有效付费等级（4 键；grade_nme 为源码原样键名，疑似 grade_name 拼写缺陷）。 */
	@Schema(name = "EcxMemberDetailVipGrade")
	public static class VipGradeDoc {

		@Schema(description = "付费等级 vip_grade_id（整型）", example = "2")
		public Long id;

		@Schema(description = "付费类型（vip_grade_rel_user.vip_type 原样；null 空串）", example = "")
		public String type;

		@Schema(description = "到期时间（Unix 秒，整型；仅未过期行返回；示例为格式演示值）", example = "1735689600")
		public Long end_date;

		@Schema(description = "付费等级名称（**键名为 grade_nme，源码拼写如此**；查无空串）", example = "Gold")
		public String grade_nme;
	}

	/** 储值汇总（2 键）。 */
	@Schema(name = "EcxMemberDetailDeposit")
	public static class DepositDoc {

		@Schema(description = "当前储值余额（元，字符串 2 位小数；分 HALF_UP 除 100）", example = "0.00")
		public String have;

		@Schema(description = "累计成功充值（元，字符串 2 位小数）", example = "0.00")
		public String total;
	}
}