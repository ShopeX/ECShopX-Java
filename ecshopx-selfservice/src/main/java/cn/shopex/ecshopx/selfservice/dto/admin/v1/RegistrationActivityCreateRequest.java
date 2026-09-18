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

package cn.shopex.ecshopx.selfservice.dto.admin.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RegistrationActivityCreateRequest {

	@JsonProperty("activity_id")
	private Long activityId;

	@JsonProperty("temp_id")
	private Long tempId;

	@JsonProperty("activity_name")
	private String activityName;

	@JsonProperty("start_time")
	private Object startTimeRaw;

	@JsonProperty("end_time")
	private Object endTimeRaw;

	@JsonProperty("join_limit")
	private Integer joinLimit;

	@JsonProperty("is_sms_notice")
	private Object isSmsNotice;

	@JsonProperty("is_wxapp_notice")
	private Object isWxappNotice;

	private Object area;

	private String place;

	private String address;

	private String intro;

	@JsonProperty("show_fields")
	private Object showFields;

	private Object pics;

	@JsonProperty("gift_points")
	private Integer giftPoints;

	@JsonProperty("is_allow_duplicate")
	private Object isAllowDuplicate;

	@JsonProperty("is_allow_cancel")
	private Object isAllowCancel;

	@JsonProperty("is_offline_verify")
	private Object isOfflineVerify;

	@JsonProperty("is_need_check")
	private Object isNeedCheck;

	@JsonProperty("is_white_list")
	private Object isWhiteList;

	@JsonProperty("enterprise_ids")
	private Object enterpriseIds;

	@JsonProperty("group_no")
	private String groupNo;

	@JsonProperty("member_level")
	private Object memberLevel;

	@JsonProperty("distributor_ids")
	private Object distributorIds;

	@JsonProperty("join_tips")
	private String joinTips;

	@JsonProperty("submit_form_tips")
	private String submitFormTips;

	private String content;

	@JsonProperty("distributor_id")
	private Long distributorId;
}
