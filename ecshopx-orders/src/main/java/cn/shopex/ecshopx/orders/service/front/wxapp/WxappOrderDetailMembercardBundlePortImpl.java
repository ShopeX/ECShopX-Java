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

import cn.shopex.ecshopx.common.order.front.WxappOrderDetailMembercardBundlePort;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.mapper.WxappVipGradeOrderDetailLiteMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailPayloadMaps;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderDetailMembercardBundlePortImpl implements WxappOrderDetailMembercardBundlePort {

	private final WxappVipGradeOrderDetailLiteMapper wxappVipGradeOrderDetailLiteMapper;
	private final TradeMapper tradeMapper;

	public WxappOrderDetailMembercardBundlePortImpl(
			WxappVipGradeOrderDetailLiteMapper wxappVipGradeOrderDetailLiteMapper, TradeMapper tradeMapper) {
		this.wxappVipGradeOrderDetailLiteMapper = wxappVipGradeOrderDetailLiteMapper;
		this.tradeMapper = tradeMapper;
	}

	@Override
	public Map<String, Object> buildMembercardOrderDetailBundle(
			long companyId, String orderIdRaw, boolean checkAftersales) {
		long orderIdNum = Long.parseLong(orderIdRaw.trim());
		Map<String, Object> row = wxappVipGradeOrderDetailLiteMapper.selectRow(companyId, orderIdNum);
		Map<String, Object> out = new LinkedHashMap<>();
		Map<String, Object> orderInfo = new LinkedHashMap<>();
		Map<String, Object> tradeInfo = new LinkedHashMap<>();
		out.put("orderInfo", orderInfo);
		out.put("tradeInfo", tradeInfo);
		out.put("tradeList", new ArrayList<Map<String, Object>>());
		out.put("cancelData", new LinkedHashMap<String, Object>());
		out.put("afterSaleInfo", new ArrayList<Map<String, Object>>());
		if (row == null || row.isEmpty()) {
			return out;
		}
		orderInfo.putAll(row);
		orderInfo.put("order_id", String.valueOf(orderIdNum));
		orderInfo.put("company_id", companyId);
		orderInfo.put("order_type", "membercard");
		Object vg = row.get("vip_grade_id");
		orderInfo.put("item_id", vg);
		orderInfo.put("item_num", 1);
		int price = intVal(row.get("price"));
		orderInfo.put("total_fee", price);
		orderInfo.put("freight_fee", 0);
		orderInfo.put("cost_fee", 0);
		orderInfo.put("item_fee", price);
		Object created = row.get("created");
		orderInfo.put("create_time", created);

		List<Trade> trades =
				tradeMapper.selectList(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getOrderId, String.valueOf(orderIdNum))
								.orderByAsc(Trade::getTimeStart));
		if (!trades.isEmpty()) {
			Trade t = trades.get(0);
			tradeInfo.putAll(AdminOrderDetailPayloadMaps.tradeToMap(t));
			Object pt = tradeInfo.get("pay_type");
			if (pt != null) {
				orderInfo.put("pay_type", pt);
			}
		}
		return out;
	}

	private static int intVal(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
