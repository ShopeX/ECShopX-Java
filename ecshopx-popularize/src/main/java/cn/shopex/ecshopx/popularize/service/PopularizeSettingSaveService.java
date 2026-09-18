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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import cn.shopex.ecshopx.popularize.config.PopularizeCommonImageDefaultsProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeSettingSaveService {

	private static final String REDIS_POPULARIZE_CONFIG_PREFIX = "popularizeConfig:";
	private static final String REDIS_IS_OPEN_PREFIX = "isOpenPopularize:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final PopularizeCommonImageDefaultsProperties popularizeCommonImageDefaultsProperties;

	public PopularizeSettingSaveService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper,
			PointMemberRuleReadService pointMemberRuleReadService,
			PopularizeCommonImageDefaultsProperties popularizeCommonImageDefaultsProperties) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.popularizeCommonImageDefaultsProperties = popularizeCommonImageDefaultsProperties;
	}

	public Map<String, Object> getMergedPopularizeConfig(long companyId) {
		return new LinkedHashMap<>(loadConfigBaseline(companyId));
	}

	public Map<String, Object> getPromoterBanner(long companyId) {
		Map<String, Object> baseline = new LinkedHashMap<>(loadConfigBaseline(companyId));
		Object raw = baseline.get("banner_img");
		String banner = (raw == null) ? "" : String.valueOf(raw);
		return Map.<String, Object>of("banner_img", banner);
	}

	public Map<String, Object> getPromoterCustompage(long companyId) {
		Map<String, Object> baseline = new LinkedHashMap<>(loadConfigBaseline(companyId));
		boolean hasKey = baseline.containsKey("custompage_template_id");
		Object raw = hasKey ? baseline.get("custompage_template_id") : null;
		Object resolved;
		if (!hasKey || raw == null) {
			resolved = "";
		} else if (raw instanceof Number) {
			resolved = raw;
		} else if (raw instanceof String) {
			resolved = raw;
		} else {
			resolved = String.valueOf(raw);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("custompage_template_id", resolved);
		return out;
	}

	public String getOpenPopularizeLiteral(long companyId) {
		String openRaw = stringRedisTemplate.opsForValue().get(REDIS_IS_OPEN_PREFIX + companyId);
		return isOpenPopularizeRedisTrue(openRaw) ? "true" : "false";
	}

	public Map<String, Object> resolveNormalizedChangePromoterBlock(long companyId) {
		Map<String, Object> cfg = new LinkedHashMap<>(loadConfigBaseline(companyId));
		ensureChangePromoterForDisplay(cfg);
		Object raw = cfg.get("change_promoter");
		if (!(raw instanceof Map<?, ?>)) {
			return new LinkedHashMap<>(defaultChangePromoterRoot());
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> cp = (Map<String, Object>) raw;
		return new LinkedHashMap<>(cp);
	}

	/**
	 * Config snapshot taken before {@code applyCommonImageDefaults}, point rules, and boolean coercions run on
	 * the full config path, so callers can treat a key as user-defined when {@link Map#containsKey} is true and
	 * the value is non-null.
	 */
	public Map<String, Object> getConfigLayerBeforePopularizeDisplayDefaults(long companyId) {
		Map<String, Object> config = new LinkedHashMap<>(loadConfigBaseline(companyId));
		normalizeGoodsForApi(config);
		ensureChangePromoterForDisplay(config);
		String openRaw = stringRedisTemplate.opsForValue().get(REDIS_IS_OPEN_PREFIX + companyId);
		config.put("isOpenPopularize", isOpenPopularizeRedisTrue(openRaw) ? "true" : "false");
		return config;
	}

	public Map<String, Object> getConfig(long companyId, String operatorType, String pathSource) {
		Map<String, Object> config = new LinkedHashMap<>(loadConfigBaseline(companyId));
		normalizeGoodsForApi(config);
		ensureChangePromoterForDisplay(config);
		boolean singleTier =
				"distributor".equals(operatorType)
						|| "merchant".equals(operatorType)
						|| (pathSource != null
								&& "/sellers/popularize/popularizegoods".equals(pathSource));
		if (singleTier) {
			applySingleTierPopularizeRatioNames(config);
		}
		String openRaw = stringRedisTemplate.opsForValue().get(REDIS_IS_OPEN_PREFIX + companyId);
		config.put("isOpenPopularize", isOpenPopularizeRedisTrue(openRaw) ? "true" : "false");
		applyCommonImageDefaults(config, popularizeCommonImageDefaultsProperties);
		config.put("is_open_point", pointMemberRuleReadService.getIsOpenPoint(companyId));
		Object rawId = config.get("internalOpenIdentity");
		boolean internal = rawId != null && "true".equals(String.valueOf(rawId).trim());
		config.put("internalOpenIdentity", internal);
		return config;
	}

	public void setConfig(long companyId, Map<String, Object> input) {
		openPopularize(companyId, input);
		Map<String, Object> config = loadConfigBaseline(companyId);

		putIfPresentFromInputElseKeep(config, input, "limit_rebate");
		putIfPresentFromInputElseKeep(config, input, "limit_time");
		putIfPresentFromInputElseKeep(config, input, "goods");
		putIfPresentFromInputElseKeep(config, input, "isOpenGuide");
		putIfPresentFromInputElseKeep(config, input, "isOpenShop");
		putIfPresentFromInputElseKeep(config, input, "isOpenRecharge");

		if (input.containsKey("is_open_wechat")) {
			config.put("is_open_wechat", input.get("is_open_wechat"));
		} else {
			config.put("is_open_wechat", false);
		}
		putIfKeyPresent(config, input, "internalOpenIdentity");
		putIfKeyPresent(config, input, "isOpenPromoterInformation");
		putIfKeyPresent(config, input, "banner_img");
		putIfKeyPresent(config, input, "shop_img");
		putIfKeyPresent(config, input, "share_title");
		putIfKeyPresent(config, input, "share_des");
		putIfKeyPresent(config, input, "applets_share_img");
		putIfKeyPresent(config, input, "h5_share_img");
		putIfKeyPresent(config, input, "custompage_template_id");

		config.put("qrcode_bg_img", input.getOrDefault("qrcode_bg_img", ""));
		config.put("promoter_qrcode_bg_img", input.getOrDefault("promoter_qrcode_bg_img", ""));

		Object ctv = input.get("commission_type");
		String cs = ctv == null ? "" : String.valueOf(ctv).trim();
		if ("money".equals(cs) || "point".equals(cs)) {
			config.put("commission_type", cs);
		} else {
			config.put("commission_type", "money");
		}

		if (input.containsKey("change_promoter") && truthyMap(input.get("change_promoter"))) {
			config.put("change_promoter", input.get("change_promoter"));
		}

		if ("point".equals(String.valueOf(config.get("commission_type")))) {
			Map<String, Object> rule = pointMemberRuleReadService.getPointRule(companyId);
			if (!memberPointDeductionBothEnabled(rule)) {
				throw new ResourceException("未打开积分设置不可设置积分返佣");
			}
		}

		applyPopularizeRatioBranch(config, input);
		applyRechargeBranch(config, input);

		String json;
		try {
			json = objectMapper.writeValueAsString(config);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
		stringRedisTemplate.opsForValue().set(REDIS_POPULARIZE_CONFIG_PREFIX + companyId, json);
	}

	private static void putIfKeyPresent(Map<String, Object> config, Map<String, Object> input, String key) {
		if (input.containsKey(key)) {
			config.put(key, input.get(key));
		}
	}

	private static String resolveCommissionType(Object raw) {
		if (raw == null) {
			return "money";
		}
		String s = String.valueOf(raw).trim();
		if ("money".equals(s) || "point".equals(s)) {
			return s;
		}
		return "money";
	}

	private void openPopularize(long companyId, Map<String, Object> input) {
		Object raw = input.containsKey("isOpenPopularize") ? input.get("isOpenPopularize") : null;
		boolean on = isOpenPopularizeTruthy(raw);
		stringRedisTemplate.opsForValue().set(REDIS_IS_OPEN_PREFIX + companyId, on ? "true" : "false");
	}

	private static boolean isOpenPopularizeTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return false;
		}
		if ("1".equals(s)) {
			return true;
		}
		return "true".equalsIgnoreCase(s);
	}

	private Map<String, Object> loadConfigBaseline(long companyId) {
		String raw = stringRedisTemplate.opsForValue().get(REDIS_POPULARIZE_CONFIG_PREFIX + companyId);
		Map<String, Object> config;
		if (!StringUtils.hasText(raw)) {
			config = buildDefaultConfig();
		} else {
			try {
				Map<String, Object> parsed =
						objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
				config = parsed != null ? new LinkedHashMap<>(parsed) : buildDefaultConfig();
			} catch (Exception e) {
				config = buildDefaultConfig();
			}
		}
		ensurePopularizeRatioNestedDefaults(config);
		ensureRechargeProfitNestedDefaults(config);
		Object ct = config.get("commission_type");
		config.put("commission_type", ct != null ? ct : "money");
		Object g = config.get("goods");
		if (g == null) {
			config.put("goods", "all");
		} else if (g instanceof Map<?, ?> m && m.isEmpty()) {
			config.put("goods", new ArrayList<Object>());
		}
		String openRaw = stringRedisTemplate.opsForValue().get(REDIS_IS_OPEN_PREFIX + companyId);
		config.put("isOpenPopularize", isOpenPopularizeRedisTrue(openRaw) ? "true" : "false");
		return config;
	}

	private static boolean isOpenPopularizeRedisTrue(String openRaw) {
		return openRaw != null && "true".equals(openRaw);
	}

	private void applySingleTierPopularizeRatioNames(Map<String, Object> config) {
		Object prRaw = config.get("popularize_ratio");
		if (!(prRaw instanceof Map<?, ?>)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> pr = (Map<String, Object>) prRaw;
		applySingleTierPopularizeRatioBranch(pr, "profit");
		applySingleTierPopularizeRatioBranch(pr, "order_money");
	}

	private static void applySingleTierPopularizeRatioBranch(Map<String, Object> pr, String branchKey) {
		Object branchRaw = pr.get(branchKey);
		if (!(branchRaw instanceof Map<?, ?>)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> branch = (Map<String, Object>) branchRaw;
		Object fl = branch.get("first_level");
		if (fl instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			Map<String, Object> first = (Map<String, Object>) fl;
			first.put("name", "业绩");
		} else {
			ensureLevelEntry(branch, "first_level", "上级");
			Object fl2 = branch.get("first_level");
			if (fl2 instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Object> first = (Map<String, Object>) fl2;
				first.put("name", "业绩");
			}
		}
		branch.remove("second_level");
	}

	private void applyCommonImageDefaults(
			Map<String, Object> config, PopularizeCommonImageDefaultsProperties p) {
		putCoalesceThenTruthyDefault(config, "banner_img", p.getDistributionDefaultBanner());
		putCoalesceThenTruthyDefault(config, "applets_share_img", p.getDistributionDefaultWeapp());
		putNullishCoalesceString(config, "h5_share_img", p.getDistributionDefaultPoster());
		putNullishCoalesceString(config, "qrcode_bg_img", p.getQrcodeBgImg());
		putNullishCoalesceString(config, "promoter_qrcode_bg_img", p.getPromoterQrcodeBgImg());
	}

	/**
	 * Missing/null then default; then empty/falsy string replaced by default (same as double assignment
	 * on banner / applets in the reference stack).
	 */
	private static void putCoalesceThenTruthyDefault(
			Map<String, Object> config, String key, String propDefault) {
		String d = propDefault != null ? propDefault : "";
		if (!config.containsKey(key) || config.get(key) == null) {
			config.put(key, d);
			return;
		}
		String v = String.valueOf(config.get(key));
		if (v.isEmpty()) {
			config.put(key, d);
		} else {
			config.put(key, v);
		}
	}

	/** Key absent or value null only; explicit empty string is kept. */
	private static void putNullishCoalesceString(Map<String, Object> config, String key, String propDefault) {
		String d = propDefault != null ? propDefault : "";
		if (!config.containsKey(key) || config.get(key) == null) {
			config.put(key, d);
		} else {
			config.put(key, String.valueOf(config.get(key)));
		}
	}

	private static void normalizeGoodsForApi(Map<String, Object> config) {
		Object g = config.get("goods");
		if (g == null) {
			config.put("goods", "all");
			return;
		}
		if (g instanceof List<?>) {
			return;
		}
		if (g instanceof Map<?, ?> m) {
			if (m.isEmpty()) {
				config.put("goods", new ArrayList<Object>());
			}
			return;
		}
	}

	private static void ensureChangePromoterForDisplay(Map<String, Object> config) {
		Object raw = config.get("change_promoter");
		Map<String, Object> defRoot = defaultChangePromoterRoot();
		if (!(raw instanceof Map<?, ?> m) || m.isEmpty()) {
			config.put("change_promoter", defRoot);
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> cp = new LinkedHashMap<>((Map<String, Object>) m);
		if (!cp.containsKey("type")) {
			cp.put("type", defRoot.get("type"));
		}
		Map<String, Object> mergedFilter = new LinkedHashMap<>(defaultChangePromoterFilter());
		Object fObj = cp.get("filter");
		if (fObj instanceof Map<?, ?> fm) {
			for (Map.Entry<?, ?> e : fm.entrySet()) {
				mergedFilter.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		cp.put("filter", mergedFilter);
		config.put("change_promoter", cp);
	}

	private static Map<String, Object> defaultChangePromoterFilter() {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("no_threshold", 0);
		filter.put("vip_grade", "vip");
		filter.put("consume_money", 0);
		filter.put("order_num", 0);
		return filter;
	}

	private static Map<String, Object> defaultChangePromoterRoot() {
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("type", "no_threshold");
		root.put("filter", defaultChangePromoterFilter());
		return root;
	}

	private static Map<String, Object> buildDefaultConfig() {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put("limit_rebate", 1);
		config.put("limit_time", 0);
		config.put("is_open_wechat", false);
		config.put("isOpenGuide", false);
		config.put("isOpenShop", false);
		config.put("isOpenRecharge", false);
		config.put("goods", "all");
		config.put("banner_img", "");
		config.put("shop_img", "");
		config.put("qrcode_bg_img", "");
		config.put("promoter_qrcode_bg_img", "");
		config.put("share_title", "");
		config.put("share_des", "");
		config.put("applets_share_img", "");
		config.put("internalOpenIdentity", false);
		config.put("isOpenPromoterInformation", false);
		config.put("custompage_template_id", 0);
		config.put("commission_type", "money");
		config.put("change_promoter", defaultChangePromoterRoot());
		config.put("popularize_ratio", defaultPopularizeRatioRoot());
		config.put("recharge", defaultRechargeRoot());
		return config;
	}

	private static Map<String, Object> defaultPopularizeRatioRoot() {
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("type", "profit");
		root.put("profit", defaultRatioBranchLevels());
		root.put("order_money", defaultRatioBranchLevels());
		return root;
	}

	private static Map<String, Object> defaultRatioBranchLevels() {
		Map<String, Object> branch = new LinkedHashMap<>();
		branch.put("first_level", levelEntry(0, "上级"));
		branch.put("second_level", levelEntry(0, "上上级"));
		return branch;
	}

	private static Map<String, Object> levelEntry(int ratio, String name) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("ratio", ratio);
		m.put("name", name);
		return m;
	}

	private static Map<String, Object> defaultRechargeRoot() {
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("profit", defaultRatioBranchLevels());
		return r;
	}

	private static void ensurePopularizeRatioNestedDefaults(Map<String, Object> config) {
		Object prRaw = config.get("popularize_ratio");
		if (!(prRaw instanceof Map<?, ?>)) {
			config.put("popularize_ratio", defaultPopularizeRatioRoot());
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> pr = (Map<String, Object>) prRaw;
		ensureRatioTypeBranch(pr, "profit");
		ensureRatioTypeBranch(pr, "order_money");
	}

	private static void ensureRatioTypeBranch(Map<String, Object> pr, String typeKey) {
		Object branchRaw = pr.get(typeKey);
		if (!(branchRaw instanceof Map<?, ?>)) {
			pr.put(typeKey, defaultRatioBranchLevels());
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> branch = (Map<String, Object>) branchRaw;
		ensureLevelEntry(branch, "first_level", "上级");
		ensureLevelEntry(branch, "second_level", "上上级");
	}

	private static void ensureLevelEntry(Map<String, Object> branch, String levelKey, String defaultName) {
		Object lv = branch.get(levelKey);
		if (!(lv instanceof Map<?, ?>)) {
			branch.put(levelKey, levelEntry(0, defaultName));
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> lm = (Map<String, Object>) lv;
		if (!lm.containsKey("ratio")) {
			lm.put("ratio", 0);
		}
		if (!lm.containsKey("name")) {
			lm.put("name", defaultName);
		}
	}

	private static void ensureRechargeProfitNestedDefaults(Map<String, Object> config) {
		Object rRaw = config.get("recharge");
		if (!(rRaw instanceof Map<?, ?>)) {
			config.put("recharge", defaultRechargeRoot());
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> recharge = (Map<String, Object>) rRaw;
		Object pRaw = recharge.get("profit");
		if (!(pRaw instanceof Map<?, ?>)) {
			recharge.put("profit", defaultRatioBranchLevels());
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> profit = (Map<String, Object>) pRaw;
		ensureLevelEntry(profit, "first_level", "上级");
		ensureLevelEntry(profit, "second_level", "上上级");
	}

	private static boolean memberPointDeductionBothEnabled(Map<String, Object> rule) {
		Object m = rule.get("isOpenMemberPoint");
		Object d = rule.get("isOpenDeductPoint");
		return redisConfigFlagTrue(m) && redisConfigFlagTrue(d);
	}

	private static boolean redisConfigFlagTrue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		return "true".equals(String.valueOf(v));
	}

	private static void putIfPresentFromInputElseKeep(
			Map<String, Object> config, Map<String, Object> input, String key) {
		if (input.containsKey(key) && input.get(key) != null) {
			config.put(key, input.get(key));
		}
	}

	private static Map<String, Object> copyPopularizeRatioBaselineForMutation(Object raw) {
		if (raw == null || !(raw instanceof Map<?, ?>)) {
			return new LinkedHashMap<>();
		}
		Map<?, ?> src = (Map<?, ?>) raw;
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : src.entrySet()) {
			String k0 = String.valueOf(entry.getKey());
			Object v0 = entry.getValue();
			if (("profit".equals(k0) || "order_money".equals(k0)) && v0 instanceof Map<?, ?> m) {
				Map<String, Object> branchCopy = new LinkedHashMap<>();
				for (Map.Entry<?, ?> le : m.entrySet()) {
					String lk = String.valueOf(le.getKey());
					Object lv = le.getValue();
					branchCopy.put(
							lk,
							lv instanceof Map<?, ?> mm ? new LinkedHashMap<>((Map<?, ?>) mm) : lv);
				}
				out.put(k0, branchCopy);
			} else if (v0 instanceof Map<?, ?> m) {
				out.put(k0, new LinkedHashMap<>((Map<?, ?>) m));
			} else {
				out.put(k0, v0);
			}
		}
		return out;
	}

	private static Map<String, Object> copyRechargeBaselineForMutation(Object raw) {
		if (raw == null || !(raw instanceof Map<?, ?>)) {
			return new LinkedHashMap<>();
		}
		Map<?, ?> src = (Map<?, ?>) raw;
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : src.entrySet()) {
			String k0 = String.valueOf(entry.getKey());
			Object v0 = entry.getValue();
			if ("profit".equals(k0) && v0 instanceof Map<?, ?> m) {
				Map<String, Object> branchCopy = new LinkedHashMap<>();
				for (Map.Entry<?, ?> le : m.entrySet()) {
					String lk = String.valueOf(le.getKey());
					Object lv = le.getValue();
					branchCopy.put(
							lk,
							lv instanceof Map<?, ?> mm ? new LinkedHashMap<>((Map<?, ?>) mm) : lv);
				}
				out.put(k0, branchCopy);
			} else if (v0 instanceof Map<?, ?> m) {
				out.put(k0, new LinkedHashMap<>((Map<?, ?>) m));
			} else {
				out.put(k0, v0);
			}
		}
		return out;
	}

	private static void applyPopularizeRatioBranch(Map<String, Object> config, Map<String, Object> input) {
		if (!input.containsKey("popularize_ratio")) {
			return;
		}
		Object prObj = input.get("popularize_ratio");
		if (!(prObj instanceof Map<?, ?>) || ((Map<?, ?>) prObj).isEmpty()) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> prIn = (Map<String, Object>) prObj;
		Map<String, Object> cfgPr = copyPopularizeRatioBaselineForMutation(config.get("popularize_ratio"));
		config.put("popularize_ratio", cfgPr);

		String ratioType = Optional.ofNullable(prIn.get("type"))
				.map(String::valueOf)
				.filter(s -> !s.isEmpty())
				.orElse("profit");
		cfgPr.put("type", ratioType);

		double sum = 0;
		Object rb = prIn.get(ratioType);
		if (rb instanceof Map<?, ?> rbMap) {
			sum += ratioFromLevel(rbMap, "first_level");
			sum += ratioFromLevel(rbMap, "second_level");
		}
		if ("profit".equals(ratioType) && sum > 100) {
			throw new ResourceException("按利润分佣不能总分佣比例不可超过100%");
		}
		if ("order_money".equals(ratioType) && sum > 50) {
			throw new ResourceException("按订单金额分佣不能总分佣比例不可超过50%");
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> ratioTypeMap = (Map<String, Object>)
				cfgPr.computeIfAbsent(ratioType, k -> new LinkedHashMap<String, Object>());
		syncPopularizeLevelFromInput(prIn, ratioType, ratioTypeMap, "first_level", "上级");
		syncPopularizeLevelFromInput(prIn, ratioType, ratioTypeMap, "second_level", "上上级");
	}

	private static void syncPopularizeLevelFromInput(
			Map<String, Object> prIn,
			String ratioType,
			Map<String, Object> ratioTypeMap,
			String levelKey,
			String fixedName) {
		double ratio = 0;
		Object rb = prIn.get(ratioType);
		if (rb instanceof Map<?, ?> rbMap) {
			Object lv = rbMap.get(levelKey);
			if (lv instanceof Map<?, ?> lm) {
				ratio = toDouble(lm.get("ratio"));
			}
		}
		Map<String, Object> levelObj = new LinkedHashMap<>();
		levelObj.put("ratio", ratio);
		levelObj.put("name", fixedName);
		ratioTypeMap.put(levelKey, levelObj);
	}

	private static double ratioFromLevel(Map<?, ?> rbMap, String levelKey) {
		Object lv = rbMap.get(levelKey);
		if (!(lv instanceof Map<?, ?> lm)) {
			return 0;
		}
		return toDouble(lm.get("ratio"));
	}

	private static void applyRechargeBranch(Map<String, Object> config, Map<String, Object> input) {
		if (!input.containsKey("recharge")) {
			return;
		}
		Object reObj = input.get("recharge");
		if (!(reObj instanceof Map<?, ?>) || ((Map<?, ?>) reObj).isEmpty()) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> rechargeIn = (Map<String, Object>) reObj;
		Map<String, Object> cfgRe = copyRechargeBaselineForMutation(config.get("recharge"));
		config.put("recharge", cfgRe);

		Map<String, Object> profitIn = new LinkedHashMap<>();
		Object pRaw = rechargeIn.get("profit");
		if (pRaw instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			Map<String, Object> cast = (Map<String, Object>) pRaw;
			profitIn = cast;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> profitOut = (Map<String, Object>)
				cfgRe.computeIfAbsent("profit", k -> new LinkedHashMap<String, Object>());
		putRechargeLevel(profitIn, profitOut, "first_level", "上级");
		putRechargeLevel(profitIn, profitOut, "second_level", "上上级");
	}

	private static void putRechargeLevel(
			Map<String, Object> profitIn,
			Map<String, Object> profitOut,
			String levelKey,
			String fixedName) {
		double ratio = 0;
		Object lv = profitIn.get(levelKey);
		if (lv instanceof Map<?, ?> lm) {
			ratio = toDouble(lm.get("ratio"));
		}
		Map<String, Object> levelObj = new LinkedHashMap<>();
		levelObj.put("ratio", ratio);
		levelObj.put("name", fixedName);
		profitOut.put(levelKey, levelObj);
	}

	private static double toDouble(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static boolean truthyMap(Object o) {
		return o instanceof Map<?, ?> m && !m.isEmpty();
	}
}
