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
 * 仅 OpenAPI 文档；会员列表行（26 固定键），来源
 * {@code selectMemberV2ListForOpenapi}（22 SQL 列，inviter_id 移除）+
 * {@code enrichList}（inviter/salesperson/tags/vip_grades + handleDataToList：
 * created/updated 格式化、disabled→status、habbit 解析、枚举归一）。
 * 无 point/deposit（区别于 detail 行）。
 */
@Schema(name = "EcxMemberListRow")
public class EcxMemberListRowDoc {

	@Schema(description = "会员 user_id（整型，降序排序键）", example = "50")
	public Long user_id;

	@Schema(description = "手机号（密文解密明文）", example = "13800138000")
	public String mobile;

	@Schema(description = "来源渠道（库值）", example = "openapi")
	public String source_from;

	@Schema(
			description = "会员状态（整型反演：disabled 0→1 启用/1→0 禁用/其他→null；disabled 键已移除）",
			nullable = true,
			example = "1")
	public Integer status;

	@Schema(
			description = "创建时间（yyyy-MM-dd HH:mm:ss，系统时区；非数值为空串；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String created;

	@Schema(
			description = "更新时间（yyyy-MM-dd HH:mm:ss，系统时区；示例为格式演示值）",
			example = "2026-01-01 00:00:00")
	public String updated;

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

	@Schema(
			description = "性别（归一整数枚举：0 未知/1 男/2 女；库内中文标签反查键；无法识别为 null）",
			nullable = true,
			example = "0")
	public Integer sex;

	@Schema(description = "生日（yyyy-MM-dd 字符串；可为 null/空串；示例为格式演示值）", nullable = true, example = "1990-01-01")
	public String birthday;

	@Schema(
			description = "爱好数组（库内 JSON 解析；元素 name/ischecked（\"true\"/\"false\" 字符串归一布尔）；解析失败空数组）")
	public List<EcxMemberInfoUpdateBodyDoc.HabbitItemDoc> habbit;

	@Schema(
			description = "学历（归一整数枚举：0 硕士及以上/1 本科/2 大专/3 高中·中专及以下/4 其他；无法识别为 null）",
			nullable = true,
			example = "4")
	public Integer edu_background;

	@Schema(
			description = "年收入（归一整数枚举：0 5万以下/1 5万~15万/2 15万~30万/3 30万以上/4 其他；无法识别为 null）",
			nullable = true,
			example = "4")
	public Integer income;

	@Schema(
			description =
					"行业（归一整数枚举 0-12：0 金融/1 计算机/2 媒体/3 政府/4 房地产/5 咨询/6 加工制造/"
							+ "7 教育培训/8 医疗保健/9 运输物流/10 零售贸易/11 旅游/12 其他；无法识别为 null）",
			nullable = true,
			example = "12")
	public Integer industry;

	@Schema(description = "邮箱（库值，可为 null）", nullable = true)
	public String email;

	@Schema(description = "地址（库值，可为 null）", nullable = true)
	public String address;

	@Schema(description = "微信 unionid（关联；无关联为 null）", nullable = true)
	public String unionid;

	@Schema(description = "等级名称（JOIN；查无为 null）", nullable = true, example = "普通会员")
	public String grade_name;

	@Schema(description = "推荐人（id/name/mobile 3 键；无推荐人 id=0 其余空串）")
	public EcxMemberDetailDataDoc.InviterDoc inviter;

	@Schema(
			description = "绑定导购（id/name/mobile 3 键；无绑定 id=0 其余空串；salesperson_mobile 过滤时统一显示该导购）")
	public EcxMemberDetailDataDoc.SalespersonDoc salesperson;

	@Schema(description = "标签数组（id/name；无标签空数组）")
	public List<EcxMemberDetailDataDoc.TagDoc> tags;

	@Schema(
			description = "有效付费等级数组（id/type/end_date/grade_nme——源码键名拼写如此；仅未过期行；无则空数组）")
	public List<EcxMemberDetailDataDoc.VipGradeDoc> vip_grades;
}