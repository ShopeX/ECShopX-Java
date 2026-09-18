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

package cn.shopex.ecshopx.orders.integration;

import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailPayloadMaps;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OrderNormalOrderItemsReadPortImpl implements OrderNormalOrderItemsReadPort {

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;

	public OrderNormalOrderItemsReadPortImpl(NormalOrdersItemsMapper normalOrdersItemsMapper) {
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
	}

	@Override
	public List<Map<String, Object>> listItems(long companyId, long orderId) {
		List<NormalOrdersItems> rows =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));
		List<Map<String, Object>> out = new ArrayList<>();
		for (NormalOrdersItems it : rows) {
			out.add(itemToMap(it));
		}
		return out;
	}

	@Override
	public List<Map<String, Object>> listServiceOrderItems(long companyId, long orderId) {
		List<NormalOrdersItems> rows =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));
		List<Map<String, Object>> out = new ArrayList<>();
		for (NormalOrdersItems it : rows) {
			out.add(AdminOrderDetailPayloadMaps.itemToMap(it));
		}
		return out;
	}

	private static Map<String, Object> itemToMap(NormalOrdersItems it) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", it.getId());
		m.put("num", it.getNum());
		m.put("total_fee", it.getTotalFee() == null ? 0 : it.getTotalFee());
		m.put("item_fee", it.getItemFee() == null ? 0 : it.getItemFee());
		m.put("point", it.getPoint() == null ? 0 : it.getPoint());
		m.put("supplier_id", it.getSupplierId() == null ? 0L : it.getSupplierId());
		m.put("distributor_id", it.getDistributorId() == null ? 0L : it.getDistributorId());
		m.put("goods_id", it.getGoodsId() == null ? 0L : it.getGoodsId());
		m.put("item_id", it.getItemId() == null ? 0L : it.getItemId());
		m.put("item_bn", it.getItemBn());
		m.put("price", it.getPrice() == null ? 0 : it.getPrice());
		m.put("item_name", it.getItemName());
		m.put("order_item_type", it.getOrderItemType());
		m.put("pic", it.getPic());
		m.put("delivery_status", it.getDeliveryStatus());
		m.put("delivery_item_num", it.getDeliveryItemNum() == null ? 0 : it.getDeliveryItemNum());
		m.put("cancel_item_num", it.getCancelItemNum() == null ? 0 : it.getCancelItemNum());
		m.put("item_spec_desc", it.getItemSpecDesc());
		m.put("get_points", it.getGetPoints() == null ? 0 : it.getGetPoints());
		return m;
	}
}
