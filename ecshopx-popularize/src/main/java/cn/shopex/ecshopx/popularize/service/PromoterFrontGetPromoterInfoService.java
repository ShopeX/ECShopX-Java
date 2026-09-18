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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.companys.service.setting.WxShopsSettingGetService;
import cn.shopex.ecshopx.popularize.support.PopularizeQueryFlagTruthy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromoterFrontGetPromoterInfoService {

	private final PromoterFrontPromoterInfoReadService promoterFrontPromoterInfoReadService;
	private final PopularizeSettingSaveService popularizeSettingSaveService;
	private final WxShopsSettingGetService wxShopsSettingGetService;
	private final PromoterFrontPromoterChildrenListService promoterFrontPromoterChildrenListService;
	private final boolean oemShuyun;

	public PromoterFrontGetPromoterInfoService(
			PromoterFrontPromoterInfoReadService promoterFrontPromoterInfoReadService,
			PopularizeSettingSaveService popularizeSettingSaveService,
			WxShopsSettingGetService wxShopsSettingGetService,
			PromoterFrontPromoterChildrenListService promoterFrontPromoterChildrenListService,
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.promoterFrontPromoterInfoReadService = promoterFrontPromoterInfoReadService;
		this.popularizeSettingSaveService = popularizeSettingSaveService;
		this.wxShopsSettingGetService = wxShopsSettingGetService;
		this.promoterFrontPromoterChildrenListService = promoterFrontPromoterChildrenListService;
		this.oemShuyun = oemShuyun;
	}

	public Object getPromoterInfo(
			long companyId,
			long selfId,
			String userIdRaw,
			Map<String, Object> authClaims,
			String wxSettingCountryCode) {
		boolean queryOthers = PopularizeQueryFlagTruthy.isTruthyForH5QueryFlag(userIdRaw);
		long targetUserId;
		if (queryOthers) {
			if (userIdRaw == null) {
				targetUserId = 0L;
			} else {
				String t = userIdRaw.strip();
				try {
					targetUserId = Long.parseLong(t);
				} catch (NumberFormatException e) {
					targetUserId = 0L;
				}
				if (targetUserId <= 0L) {
					targetUserId = 0L;
				}
			}
		} else {
			targetUserId = selfId;
		}
		if (targetUserId <= 0L) {
			return Collections.emptyList();
		}
		Map<String, Object> info = promoterFrontPromoterInfoReadService.getPromoterInfoMap(companyId, targetUserId);
		if (info == null || info.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, Object> config = popularizeSettingSaveService.getConfig(companyId, null, null);
		String isOpenShopStr = literalChainConfigString(config, "isOpenShop");
		String isOpenPopularizeStr = literalChainConfigString(config, "isOpenPopularize");
		Map<String, Object> selfInfoMap = promoterFrontPromoterInfoReadService.getPromoterInfoMap(companyId, selfId);

		LinkedHashMap<String, Object> data;
		if (queryOthers) {
			data = buildQueryOthersWhitelist(info);
			appendSelfAndParentNested(data, selfInfoMap, isOpenShopStr, isOpenPopularizeStr);
		} else {
			data = new LinkedHashMap<>(info);
			data.put("selfInfo", null);
			overlayNicknameMobileFromClaims(data, authClaims);
		}

		applyOpenShopFlagsFromConfig(data, config);
		putHeadquartersLogoIfBrandPresent(data, companyId, wxSettingCountryCode);
		applyPhaseBConfigKeys(data, config);
		putQrcodeBgImg(data, config);
		data.put("is_valid", computeTopIsValid(info, isOpenShopStr, isOpenPopularizeStr));
		if (oemShuyun) {
			applyOemShuyunBlock(data, config, selfInfoMap, companyId, selfId, isOpenPopularizeStr);
		}
		ensureParentInfoEmptyShellForMasking(data);
		applySensitiveMasking(data);
		data.put("type_promoter", "商家业务员");
		return data;
	}

	/**
	 * Returns the configuration entry for {@code key} as a string for callers that compare against exact flag
	 * literals. Values are taken from {@code config.get(key)} without trimming. A {@code null} value (including a
	 * missing entry) yields an empty string. Boxed {@link Boolean#TRUE} maps to {@code "true"} and boxed {@link
	 * Boolean#FALSE} maps to an empty string (not {@code "false"}), so inactive boolean flags do not accidentally
	 * satisfy equality checks against the literal {@code "false"} used for string-encoded configuration elsewhere in
	 * this service. All other non-null values use {@link String#valueOf(Object)}.
	 */
	private static String literalChainConfigString(Map<String, Object> config, String key) {
		if (config == null) {
			return "";
		}
		Object v = config.get(key);
		if (v == null) {
			return "";
		}
		if (v instanceof Boolean b) {
			return b ? "true" : "";
		}
		return String.valueOf(v);
	}

	/**
	 * Sensitive-data masking expects {@code parent_info} to exist; when it is absent or empty, supply an empty
	 * shell (four snake_case keys) so masking still runs on a stable shape.
	 */
	private static void ensureParentInfoEmptyShellForMasking(LinkedHashMap<String, Object> data) {
		Object pi = data.get("parent_info");
		if (pi instanceof Map<?, ?> pm && !pm.isEmpty()) {
			return;
		}
		LinkedHashMap<String, Object> shell = new LinkedHashMap<>();
		shell.put("mobile", "");
		shell.put("region_mobile", "");
		shell.put("username", "");
		shell.put("nickname", "");
		data.put("parent_info", shell);
	}

	private static LinkedHashMap<String, Object> buildQueryOthersWhitelist(Map<String, Object> info) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("user_id", info.get("user_id"));
		data.put("is_promoter", info.get("is_promoter"));
		data.put("disabled", info.get("disabled"));
		data.put("grade_level", info.get("grade_level"));
		data.put("shop_name", info.get("shop_name"));
		data.put("brief", info.get("brief"));
		data.put("shop_pic", info.get("shop_pic"));
		data.put("headimgurl", info.get("headimgurl") == null ? "" : String.valueOf(info.get("headimgurl")));
		data.put("username", info.get("username") == null ? "" : String.valueOf(info.get("username")));
		data.put("nickname", info.get("nickname") == null ? "" : String.valueOf(info.get("nickname")));
		data.put("mobile", info.get("mobile") == null ? "" : String.valueOf(info.get("mobile")));
		return data;
	}

	private void appendSelfAndParentNested(
			LinkedHashMap<String, Object> data,
			Map<String, Object> selfInfoMap,
			String isOpenShopStr,
			String isOpenPopularizeStr) {
		if (selfInfoMap == null || selfInfoMap.isEmpty()) {
			return;
		}
		LinkedHashMap<String, Object> selfNested = new LinkedHashMap<>();
		Object selfUid = selfInfoMap.get("user_id");
		selfNested.put("user_id", selfUid == null ? null : String.valueOf(selfUid));
		selfNested.put(
				"is_valid",
				computeNestedSelfIsValid(data, selfInfoMap, isOpenShopStr, isOpenPopularizeStr));
		data.put("selfInfo", selfNested);

		Object pi = selfInfoMap.get("parent_info");
		if (pi instanceof Map<?, ?> rawPm && !rawPm.isEmpty()) {
			@SuppressWarnings("unchecked")
			Map<String, Object> pm = (Map<String, Object>) pi;
			LinkedHashMap<String, Object> parentNested = new LinkedHashMap<>();
			parentNested.put("user_id", pm.get("user_id"));
			parentNested.put(
					"is_valid",
					computeNestedParentIsValid(data, pm, isOpenShopStr, isOpenPopularizeStr));
			data.put("parentInfo", parentNested);
		}
	}

	private static void overlayNicknameMobileFromClaims(
			LinkedHashMap<String, Object> data, Map<String, Object> authClaims) {
		for (String k : List.of("nickname", "mobile")) {
			Object raw = authClaims.get(k);
			String s = raw == null ? "" : String.valueOf(raw);
			boolean shouldOverlay = !s.isEmpty() && !"0".equals(s);
			if (shouldOverlay) {
				data.put(k, raw);
			}
		}
	}

	private static void applyOpenShopFlagsFromConfig(LinkedHashMap<String, Object> data, Map<String, Object> config) {
		Object raw = config.get("isOpenShop");
		boolean v = raw != null && "true".equals(String.valueOf(raw).trim());
		data.put("isOpenShop", v);
		raw = config.get("isOpenPopularize");
		v = raw != null && "true".equals(String.valueOf(raw).trim());
		data.put("isOpenPopularize", v);
	}

	private void putHeadquartersLogoIfBrandPresent(
			LinkedHashMap<String, Object> data, long companyId, String wxSettingCountryCode) {
		Map<String, Object> brand = wxShopsSettingGetService.getWxShopsSetting(companyId, wxSettingCountryCode);
		if (brand != null && !brand.isEmpty()) {
			Object v = brand.get("logo");
			data.put("headquarters_logo", Objects.toString(v, null));
		}
	}

	private static void applyPhaseBConfigKeys(LinkedHashMap<String, Object> data, Map<String, Object> config) {
		if (config != null && config.containsKey("isOpenPromoterInformation")) {
			Object v = config.get("isOpenPromoterInformation");
			data.put("isOpenPromoterInformation", "true".equals(String.valueOf(v).trim()));
		}
		if (config != null && config.containsKey("shop_img")) {
			data.put("shop_img", String.valueOf(config.get("shop_img")));
		}
		if (config != null && config.containsKey("banner_img")) {
			data.put("banner_img", String.valueOf(config.get("banner_img")));
		}
		if (config != null && config.containsKey("share_title")) {
			data.put("share_title", String.valueOf(config.get("share_title")));
		}
		if (config != null && config.containsKey("share_des")) {
			data.put("share_des", String.valueOf(config.get("share_des")));
		}
		if (config != null && config.containsKey("applets_share_img")) {
			data.put("applets_share_img", String.valueOf(config.get("applets_share_img")));
		}
		if (config != null && config.containsKey("h5_share_img")) {
			data.put("h5_share_img", String.valueOf(config.get("h5_share_img")));
		}
	}

	private static void putQrcodeBgImg(LinkedHashMap<String, Object> data, Map<String, Object> config) {
		Object v = config == null ? null : config.get("qrcode_bg_img");
		data.put("qrcode_bg_img", v == null ? "" : String.valueOf(v));
	}

	private static boolean computeTopIsValid(
			Map<String, Object> info, String isOpenShopStr, String isOpenPopularizeStr) {
		// Only the misspelled popularize literal ('fasle') closes the top-level validity check for that flag.
		boolean topInvalid =
				"false".equals(isOpenShopStr)
						|| "fasle".equals(isOpenPopularizeStr)
						|| intNorm(info.get("is_promoter")) != 1
						|| intNorm(info.get("disabled")) != 0
						|| (1 != intNorm(info.get("shop_status")));
		return !topInvalid;
	}

	private static boolean computeNestedSelfIsValid(
			Map<String, Object> data,
			Map<String, Object> selfInfoMap,
			String isOpenShopStr,
			String isOpenPopularizeStr) {
		boolean cfg = "false".equals(isOpenShopStr) || "fasle".equals(isOpenPopularizeStr);
		boolean inv =
				cfg
						|| intNorm(data.get("is_promoter")) != 1
						|| intNorm(data.get("disabled")) != 0
						|| (1 != intNorm(selfInfoMap.get("shop_status")));
		return !inv;
	}

	private static boolean computeNestedParentIsValid(
			Map<String, Object> data,
			Map<String, Object> parentRow,
			String isOpenShopStr,
			String isOpenPopularizeStr) {
		boolean cfg = "false".equals(isOpenShopStr) || "fasle".equals(isOpenPopularizeStr);
		boolean inv =
				cfg
						|| intNorm(data.get("is_promoter")) != 1
						|| intNorm(data.get("disabled")) != 0
						|| (1 != intNorm(parentRow.get("shop_status")));
		return !inv;
	}

	private void applyOemShuyunBlock(
			LinkedHashMap<String, Object> data,
			Map<String, Object> config,
			Map<String, Object> selfInfoMap,
			long companyId,
			long selfId,
			String isOpenPopularizeStr) {
		Object rawQ = config.get("promoter_qrcode_bg_img");
		data.put("promoter_qrcode_bg_img", rawQ == null ? "" : String.valueOf(rawQ));
		data.put("is_show_promoter_qrcode", Boolean.FALSE);
		boolean isFirstPromoter = false;
		if (selfInfoMap != null && !selfInfoMap.isEmpty()) {
			long identityId = longFrom(selfInfoMap.get("identity_id"));
			int sub = intNorm(selfInfoMap.get("is_subordinates"));
			if (identityId > 0L && sub == 1) {
				isFirstPromoter = true;
			}
		}
		boolean promoterInvalid =
				"fasle".equals(isOpenPopularizeStr)
						|| intNorm(data.get("is_promoter")) != 1
						|| intNorm(data.get("disabled")) != 0;
		boolean promoterIsValid = !promoterInvalid;

		Object cpRaw = config.get("change_promoter");
		String changePromoterType = "";
		if (cpRaw instanceof Map<?, ?> cp) {
			Object t = cp.get("type");
			changePromoterType = t == null ? "" : String.valueOf(t);
		}
		String internalOpenIdentityStr = String.valueOf(config.get("internalOpenIdentity"));
		if (promoterIsValid
				&& "internal".equals(changePromoterType)
				&& isFirstPromoter
				&& "true".equals(internalOpenIdentityStr)) {
			data.put("is_show_promoter_qrcode", Boolean.TRUE);
			LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
			filter.put("company_id", companyId);
			filter.put("user_id", selfId);
			filter.put("is_promoter", Integer.valueOf(1));
			long cnt = promoterFrontPromoterChildrenListService.getPromoterchildrenCount(filter, 1);
			data.put("child_promoter_num", cnt);
		}
		Number childNum = (Number) data.getOrDefault("child_promoter_num", 0L);
		data.put("show_child_promoter", childNum.longValue() > 0L ? Boolean.TRUE : Boolean.FALSE);
	}

	private void applySensitiveMasking(LinkedHashMap<String, Object> data) {
		maskTopScalarField(data, "mobile", true);
		maskTopScalarField(data, "username", false);
		maskTopScalarField(data, "pmobile", true);
		maskTopScalarField(data, "nickname", false);

		Object pi = data.get("parent_info");
		if (!(pi instanceof Map<?, ?>)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> pm = (Map<String, Object>) pi;
		maskParentSub(pm, "pmobile", true);
		maskParentSub(pm, "mobile", true);
		maskParentSub(pm, "region_mobile", true);
		maskParentSub(pm, "username", false);
		maskParentSub(pm, "nickname", false);
		data.put("parent_info", pm);
	}

	private void maskTopScalarField(LinkedHashMap<String, Object> data, String key, boolean mobile) {
		Object v = data.get(key);
		if (v != null && StringUtils.hasText(String.valueOf(v))) {
			String raw = String.valueOf(v);
			data.put(
					key,
					mobile
							? DataMasking.maskMobile(raw)
							: DataMasking.maskTruenameIfBlocked(raw, 1));
		} else {
			data.put(key, "");
		}
	}

	private void maskParentSub(Map<String, Object> pm, String key, boolean mobile) {
		if (!pm.containsKey(key)) {
			return;
		}
		Object v = pm.get(key);
		if (v != null && StringUtils.hasText(String.valueOf(v))) {
			String raw = String.valueOf(v);
			pm.put(
					key,
					mobile
							? DataMasking.maskMobile(raw)
							: DataMasking.maskTruenameIfBlocked(raw, 1));
		} else {
			pm.put(key, "");
		}
	}

	private static int intNorm(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longFrom(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
