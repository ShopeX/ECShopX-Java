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

package cn.shopex.ecshopx.distribution.service.distributorvalid.dto;

import org.springframework.util.StringUtils;

public record DistributorIsValidQuery(
		String lngRaw,
		String latRaw,
		String distributorIdRaw,
		boolean distributorIdProvided,
		boolean lngProvided,
		boolean latProvided,
		String showType,
		String showScoreRaw,
		String showMarketingRaw,
		String showSalesCountRaw,
		int isNostores,
		String cartType,
		String orderType,
		String seckillId,
		String seckillTicket,
		String iscrossborder,
		String bargainId) {

	/** 对齐 PHP：无 distributor_id/lng/lat 且进店规则 radio_type=1 时兜底线上总店。 */
	public boolean preferVirtualStoreByInRule(int radioType) {
		return !distributorIdProvided && !lngProvided && !latProvided && radioType == 1;
	}

	public static DistributorIsValidQuery of(
			String lngParam,
			String latParam,
			String distributorIdRaw,
			String showType,
			String showScoreRaw,
			String showMarketingRaw,
			String showSalesCountRaw,
			Integer isNostoresRaw,
			String cartTypeRaw,
			String orderTypeRaw,
			String seckillIdRaw,
			String seckillTicketRaw,
			String iscrossborderRaw,
			String bargainIdRaw) {
		String lngRaw;
		String vLng = lngParam;
		if (vLng != null && !vLng.trim().isEmpty()) {
			lngRaw = vLng.trim();
		} else {
			lngRaw = "0";
		}
		String latRaw;
		String vLat = latParam;
		if (vLat != null && !vLat.trim().isEmpty()) {
			latRaw = vLat.trim();
		} else {
			latRaw = "0";
		}
		int isNostores = isNostoresRaw == null ? 0 : isNostoresRaw.intValue();
		String cartType = cartTypeRaw == null ? "cart" : cartTypeRaw;
		String orderType = orderTypeRaw == null ? "service" : orderTypeRaw;
		String seckillId = seckillIdRaw == null ? "" : seckillIdRaw;
		String seckillTicket = seckillTicketRaw == null ? "" : seckillTicketRaw;
		String iscrossborder = iscrossborderRaw == null ? "" : iscrossborderRaw;
		String bargainId = bargainIdRaw == null ? "0" : bargainIdRaw.trim();
		String did = distributorIdRaw == null ? null : distributorIdRaw.trim();
		boolean distributorIdProvided = distributorIdRaw != null && StringUtils.hasText(distributorIdRaw.trim());
		boolean lngProvided = lngParam != null && StringUtils.hasText(lngParam.trim());
		boolean latProvided = latParam != null && StringUtils.hasText(latParam.trim());
		String st = showType == null ? null : showType.trim();
		String ss = showScoreRaw == null ? null : showScoreRaw.trim();
		String sm = showMarketingRaw == null ? null : showMarketingRaw.trim();
		String sc = showSalesCountRaw == null ? null : showSalesCountRaw.trim();
		return new DistributorIsValidQuery(
				lngRaw,
				latRaw,
				did,
				distributorIdProvided,
				lngProvided,
				latProvided,
				st,
				ss,
				sm,
				sc,
				isNostores,
				cartType,
				orderType,
				seckillId,
				seckillTicket,
				iscrossborder,
				bargainId);
	}

	public static boolean showFlagTruthy(String raw) {
		return raw != null && StringUtils.hasText(raw.trim());
	}
}
