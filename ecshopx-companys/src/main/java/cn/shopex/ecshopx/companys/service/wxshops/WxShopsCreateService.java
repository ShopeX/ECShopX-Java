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

package cn.shopex.ecshopx.companys.service.wxshops;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.WxShopsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class WxShopsCreateService {

	private static final Pattern CONTRACT_PHONE =
			Pattern.compile("^\\d{3,4}-?\\d{7,8}(-?\\d{2,6})?$");
	private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("HH:mm");

	private final WxShopsMapper wxShopsMapper;
	private final ObjectMapper objectMapper;

	public WxShopsCreateService(WxShopsMapper wxShopsMapper, ObjectMapper objectMapper) {
		this.wxShopsMapper = wxShopsMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> createWxShop(
			Map<String, Object> input, Map<String, Object> operatorJwt, long companyId) {
		assertShopIdsGate(operatorJwt.get("shop_ids"));
		Map<String, Object> m = input;

		if (isEmptyLikeRequired(m.get("store_name"))) {
			throw new BadRequestException("请填写门店名称");
		}
		if (isEmptyLikeRequired(m.get("address"))) {
			throw new BadRequestException("请填写门店经纬度");
		}
		Object picRaw = m.get("pic_list");
		if (isEmptyLikeRequired(picRaw)) {
			throw new BadRequestException("请上传门店图片");
		}
		List<String> picUrls = normalizePicListToNonEmptyStrings(picRaw);
		for (String u : picUrls) {
			if (!isValidHttpUrl(u)) {
				throw new BadRequestException("请填写正确图片地址.");
			}
		}

		if (isEmptyLikeRequired(m.get("contract_phone"))) {
			throw new BadRequestException("请填写客服电话");
		}
		String contractPhone = stringTrimmed(m.get("contract_phone"));
		if (!CONTRACT_PHONE.matcher(contractPhone).matches()) {
			throw new BadRequestException("客服电话格式错误");
		}

		Object hourRaw = m.get("hour");
		if (isEmptyLikeRequired(hourRaw)) {
			throw new BadRequestException("请选择营业时间");
		}
		List<Object> hourPair = asLengthTwoList(hourRaw);
		ZoneId zone = ZoneId.systemDefault();
		String hourStr =
				formatHourSegment(hourPair.get(0), zone) + " - " + formatHourSegment(hourPair.get(1), zone);

		int addType = parseAddTypeInSetOrThrow(m);

		if (addType == 2) {
			if (isEmptyLikeRequired(m.get("company_name"))) {
				throw new BadRequestException("请填写经营资质名称");
			}
		}
		if (m.containsKey("company_name") && m.get("company_name") != null) {
			String cn = stringTrimmed(m.get("company_name"));
			if (!cn.isEmpty() && cn.length() > 30) {
				throw new BadRequestException("经营资质名称长度超过限制");
			}
		}
		if (m.containsKey("credential") && m.get("credential") != null) {
			String cred = stringTrimmed(m.get("credential"));
			if (!cred.isEmpty() && cred.length() > 30) {
				throw new BadRequestException("经营资质证件号长度超过限制");
			}
		}
		if (addType == 2) {
			assertQualificationListRequired(m.get("qualification_list"));
		}

		String picListJson;
		try {
			picListJson = objectMapper.writeValueAsString(picUrls);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}

		WxShops entity = new WxShops();

		long count =
				wxShopsMapper.selectCount(new LambdaQueryWrapper<WxShops>().eq(WxShops::getCompanyId, companyId));
		if (count == 0) {
			entity.setIsDefault(true);
		}

		applyJwtDistributor(entity, operatorJwt);
		applyOptionalTruthyFields(entity, m);

		entity.setAddress(stringTrimmed(m.get("address")));
		entity.setPicList(picListJson);
		entity.setContractPhone(contractPhone);
		entity.setHour(hourStr);
		entity.setStoreName(stringTrimmed(m.get("store_name")));
		entity.setAddType(addType);
		entity.setCompanyId(companyId);

		applyStatusIfTruthy(entity, m);
		applyIsDirectStoreAndIsOpen(entity, m);

		int nowUnix = (int) (System.currentTimeMillis() / 1000);
		entity.setCreated(nowUnix);
		entity.setUpdated(nowUnix);

		try {
			wxShopsMapper.insert(entity);
		} catch (IllegalArgumentException e) {
			throw mapEntityIllegalArg(e);
		}

		return buildResponseMap(entity);
	}

	private static BadRequestException mapEntityIllegalArg(IllegalArgumentException e) {
		String msg = e.getMessage();
		if (msg != null
				&& (msg.contains("add_type")
						|| msg.contains("is_domestic")
						|| msg.contains("is_direct_store")
						|| msg.contains("status"))) {
			return new BadRequestException(msg);
		}
		return new BadRequestException(msg != null ? msg : "参数错误");
	}

	private void applyJwtDistributor(WxShops entity, Map<String, Object> jwt) {
		if (!jwt.containsKey("distributor_id")) {
			return;
		}
		Object dRaw = jwt.get("distributor_id");
		if (dRaw == null) {
			return;
		}
		long parsed = parseDistributorIdStrict(dRaw);
		entity.setDistributorId(parsed);
		if (parsed != 0L) {
			entity.setExpiredAt(4701945600L);
		}
	}

	private static long parseDistributorIdStrict(Object v) {
		try {
			if (v instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("分销商ID格式错误");
		}
	}

	private void applyOptionalTruthyFields(WxShops entity, Map<String, Object> m) {
		if (m.containsKey("map_poi_id") && isTruthyValue(m.get("map_poi_id"))) {
			entity.setMapPoiId(stringTrimmed(m.get("map_poi_id")));
		}
		if (m.containsKey("poi_id") && isTruthyValue(m.get("poi_id"))) {
			entity.setPoiId(stringTrimmed(m.get("poi_id")));
		}
		if (m.containsKey("lng") && isTruthyValue(m.get("lng"))) {
			entity.setLng(stringTrimmed(m.get("lng")));
		}
		if (m.containsKey("lat") && isTruthyValue(m.get("lat"))) {
			entity.setLat(stringTrimmed(m.get("lat")));
		}
		if (m.containsKey("category") && isTruthyValue(m.get("category"))) {
			entity.setCategory(stringTrimmed(m.get("category")));
		}
		if (m.containsKey("credential") && isTruthyValue(m.get("credential"))) {
			entity.setCredential(stringTrimmed(m.get("credential")));
		}
		if (m.containsKey("company_name") && isTruthyValue(m.get("company_name"))) {
			entity.setCompanyName(stringTrimmed(m.get("company_name")));
		}
		if (m.containsKey("qualification_list") && isTruthyValue(m.get("qualification_list"))) {
			entity.setQualificationList(toJsonStorage(m.get("qualification_list")));
		}
		if (m.containsKey("card_id") && isTruthyValue(m.get("card_id"))) {
			entity.setCardId(stringTrimmed(m.get("card_id")));
		}
		if (m.containsKey("audit_id") && isTruthyValue(m.get("audit_id"))) {
			entity.setAuditId(stringTrimmed(m.get("audit_id")));
		}
		if (m.containsKey("is_domestic") && isTruthyValue(m.get("is_domestic"))) {
			int dom = parseSmallEnum(m.get("is_domestic"), "is_domestic");
			if (dom != 1 && dom != 2) {
				throw new BadRequestException("Invalid api param is_domestic");
			}
			entity.setIsDomestic(dom);
		}
		if (m.containsKey("country") && isTruthyValue(m.get("country"))) {
			entity.setCountry(stringTrimmed(m.get("country")));
		}
		if (m.containsKey("city") && isTruthyValue(m.get("city"))) {
			entity.setCity(stringTrimmed(m.get("city")));
		}
	}

	private String toJsonStorage(Object v) {
		if (v instanceof String s) {
			return s;
		}
		try {
			return objectMapper.writeValueAsString(v);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}
	}

	private void applyStatusIfTruthy(WxShops entity, Map<String, Object> m) {
		if (!m.containsKey("status") || !isTruthyValue(m.get("status"))) {
			return;
		}
		int st = parseSmallEnum(m.get("status"), "status");
		if (st < 1 || st > 4) {
			throw new BadRequestException("Invalid status");
		}
		entity.setStatus(st);
	}

	private void applyIsDirectStoreAndIsOpen(WxShops entity, Map<String, Object> m) {
		if (!m.containsKey("is_direct_store") || !isTruthyValue(m.get("is_direct_store"))) {
			entity.setIsDirectStore(1);
		} else {
			int ids = parseSmallEnum(m.get("is_direct_store"), "is_direct_store");
			if (ids != 1 && ids != 2) {
				throw new BadRequestException("Invalid api param is_direct_store");
			}
			entity.setIsDirectStore(ids);
		}

		if (!m.containsKey("is_open")) {
			entity.setIsOpen(true);
		} else {
			entity.setIsOpen(toBooleanLoose(m.get("is_open")));
		}
	}

	private static boolean toBooleanLoose(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = stringTrimmed(v);
		if (s.isEmpty()) {
			return false;
		}
		if ("1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s)) {
			return true;
		}
		if ("0".equals(s) || "false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s)) {
			return false;
		}
		return isTruthyValue(v);
	}

	private static int parseSmallEnum(Object raw, String fieldHint) {
		try {
			if (raw instanceof Number n) {
				return n.intValue();
			}
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			if ("is_direct_store".equals(fieldHint)) {
				throw new BadRequestException("Invalid api param is_direct_store");
			}
			if ("is_domestic".equals(fieldHint)) {
				throw new BadRequestException("Invalid api param is_domestic");
			}
			if ("status".equals(fieldHint)) {
				throw new BadRequestException("Invalid status");
			}
			throw new BadRequestException("Invalid status");
		}
	}

	private Map<String, Object> buildResponseMap(WxShops e) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("wx_shop_id", e.getWxShopId());
		out.put("map_poi_id", e.getMapPoiId());
		out.put("store_name", e.getStoreName());
		out.put("poi_id", e.getPoiId());
		out.put("lng", e.getLng());
		out.put("lat", e.getLat());
		out.put("address", e.getAddress());
		out.put("category", e.getCategory());
		out.put("pic_list", e.getPicList());
		out.put("contract_phone", e.getContractPhone());
		out.put("hour", e.getHour());
		out.put("add_type", e.getAddType());
		out.put("credential", e.getCredential());
		out.put("company_name", e.getCompanyName());
		out.put("qualification_list", e.getQualificationList());
		out.put("card_id", e.getCardId());
		out.put("status", e.getStatus());
		out.put("company_id", e.getCompanyId());
		out.put("distributor_id", e.getDistributorId());
		out.put("is_domestic", e.getIsDomestic() != null ? e.getIsDomestic() : 1);
		out.put("country", e.getCountry());
		out.put("city", e.getCity());
		out.put("is_direct_store", e.getIsDirectStore() != null ? e.getIsDirectStore() : 1);
		boolean open = e.getIsOpen() == null || Boolean.TRUE.equals(e.getIsOpen());
		out.put("is_open", open ? 1 : 0);
		out.put("created", e.getCreated());
		out.put("updated", e.getUpdated());
		return out;
	}

	private void assertShopIdsGate(Object v) {
		if (v == null) {
			return;
		}
		if (v instanceof Collection<?> c) {
			if (c.isEmpty()) {
				return;
			}
			throw new ForbiddenException("您没有添加门店的权限");
		}
		if (v instanceof Map<?, ?> map) {
			if (map.isEmpty()) {
				return;
			}
			throw new ForbiddenException("您没有添加门店的权限");
		}
		if (v instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if (s.isEmpty() || "0".contentEquals(s)) {
				return;
			}
			// JWT may carry shop_ids as JSON text (e.g. "[]"); align with WxShopsJwtShopIdWhitelist
			try {
				List<?> parsed = objectMapper.readValue(s, new TypeReference<List<?>>() {});
				if (parsed == null || parsed.isEmpty()) {
					return;
				}
			} catch (JsonProcessingException e) {
				throw new ForbiddenException("您没有添加门店的权限");
			}
			throw new ForbiddenException("您没有添加门店的权限");
		}
		if (v instanceof Number n) {
			if (n.doubleValue() == 0.0) {
				return;
			}
			throw new ForbiddenException("您没有添加门店的权限");
		}
		if (Boolean.FALSE.equals(v)) {
			return;
		}
		if (Boolean.TRUE.equals(v)) {
			throw new ForbiddenException("您没有添加门店的权限");
		}
		if (v instanceof Object[] arr) {
			if (arr.length == 0) {
				return;
			}
			throw new ForbiddenException("您没有添加门店的权限");
		}
		throw new ForbiddenException("您没有添加门店的权限");
	}

	private static boolean isEmptyLikeRequired(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return s.trim().isEmpty();
		}
		return false;
	}

	private static String stringTrimmed(Object v) {
		if (v == null) {
			return "";
		}
		return v.toString().trim();
	}

	private List<String> normalizePicListToNonEmptyStrings(Object raw) {
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("请上传门店图片");
			}
			try {
				List<?> arr = objectMapper.readValue(t, new TypeReference<List<?>>() {});
				if (arr == null || arr.isEmpty()) {
					throw new BadRequestException("请上传门店图片");
				}
				List<String> out = new ArrayList<>(arr.size());
				for (Object o : arr) {
					out.add(o == null ? "" : o.toString());
				}
				return out;
			} catch (JsonProcessingException e) {
				throw new BadRequestException("请上传门店图片");
			}
		}
		if (raw instanceof Collection<?> c) {
			if (c.isEmpty()) {
				throw new BadRequestException("请上传门店图片");
			}
			List<String> out = new ArrayList<>(c.size());
			for (Object o : c) {
				out.add(o == null ? "" : o.toString());
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			if (arr.length == 0) {
				throw new BadRequestException("请上传门店图片");
			}
			List<String> out = new ArrayList<>(arr.length);
			for (Object o : arr) {
				out.add(o == null ? "" : o.toString());
			}
			return out;
		}
		throw new BadRequestException("请上传门店图片");
	}

	private static boolean isValidHttpUrl(String s) {
		if (s == null) {
			return false;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return false;
		}
		try {
			java.net.URI uri = java.net.URI.create(t);
			String scheme = uri.getScheme();
			if (scheme == null) {
				return false;
			}
			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
				return false;
			}
			String host = uri.getHost();
			return host != null && !host.isEmpty();
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static List<Object> asLengthTwoList(Object hourRaw) {
		if (hourRaw instanceof List<?> l) {
			if (l.size() != 2) {
				throw new BadRequestException("请选择营业时间");
			}
			return List.of(l.get(0), l.get(1));
		}
		if (hourRaw instanceof Object[] arr) {
			if (arr.length != 2) {
				throw new BadRequestException("请选择营业时间");
			}
			return List.of(arr[0], arr[1]);
		}
		throw new BadRequestException("请选择营业时间");
	}

	private static String formatHourSegment(Object raw, ZoneId zone) {
		Long sec = DateExpressionParser.parseToEpochSecond(raw, zone);
		if (sec != null) {
			return Instant.ofEpochSecond(sec).atZone(zone).format(HOUR_FMT);
		}
		String s = raw != null ? raw.toString().trim() : "";
		if (s.isEmpty()) {
			throw new BadRequestException("请选择营业时间");
		}
		for (DateTimeFormatter f :
				List.of(
						DateTimeFormatter.ofPattern("H:mm"),
						DateTimeFormatter.ofPattern("HH:mm"),
						DateTimeFormatter.ofPattern("k:mm"))) {
			try {
				LocalTime lt = LocalTime.parse(s, f);
				return lt.format(HOUR_FMT);
			} catch (DateTimeParseException ignored) {
				// try next
			}
		}
		try {
			ZonedDateTime zdt = ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME);
			return zdt.withZoneSameInstant(zone).format(HOUR_FMT);
		} catch (DateTimeParseException ignored) {
			// fall through
		}
		throw new BadRequestException("请选择营业时间");
	}

	private static int parseAddTypeInSetOrThrow(Map<String, Object> m) {
		Object raw = m.get("add_type");
		if (isEmptyLikeRequired(raw)) {
			throw new BadRequestException("请选择经营资质主体");
		}
		try {
			int v;
			if (raw instanceof Number n) {
				v = n.intValue();
			} else {
				v = Integer.parseInt(raw.toString().trim());
			}
			if (v != 1 && v != 2 && v != 3) {
				throw new BadRequestException("请选择经营资质主体");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("请选择经营资质主体");
		}
	}

	private static void assertQualificationListRequired(Object v) {
		if (v == null) {
			throw new BadRequestException("请上传相关证明材料");
		}
		if (v instanceof String s) {
			if (s.trim().isEmpty()) {
				throw new BadRequestException("请上传相关证明材料");
			}
			return;
		}
		if (v instanceof Collection<?> c) {
			if (c.isEmpty()) {
				throw new BadRequestException("请上传相关证明材料");
			}
			return;
		}
		if (v instanceof Object[] arr) {
			if (arr.length == 0) {
				throw new BadRequestException("请上传相关证明材料");
			}
			return;
		}
		throw new BadRequestException("请上传相关证明材料");
	}

	private static boolean isTruthyValue(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() != 0.0;
		}
		if (v instanceof CharSequence cs) {
			if (cs.length() == 0) {
				return false;
			}
			return !cs.toString().contentEquals("0");
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> map) {
			return !map.isEmpty();
		}
		if (v instanceof Object[] arr) {
			return arr.length > 0;
		}
		return true;
	}
}
