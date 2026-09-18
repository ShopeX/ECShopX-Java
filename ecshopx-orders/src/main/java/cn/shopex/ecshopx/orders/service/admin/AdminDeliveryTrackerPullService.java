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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.orders.service.logistics.KdniaoTrackQueryService;
import cn.shopex.ecshopx.orders.service.logistics.Kuaidi100TrackQueryService;
import cn.shopex.ecshopx.orders.service.setting.KuaidiSettingRedisService;
import cn.shopex.ecshopx.superadmin.domain.Logistics;
import cn.shopex.ecshopx.superadmin.mapper.LogisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AdminDeliveryTrackerPullService {

	private final KuaidiSettingRedisService kuaidiSettingRedisService;
	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final LogisticsMapper logisticsMapper;
	private final Kuaidi100TrackQueryService kuaidi100TrackQueryService;
	private final KdniaoTrackQueryService kdniaoTrackQueryService;

	public AdminDeliveryTrackerPullService(
			KuaidiSettingRedisService kuaidiSettingRedisService,
			OrdersDeliveryMapper ordersDeliveryMapper,
			LogisticsMapper logisticsMapper,
			Kuaidi100TrackQueryService kuaidi100TrackQueryService,
			KdniaoTrackQueryService kdniaoTrackQueryService) {
		this.kuaidiSettingRedisService = kuaidiSettingRedisService;
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.logisticsMapper = logisticsMapper;
		this.kuaidi100TrackQueryService = kuaidi100TrackQueryService;
		this.kdniaoTrackQueryService = kdniaoTrackQueryService;
	}

	public List<LinkedHashMap<String, String>> trackerpull(
			long companyId, String deliveryCorp, String deliveryCode, boolean useKuaidi100Branch) {
		String corp = deliveryCorp == null ? "" : deliveryCorp.trim();
		String code = deliveryCode == null ? "" : deliveryCode.trim();
		if (corp.isEmpty()) {
			throw new BadRequestException("快递公司编码不能为空");
		}
		if (code.isEmpty()) {
			throw new BadRequestException("快递单号编码不能为空");
		}
		if (useKuaidi100Branch) {
			JsonNode cfg = kuaidiSettingRedisService.getKuaidiSettingJson(companyId, "kuaidi100");
			OrdersDelivery row =
					ordersDeliveryMapper.selectOne(
							new LambdaQueryWrapper<OrdersDelivery>()
									.eq(OrdersDelivery::getCompanyId, companyId)
									.eq(OrdersDelivery::getDeliveryCorp, corp)
									.eq(OrdersDelivery::getDeliveryCode, code)
									.last("LIMIT 1"));
			String receiverMobile =
					row == null || row.getReceiverMobile() == null ? "" : row.getReceiverMobile().trim();
			String comParam;
			if (corp.equals(corp.toUpperCase(Locale.ROOT))) {
				Logistics logistics =
						logisticsMapper.selectOne(
								new LambdaQueryWrapper<Logistics>()
										.eq(Logistics::getCorpCode, corp)
										.last("LIMIT 1"));
				if (logistics == null
						|| logistics.getKuaidiCode() == null
						|| logistics.getKuaidiCode().isBlank()) {
					comParam = corp;
				} else {
					comParam = logistics.getKuaidiCode().trim();
				}
			} else {
				comParam = corp;
			}
			return kuaidi100TrackQueryService.queryTraces(companyId, comParam, code, receiverMobile, cfg);
		}
		JsonNode cfg = kuaidiSettingRedisService.getKuaidiSettingJson(companyId, "kdniao");
		return kdniaoTrackQueryService.queryTraces(companyId, corp, code, cfg);
	}
}
