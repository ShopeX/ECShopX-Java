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
 * 仅 OpenAPI 文档；PATCH 请求可选 JSON body，14 键 mergeAll（body 含键取原值，否则 query mergeString trim）
 * + habbit 仅 body 键，来源 {@code OpenapiMemberV2UpdateDetailHandlerParams}。
 * 注意：本接口虽为 PATCH，服务层为**全量覆盖**语义（{@code OpenapiThirdApiV2MemberUpdateDetailService}
 * 经 buildRequestData 默认值重建后恒 UPDATE 两表全部动作列），未传键回落默认值而非保留原值。
 */
@Schema(
		name = "EcxMemberInfoUpdateBody",
		description =
				"修改会员信息参数（15 键，body 含键优先否则 query 同名键；habbit 仅 body）。"
						+ " 全量覆盖语义：未传键回落默认值（inviter 清空、status=1 启用、sex=0、"
						+ "username/avatar/email/address 空串、habbit=[]、edu=4/income=4/industry=12），"
						+ "不保留原值。birthday 实际必填（缺失/空串报「生日格式填写错误」）。"
						+ "user_id/remarks 被接收但忽略；sex 传非 0 值恒报错（双重校验缺口）。")
public class EcxMemberInfoUpdateBodyDoc {

	@Schema(
			description = "遗留参数：被接收合并但服务不消费（定位仅按 mobile）",
			deprecated = true)
	public String user_id;

	@Schema(
			description =
					"会员手机号（明文，定位键）；必填，trim 后须 ^1[3456789][0-9]{9}$；"
							+ "缺失/空白/格式错统一报 E5502「会员手机号已存在」（PHP 旧契约文案，实为格式校验）；"
							+ "查无会员 E5501「会员找不到」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "13800138000")
	public String mobile;

	@Schema(
			description =
					"推荐人手机号；键存在且非空须手机号格式（否则 E5001「推荐人手机号填写错误」）；"
							+ "非空非\"0\"时查推荐人（查无 E5511「会员的推荐人找不到」）；"
							+ "未传/空/\"0\" → inviter_id 清 0（全量覆盖，不保留原推荐人）",
			example = "13800138000")
	public String inviter_mobile;

	@Schema(
			description =
					"会员状态；键存在时须 0（禁用）/1（启用）（空串按 1；其他值 E5001「会员状态填写错误」）；"
							+ "未传默认 1——恒写 disabled=(status!=1)，未传会把已禁用会员重新启用；"
							+ "禁用/启用触发新客系统标签对账",
			example = "1")
	public String status;

	@Schema(
			description = "遗留参数：被接收并校验（键存在须字符串或 null，否则 E5001「备注填写错误」），但服务不消费不落库",
			deprecated = true)
	public String remarks;

	@Schema(
			description =
					"姓名/昵称；键存在须字符串或 null（否则 E5001「姓名/昵称填写错误」）；"
							+ "恒写（加密落库），未传写空串（清空原值）",
			example = "张三")
	public String username;

	@Schema(
			description =
					"头像 URL；键存在须字符串或 null（否则 E5001「头像url填写错误」）；恒写，未传写空串（示例为格式演示值）",
			example = "https://example.test/key.jpg")
	public String avatar;

	@Schema(
			description =
					"性别；恒写且仅 0（未知）可成功：键存在传非 0（含 1/2）报 E5001「性别填写错误」"
							+ "（action 层仅许 0，服务层 0/1/2 枚举不可达）；未传/空串按 0",
			example = "0")
	public String sex;

	@Schema(
			description =
					"生日；**实际必填**：缺失/空串报 E5001「生日格式填写错误」（服务层无条件校验）；"
							+ "格式 yyyy-MM-dd（推荐）；yyyy-MM-dd HH:mm:ss 形态可通过输入校验，"
							+ "但落库前按 ISO 日期重解析会失败 → E5001「生日格式填写错误」（实际不可用）；"
							+ "非法格式 E5001「生日填写错误」；写入 ISO 日期并拆 year/month/day（示例为格式演示值）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "1990-01-01")
	public String birthday;

	@Schema(
			description =
					"学历枚举；键存在须 0-4 整数（空串按 -1 报错；否则 E5001「学历填写错误」）；"
							+ "未传默认 4（恒写，覆盖原值）",
			example = "4")
	public String edu_background;

	@Schema(
			description =
					"年收入枚举；键存在须 0-4 整数（否则 E5001「年收入填写错误」）；未传默认 4（恒写，覆盖原值）",
			example = "4")
	public String income;

	@Schema(
			description =
					"行业枚举；键存在须 0-12 整数（否则 E5001「行业填写错误」）；未传默认 12（恒写，覆盖原值）",
			example = "12")
	public String industry;

	@Schema(
			description =
					"邮箱；键存在且非空须邮箱格式（否则 E5001「email填写错误」）；恒写，未传写空串（清空原值）；示例为格式演示值",
			example = "zhangsan@example.test")
	public String email;

	@Schema(
			description =
					"地址；键存在须字符串或 null（否则 E5001「地址填写错误」）；恒写，未传写空串（清空原值）",
			example = "")
	public String address;

	@Schema(
			description =
					"爱好列表（仅 body 键，query 不支持）；数组或 JSON 数组字符串，元素仅保留 name/ischecked 两键："
							+ "name 须非空字符串、ischecked 须布尔或 \"true\"/\"false\"（忽略大小写），否则 E5001「爱好填写错误」；"
							+ "字符串解析失败静默按 []；恒写 JSON，未传写 []（清空原值）；元素键值示例为格式演示",
			example = "[{\"name\":\"阅读\",\"ischecked\":true}]")
	public List<HabbitItemDoc> habbit;

	/** 爱好元素（name/ischecked 两键）。 */
	@Schema(name = "EcxMemberInfoUpdateHabbitItem")
	public static class HabbitItemDoc {

		@Schema(description = "爱好名称；必填非空字符串（示例为格式演示值）", example = "阅读")
		public String name;

		@Schema(description = "是否勾选；布尔或 \"true\"/\"false\" 字符串（落库归一为布尔）", example = "true")
		public Boolean ischecked;
	}
}