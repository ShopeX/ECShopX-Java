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

package cn.shopex.ecshopx.members.dispatch;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.service.admin.dto.AdminMemberBatchOperatingMemberQueryFilter;
import cn.shopex.ecshopx.members.service.export.AdminMemberExportJobContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AdminMemberExportFileJobPayloadSupport {

	private AdminMemberExportFileJobPayloadSupport() {}

	public static Map<String, Object> toPayload(AdminMemberExportJobContext ctx) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", AdminMemberExportFileJobTypes.TYPE_ADMIN_MEMBER_EXPORT);
		payload.put("company_id", ctx.companyId());
		payload.put("operator_id", ctx.operatorId());
		payload.put("supplier_id", ctx.supplierId());
		payload.put("merchant_id", ctx.merchantId());
		payload.put("datapass_block", ctx.datapassBlock() ? 1 : 0);
		payload.put("filter", toFilterMap(ctx.queryFilter()));
		return payload;
	}

	public static AdminMemberExportJobContext contextFromPayload(Map<String, Object> payload) {
		long companyId = asLong(payload.get("company_id"));
		long operatorId = asLong(payload.get("operator_id"));
		long supplierId = asLong(payload.get("supplier_id"));
		long merchantId = asLong(payload.get("merchant_id"));
		boolean datapassBlock = asInt(payload.get("datapass_block")) != 0;
		Object rawFilter = payload.get("filter");
		if (!(rawFilter instanceof Map<?, ?> rawMap)) {
			throw new BadRequestException("payload.filter must be a map");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> filterMap = (Map<String, Object>) rawMap;
		AdminMemberBatchOperatingMemberQueryFilter filter = filterFromMap(filterMap);
		return new AdminMemberExportJobContext(companyId, operatorId, supplierId, merchantId, datapassBlock, filter);
	}

	private static Map<String, Object> toFilterMap(AdminMemberBatchOperatingMemberQueryFilter f) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", f.getCompanyId());
		if (f.getMembersGradeId() != null) {
			m.put("members_grade_id", f.getMembersGradeId());
		}
		if (f.getUserIdsIn() != null && !f.getUserIdsIn().isEmpty()) {
			m.put("user_ids_in", f.getUserIdsIn());
		}
		if (f.getUserIdsNotIn() != null && !f.getUserIdsNotIn().isEmpty()) {
			m.put("user_ids_not_in", f.getUserIdsNotIn());
		}
		if (f.getMobileEqEncrypted() != null) {
			m.put("mobile_eq_encrypted", f.getMobileEqEncrypted());
		}
		if (f.getRemarksLike() != null) {
			m.put("remarks_like", f.getRemarksLike());
		}
		if (f.getInviterId() != null) {
			m.put("inviter_id", f.getInviterId());
		}
		if (f.getUserCardCode() != null) {
			m.put("user_card_code", f.getUserCardCode());
		}
		if (f.getUsernameEqEncrypted() != null) {
			m.put("username_eq_encrypted", f.getUsernameEqEncrypted());
		}
		if (f.getNameEq() != null) {
			m.put("name_eq", f.getNameEq());
		}
		if (f.getCreatedGte() != null) {
			m.put("created_gte", f.getCreatedGte());
		}
		if (f.getCreatedLte() != null) {
			m.put("created_lte", f.getCreatedLte());
		}
		if (f.getBirthdayGte() != null) {
			m.put("birthday_gte", f.getBirthdayGte());
		}
		if (f.getBirthdayLte() != null) {
			m.put("birthday_lte", f.getBirthdayLte());
		}
		if (f.getHaveConsume() != null) {
			m.put("have_consume", f.getHaveConsume());
		}
		if (f.getShopIds() != null && !f.getShopIds().isEmpty()) {
			m.put("shop_ids", f.getShopIds());
		}
		if (f.getDistributorIds() != null && !f.getDistributorIds().isEmpty()) {
			m.put("distributor_ids", f.getDistributorIds());
		}
		if (f.getTagIds() != null && !f.getTagIds().isEmpty()) {
			m.put("tag_ids", f.getTagIds());
		}
		if (f.getPointGte() != null) {
			m.put("point_gte", f.getPointGte());
		}
		if (f.getPointLte() != null) {
			m.put("point_lte", f.getPointLte());
		}
		if (f.getPointEq() != null) {
			m.put("point_eq", f.getPointEq());
		}
		return m;
	}

	private static AdminMemberBatchOperatingMemberQueryFilter filterFromMap(Map<String, Object> m) {
		AdminMemberBatchOperatingMemberQueryFilter f = new AdminMemberBatchOperatingMemberQueryFilter();
		f.setCompanyId(asLong(m.get("company_id")));
		Long membersGradeId = asLongObject(m.get("members_grade_id"));
		if (membersGradeId != null) {
			f.setMembersGradeId(membersGradeId);
		}
		List<Long> userIdsIn = toLongList(m.get("user_ids_in"));
		if (userIdsIn != null) {
			f.setUserIdsIn(userIdsIn);
		}
		List<Long> userIdsNotIn = toLongList(m.get("user_ids_not_in"));
		if (userIdsNotIn != null) {
			f.setUserIdsNotIn(userIdsNotIn);
		}
		putStringIfPresent(m, "mobile_eq_encrypted", f::setMobileEqEncrypted);
		putStringIfPresent(m, "remarks_like", f::setRemarksLike);
		Long inviterId = asLongObject(m.get("inviter_id"));
		if (inviterId != null) {
			f.setInviterId(inviterId);
		}
		putStringIfPresent(m, "user_card_code", f::setUserCardCode);
		putStringIfPresent(m, "username_eq_encrypted", f::setUsernameEqEncrypted);
		putStringIfPresent(m, "name_eq", f::setNameEq);
		Long createdGte = asLongObject(m.get("created_gte"));
		if (createdGte != null) {
			f.setCreatedGte(createdGte);
		}
		Long createdLte = asLongObject(m.get("created_lte"));
		if (createdLte != null) {
			f.setCreatedLte(createdLte);
		}
		putStringIfPresent(m, "birthday_gte", f::setBirthdayGte);
		putStringIfPresent(m, "birthday_lte", f::setBirthdayLte);
		Boolean haveConsume = asBooleanObject(m.get("have_consume"));
		if (haveConsume != null) {
			f.setHaveConsume(haveConsume);
		}
		List<Long> shopIds = toLongList(m.get("shop_ids"));
		if (shopIds != null) {
			f.setShopIds(shopIds);
		}
		List<Long> distributorIds = toLongList(m.get("distributor_ids"));
		if (distributorIds != null) {
			f.setDistributorIds(distributorIds);
		}
		List<Long> tagIds = toLongList(m.get("tag_ids"));
		if (tagIds != null) {
			f.setTagIds(tagIds);
		}
		Long pointGte = asLongObject(m.get("point_gte"));
		if (pointGte != null) {
			f.setPointGte(pointGte);
		}
		Long pointLte = asLongObject(m.get("point_lte"));
		if (pointLte != null) {
			f.setPointLte(pointLte);
		}
		Long pointEq = asLongObject(m.get("point_eq"));
		if (pointEq != null) {
			f.setPointEq(pointEq);
		}
		return f;
	}

	private static void putStringIfPresent(
			Map<String, Object> m, String key, java.util.function.Consumer<String> setter) {
		Object v = m.get(key);
		if (v != null) {
			setter.accept(String.valueOf(v));
		}
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static int asInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v).trim());
	}

	private static Long asLongObject(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return null;
		}
		return Long.parseLong(s);
	}

	private static Boolean asBooleanObject(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		return Boolean.parseBoolean(String.valueOf(v));
	}

	private static List<Long> toLongList(Object o) {
		if (o == null) {
			return null;
		}
		if (!(o instanceof List<?> raw)) {
			return null;
		}
		List<Long> out = new ArrayList<>(raw.size());
		for (Object e : raw) {
			if (e instanceof Number n) {
				out.add(n.longValue());
			} else {
				out.add(Long.parseLong(String.valueOf(e).trim()));
			}
		}
		return out.isEmpty() ? null : out;
	}
}
