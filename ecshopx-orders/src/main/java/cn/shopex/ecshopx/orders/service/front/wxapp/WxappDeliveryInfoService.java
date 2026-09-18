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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminDeliveryTrackerPullService;
import cn.shopex.ecshopx.orders.service.logistics.SfbspTrackQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappDeliveryInfoService {

	private static final DateTimeFormatter ACCEPT_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final AdminDeliveryTrackerPullService adminDeliveryTrackerPullService;
	private final SfbspTrackQueryService sfbspTrackQueryService;

	public WxappDeliveryInfoService(
			OrdersDeliveryMapper ordersDeliveryMapper,
			AdminDeliveryTrackerPullService adminDeliveryTrackerPullService,
			SfbspTrackQueryService sfbspTrackQueryService) {
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.adminDeliveryTrackerPullService = adminDeliveryTrackerPullService;
		this.sfbspTrackQueryService = sfbspTrackQueryService;
	}

	public List<LinkedHashMap<String, String>> deliveryInfo(
			long companyId, String deliveryIdTrimmed, Object authUserIdRaw) {
		Long id;
		try {
			id = Long.parseLong(deliveryIdTrimmed);
		} catch (NumberFormatException e) {
			return singlePlaceholderTrace();
		}

		OrdersDelivery row =
				ordersDeliveryMapper.selectOne(
						new LambdaQueryWrapper<OrdersDelivery>()
								.eq(OrdersDelivery::getOrdersDeliveryId, id));

		if (row == null) {
			return singlePlaceholderTrace();
		}

		if (authUserIdRaw != null && StringUtils.hasText(String.valueOf(authUserIdRaw).trim())) {
			String authStr = String.valueOf(authUserIdRaw).trim();
			String rowStr = row.getUserId() == null ? "" : String.valueOf(row.getUserId()).trim();
			if (!authStr.equals(rowStr)) {
				return singlePlaceholderTrace();
			}
		}

		try {
			List<LinkedHashMap<String, String>> sfBsp =
					sfbspTrackQueryService.queryTracesIfApplicable(companyId, row);
			if (sfBsp != null) {
				sortTracesByAcceptTimeDesc(sfBsp);
				return sfBsp;
			}
			boolean useKuaidi100 =
					row.getDeliveryCorpSource() != null
							&& "kuaidi100".equals(row.getDeliveryCorpSource().trim());
			List<LinkedHashMap<String, String>> pulled =
					adminDeliveryTrackerPullService.trackerpull(
							companyId, row.getDeliveryCorp(), row.getDeliveryCode(), useKuaidi100);
			sortTracesByAcceptTimeDesc(pulled);
			return pulled;
		} catch (Exception e) {
			return singlePlaceholderTrace();
		}
	}

	private static void sortTracesByAcceptTimeDesc(List<LinkedHashMap<String, String>> list) {
		list.sort(
				Comparator.comparing(
						(Map<String, String> m) -> m.getOrDefault("AcceptTime", ""),
						Comparator.reverseOrder()));
	}

	private static List<LinkedHashMap<String, String>> singlePlaceholderTrace() {
		LinkedHashMap<String, String> one = new LinkedHashMap<>();
		one.put("AcceptTime", ACCEPT_TIME_FMT.format(LocalDateTime.now()));
		one.put("AcceptStation", "暂无物流信息");
		return List.of(one);
	}
}
