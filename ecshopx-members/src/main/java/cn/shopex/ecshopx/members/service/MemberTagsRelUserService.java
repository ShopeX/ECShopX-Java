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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Admin-facing member–tag relation orchestration.
 * <p>
 * When both member identifiers and tag identifiers are supplied as collection- or array-shaped
 * payloads, normalized bulk writes go through {@link MemberRelTagsBatchCreateService#createRelTags};
 * that dual-array path aligns with the reference stack {@code UserData} {@code userRelTag} behaviour
 * when both {@code user_ids} and {@code tag_ids} are array-shaped inputs. After the transaction
 * commits, {@link cn.shopex.ecshopx.members.service.reltag.MemberTagRelationPushCoordinator} chooses
 * outbound relation sync (async dispatch for large batches, synchronous calls for smaller sets).
 * <p>
 * Admin surface: {@code POST /api/v1/member/reltag} with a merchant-operator JWT; API documentation
 * and exposure differ from the legacy wxapp reltag route (path and authentication model).
 */
@Service
public class MemberTagsRelUserService {

	private final MemberRelTagsBatchCreateService memberRelTagsBatchCreateService;

	public MemberTagsRelUserService(MemberRelTagsBatchCreateService memberRelTagsBatchCreateService) {
		this.memberRelTagsBatchCreateService = memberRelTagsBatchCreateService;
	}

	public void tagsRelUserDel(long companyId, Object rawUserId, Object rawTagId) {
		String tagStr = rawTagId == null ? "" : String.valueOf(rawTagId).trim();
		if ("crm".equals(tagStr)) {
			throw new ResourceException("标签不能关闭");
		}
		long userId = parseSingleUserIdScalar(rawUserId);
		long tagId = parseSingleTagIdScalar(rawTagId);
		memberRelTagsBatchCreateService.delRelMemberTag(companyId, userId, tagId);
	}

	/**
	 * When both {@code rawUserIds} and {@code rawTagIds} are collection- or array-shaped, normalizes
	 * distinct positive identifiers and delegates persisted bulk relation writes via
	 * {@link MemberRelTagsBatchCreateService#createRelTags}. That dual-collection path aligns with the
	 * reference stack {@code UserData} {@code userRelTag} behaviour when both {@code user_ids} and
	 * {@code tag_ids} are array-shaped inputs. After the transaction commits,
	 * {@link cn.shopex.ecshopx.members.service.reltag.MemberTagRelationPushCoordinator} chooses outbound
	 * relation sync (async dispatch for large batches, synchronous calls for smaller sets).
	 * <p>
	 * Admin surface: {@code POST /api/v1/member/reltag} with a merchant-operator JWT; API documentation
	 * and exposure differ from the legacy wxapp reltag route (path and authentication model).
	 *
	 * @param companyId tenant scope
	 * @param rawUserIds member id(s); iterable, array, or scalar depending on branch
	 * @param rawTagIds tag id(s); iterable, array, scalar, or comma-separated string depending on branch
	 */
	public void tagsRelUser(long companyId, Object rawUserIds, Object rawTagIds) {
		if (isIterableOrArrayPayload(rawUserIds) && isIterableOrArrayPayload(rawTagIds)) {
			List<Long> userIds = toDistinctPositiveLongList(rawUserIds);
			List<Long> tagIds = toDistinctPositiveLongList(rawTagIds);
			if (userIds.isEmpty()) {
				throw new BadRequestException("请选择会员");
			}
			if (tagIds.isEmpty()) {
				throw new BadRequestException("请选择标签");
			}
			memberRelTagsBatchCreateService.createRelTags(userIds, tagIds, companyId);
			return;
		}
		if (!isIterableOrArrayPayload(rawUserIds)) {
			long userId = parseSingleUserIdScalar(rawUserIds);
			List<Long> tagIds = normalizeTagIdsForReplaceBranch(rawTagIds);
			memberRelTagsBatchCreateService.createRelTagsByUserId(userId, tagIds, companyId);
			return;
		}
		List<Long> userIds = toDistinctPositiveLongList(rawUserIds);
		long tagId = parseSingleTagIdScalar(rawTagIds);
		if (userIds.isEmpty()) {
			throw new BadRequestException("请选择会员");
		}
		memberRelTagsBatchCreateService.createRelTagsByTagId(userIds, tagId, companyId);
	}

	private static boolean isIterableOrArrayPayload(Object o) {
		if (o instanceof Collection<?>) {
			return true;
		}
		return o != null && o.getClass().isArray() && !(o instanceof byte[]);
	}

	private static long parseSingleUserIdScalar(Object raw) {
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0) {
				throw new BadRequestException("请选择会员");
			}
			return v;
		}
		String s = raw == null ? "" : raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("请选择会员");
		}
		if ("0".equals(s)) {
			throw new BadRequestException("请选择会员");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0) {
				throw new BadRequestException("请选择会员");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("会员参数格式错误");
		}
	}

	private static long parseSingleTagIdScalar(Object raw) {
		if (raw instanceof String str && str.contains(",")) {
			throw new BadRequestException("标签参数格式错误");
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v <= 0) {
				throw new BadRequestException("标签参数格式错误");
			}
			return v;
		}
		String s = raw == null ? "" : raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("标签参数格式错误");
		}
		if ("0".equals(s)) {
			throw new BadRequestException("标签参数格式错误");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0) {
				throw new BadRequestException("标签参数格式错误");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("标签参数格式错误");
		}
	}

	private static List<Long> normalizeTagIdsForReplaceBranch(Object rawTagIds) {
		if (isIterableOrArrayPayload(rawTagIds)) {
			return toDistinctPositiveLongList(rawTagIds);
		}
		if (rawTagIds instanceof String s) {
			String[] parts = s.split(",", -1);
			LinkedHashSet<Long> ordered = new LinkedHashSet<>();
			for (String part : parts) {
				String t = part == null ? "" : part.trim();
				if (!StringUtils.hasText(t)) {
					throw new BadRequestException("标签参数格式错误");
				}
				long v;
				try {
					v = Long.parseLong(t);
				} catch (NumberFormatException e) {
					throw new BadRequestException("标签参数格式错误");
				}
				if (v <= 0) {
					throw new BadRequestException("标签参数格式错误");
				}
				ordered.add(v);
			}
			return new ArrayList<>(ordered);
		}
		if (rawTagIds instanceof Number n) {
			long v = n.longValue();
			if (v <= 0) {
				throw new BadRequestException("标签参数格式错误");
			}
			return List.of(v);
		}
		throw new BadRequestException("标签参数格式错误");
	}

	private static List<Long> toDistinctPositiveLongList(Object arrayLike) {
		LinkedHashSet<Long> set = new LinkedHashSet<>();
		if (arrayLike instanceof Collection<?> c) {
			for (Object o : c) {
				addParsedPositiveLong(o, set);
			}
			return new ArrayList<>(set);
		}
		if (arrayLike != null && arrayLike.getClass().isArray()) {
			addFromArray(arrayLike, set);
		}
		return new ArrayList<>(set);
	}

	private static void addFromArray(Object arr, LinkedHashSet<Long> set) {
		if (arr instanceof Object[] a) {
			for (Object o : a) {
				addParsedPositiveLong(o, set);
			}
		} else if (arr instanceof long[] a) {
			for (long x : a) {
				if (x > 0) {
					set.add(x);
				}
			}
		} else if (arr instanceof int[] a) {
			for (int x : a) {
				if (x > 0) {
					set.add((long) x);
				}
			}
		} else if (arr instanceof short[] a) {
			for (short x : a) {
				if (x > 0) {
					set.add((long) x);
				}
			}
		} else if (arr instanceof byte[] a) {
			if (a.length == 0) {
				return;
			}
			String s = new String(a, StandardCharsets.UTF_8).trim();
			if (!StringUtils.hasText(s)) {
				return;
			}
			for (String part : s.split(",", -1)) {
				addParsedPositiveLong(part, set);
			}
		} else if (arr instanceof char[] a) {
			if (a.length == 0) {
				return;
			}
			String s = String.valueOf(a).trim();
			if (!StringUtils.hasText(s)) {
				return;
			}
			for (String part : s.split(",", -1)) {
				addParsedPositiveLong(part, set);
			}
		} else if (arr instanceof float[] a) {
			for (float x : a) {
				addPositiveLongIfWholeNumber(x, set);
			}
		} else if (arr instanceof double[] a) {
			for (double x : a) {
				addPositiveLongIfWholeNumber(x, set);
			}
		} else if (arr instanceof boolean[]) {
			return;
		}
	}

	private static void addPositiveLongIfWholeNumber(double x, LinkedHashSet<Long> set) {
		if (x <= 0 || x > Long.MAX_VALUE) {
			return;
		}
		long lv = (long) x;
		if ((double) lv != x) {
			return;
		}
		set.add(lv);
	}

	private static void addParsedPositiveLong(Object o, LinkedHashSet<Long> set) {
		if (o == null) {
			return;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			if (v > 0) {
				set.add(v);
			}
			return;
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		try {
			long v = Long.parseLong(s);
			if (v > 0) {
				set.add(v);
			}
		} catch (NumberFormatException ignored) {
		}
	}
}
