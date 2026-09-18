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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v1;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiDiscountCardListPresentationService {

	private static final String DATE_TYPE_FIX_TIME_RANGE = "DATE_TYPE_FIX_TIME_RANGE";
	private static final String DATE_TYPE_FIX_TERM = "DATE_TYPE_FIX_TERM";

	private static final Map<String, String> CARD_TYPE_OPTS = Map.of(
			"discount", "折扣券",
			"cash", "代金券",
			"gift", "兑换券");

	private static final Map<String, String> CHANNEL_OPTS = Map.of(
			"wechat", "微信·",
			"alipay", "支付宝",
			"all", "全渠道");

	private static final Map<String, String> CARD_SOURCE_OPTS = Map.of(
			"local", "本地",
			"topi", "TOPI",
			"zhibo", "zhibo",
			"merch", "商家券");

	private static final DateTimeFormatter EPOCH_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public void apply(Map<String, Object> result, List<Map<String, Object>> list) {
		if (list != null && !list.isEmpty()) {
			long nowEpoch = nowEpochSeconds();
			for (int i = 0; i < list.size(); i++) {
				Map<String, Object> row = list.get(i);
				String takeEffect = "";
				String beginTime = "";
				String endTime = "";

				String dateType = String.valueOf(row.get("date_type"));
				if (DATE_TYPE_FIX_TIME_RANGE.equals(dateType)) {
					beginTime = formatEpoch(row.get("begin_date"));
					endTime = formatEpoch(row.get("end_date"));
				} else if (DATE_TYPE_FIX_TERM.equals(dateType)) {
					int relativeDays = (intValueFlexible(row.get("begin_date")) - (int) nowEpoch) / 86400;
					String beginPart = (relativeDays == 0) ? "当" : String.valueOf(relativeDays);
					Object fixedTerm = row.get("fixed_term");
					takeEffect = "领取后" + beginPart + "天生效," + fixedTerm + "天有效";
				}

				Map<String, Object> card = new LinkedHashMap<>();
				card.put("card_id", row.get("card_id"));
				card.put("title", row.get("title"));
				card.put("color", row.get("color"));
				String cardType = String.valueOf(row.get("card_type"));
				card.put("card_type", cardType);
				card.put("date_type", dateType);
				card.put("card_type_val", CARD_TYPE_OPTS.getOrDefault(cardType, ""));
				card.put("discount", row.get("discount"));
				card.put("least_cost", row.get("least_cost"));
				card.put("reduce_cost", row.get("reduce_cost"));
				card.put("quantity", intValueFlexible(row.get("quantity")));
				card.put("get_num", intValueFlexible(row.get("get_num")));

				int cardNum = 0;
				int quantity = intValueFlexible(row.get("quantity"));
				int getNum = intValueFlexible(row.get("get_num"));
				if (quantity > 0 && getNum > 0 && quantity > getNum) {
					cardNum = quantity - getNum;
				}
				card.put("card_num", cardNum);

				if (!takeEffect.isEmpty()) {
					card.put("takeEffect", takeEffect);
					card.put("begin_time", "");
					card.put("end_time", "");
					list.set(i, card);
				} else if (!beginTime.isEmpty() && !endTime.isEmpty()) {
					card.put("begin_time", beginTime);
					card.put("end_time", endTime);
					card.put("takeEffect", "");
					list.set(i, card);
				}
			}
		}

		Map<String, Object> opts = new LinkedHashMap<>();
		opts.put("card_type", CARD_TYPE_OPTS);
		opts.put("channel", CHANNEL_OPTS);
		opts.put("card_source", CARD_SOURCE_OPTS);
		result.put("opts", opts);
		result.remove("pagers");
		result.put("list", list != null ? list : List.of());
	}

	private static int intValueFlexible(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o == null) {
			return 0;
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String formatEpoch(Object epochSeconds) {
		if (epochSeconds == null) {
			return "";
		}
		long epoch = intValueFlexible(epochSeconds);
		if (epoch <= 0) {
			return "";
		}
		return Instant.ofEpochSecond(epoch)
				.atZone(ZoneId.systemDefault())
				.format(EPOCH_FORMATTER);
	}

	private static long nowEpochSeconds() {
		return Instant.now().getEpochSecond();
	}
}
