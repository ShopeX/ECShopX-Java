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

package cn.shopex.ecshopx.point.integration.members;

import cn.shopex.ecshopx.common.members.port.DmCrmMemberPointOverlayPort;
import cn.shopex.ecshopx.common.members.port.WxappMemberPointRuleAndBalancePort;
import cn.shopex.ecshopx.point.service.PointMemberInfoService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service("wxappMemberPointRuleAndBalancePortImpl")
public class WxappMemberPointRuleAndBalancePortImpl implements WxappMemberPointRuleAndBalancePort {

	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final PointMemberInfoService pointMemberInfoService;
	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final DmCrmMemberPointOverlayPort dmCrmMemberPointOverlayPort;

	public WxappMemberPointRuleAndBalancePortImpl(
			PointMemberRuleReadService pointMemberRuleReadService,
			PointMemberInfoService pointMemberInfoService,
			DmCrmSettingReadPort dmCrmSettingReadPort,
			DmCrmMemberPointOverlayPort dmCrmMemberPointOverlayPort) {
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.pointMemberInfoService = pointMemberInfoService;
		this.dmCrmSettingReadPort = dmCrmSettingReadPort;
		this.dmCrmMemberPointOverlayPort = dmCrmMemberPointOverlayPort;
	}

	@Override
	public Map<String, Object> loadRule(long companyId) {
		return pointMemberRuleReadService.getPointRule(companyId);
	}

	@Override
	public Map<String, Object> loadPointMemberInfo(long companyId, long userId) {
		return pointMemberInfoService.info(companyId, userId);
	}

	@Override
	public Map<String, Object> loadWxappPointDisplay(long companyId, long userId, String mobilePlainForOuterScope) {
		Map<String, Object> pointRow = pointMemberInfoService.info(companyId, userId);
		long pointVal = extractPointScalar(pointRow != null ? pointRow.get("point") : null);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("point", pointVal);
		if (dmCrmSettingReadPort.isPointIntegrationOpen(companyId)
				&& mobilePlainForOuterScope != null
				&& !mobilePlainForOuterScope.trim().isEmpty()) {
			Map<String, Object> overlay =
					dmCrmMemberPointOverlayPort.fetchPointOverlay(companyId, mobilePlainForOuterScope.trim());
			if (overlay != null && !overlay.isEmpty()) {
				out.put("point", toLong(overlay.get("integral")));
				out.put("frozen_point", toLong(overlay.get("frozenIntegral")));
			}
		}
		return out;
	}

	private static long extractPointScalar(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
