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

package cn.shopex.ecshopx.promotions.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 抽奖表 */
@Data
@MpTable(value = "lucky_draw_activity", comment = "抽奖表")
public class LuckyDrawActivity {

	/** 抽奖id */
	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "抽奖id")
	private Long id;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 区域id（业务停用，列保留） */
	@MpField(value = "area_id", columnType = "bigint", comment = "区域id")
	private Long areaId;

	/** 开始时间 */
	@MpField(value = "begin_time", columnType = "bigint", comment = "开始时间")
	private Long beginTime = 0L;

	/** 结束时间 */
	@MpField(value = "end_time", columnType = "bigint", comment = "结束时间")
	private Long endTime = 0L;

	/** 消耗类型，1互动分2积分（业务固定积分，列保留） */
	@MpField(value = "cost_type", columnType = "bigint", comment = "消耗类型，1互动分2积分")
	private Long costType = 1L;

	/** 具体的消耗值 */
	@MpField(value = "cost_value", columnType = "bigint", comment = "具体的消耗值")
	private Long costValue = 0L;

	/** 活动期限内总限制；0=不限制 */
	@MpField(value = "limit_total", columnType = "bigint", comment = "活动期限内总限制")
	private Long limitTotal = 0L;

	/** 按天限制；0=不限制 */
	@MpField(value = "limit_day", columnType = "bigint", comment = "按天限制")
	private Long limitDay = 0L;

	/** 活动类型，wheel为大转盘 */
	@MpField(value = "activity_type", columnType = "string", length = 64, nullable = true, comment = "活动类型，wheel为大转盘")
	private String activityType = "wheel";

	/** 活动名称 */
	@MpField(value = "activity_name", columnType = "string", length = 255, nullable = true, comment = "活动名称")
	private String activityName = "";

	/** 活动模板配置 */
	@MpField(value = "activity_template_config", columnType = "text", nullable = true, comment = "活动模板配置")
	private String activityTemplateConfig;

	/** 奖项配置 */
	@MpField(value = "prize_data", columnType = "text", nullable = true, comment = "奖项配置")
	private String prizeData;

	/** 活动说明 */
	@MpField(value = "intro", columnType = "text", nullable = true, comment = "活动说明")
	private String intro;

	/** 影响抽奖的配置版本（乐观锁） */
	@MpField(value = "config_version", columnType = "bigint", comment = "影响抽奖的配置版本，乐观锁")
	private Long configVersion = 1L;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
