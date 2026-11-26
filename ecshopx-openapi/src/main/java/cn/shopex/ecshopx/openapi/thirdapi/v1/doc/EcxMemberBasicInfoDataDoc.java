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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷（会员基础信息单对象，7 键）。
 *
 * <p>字段来源：{@code OpenapiThirdApiV1MemberBasicInfoService#executeOpenapiBasicInfo}：
 * mobile/username 解密；gradeInfo 来自 {@code OpenapiMemberBasicInfoGradePortImpl#toOpenapiGradeRow}
 *（9 键子集，zh-CN 分表覆盖）；vipgrade 来自 {@code VipGradeUserVipGradeGetService#userVipGradeGet}
 *（ifAll=false，形态随开通/持有状态变化，schema 为并集）。查无会员时 data 为空数组 []（非本对象）。
 */
@Schema(name = "EcxMemberBasicInfoData", description = "会员基础信息（查无会员时 data 为 []）")
public class EcxMemberBasicInfoDataDoc {

	@Schema(description = "会员 ID", example = "5")
	public Long user_id;

	@Schema(description = "手机号（解密后明文；空/解密失败为空串）", example = "13800000000")
	public String mobile;

	@Schema(description = "用户名（解密；无会员资料记录时为 null）", nullable = true, example = "u1")
	public String username;

	@Schema(description = "出生日期（库原样字符串，如 yyyy-MM-dd；无资料时 null）", nullable = true, example = "2000-01-02")
	public String birthday;

	@Schema(description = "注册时间（Unix 秒；null 时为空串）", example = "1764115200")
	public Object create_time;

	@Schema(description = "会员卡等级信息（grade_id 有效且存在记录时非 null，否则 null）", nullable = true)
	public GradeInfoDoc gradeInfo;

	@Schema(description = "付费会员（VIP）状态；形态随开通/持有状态变化，字段为各形态并集")
	public VipGradeDoc vipgrade;

	@Schema(name = "EcxMemberBasicInfoGradeInfo", description = "会员卡等级（OpenAPI 9 键子集）")
	public static class GradeInfoDoc {

		@Schema(description = "等级 ID", example = "1")
		public Long grade_id;

		@Schema(description = "租户/公司 ID（字符串）", example = "100")
		public String company_id;

		@Schema(description = "等级名称（zh-CN 分表覆盖）", example = "普通会员")
		public String grade_name;

		@Schema(description = "是否默认等级", example = "true")
		public Boolean default_grade;

		@Schema(description = "等级背景图 URL（zh-CN 分表覆盖）", nullable = true)
		public String background_pic_url;

		@Schema(description = "升级条件（JSON 解码为数组/对象；空/非法为 []）")
		public Object promotion_condition;

		@Schema(description = "等级权益（JSON 解码为对象/数组，含 discount 等；空/非法为 {}）")
		public Object privileges;

		@Schema(description = "等级描述（zh-CN 分表覆盖）", nullable = true)
		public String description;

		@Schema(description = "等级背景（zh-CN 分表覆盖）", nullable = true)
		public String grade_background;
	}

	@Schema(
			name = "EcxMemberBasicInfoVipGrade",
			description =
					"付费会员状态并集：未开通且无配置 {is_open:false,is_vip:false,is_had_vip:false}；"
							+ "开通未持有为等级配置行 + is_open/is_vip:false/is_had_vip:false/discount；"
							+ "持有时为关系行 + 等级配置字段（svip 优先，否则首个有效类型）")
	public static class VipGradeDoc {

		@Schema(description = "付费会员功能是否开通（存在启用等级配置）", example = "false")
		public Boolean is_open;

		@Schema(description = "当前是否有效持有", example = "false")
		public Boolean is_vip;

		@Schema(description = "是否持有（含过期）关系记录；无配置未开通形态恒 false", example = "false")
		public Boolean is_had_vip;

		@Schema(description = "付费等级配置 ID（持有/配置形态；缺省 0）", example = "1")
		public Long vip_grade_id;

		@Schema(description = "付费类型（持有形态，如 svip/vip）", example = "svip")
		public String vip_type;

		@Schema(description = "等级类型（配置形态/持有形态覆盖）", example = "svip")
		public String lv_type;

		@Schema(description = "到期时间（Unix 秒；持有形态）", example = "1766707200")
		public Long end_date;

		@Schema(description = "到期日期 yyyy-MM-dd（持有形态）", example = "2026-12-26")
		public String end_time;

		@Schema(description = "剩余天数（向上取整；持有形态，过期为 0）", example = "30")
		public Long day;

		@Schema(description = "关系是否有效（剩余天数>0；等级配置缺失时置 false）", example = "true")
		public Boolean valid;

		@Schema(description = "等级折扣（privileges.discount，缺省 0）", example = "95")
		public Integer discount;

		@Schema(description = "等级名称", nullable = true)
		public String grade_name;

		@Schema(description = "导购端标题", nullable = true)
		public String guide_title;

		@Schema(description = "等级背景图 URL", nullable = true)
		public String background_pic_url;
	}
}