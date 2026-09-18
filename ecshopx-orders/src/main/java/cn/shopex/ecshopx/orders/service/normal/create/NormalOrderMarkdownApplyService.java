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

package cn.shopex.ecshopx.orders.service.normal.create;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NormalOrderMarkdownApplyService {

	@SuppressWarnings("unchecked")
	public void applyIfMarkdownPresent(NormalOrderCreateParams p) {
		Object md = p.getParams().get("markdown");
		if (md == null) {
			return;
		}
		if (!(md instanceof Map<?, ?> markdownRaw)) {
			return;
		}
		Map<String, Object> markdown = (Map<String, Object>) markdownRaw;
		Map<String, Object> od = p.getOrderData();
		applyMarkdownPipeline(od, markdown);
	}

	public void applyMarkdownPreview(Map<String, Object> orderData, Map<String, Object> markdown) {
		applyMarkdownPipeline(orderData, markdown);
	}

	@SuppressWarnings("unchecked")
	private void applyMarkdownPipeline(Map<String, Object> od, Map<String, Object> markdown) {
		int origFreight = intVal(od.get("freight_fee"), 0);
		int pointFeeTotal = intVal(od.get("point_fee"), 0);
		int pointFreightFee = 0;
		if (pointFeeTotal > 0 && origFreight > 0) {
			List<Map<String, Object>> items = (List<Map<String, Object>>) od.get("items");
			int sumLinePoint = 0;
			if (items != null) {
				for (Map<String, Object> it : items) {
					sumLinePoint += intVal(it.get("point_fee"), 0);
				}
			}
			pointFreightFee = pointFeeTotal - sumLinePoint;
		}
		od.put("point_freight_fee", pointFreightFee);
		int cashFreight = origFreight - pointFreightFee;
		if (markdown.containsKey("freight_fee") && markdown.get("freight_fee") != null) {
			int overrideCash = intVal(markdown.get("freight_fee"), 0);
			if (overrideCash < 0) {
				throw new ResourceException("运费不能为负");
			}
			cashFreight = overrideCash;
		}
		int newFullFreight = cashFreight + pointFreightFee;
		long oldOrderTotal = longVal(od.get("total_fee"), 0L);
		od.put("total_fee", oldOrderTotal + (newFullFreight - origFreight));
		od.put("freight_fee", newFullFreight);

		String downType = String.valueOf(markdown.getOrDefault("down_type", ""));
		if ("total".equals(downType)) {
			applyTotalMarkdown(od, markdown, cashFreight);
		} else if ("items".equals(downType)) {
			applyItemsMarkdown(od, markdown, cashFreight);
		}
		long tf = longVal(od.get("total_fee"), 0L);
		od.put("item_total_fee", tf - intVal(od.get("freight_fee"), 0));
	}

	@SuppressWarnings("unchecked")
	private static void applyTotalMarkdown(Map<String, Object> od, Map<?, ?> markdown, int freightFee) {
		List<Map<String, Object>> items = (List<Map<String, Object>>) od.get("items");
		if (items == null || items.isEmpty()) {
			return;
		}
		long newTotal = longVal(markdown.get("total_fee"), -1L);
		if (newTotal < 0L) {
			return;
		}
		long oldSum = 0L;
		for (Map<String, Object> it : items) {
			oldSum += intVal(it.get("total_fee"), 0);
		}
		if (oldSum == newTotal) {
			return;
		}
		long left = newTotal;
		Map<String, Object> topDiscount = discountMapRoot(od);
		for (int i = 0; i < items.size(); i++) {
			Map<String, Object> it = items.get(i);
			int oldLine = intVal(it.get("total_fee"), 0);
			long newLine;
			if (i == items.size() - 1) {
				newLine = left;
			} else {
				newLine = BigDecimal.valueOf(newTotal)
						.multiply(BigDecimal.valueOf(oldLine))
						.divide(BigDecimal.valueOf(oldSum), 0, RoundingMode.DOWN)
						.longValue();
			}
			int markDown = oldLine - (int) newLine;
			if (markDown < 0) {
				throw new ResourceException("改价金额不能超过商品原价");
			}
			if (markDown != 0) {
				it.put("discount_fee", intVal(it.get("discount_fee"), 0) + markDown);
				Map<String, Object> di = singleMarkDownDiscount(markDown);
				mergeItemDiscount(it, di);
				it.put("total_fee", (int) newLine);
			}
			left -= newLine;
		}
		int markDownOrder = (int) (oldSum - newTotal);
		if (markDownOrder < 0) {
			throw new ResourceException("改价金额不能超过商品原价");
		}
		od.put("discount_fee", intVal(od.get("discount_fee"), 0) + markDownOrder);
		topDiscount.put("mark_down", orderMarkDownEntry(markDownOrder));
		od.put("total_fee", newTotal + freightFee);
	}

	@SuppressWarnings("unchecked")
	private static void applyItemsMarkdown(Map<String, Object> od, Map<?, ?> markdown, int freightFee) {
		Object itemsMdRaw = markdown.get("items");
		if (!(itemsMdRaw instanceof List<?> mdList)) {
			return;
		}
		Map<Long, Map<String, Object>> byItem = new LinkedHashMap<>();
		for (Object o : mdList) {
			if (o instanceof Map<?, ?> m) {
				long iid = longVal(m.get("item_id"), 0L);
				if (iid > 0L) {
					@SuppressWarnings("unchecked")
					Map<String, Object> mm = (Map<String, Object>) m;
					byItem.put(iid, mm);
				}
			}
		}
		List<Map<String, Object>> items = (List<Map<String, Object>>) od.get("items");
		if (items == null) {
			return;
		}
		long orderExtraDiscount = 0L;
		for (Map<String, Object> it : items) {
			long itemId = longVal(it.get("item_id"), 0L);
			Map<String, Object> spec = byItem.get(itemId);
			if (spec == null) {
				continue;
			}
			int oldLine = intVal(it.get("total_fee"), 0);
			long newLine;
			if (spec.containsKey("discount") && spec.get("discount") != null) {
				int disc = intVal(spec.get("discount"), 0);
				newLine = BigDecimal.valueOf(oldLine)
						.multiply(BigDecimal.valueOf(disc))
						.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
						.longValue();
			} else {
				newLine = longVal(spec.get("total_fee"), oldLine);
			}
			int markDown = oldLine - (int) newLine;
			if (markDown < 0) {
				throw new ResourceException("改价金额不能超过商品原价");
			}
			if (markDown != 0) {
				it.put("discount_fee", intVal(it.get("discount_fee"), 0) + markDown);
				mergeItemDiscount(it, singleMarkDownDiscount(markDown));
				it.put("total_fee", (int) newLine);
				orderExtraDiscount += markDown;
			}
		}
		if (orderExtraDiscount > 0) {
			discountMapRoot(od).put("mark_down", orderMarkDownEntry((int) orderExtraDiscount));
			od.put("discount_fee", intVal(od.get("discount_fee"), 0) + (int) orderExtraDiscount);
		}
		long sum = 0L;
		for (Map<String, Object> it : items) {
			sum += intVal(it.get("total_fee"), 0);
		}
		od.put("item_fee", String.valueOf(sum));
		od.put("total_fee", sum + freightFee);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> discountMapRoot(Map<String, Object> od) {
		Object di = od.get("discount_info");
		if (di instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		Map<String, Object> m = new LinkedHashMap<>();
		od.put("discount_info", m);
		return m;
	}

	@SuppressWarnings("unchecked")
	private static void mergeItemDiscount(Map<String, Object> item, Map<String, Object> entry) {
		Object cur = item.get("discount_info");
		List<Map<String, Object>> lst;
		if (cur instanceof List<?> l) {
			lst = new ArrayList<>();
			for (Object o : l) {
				if (o instanceof Map<?, ?> mm) {
					lst.add((Map<String, Object>) mm);
				}
			}
		} else {
			lst = new ArrayList<>();
		}
		lst.add(entry);
		item.put("discount_info", lst);
	}

	private static Map<String, Object> singleMarkDownDiscount(int fee) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("type", "mark_down");
		m.put("info", "订单改价");
		m.put("rule", "订单改价优惠");
		m.put("discount_fee", fee);
		return m;
	}

	private static Map<String, Object> orderMarkDownEntry(int fee) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("type", "mark_down");
		m.put("info", "订单改价");
		m.put("rule", "订单改价优惠");
		m.put("discount_fee", fee);
		return m;
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
