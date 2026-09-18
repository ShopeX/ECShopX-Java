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

package cn.shopex.ecshopx.common.dispatch;

public final class MembersDispatchEventNames {

	public static final String EVENT_CREATE_MEMBER_SUCCESS = "event:members:create_member_success";

	public static final String EVENT_UPDATE_MEMBER_SUCCESS = "event:members:update_member_success";

	/** Queued listener syncing new members to Shopex CRM after registration success. */
	public static final String LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_ADD_MEMBER =
			"listener:thirdparty.shopex_crm.sync_add_member";

	public static final String LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_UPDATE_MEMBER =
			"listener:thirdparty.shopex_crm.sync_update_member";

	/** Queued listener for member creation success in-app notice; uses the {@code slow} queue. */
	public static final String LISTENER_MEMBERS_CREATE_MEMBER_SUCCESS_NOTICE =
			"listener:members.create_member_success_notice";

	/**
	 * Queued listener for register-point rewards from Redis register-point configuration; uses the
	 * {@code default} queue.
	 */
	public static final String LISTENER_MEMBERS_REGISTER_POINT = "listener:members.register_point";

	/**
	 * Queued listener for DataCube daily register counters (Redis hash under {@code datecube_tracklog});
	 * uses the {@code slow} queue.
	 */
	public static final String LISTENER_DATACUBE_REGISTER_NUM_STATS = "listener:datacube.register_num_stats";

	public static final String EVENT_SYNC_WECHAT_FANS = "event:members:sync_wechat_fans";

	public static final String EVENT_SYNC_WECHAT_TAGS = "event:members:sync_wechat_tags";

	/** 数云开放平台：建会员成功 → OFFLINE member.register（店务路径骨架） */
	public static final String LISTENER_SHUYUN_OPEN_PLATFORM_MEMBER_REGISTER_ON_CREATE_MEMBER_SUCCESS =
			"listener:shuyun.open_platform.member_register_on_create_member_success";

	private MembersDispatchEventNames() {
	}
}
