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

public final class MembersBundleDispatchJobNames {

	public static final String GROUP_SEND_SMS_JOB = "job:183:MembersBundle\\Jobs\\GroupSendSms";

	public static final String BATCH_ACTION_MEMBERS_JOB =
			"job:191:MembersBundle\\Jobs\\BatchActionMembers";

	public static final String UPDATE_ADDRESS_LAT_AND_LNG_JOB =
			"job:184:MembersBundle\\Jobs\\UpdateAddressLatAndLngJob";

	public static final String BIND_SALSEPERSON_JOB = "job:185:MembersBundle\\Jobs\\BindSalseperson";

	public static final String BIND_SALSEPERSON_JOB_DM_MEMBER_REGISTER =
			"job:186:MembersBundle\\Jobs\\BindSalseperson";

	public static final String BIND_SALSEPERSON_JOB_WXAPP_BIND_SALESPERSON =
			"job:194:MembersBundle\\Jobs\\BindSalseperson";

	public static final String PUSH_MEMBER_TAG_RELATION_JOB =
			"job:188:MembersBundle\\Jobs\\PushMemberTagRelationJob";

	/** 邮箱注册成功后异步发送激活链接邮件；default 队列。 */
	public static final String SEND_MEMBER_EMAIL_ACTIVATION_JOB =
			"job:195:MembersBundle\\Jobs\\SendMemberEmailActivationJob";

	private MembersBundleDispatchJobNames() {
	}
}
