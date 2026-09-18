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

package cn.shopex.ecshopx.point.service;

import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformPointPort;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.point.service.shuyun.ShuyunMemberPointSyncPort;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmMemberCurrentIntegralPort;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PointMemberInfoService {

	private final boolean oemShuyun;
	private final ShuyunMemberPointSyncPort shuyunMemberPointSyncPort;
	private final PointMemberBalanceReadService pointMemberBalanceReadService;
	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final MemberAccountService memberAccountService;
	private final DmCrmMemberCurrentIntegralPort dmCrmMemberCurrentIntegralPort;
	private final ShuyunOpenPlatformPointPort openPlatformPointPort;

	public PointMemberInfoService(
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun,
			ShuyunMemberPointSyncPort shuyunMemberPointSyncPort,
			PointMemberBalanceReadService pointMemberBalanceReadService,
			DmCrmSettingReadPort dmCrmSettingReadPort,
			MemberAccountService memberAccountService,
			DmCrmMemberCurrentIntegralPort dmCrmMemberCurrentIntegralPort,
			ShuyunOpenPlatformPointPort openPlatformPointPort) {
		this.oemShuyun = oemShuyun;
		this.shuyunMemberPointSyncPort = shuyunMemberPointSyncPort;
		this.pointMemberBalanceReadService = pointMemberBalanceReadService;
		this.dmCrmSettingReadPort = dmCrmSettingReadPort;
		this.memberAccountService = memberAccountService;
		this.dmCrmMemberCurrentIntegralPort = dmCrmMemberCurrentIntegralPort;
		this.openPlatformPointPort = openPlatformPointPort;
	}

	public Map<String, Object> info(long companyId, long userId) {
		long point;
		if (openPlatformPointPort.isOpenPlatformPointEnabled(companyId)) {
			Long remote = openPlatformPointPort.queryValidPoint(companyId, userId);
			if (remote != null) {
				point = remote;
			} else {
				point = pointMemberBalanceReadService.getPointBalance(companyId, userId);
			}
		} else if (oemShuyun) {
			point = shuyunMemberPointSyncPort.readMemberPointBalance(companyId, userId);
		} else {
			point = pointMemberBalanceReadService.getPointBalance(companyId, userId);
		}

		// 达摩与开放平台互斥：OPEN 启用时不以达摩覆盖（对齐 PHP getInfo）
		if (dmCrmSettingReadPort.isPointIntegrationOpen(companyId)
				&& !openPlatformPointPort.isOpenPlatformPointEnabled(companyId)) {
			Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
			Object mobileRaw = memberInfo != null ? memberInfo.get("mobile") : null;
			String mobile = mobileRaw != null ? String.valueOf(mobileRaw) : "";
			try {
				point = dmCrmMemberCurrentIntegralPort.fetchCurrentIntegral(companyId, mobile);
			} catch (RuntimeException e) {
				// 外部积分接口不可用或返回异常时保留上文本地/数云余额，避免读详情类聚合接口整体失败
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("user_id", userId);
		data.put("point", point);
		return data;
	}
}
