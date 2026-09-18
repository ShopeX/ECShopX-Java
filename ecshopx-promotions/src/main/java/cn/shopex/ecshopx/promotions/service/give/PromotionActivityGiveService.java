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

package cn.shopex.ecshopx.promotions.service.give;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.promotions.port.ScheduleGivePromotionsActivityDispatchPublisher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PromotionActivityGiveService {

	private final ScheduleGivePromotionsActivityDispatchPublisher scheduleGivePromotionsActivityDispatchPublisher;

	public PromotionActivityGiveService(
			ScheduleGivePromotionsActivityDispatchPublisher scheduleGivePromotionsActivityDispatchPublisher) {
		this.scheduleGivePromotionsActivityDispatchPublisher = scheduleGivePromotionsActivityDispatchPublisher;
	}

	public void give(
			long companyId,
			long distributorId,
			String sender,
			List<Long> userIds,
			List<Long> couponCardIds,
			String sourceFrom) {
		scheduleGivePromotionsActivityDispatchPublisher.scheduleGive(
				companyId,
				distributorId,
				sender,
				userIds,
				couponCardIds,
				sourceFrom,
				Instant.now().getEpochSecond());
	}

	public static List<Long> parseRequiredLongArray(Object raw, String emptyMessage) {
		if (raw == null) {
			throw new ResourceException(emptyMessage);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new ResourceException(emptyMessage);
			}
			List<Long> out = new ArrayList<>();
			for (String part : t.split(",")) {
				String p = part.trim();
				if (p.isEmpty()) {
					throw new ResourceException(emptyMessage);
				}
				try {
					long v = Long.parseLong(p);
					if (v <= 0L) {
						throw new ResourceException(emptyMessage);
					}
					out.add(v);
				} catch (NumberFormatException e) {
					throw new ResourceException(emptyMessage);
				}
			}
			return List.copyOf(out);
		}
		if (raw instanceof Collection<?> c) {
			if (c.isEmpty()) {
				throw new ResourceException(emptyMessage);
			}
			List<Long> out = new ArrayList<>();
			for (Object el : c) {
				long v = parseLongElement(el, emptyMessage);
				if (v <= 0L) {
					throw new ResourceException(emptyMessage);
				}
				out.add(v);
			}
			return List.copyOf(out);
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0L) {
				throw new ResourceException(emptyMessage);
			}
			return List.of(v);
		}
		throw new ResourceException(emptyMessage);
	}

	private static long parseLongElement(Object el, String emptyMessage) {
		if (el == null) {
			throw new ResourceException(emptyMessage);
		}
		if (el instanceof Number n) {
			return n.longValue();
		}
		if (el instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new ResourceException(emptyMessage);
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new ResourceException(emptyMessage);
			}
		}
		String t = el.toString().trim();
		if (t.isEmpty()) {
			throw new ResourceException(emptyMessage);
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException(emptyMessage);
		}
	}
}
