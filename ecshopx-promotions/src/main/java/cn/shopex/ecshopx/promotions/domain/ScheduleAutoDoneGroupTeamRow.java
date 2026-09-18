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

import lombok.Data;

/**
 * 自动成团任务分页查询行：团表字段与活动上的人数、机械人开关等。
 */
@Data
public class ScheduleAutoDoneGroupTeamRow {

	private Long id;
	private String teamId;
	private Long companyId;
	private Long actId;
	private Long endTime;
	private Long joinPersonNum;
	private Long teamStatus;
	private String groupGoodsType;
	private Boolean disabled;
	private Integer created;

	/** 活动上成团人数 a.person_num */
	private Long actPersonNum;
	/** 活动上是否开启机器人 a.robot */
	private Boolean actRobot;
}
