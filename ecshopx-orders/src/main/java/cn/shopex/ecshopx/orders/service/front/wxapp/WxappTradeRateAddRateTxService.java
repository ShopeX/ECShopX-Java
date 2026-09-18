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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.TradeRate;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeRateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WxappTradeRateAddRateTxService {

	private final TradeRateMapper tradeRateMapper;

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;

	private final NormalOrdersMapper normalOrdersMapper;

	public WxappTradeRateAddRateTxService(
			TradeRateMapper tradeRateMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrdersMapper normalOrdersMapper) {
		this.tradeRateMapper = tradeRateMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Object addRate(
			NormalOrders order,
			long orderIdNum,
			long companyId,
			long userId,
			String unionid,
			boolean anonymous,
			List<RateLine> rates) {
		try {
			return addRateInternal(order, orderIdNum, companyId, userId, unionid, anonymous, rates);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			String msg = e.getMessage();
			throw new ResourceException(msg == null || msg.isEmpty() ? "操作失败" : msg);
		}
	}

	private Object addRateInternal(
			NormalOrders order,
			long orderIdNum,
			long companyId,
			long userId,
			String unionid,
			boolean anonymous,
			List<RateLine> rates) {
		String orderClass = order.getOrderClass() == null ? "normal" : order.getOrderClass().trim();
		boolean pointsmall = "pointsmall".equalsIgnoreCase(orderClass);
		List<Map<String, Object>> result = new ArrayList<>();
		int now = (int) (System.currentTimeMillis() / 1000);
		String orderIdStr = String.valueOf(orderIdNum);

		for (RateLine line : rates) {
			LambdaQueryWrapper<TradeRate> existsQ =
					new LambdaQueryWrapper<TradeRate>()
							.eq(TradeRate::getCompanyId, companyId)
							.eq(TradeRate::getOrderId, orderIdStr)
							.eq(TradeRate::getItemId, line.getItemId());
			if (tradeRateMapper.selectCount(existsQ) > 0) {
				continue;
			}

			Map<String, Object> itemRow =
					pointsmall
							? tradeRateMapper.selectPointsmallItemsRowMapByItemIdAndCompanyId(
									line.getItemId(), companyId)
							: tradeRateMapper.selectItemsRowMapByItemIdAndCompanyId(
									line.getItemId(), companyId);
			if (itemRow == null || itemRow.isEmpty()) {
				throw new ResourceException("商品不存在");
			}

			long goodsId = extractGoodsId(itemRow);

			NormalOrdersItems row =
					normalOrdersItemsMapper.selectOne(
							new LambdaQueryWrapper<NormalOrdersItems>()
									.eq(NormalOrdersItems::getCompanyId, companyId)
									.eq(NormalOrdersItems::getOrderId, orderIdNum)
									.eq(NormalOrdersItems::getItemId, line.getItemId()));
			String itemSpecDesc =
					row == null || row.getItemSpecDesc() == null ? "" : row.getItemSpecDesc();

			TradeRate entity = new TradeRate();
			entity.setCompanyId(companyId);
			entity.setItemId(line.getItemId());
			entity.setGoodsId(goodsId);
			entity.setOrderId(orderIdStr);
			entity.setUserId(userId);
			entity.setUnionid(unionid);
			entity.setItemSpecDesc(itemSpecDesc);
			entity.setOrderType(pointsmall ? "pointsmall" : "normal");
			entity.setAnonymous(anonymous);
			entity.setContent(line.getContent());
			entity.setContentLen(
					line.getContent() == null
							? 0
							: line.getContent().getBytes(StandardCharsets.UTF_8).length);
			entity.setRatePic(line.getPics().isEmpty() ? "" : String.join(",", line.getPics()));
			entity.setRatePicNum(line.getPics().size());
			entity.setIsReply(false);
			entity.setDisabled(false);
			if (line.getStar() != 0) {
				entity.setStar(line.getStar());
			}
			entity.setCreated(now);
			entity.setUpdated(now);

			tradeRateMapper.insert(entity);

			LambdaUpdateWrapper<NormalOrdersItems> u =
					new LambdaUpdateWrapper<NormalOrdersItems>()
							.eq(NormalOrdersItems::getCompanyId, companyId)
							.eq(NormalOrdersItems::getOrderId, orderIdNum)
							.eq(NormalOrdersItems::getItemId, line.getItemId())
							.set(NormalOrdersItems::getIsRate, true);
			if (normalOrdersItemsMapper.update(null, u) != 1) {
				throw new ResourceException("订单商品不存在");
			}

			result.add(tradeRateEntityToSnakeMap(entity));
		}

		if (result.isEmpty()) {
			Map<String, Object> idle = new LinkedHashMap<>();
			idle.put("status", 0);
			return idle;
		}

		order.setIsRate(true);
		if (normalOrdersMapper.updateById(order) != 1) {
			throw new ResourceException("订单不存在");
		}
		return result;
	}

	private static long extractGoodsId(Map<String, Object> itemRow) {
		Object g1 = itemRow.get("goods_id");
		Object g2 = itemRow.get("goodsId");
		long from1 = longFromMapValue(g1);
		if (from1 > 0L) {
			return from1;
		}
		long from2 = longFromMapValue(g2);
		return from2 > 0L ? from2 : 0L;
	}

	private static long longFromMapValue(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, Object> tradeRateEntityToSnakeMap(TradeRate entity) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("rate_id", entity.getRateId());
		map.put("company_id", entity.getCompanyId());
		map.put("item_id", entity.getItemId());
		map.put("goods_id", entity.getGoodsId());
		map.put("order_id", entity.getOrderId());
		map.put("user_id", entity.getUserId());
		map.put("rate_pic", entity.getRatePic());
		map.put("rate_pic_num", entity.getRatePicNum());
		map.put("content", entity.getContent());
		map.put("content_len", entity.getContentLen());
		map.put("is_reply", entity.getIsReply() != null && entity.getIsReply() ? 1 : 0);
		map.put("disabled", entity.getDisabled() != null && entity.getDisabled() ? 1 : 0);
		map.put(
				"anonymous",
				entity.getAnonymous() != null && entity.getAnonymous() ? 1 : 0);
		if (entity.getStar() == null) {
			map.put("star", null);
		} else {
			map.put("star", entity.getStar().intValue());
		}
		map.put("created", entity.getCreated());
		map.put("updated", entity.getUpdated());
		map.put("unionid", entity.getUnionid());
		map.put("item_spec_desc", entity.getItemSpecDesc());
		map.put("order_type", entity.getOrderType());
		return map;
	}

	public static final class RateLine {
		private final long itemId;
		private final String content;
		private final int star;
		private final List<String> pics;

		public RateLine(long itemId, String content, int star, List<String> pics) {
			this.itemId = itemId;
			this.content = content;
			this.star = star;
			this.pics = pics == null ? Collections.emptyList() : pics;
		}

		public long getItemId() {
			return itemId;
		}

		public String getContent() {
			return content;
		}

		public int getStar() {
			return star;
		}

		public List<String> getPics() {
			return pics;
		}
	}
}
