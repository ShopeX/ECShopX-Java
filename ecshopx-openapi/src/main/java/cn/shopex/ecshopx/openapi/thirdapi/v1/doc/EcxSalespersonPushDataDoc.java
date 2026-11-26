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
 * 仅 OpenAPI 文档；成功信封 data。新建/更新后按主键回读 shop_salesperson，
 * 经 {@code DistributorShoppingGuideAddService#buildMobileFindDataMap} 组装的 23 键（LinkedHashMap 顺序）。
 */
@Schema(
		name = "EcxSalespersonPushData",
		description =
				"落库后回读的导购员完整行。name/mobile 解密返回明文；"
						+ "salesperson_id/company_id/user_id 为字符串形态；role 为原始字符串（新建固定 \"0\"）。")
public class EcxSalespersonPushDataDoc {

	@Schema(description = "导购员 ID（字符串形态）", example = "77")
	public String salesperson_id;

	@Schema(description = "导购名称（解密明文；空值回退空串）", example = "导购员甲")
	public String name;

	@Schema(description = "手机号（解密明文；未设置时为空串）", example = "13800000000")
	public String mobile;

	@Schema(
			description = "创建时间 Unix 秒（created_time 可解析时取其值，否则回退 created，均无为 0）",
			example = "1700000000")
	public Long created_time;

	@Schema(description = "人员类型，本接口恒为 shopping_guide", example = "shopping_guide")
	public String salesperson_type;

	@Schema(description = "租户 company_id（字符串形态）", example = "100")
	public String company_id;

	@Schema(description = "关联会员 ID（字符串形态；新建固定 \"0\"）", example = "0")
	public String user_id;

	@Schema(description = "导购引入会员数（空值回退 0；新建为 0）", example = "0")
	public Integer child_count;

	@Schema(description = "是否启用：字符串 \"true\"/\"false\"（salesperson_status=1 → true）", example = "true")
	public String is_valid;

	@Schema(description = "门店 ID（字符串；新建固定 \"0\"，本接口不写门店）", example = "0")
	public String shop_id;

	@Schema(description = "门店名称；本接口不写，新建为 null", nullable = true)
	public String shop_name;

	@Schema(description = "导购员编号（新建固定空串）", example = "")
	public String number;

	@Schema(description = "会员好友数（空值回退 0；新建为 0）", example = "0")
	public Integer friend_count;

	@Schema(description = "企业微信头像 URL（请求原样落库；未提供为 null）", nullable = true,
			example = "https://example.test/avatar.jpg")
	public String avatar;

	@Schema(
			description = "企业微信 userid；更新分支提供 PHP-truthy 的 NewUserID 且不同于原值时为改写后的新值",
			example = "work-u")
	public String work_userid;

	@Schema(description = "企业微信内部应用配置 ID；本接口不写，新建为 null", nullable = true)
	public String work_configid;

	@Schema(description = "企业微信联系我配置 ID（缺省空串）", example = "")
	public String work_qrcode_configid;

	@Schema(description = "导购权限集合原始字符串（新建固定 \"0\"；本接口更新不改写）", example = "0")
	public String role;

	@Schema(description = "职务（空值回退空串）", example = "店长")
	public String salesperson_job;

	@Schema(description = "员工类型：1 员工 / 2 编外（空值回退 0）", example = "1")
	public Integer employee_status;

	@Schema(description = "创建时间 Unix 秒（新建写入当前秒；更新不变）", example = "1700000000")
	public Long created;

	@Schema(description = "更新时间 Unix 秒（新建/更新均写当前秒）", example = "1700000001")
	public Long updated;

	@Schema(description = "企业微信明文 userid；本接口不写，新建为 null", nullable = true)
	public String work_clear_userid;
}