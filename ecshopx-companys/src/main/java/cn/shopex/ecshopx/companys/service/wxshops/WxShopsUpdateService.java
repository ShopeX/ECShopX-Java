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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.companys.domain.Resources;
import cn.shopex.ecshopx.companys.domain.WxShops;
import cn.shopex.ecshopx.companys.mapper.ResourcesMapper;
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
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class WxShopsUpdateService {

	private static final Pattern CONTRACT_PHONE =
			Pattern.compile("^\\d{3,4}-?\\d{7,8}(-?\\d{2,6})?$");
	private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("HH:mm");

	private final WxShopsJwtShopIdWhitelist whitelist;
	private final WxShopsMapper wxShopsMapper;
	private final ResourcesMapper resourcesMapper;
	private final ObjectMapper objectMapper;

	public WxShopsUpdateService(
			WxShopsJwtShopIdWhitelist whitelist,
			WxShopsMapper wxShopsMapper,
			ResourcesMapper resourcesMapper,
			ObjectMapper objectMapper) {
		this.whitelist = whitelist;
		this.wxShopsMapper = wxShopsMapper;
		this.resourcesMapper = resourcesMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> updateWxShops(
			long wxShopId,
			Map<String, Object> merged,
			Map<String, Object> operatorJwt,
			long companyId) {
		List<Long> allowed = whitelist.allowedShopIds(operatorJwt.get("shop_ids"));
		if (!allowed.isEmpty() && !allowed.contains(wxShopId)) {
			throw new ForbiddenException("您没有此项操作权限");
		}

		if (isEmptyLikeRequired(merged.get("store_name"))) {
			throw new BadRequestException("请填写门店名称");
		}
		if (isEmptyLikeRequired(merged.get("address"))) {
			throw new BadRequestException("请填写门店经纬度");
		}
		Object picRaw = merged.get("pic_list");
		if (isEmptyLikeRequired(picRaw)) {
			throw new BadRequestException("请上传门店图片");
		}
		List<String> picUrls = normalizePicListToNonEmptyStrings(picRaw);
		for (String u : picUrls) {
			if (!isValidHttpUrl(u)) {
				throw new BadRequestException("门店图片必填且要微信返回的正确图片地址.");
			}
		}

		if (isEmptyLikeRequired(merged.get("contract_phone"))) {
			throw new BadRequestException("请填写客服电话");
		}
		String contractPhone = stringTrimmed(merged.get("contract_phone"));
		if (!CONTRACT_PHONE.matcher(contractPhone).matches()) {
			throw new BadRequestException("客服电话格式错误");
		}

		Object hourRaw = merged.get("hour");
		if (isEmptyLikeRequired(hourRaw)) {
			throw new BadRequestException("请选择营业时间");
		}
		List<Object> hourPair = asLengthTwoList(hourRaw);
		ZoneId zone = ZoneId.systemDefault();
		String hourStr =
				formatHourSegment(hourPair.get(0), zone) + " - " + formatHourSegment(hourPair.get(1), zone);

		int addType = parseAddTypeInSetOrThrow(merged);

		if (addType == 2) {
			if (isEmptyLikeRequired(merged.get("company_name"))) {
				throw new BadRequestException("请填写经营资质名称");
			}
		}
		if (merged.containsKey("company_name") && merged.get("company_name") != null) {
			String cn = stringTrimmed(merged.get("company_name"));
			if (!cn.isEmpty() && cn.length() > 30) {
				throw new BadRequestException("经营资质名称长度超过限制");
			}
		}
		if (merged.containsKey("credential") && merged.get("credential") != null) {
			String cred = stringTrimmed(merged.get("credential"));
			if (!cred.isEmpty() && cred.length() > 30) {
				throw new BadRequestException("经营资质证件号长度超过限制");
			}
		}
		if (addType == 2) {
			assertQualificationListRequired(merged.get("qualification_list"));
		}

		String picListJson;
		try {
			picListJson = objectMapper.writeValueAsString(picUrls);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}

		WxShops wx = wxShopsMapper.selectById(wxShopId);
		if (wx == null) {
			throw new ResourceException("需要更新的门店不存在");
		}

		if (!Objects.equals(companyId, wx.getCompanyId())) {
			throw new ResourceException("请确认您的门店信息后再提交.");
		}

		Long renewalExpiredAt = null;
		boolean carriesResourceId =
				merged.containsKey("resource_id") && merged.get("resource_id") != null;
		Long ridExisting = wx.getResourceId();
		boolean noResource = ridExisting == null || ridExisting == 0L;
		Long expExisting = wx.getExpiredAt();
		int now = (int) (System.currentTimeMillis() / 1000L);
		boolean expired = expExisting == null || now > expExisting;

		if (carriesResourceId && (noResource || expired)) {
			long reqRid;
			try {
				reqRid = Long.parseLong(merged.get("resource_id").toString().trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("资源包ID格式错误");
			}
			Resources resourceInfo =
					resourcesMapper.selectOne(
							new LambdaQueryWrapper<Resources>()
									.eq(Resources::getCompanyId, companyId)
									.eq(Resources::getResourceId, reqRid));
			Long resExp = resourceInfo == null ? null : resourceInfo.getExpiredAt();
			boolean resourceExpired = resourceInfo == null || now > (resExp == null ? 0L : resExp);
			Integer leftShopNum = resourceInfo == null ? null : resourceInfo.getLeftShopNum();
			if (resourceInfo == null
					|| resourceExpired
					|| leftShopNum == null
					|| leftShopNum <= 0) {
				throw new ResourceException("resource_id=" + reqRid + "的资源包不可用！");
			}
			renewalExpiredAt = resourceInfo.getExpiredAt();
		}

		applyRepositoryStyleFieldUpdates(wx, merged, picListJson, hourStr, contractPhone, addType);
		applyIsDirectStoreAndIsOpen(wx, merged);
		wx.setCompanyId(companyId);
		if (renewalExpiredAt != null) {
			wx.setExpiredAt(renewalExpiredAt);
		}
		wx.setUpdated((int) (System.currentTimeMillis() / 1000L));

		try {
			wxShopsMapper.updateById(wx);
		} catch (IllegalArgumentException e) {
			throw mapEntityIllegalArg(e);
		}

		return toWxShopUpdateRowMap(wx);
	}

	private void applyRepositoryStyleFieldUpdates(
			WxShops wx,
			Map<String, Object> merged,
			String picListJson,
			String hourStr,
			String contractPhone,
			int addType) {
		if (merged.containsKey("map_poi_id") && isTruthyValue(merged.get("map_poi_id"))) {
			wx.setMapPoiId(stringTrimmed(merged.get("map_poi_id")));
		}
		if (merged.containsKey("store_name") && isTruthyValue(merged.get("store_name"))) {
			wx.setStoreName(stringTrimmed(merged.get("store_name")));
		}
		if (merged.containsKey("poi_id") && isTruthyValue(merged.get("poi_id"))) {
			wx.setPoiId(stringTrimmed(merged.get("poi_id")));
		}
		if (merged.containsKey("lng") && isTruthyValue(merged.get("lng"))) {
			wx.setLng(stringTrimmed(merged.get("lng")));
		}
		if (merged.containsKey("lat") && isTruthyValue(merged.get("lat"))) {
			wx.setLat(stringTrimmed(merged.get("lat")));
		}
		if (merged.containsKey("address") && isTruthyValue(merged.get("address"))) {
			wx.setAddress(stringTrimmed(merged.get("address")));
		}
		if (merged.containsKey("category") && isTruthyValue(merged.get("category"))) {
			wx.setCategory(stringTrimmed(merged.get("category")));
		}
		if (merged.containsKey("pic_list") && isTruthyValue(merged.get("pic_list"))) {
			wx.setPicList(picListJson);
		}
		if (merged.containsKey("contract_phone") && isTruthyValue(merged.get("contract_phone"))) {
			wx.setContractPhone(contractPhone);
		}
		if (merged.containsKey("hour") && isTruthyValue(merged.get("hour"))) {
			wx.setHour(hourStr);
		}
		if (merged.containsKey("add_type") && isTruthyValue(merged.get("add_type"))) {
			wx.setAddType(addType);
		}
		if (merged.containsKey("credential") && isTruthyValue(merged.get("credential"))) {
			wx.setCredential(stringTrimmed(merged.get("credential")));
		}
		if (merged.containsKey("company_name") && isTruthyValue(merged.get("company_name"))) {
			wx.setCompanyName(stringTrimmed(merged.get("company_name")));
		}
		if (merged.containsKey("qualification_list") && isTruthyValue(merged.get("qualification_list"))) {
			wx.setQualificationList(toJsonStorage(merged.get("qualification_list")));
		}
		if (merged.containsKey("card_id") && isTruthyValue(merged.get("card_id"))) {
			wx.setCardId(stringTrimmed(merged.get("card_id")));
		}
		if (merged.containsKey("status") && isTruthyValue(merged.get("status"))) {
			wx.setStatus(parseFlexiblePositiveInt(merged.get("status"), "status"));
		}
		if (merged.containsKey("errmsg") && isTruthyValue(merged.get("errmsg"))) {
			wx.setErrmsg(stringTrimmed(merged.get("errmsg")));
		}
		if (merged.containsKey("audit_id") && isTruthyValue(merged.get("audit_id"))) {
			wx.setAuditId(stringTrimmed(merged.get("audit_id")));
		}
		if (merged.containsKey("resource_id") && isTruthyValue(merged.get("resource_id"))) {
			wx.setResourceId(parseFlexibleLong(merged.get("resource_id")));
		}
		if (merged.containsKey("expired_at") && isTruthyValue(merged.get("expired_at"))) {
			wx.setExpiredAt(parseFlexibleLong(merged.get("expired_at")));
		}
		if (merged.containsKey("country") && isTruthyValue(merged.get("country"))) {
			wx.setCountry(stringTrimmed(merged.get("country")));
		}
		if (merged.containsKey("city") && isTruthyValue(merged.get("city"))) {
			wx.setCity(stringTrimmed(merged.get("city")));
		}
	}

	private static int parseFlexiblePositiveInt(Object raw, String fieldHint) {
		try {
			if (raw instanceof Number n) {
				return n.intValue();
			}
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			if ("status".equals(fieldHint)) {
				throw new BadRequestException("Invalid status");
			}
			throw new BadRequestException("参数类型错误");
		}
	}

	private static long parseFlexibleLong(Object raw) {
		try {
			if (raw instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数类型错误");
		}
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

	private Map<String, Object> toWxShopUpdateRowMap(WxShops e) {
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
		out.put("created", e.getCreated());
		out.put("updated", e.getUpdated());
		out.put("expired_at", e.getExpiredAt());
		out.put("resource_id", e.getResourceId());
		out.put("is_domestic", e.getIsDomestic() != null ? e.getIsDomestic() : 1);
		out.put("distributor_id", e.getDistributorId());
		Object isOpenOut =
				e.getIsOpen() == null || Boolean.FALSE.equals(e.getIsOpen()) ? Boolean.TRUE : e.getIsOpen();
		out.put("is_open", isOpenOut);
		out.put("country", e.getCountry());
		out.put("city", e.getCity());
		out.put("is_direct_store", e.getIsDirectStore() != null ? e.getIsDirectStore() : 1);
		return out;
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
