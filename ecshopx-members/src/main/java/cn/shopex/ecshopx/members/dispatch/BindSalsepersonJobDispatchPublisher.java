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

package cn.shopex.ecshopx.members.dispatch;

import cn.shopex.ecshopx.common.dispatch.MembersBundleDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BindSalsepersonJobDispatchPublisher {

	private static final DispatchOptions BIND_SALSEPERSON_JOB_OPTIONS =
			new DispatchOptions(
					DispatchMode.ASYNC,
					DispatchDriverType.REDIS,
					"slow",
					null,
					RetryPolicy.platformDefault());

	private final DispatchFacade dispatchFacade;

	public BindSalsepersonJobDispatchPublisher(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	public void enqueueBindSalseperson(
			long companyId, String unionid, String workUserid, int customerType, String mobile, long userId) {
		dispatchBindSalsepersonJob(
				MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB,
				companyId,
				unionid,
				workUserid,
				customerType,
				mobile,
				userId);
	}

	/**
	 * Enqueues bind-salesperson job after Damo-backed member registration: payload {@code user_id} is forced to
	 * {@code 0}, matching the five-argument job dispatch shape.
	 */
	public void enqueueBindSalsepersonAfterDmMemberCreate(
			long companyId, String unionid, String workUserid, int customerType, String mobile) {
		dispatchBindSalsepersonJob(
				MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB_DM_MEMBER_REGISTER,
				companyId,
				unionid,
				workUserid,
				customerType,
				mobile,
				0L);
	}

	/**
	 * Enqueues bind-salesperson job after WeChat mini-program {@code bindSalesperson} API persists
	 * {@code WorkWechatRel}; uses {@code job:194} message name for this entry point.
	 */
	public void enqueueBindSalsepersonAfterWxappBindSalesperson(
			long companyId, String unionid, String workUserid, int customerType, String mobile, long userId) {
		dispatchBindSalsepersonJob(
				MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB_WXAPP_BIND_SALESPERSON,
				companyId,
				unionid,
				workUserid,
				customerType,
				mobile,
				userId);
	}

	private void dispatchBindSalsepersonJob(
			String messageName,
			long companyId,
			String unionid,
			String workUserid,
			int customerType,
			String mobile,
			long userId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("unionid", unionid == null ? "" : unionid);
		payload.put("work_userid", workUserid);
		payload.put("customer_type", customerType);
		payload.put("mobile", mobile == null ? "" : mobile);
		payload.put("user_id", userId);
		dispatchFacade.dispatchJob(messageName, payload, BIND_SALSEPERSON_JOB_OPTIONS);
	}
}
