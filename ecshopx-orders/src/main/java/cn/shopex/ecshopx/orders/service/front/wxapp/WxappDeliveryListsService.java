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

import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.domain.OrdersDeliveryItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryItemsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminDeliveryTrackerPullService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappDeliveryListsService {

	private final OrdersDeliveryMapper ordersDeliveryMapper;
	private final OrdersDeliveryItemsMapper ordersDeliveryItemsMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final AdminDeliveryTrackerPullService adminDeliveryTrackerPullService;

	public WxappDeliveryListsService(
			OrdersDeliveryMapper ordersDeliveryMapper,
			OrdersDeliveryItemsMapper ordersDeliveryItemsMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			AdminDeliveryTrackerPullService adminDeliveryTrackerPullService) {
		this.ordersDeliveryMapper = ordersDeliveryMapper;
		this.ordersDeliveryItemsMapper = ordersDeliveryItemsMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.adminDeliveryTrackerPullService = adminDeliveryTrackerPullService;
	}

	public Map<String, Object> lists(long companyId, Long orderId) {
		LambdaQueryWrapper<OrdersDelivery> w = new LambdaQueryWrapper<>();
		w.eq(OrdersDelivery::getCompanyId, companyId);
		if (orderId == null) {
			w.isNull(OrdersDelivery::getOrderId);
		} else {
			w.eq(OrdersDelivery::getOrderId, orderId);
		}
		w.orderByAsc(OrdersDelivery::getOrdersDeliveryId);
		List<OrdersDelivery> deliveryList = ordersDeliveryMapper.selectList(w);

		int deliveryNum = deliveryList.size();
		List<Map<String, Object>> data = new ArrayList<>();

		for (OrdersDelivery val : deliveryList) {
			int itemsNum = 0;
			List<Map<String, Object>> items = new ArrayList<>();
			List<OrdersDeliveryItems> itemRows =
					ordersDeliveryItemsMapper.selectList(
							new LambdaQueryWrapper<OrdersDeliveryItems>()
									.eq(OrdersDeliveryItems::getOrdersDeliveryId, val.getOrdersDeliveryId()));
			for (OrdersDeliveryItems it : itemRows) {
				Map<String, Object> picMap = new LinkedHashMap<>();
				picMap.put("pic", emptyIfNull(it.getPic()));
				items.add(picMap);
				itemsNum += nullToZero(it.getNum());
			}
			String deliveryInfo = firstAcceptStation(companyId, val);
			LinkedHashMap<String, Object> line = new LinkedHashMap<>();
			line.put("delivery_id", val.getOrdersDeliveryId());
			line.put("delivery_corp", emptyIfNull(val.getDeliveryCorp()));
			line.put("delivery_corp_name", emptyIfNull(val.getDeliveryCorpName()));
			line.put("delivery_code", emptyIfNull(val.getDeliveryCode()));
			line.put("items", items);
			line.put("items_num", itemsNum);
			line.put("status_msg", "已发货");
			line.put("delivery_info", deliveryInfo);
			data.add(line);
		}

		LambdaQueryWrapper<NormalOrdersItems> q = new LambdaQueryWrapper<>();
		q.select(
				NormalOrdersItems::getCompanyId,
				NormalOrdersItems::getOrderId,
				NormalOrdersItems::getDeliveryStatus,
				NormalOrdersItems::getNum,
				NormalOrdersItems::getCancelItemNum,
				NormalOrdersItems::getDeliveryItemNum,
				NormalOrdersItems::getPic);
		q.eq(NormalOrdersItems::getCompanyId, companyId);
		if (orderId == null) {
			q.isNull(NormalOrdersItems::getOrderId);
		} else {
			q.eq(NormalOrdersItems::getOrderId, orderId);
		}
		List<NormalOrdersItems> normalRows = normalOrdersItemsMapper.selectList(q);

		List<Map<String, Object>> pendingItems = new ArrayList<>();
		int pendingItemsNum = 0;
		for (NormalOrdersItems row : normalRows) {
			if ("DONE".equals(row.getDeliveryStatus())) {
				continue;
			}
			int num =
					nullToZero(row.getNum())
							- nullToZero(row.getCancelItemNum())
							- nullToZero(row.getDeliveryItemNum());
			if (num <= 0) {
				continue;
			}
			Map<String, Object> picMap = new LinkedHashMap<>();
			picMap.put("pic", emptyIfNull(row.getPic()));
			pendingItems.add(picMap);
			pendingItemsNum += num;
		}

		if (!pendingItems.isEmpty()) {
			LinkedHashMap<String, Object> syn = new LinkedHashMap<>();
			syn.put("delivery_id", "");
			syn.put("delivery_corp", "");
			syn.put("delivery_corp_name", "");
			syn.put("delivery_code", "");
			syn.put("items", pendingItems);
			syn.put("items_num", pendingItemsNum);
			syn.put("status_msg", "未发货");
			syn.put("delivery_info", "");
			data.add(syn);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("delivery_num", deliveryNum);
		out.put("list", data);
		return out;
	}

	private String firstAcceptStation(long companyId, OrdersDelivery row) {
		boolean useKuaidi100 =
				row.getDeliveryCorpSource() != null
						&& "kuaidi100".equals(row.getDeliveryCorpSource().trim());
		try {
			List<LinkedHashMap<String, String>> list =
					adminDeliveryTrackerPullService.trackerpull(
							companyId, row.getDeliveryCorp(), row.getDeliveryCode(), useKuaidi100);
			if (list == null || list.isEmpty()) {
				return "";
			}
			String accept = list.get(0).get("AcceptStation");
			return accept == null ? "" : accept;
		} catch (Exception e) {
			return "暂无物流信息";
		}
	}

	private static int nullToZero(Integer v) {
		return v == null ? 0 : v;
	}

	private static String emptyIfNull(String s) {
		return s == null ? "" : s;
	}
}
