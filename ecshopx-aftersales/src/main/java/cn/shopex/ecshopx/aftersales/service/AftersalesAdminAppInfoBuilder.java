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

package cn.shopex.ecshopx.aftersales.service;

import cn.shopex.ecshopx.common.port.order.OrderNormalOrderServiceOrderDataReadPort;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AftersalesAdminAppInfoBuilder {

	private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
	private static final DateTimeFormatter UPDATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final OrderNormalOrderServiceOrderDataReadPort orderNormalOrderServiceOrderDataReadPort;

	public AftersalesAdminAppInfoBuilder(
			OrderNormalOrderServiceOrderDataReadPort orderNormalOrderServiceOrderDataReadPort) {
		this.orderNormalOrderServiceOrderDataReadPort = orderNormalOrderServiceOrderDataReadPort;
	}

	public Map<String, Object> buildAppInfo(
			Map<String, Object> aftersaleMainRow,
			long orderId,
			long companyId,
			Map<Long, Optional<Map<String, Object>>> orderHeaderCache,
			boolean isDetail) {
		Optional<Map<String, Object>> orderInfo =
				orderHeaderCache.computeIfAbsent(
						orderId,
						oid ->
								oid > 0
										? orderNormalOrderServiceOrderDataReadPort.getServiceOrderData(
												companyId, oid)
										: Optional.empty());

		int ctime = intVal(aftersaleMainRow.get("create_time"));
		String createDate =
				ctime > 0
						? Instant.ofEpochSecond(ctime).atZone(DEFAULT_ZONE).format(UPDATE_TIME_FMT)
						: "";

		List<String> buttonKeys = new ArrayList<>();
		if (!isDetail) {
			buttonKeys.add("mark");
		}
		int progress = intVal(aftersaleMainRow.get("progress"));
		if (progress == 0) {
			buttonKeys.add("check");
			buttonKeys.add("contact");
		} else if (progress == 2) {
			buttonKeys.add("confirm");
			buttonKeys.add("contact");
		} else {
			buttonKeys.add("contact");
		}
		List<Map<String, Object>> buttons = buttonMaps(buttonKeys);

		String statusMsg = statusMsg(intVal(aftersaleMainRow.get("aftersales_status")));
		String progressMsg = progressMsgList(aftersaleMainRow, progress, isDetail);

		Map<String, Object> app = new LinkedHashMap<>();
		app.put("buttons", buttons);
		app.put("progress_msg", progressMsg);
		app.put("status_msg", statusMsg);
		app.put("order_info", orderInfo.orElseGet(LinkedHashMap::new));
		app.put("create_date", createDate);
		return app;
	}

	private static String statusMsg(int aftersalesStatus) {
		return switch (aftersalesStatus) {
			case 0 -> "待处理";
			case 1 -> "处理中";
			case 2 -> "已处理";
			case 3 -> "已驳回";
			case 4 -> "已关闭";
			default -> "";
		};
	}

	private String progressMsgList(Map<String, Object> aftersale, int progress, boolean isDetail) {
		String type = str(aftersale.get("aftersales_type"));
		String returnType = str(aftersale.get("return_type"));
		return switch (progress) {
			case 0 -> {
				String base = "等待商家处理";
				if (!isDetail) {
					if ("ONLY_REFUND".equals(type)) {
						yield base + "-仅退款";
					}
					yield base + "-退货退款";
				}
				yield base;
			}
			case 1 -> "商家接受申请，等待消费者回寄";
			case 2 -> "offline".equalsIgnoreCase(returnType) ? "消费者已到店退货" : "消费者回寄，等待商家收货确认";
			case 8 -> "商家确认收货，等待审核退款";
			case 3 -> "售后已驳回";
			case 4 -> "售后已处理";
			case 7 -> "消费者已撤销";
			case 9 -> "退款处理中";
			case 5 -> "退款已驳回";
			case 6 -> "已完成，关闭";
			default -> "";
		};
	}

	private static List<Map<String, Object>> buttonMaps(List<String> types) {
		Map<String, String> names =
				Map.of(
						"mark", "备注",
						"contact", "联系客户",
						"check", "处理售后",
						"confirm", "确认收货");
		List<Map<String, Object>> out = new ArrayList<>();
		for (String t : types) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("type", t);
			m.put("name", names.getOrDefault(t, t));
			out.add(m);
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

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
