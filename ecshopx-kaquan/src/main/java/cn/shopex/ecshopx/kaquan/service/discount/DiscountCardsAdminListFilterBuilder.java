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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DiscountCardsAdminListFilterBuilder {

	private static final List<String> DEFAULT_CARD_TYPES = List.of("cash", "discount", "new_gift", "money");

	public LambdaQueryWrapper<DiscountCards> build(long companyId, Map<String, Object> merged, String from,
			long sourceId, long nowEpochSeconds) {
		LambdaQueryWrapper<DiscountCards> w = new LambdaQueryWrapper<>();
		w.eq(DiscountCards::getCompanyId, companyId);

		List<Integer> statusList = parseStatusIn(merged.get("status"));
		if (!statusList.isEmpty()) {
			w.in(DiscountCards::getKqStatus, statusList);
		}

		applyDateStatus(w, merged.get("date_status"), nowEpochSeconds);
		applyCardType(w, merged.get("card_type"));

		Object titleObj = merged.get("title");
		if (titleObj != null && StringUtils.hasText(String.valueOf(titleObj).trim())) {
			w.like(DiscountCards::getTitle, String.valueOf(titleObj).trim());
		}

		int isGuide = DiscountCardParamNormalize.parseIntFlexible(merged.get("is_guide"), 100);
		if (isGuide != 100) {
			if (isGuide == 1) {
				w.eq(DiscountCards::getCouponType, "guide");
			} else {
				w.eq(DiscountCards::getCouponType, "mall");
			}
		}

		boolean storeSelf = "true".equals(DiscountCardParamNormalize.stringVal(merged.get("store_self")));
		if (storeSelf) {
			String pattern = "%" + "%,%" + "%";
			w.apply("distributor_id LIKE {0}", pattern);
		} else {
			Object distObj = merged.get("distributor_id");
			if (distObj != null && StringUtils.hasText(String.valueOf(distObj).trim())) {
				String id = String.valueOf(distObj).trim();
				w.like(DiscountCards::getDistributorId, "," + id + ",");
			}
		}

		Object receiveObj = merged.get("receive");
		if (receiveObj != null && StringUtils.hasText(String.valueOf(receiveObj).trim())) {
			w.eq(DiscountCards::getReceive, String.valueOf(receiveObj).trim());
		}

		if ("btn".equals(from)) {
			if (sourceId > 0L) {
				w.eq(DiscountCards::getSourceId, sourceId);
			}
			int threshold = (int) Math.min(nowEpochSeconds, Integer.MAX_VALUE);
			w.and(q -> q.gt(DiscountCards::getEndDate, threshold)
					.or()
					.eq(DiscountCards::getEndDate, 0)
					.or()
					.isNull(DiscountCards::getEndDate));
		}

		return w;
	}

	private static void applyDateStatus(LambdaQueryWrapper<DiscountCards> w, Object raw, long nowEpochSeconds) {
		if (raw == null) {
			return;
		}
		List<Integer> statuses = new ArrayList<>();
		for (String s : DiscountCardParamNormalize.normalizeToStringList(raw)) {
			int v = DiscountCardParamNormalize.parseIntFlexible(s, 0);
			if (v == 1 || v == 2 || v == 3) {
				statuses.add(v);
			}
		}
		if (statuses.isEmpty()) {
			return;
		}
		int now = (int) Math.min(nowEpochSeconds, Integer.MAX_VALUE);
		// 悬空 q.or() 后接嵌套 and(...) 会被 MP 拼成 AND，多值筛选恒为空；每个 date_status 分支须整体参与 OR。
		w.and(q -> {
			boolean first = true;
			for (Integer ds : statuses) {
				if (first) {
					appendDateStatusBranch(q, ds, now);
					first = false;
				} else {
					q.or(n -> appendDateStatusBranch(n, ds, now));
				}
			}
		});
	}

	private static void appendDateStatusBranch(LambdaQueryWrapper<DiscountCards> q, int ds, int now) {
		switch (ds) {
			case 1 -> q.and(n -> n.gt(DiscountCards::getBeginDate, 0).gt(DiscountCards::getBeginDate, now));
			case 2 -> q.and(n -> n.and(m -> m.lt(DiscountCards::getBeginDate, now).gt(DiscountCards::getEndDate, now))
					.or()
					.eq(DiscountCards::getEndDate, 0));
			case 3 -> q.and(n -> n.gt(DiscountCards::getEndDate, 0).lt(DiscountCards::getEndDate, now));
			default -> {
			}
		}
	}

	private static void applyCardType(LambdaQueryWrapper<DiscountCards> w, Object raw) {
		if (raw == null || !StringUtils.hasText(String.valueOf(raw).trim())) {
			w.in(DiscountCards::getCardType, DEFAULT_CARD_TYPES);
			return;
		}
		String ct = String.valueOf(raw).trim();
		if ("all".equalsIgnoreCase(ct)) {
			w.in(DiscountCards::getCardType, DEFAULT_CARD_TYPES);
			return;
		}
		List<String> parts = DiscountCardParamNormalize.normalizeToStringList(raw);
		if (parts.isEmpty()) {
			w.in(DiscountCards::getCardType, DEFAULT_CARD_TYPES);
			return;
		}
		if (parts.size() == 1) {
			w.eq(DiscountCards::getCardType, parts.get(0).toLowerCase(Locale.ROOT));
		} else {
			List<String> lowered = new ArrayList<>(parts.size());
			for (String p : parts) {
				lowered.add(p.toLowerCase(Locale.ROOT));
			}
			w.in(DiscountCards::getCardType, lowered);
		}
	}

	private static List<Integer> parseStatusIn(Object raw) {
		if (raw == null) {
			return List.of();
		}
		List<String> tokens = new ArrayList<>();
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				String s = String.valueOf(o).trim();
				if (!s.isEmpty()) {
					tokens.add(s);
				}
			}
		} else {
			String s = String.valueOf(raw).trim();
			if (s.isEmpty()) {
				return List.of();
			}
			for (String p : s.split(",")) {
				String t = p.trim();
				if (!t.isEmpty()) {
					tokens.add(t);
				}
			}
		}
		Set<Integer> out = new LinkedHashSet<>();
		for (String tok : tokens) {
			try {
				out.add(Integer.parseInt(tok.trim()));
			} catch (NumberFormatException ignored) {
			}
		}
		return new ArrayList<>(out);
	}
}
