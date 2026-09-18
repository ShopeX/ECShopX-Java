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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderCreateState;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappPhysicalNormalOrderCreateSupport {

	private static final Pattern ZIP_6 = Pattern.compile("^\\d{6}$");

	private static final Pattern ZH_NAME = Pattern.compile("^[a-zA-Z0-9\\u4e00-\\u9fa5]+$");

	private static final Pattern MOBILE_CN = Pattern.compile("^1[3456789][0-9]{9}$");

	private final WxappNormalOrderCreateOrchestrator wxappNormalOrderCreateOrchestrator;

	public WxappPhysicalNormalOrderCreateSupport(WxappNormalOrderCreateOrchestrator wxappNormalOrderCreateOrchestrator) {
		this.wxappNormalOrderCreateOrchestrator = wxappNormalOrderCreateOrchestrator;
	}

	public Map<String, Object> create(WxappOrderCreateContext ctx, String orderTypeSlug) {
		NormalOrderCreateState state = new NormalOrderCreateState();
		state.getParams().putAll(ctx.getParams());
		long userId = ctx.getUserId();
		state.getParams().put("operator_id", userId);
		state.getParams().put("order_type", orderTypeSlug);
		String orderSource = stringVal(state.getParams().get("order_source"));
		if (!StringUtils.hasText(orderSource)) {
			state.getParams().put("order_source", "member");
		}
		if (!state.getParams().containsKey("promotion") || !StringUtils.hasText(stringVal(state.getParams().get("promotion")))) {
			state.getParams().put("promotion", "normal");
		}
		state.getParams().put("is_online_order", Boolean.TRUE);
		if ("service".equals(orderTypeSlug)) {
			validateServiceOrderNeedParams(state.getParams());
		} else if ("normal".equals(orderTypeSlug) || "normal_pointsmall".equals(orderTypeSlug)) {
			validateNormalOrderReceiverBeforeCart(state.getParams());
		}
		HttpServletRequest request = ctx.getRequest();
		return wxappNormalOrderCreateOrchestrator.create(state, request, orderTypeSlug);
	}

	public Map<String, Object> getOrderTempInfo(WxappOrderCreateContext ctx, String orderTypeSlug) {
		NormalOrderCreateState state = new NormalOrderCreateState();
		state.getParams().putAll(ctx.getParams());
		long userId = ctx.getUserId();
		state.getParams().put("operator_id", userId);
		state.getParams().put("order_type", orderTypeSlug);
		String orderSource = stringVal(state.getParams().get("order_source"));
		if (!StringUtils.hasText(orderSource)) {
			state.getParams().put("order_source", "member");
		}
		if (!state.getParams().containsKey("promotion") || !StringUtils.hasText(stringVal(state.getParams().get("promotion")))) {
			state.getParams().put("promotion", "normal");
		}
		state.getParams().put("is_online_order", Boolean.TRUE);
		if ("service".equals(orderTypeSlug)) {
			validateServiceOrderNeedParams(state.getParams());
		}
		HttpServletRequest request = ctx.getRequest();
		return wxappNormalOrderCreateOrchestrator.getOrderTempInfo(state, request, orderTypeSlug);
	}

	private static void validateServiceOrderNeedParams(Map<String, Object> p) {
		if (!hasPositiveLongId(p.get("item_id"))) {
			throw new ResourceException("商品id必填");
		}
		validateServiceItemNum(p.get("item_num"));
		long companyId = longVal(p.get("company_id"), 0L);
		if (companyId <= 0L) {
			throw new ResourceException("企业id必填");
		}
		if (!p.containsKey("user_id")) {
			throw new ResourceException("用户id必填");
		}
		if (!StringUtils.hasText(stringVal(p.get("mobile")))) {
			throw new ResourceException("手机号必填");
		}
	}

	private static void validateServiceItemNum(Object raw) {
		if (raw == null) {
			throw new ResourceException("商品数量必填");
		}
		if (raw instanceof String s && !StringUtils.hasText(s.trim())) {
			throw new ResourceException("商品数量必填");
		}
		double d;
		if (raw instanceof Number n) {
			d = n.doubleValue();
		} else {
			String s = raw.toString().trim();
			if (!StringUtils.hasText(s)) {
				throw new ResourceException("商品数量必填");
			}
			try {
				d = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				throw new ResourceException("商品数量必须为整数");
			}
		}
		if (Double.isNaN(d) || Double.isInfinite(d)) {
			throw new ResourceException("商品数量必须为整数");
		}
		if (d != Math.floor(d)) {
			throw new ResourceException("商品数量必须为整数");
		}
		if (d < 1.0d) {
			throw new ResourceException("商品数量最少为1");
		}
	}

	private static void validateNormalOrderReceiverBeforeCart(Map<String, Object> p) {
		String receipt = stringVal(p.get("receipt_type"));
		if (!("logistics".equals(receipt) || "dada".equals(receipt) || "merchant".equals(receipt))) {
			return;
		}
		String zip = stringVal(p.get("receiver_zip"));
		if (!ZIP_6.matcher(zip).matches()) {
			p.put("receiver_zip", "000000");
		}
		String name = stringVal(p.get("receiver_name"));
		if (!StringUtils.hasText(name) || !ZH_NAME.matcher(name).matches()) {
			throw new ResourceException("请填写正确的收货人姓名");
		}
		String mobile = stringVal(p.get("receiver_mobile"));
		if (!StringUtils.hasText(mobile)) {
			throw new ResourceException("请填写联系方式");
		}
		if (!MOBILE_CN.matcher(mobile).matches()) {
			throw new ResourceException("请填写正确的手机号");
		}
		String stateName = stringVal(p.get("receiver_state"));
		if (!StringUtils.hasText(stateName) || !ZH_NAME.matcher(stateName).matches()) {
			throw new ResourceException("请填写正确的省份");
		}
		String city = stringVal(p.get("receiver_city"));
		if (!StringUtils.hasText(city) || !ZH_NAME.matcher(city).matches()) {
			throw new ResourceException("请填写正确的城市");
		}
		String district = stringVal(p.get("receiver_district"));
		if (!StringUtils.hasText(district) || !ZH_NAME.matcher(district).matches()) {
			throw new ResourceException("请填写正确的地区");
		}
		String address = stringVal(p.get("receiver_address"));
		if (!StringUtils.hasText(address)) {
			throw new ResourceException("请填写正确的详细地址");
		}
	}

	private static boolean hasPositiveLongId(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				return false;
			}
		}
		long id = longVal(v, 0L);
		return id > 0L;
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
