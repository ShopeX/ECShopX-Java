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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsAuditUpdateService {

	private final ItemsRepository itemsRepository;

	public ItemsAuditUpdateService(ItemsRepository itemsRepository) {
		this.itemsRepository = itemsRepository;
	}

	public void auditItems(long companyId, Map<String, Object> merged) {
		String auditStatus = nullableString(merged.get("audit_status"));
		String auditReason = nullableString(merged.get("audit_reason"));
		ItemsRepository.GoodsIdClause clause = parseGoodsIdClause(merged.get("goods_id"));
		itemsRepository.updateAuditFieldsByCompanyAndGoodsId(companyId, clause, auditStatus, auditReason);
	}

	private static String nullableString(Object o) {
		if (o == null) {
			return null;
		}
		return o.toString();
	}

	private static ItemsRepository.GoodsIdClause parseGoodsIdClause(Object raw) {
		if (raw == null) {
			return new ItemsRepository.GoodsIdClause.IsNull();
		}
		if (raw instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (!StringUtils.hasText(s)) {
				return new ItemsRepository.GoodsIdClause.IsNull();
			}
			return new ItemsRepository.GoodsIdClause.SingleId(Long.parseLong(s));
		}
		if (raw instanceof Number n) {
			return new ItemsRepository.GoodsIdClause.SingleId(n.longValue());
		}
		List<Long> ids = toLongListFromSequence(raw);
		if (!ids.isEmpty()) {
			return new ItemsRepository.GoodsIdClause.InIds(ids);
		}
		if (isSequenceLike(raw)) {
			return new ItemsRepository.GoodsIdClause.Unrestricted();
		}
		return new ItemsRepository.GoodsIdClause.SingleId(parseLongElement(raw));
	}

	private static boolean isSequenceLike(Object raw) {
		return raw instanceof Collection<?>
				|| raw instanceof Object[]
				|| raw instanceof long[]
				|| raw instanceof int[]
				|| raw instanceof short[]
				|| raw instanceof byte[]
				|| raw instanceof Integer[]
				|| raw instanceof Long[];
	}

	private static List<Long> toLongListFromSequence(Object raw) {
		if (raw instanceof Collection<?> c) {
			List<Long> ids = new ArrayList<>();
			for (Object o : c) {
				if (o != null) {
					ids.add(parseLongElement(o));
				}
			}
			return ids;
		}
		if (raw instanceof Object[] arr) {
			List<Long> ids = new ArrayList<>();
			for (Object o : arr) {
				if (o != null) {
					ids.add(parseLongElement(o));
				}
			}
			return ids;
		}
		if (raw instanceof long[] arr) {
			List<Long> ids = new ArrayList<>(arr.length);
			for (long v : arr) {
				ids.add(v);
			}
			return ids;
		}
		if (raw instanceof int[] arr) {
			List<Long> ids = new ArrayList<>(arr.length);
			for (int v : arr) {
				ids.add((long) v);
			}
			return ids;
		}
		if (raw instanceof short[] arr) {
			List<Long> ids = new ArrayList<>(arr.length);
			for (short v : arr) {
				ids.add((long) v);
			}
			return ids;
		}
		if (raw instanceof byte[] arr) {
			List<Long> ids = new ArrayList<>(arr.length);
			for (byte v : arr) {
				ids.add((long) v);
			}
			return ids;
		}
		if (raw instanceof Integer[] arr) {
			List<Long> ids = new ArrayList<>();
			for (Integer o : arr) {
				if (o != null) {
					ids.add(o.longValue());
				}
			}
			return ids;
		}
		if (raw instanceof Long[] arr) {
			List<Long> ids = new ArrayList<>();
			for (Long o : arr) {
				if (o != null) {
					ids.add(o);
				}
			}
			return ids;
		}
		return List.of();
	}

	private static long parseLongElement(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
