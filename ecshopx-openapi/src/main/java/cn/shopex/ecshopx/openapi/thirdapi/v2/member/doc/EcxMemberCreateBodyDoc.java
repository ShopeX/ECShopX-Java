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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，20 键 mergeAll（body 含键取原值，否则 query mergeString trim）
 * + habbit 仅 body 键，来源 {@code OpenapiMemberV2CreateHandlerParams}。
 * 同步创建（区别于 batch_create 异步），校验与落库见
 * {@code OpenapiThirdApiV2MemberCreateService#executeOpenapiCreateDetail}。
 */
@Schema(
		name = "EcxMemberCreateBody",
		description =
				"创建会员（单个，同步）参数（21 键，body 含键优先否则 query 同名键；habbit 仅 body）。"
						+ " 同步执行完整创建链：members/members_info/标签关联/导购绑定/unionid 关联 + 新客标签对账，"
						+ "任一步失败整体回滚。tag_ids 被接收但忽略（标签仅支持 tag_names，且须已存在）。")
public class EcxMemberCreateBodyDoc {

	@Schema(
			description =
					"会员手机号（明文）；必填非空白（E5001「手机号必填」）；trim 后须 ^1[3456789][0-9]{9}$"
							+ "（格式错 E5502「会员手机号已存在」——PHP 旧契约文案）；已被占用同报 E5502",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800138000")
	public String mobile;

	@Schema(
			description = "推荐人手机号；非空须手机号格式（E5001「推荐人手机号填写错误」）；查无 E5511「会员的推荐人找不到」",
			example = "13800000000")
	public String inviter_mobile;

	@Schema(
			description =
					"绑定导购手机号；非空须手机号格式（E5001「绑定导购手机号填写错误」）；"
							+ "导购查无 E5601「导购找不到」；已绑定 E5612（「会员已与该导购绑定」/「会员已与其他导购绑定」）",
			example = "13800000000")
	public String salesperson_mobile;

	@Schema(
			description = "微信 unionid；须字符串或 null（数字/布尔/对象 E5001「微信unionid填写错误」）；写 members_associations")
	public String union_id;

	@Schema(
			description = "会员状态；0 禁用/1 启用（空/null 默认 1；其他值 E5001「会员状态填写错误」）；disabled=(status!=1)",
			example = "1")
	public String status;

	@Schema(
			description =
					"标签名（逗号分隔字符串，逐段 trim）；仅字符串形态生效；**标签须已存在**——"
							+ "查无报 E5561「标签名为:{名称}, 标签不存在！」（不自动建标签）；示例值取自既有测试标签夹具",
			example = "聚合测试类目")
	public String tag_names;

	@Schema(
			description = "遗留参数：被接收合并但服务不消费（tagIds 恒空列表，标签仅按 tag_names 解析）",
			deprecated = true)
	public String tag_ids;

	@Schema(
			description =
					"会员卡号；空白自动生成 12 位大写随机码（Redis zset 去重）；被其他会员占用 E5522「会员卡号已存在」",
			example = "card")
	public String card_code;

	@Schema(
			description =
					"会员等级 ID（平台内部 grade_id）；非负整数（空/null 默认取租户默认等级；"
							+ "负数/非数字 E5001「会员等级ID填写错误」）；>0 时查无 E5531「会员等级找不到」",
			example = "1")
	public String grade_id;

	@Schema(description = "姓名/昵称（原样落库；缺省空串）", example = "张三")
	public String username;

	@Schema(description = "头像 URL（缺省空串；示例为格式演示值）", example = "https://example.test/key.jpg")
	public String avatar;

	@Schema(
			description = "性别；仅 0 可成功（空/null 默认 0；非 0 报 E5001「性别填写错误」，1/2 不可传）",
			example = "0")
	public String sex;

	@Schema(
			description =
					"生日；可缺省（落库空串）；非空输入校验接受 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss，"
							+ "但落库前按 ISO 日期重解析——datetime 形态失败报 E5001「生日格式填写错误」，"
							+ "实际仅 yyyy-MM-dd 可用；非法输入 E5001「生日填写错误」（示例为格式演示值）",
			example = "1990-01-01")
	public String birthday;

	@Schema(
			description =
					"爱好列表（仅 body 键；数组或 JSON 数组字符串）；元素仅保留 name（非空字符串）/ischecked"
							+ "（布尔或 \"true\"/\"false\"），否则 E5001「爱好填写错误」；落库归一为布尔；元素键值示例为格式演示",
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

	@Schema(
			description = "来源渠道；仅 default/openapi（空白默认 openapi；其他值 E5001「来源渠道填写错误」）",
			example = "openapi")
	public String source_from;
}