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

package cn.shopex.ecshopx.promotions.domain.membertask;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员任务活动 */
@Data
@MpTable(
		value = "member_task",
		comment = "会员任务活动",
		indexes = {@MpIndex(name = "idx_member_task_company", columns = {"company_id", "task_id"})})
public class MemberTask {

	@MpId(value = "task_id", type = IdType.AUTO, columnType = "bigint", comment = "会员任务ID")
	private Long taskId;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "name", columnType = "string", length = 100, comment = "任务名称")
	private String name;

	@MpField(value = "type_category", columnType = "string", length = 32, comment = "任务一级类型码")
	private String typeCategory;

	@MpField(value = "type_detail", columnType = "string", length = 32, comment = "任务二级类型码")
	private String typeDetail;

	@MpField(value = "begin_time", columnType = "integer", comment = "开始时间", defaultValue = "0")
	private Integer beginTime;

	@MpField(value = "end_time", columnType = "integer", comment = "结束时间", defaultValue = "0")
	private Integer endTime;

	@MpField(value = "published", columnType = "integer", comment = "是否已发布", defaultValue = "0")
	private Integer published;

	@MpField(value = "off_shelf", columnType = "integer", comment = "是否下架", defaultValue = "0")
	private Integer offShelf;

	@MpField(value = "reward_summary", columnType = "text", nullable = true, comment = "列表奖励摘要")
	private String rewardSummary;

	@MpField(value = "frequency_type", columnType = "string", length = 16, comment = "频次")
	private String frequencyType;

	@MpField(value = "frequency_period", columnType = "string", length = 16, nullable = true, comment = "周期")
	private String frequencyPeriod;

	@MpField(value = "audience_mode", columnType = "string", length = 16, comment = "人群模式")
	private String audienceMode;

	@MpField(value = "audience_tag_ids", columnType = "text", nullable = true, comment = "人群标签")
	private String audienceTagIds;

	@MpField(value = "audience_tag_combine", columnType = "string", length = 16, nullable = true)
	private String audienceTagCombine;

	@MpField(value = "task_rule", columnType = "text", comment = "任务规则")
	private String taskRule;

	@MpField(value = "reward_config", columnType = "text", comment = "奖励配置 JSON")
	private String rewardConfig;

	@MpField(value = "rule_config", columnType = "text", nullable = true, comment = "类型规则 JSON")
	private String ruleConfig;

	@MpField(value = "jump_page", columnType = "string", length = 512, nullable = true, comment = "跳转")
	private String jumpPage;

	@MpField(value = "config_version", columnType = "bigint", comment = "乐观锁", defaultValue = "1")
	private Long configVersion;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
