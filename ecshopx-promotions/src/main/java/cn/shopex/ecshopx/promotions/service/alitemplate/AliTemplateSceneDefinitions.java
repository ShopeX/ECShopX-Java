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

package cn.shopex.ecshopx.promotions.service.alitemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static scene metadata for Alipay mini-program notice templates. Iteration order matches the shipped
 * admin template list declaration order.
 */
public final class AliTemplateSceneDefinitions {

	private static final LinkedHashMap<String, AliTemplateSceneDefinition> ORDERED = new LinkedHashMap<>();

	static {
		ORDERED.put(
				"memberCreateSucc",
				new AliTemplateSceneDefinition(
						"注册成功提醒",
						"注册成功提醒",
						"会员提醒",
						List.of(
								row("date", "注册时间", "date"),
								row("notice", "温馨提示", "thing")),
						sendDesc("会员注册成功后触发")));
		ORDERED.put(
				"paymentSucc",
				new AliTemplateSceneDefinition(
						"订单支付成功通知",
						"订单支付成功通知",
						"交易提醒",
						List.of(
								row("order_id", "订单号", "character_string"),
								row("pay_money", "订单金额", "amount"),
								row("item_name", "订单商品", "thing"),
								row("pay_date", "支付时间", "date"),
								row("receipt_type", "取货方式", "thing")),
						sendDesc("订单支付后触发")));
		LinkedHashMap<String, Object> payOrdersSend = new LinkedHashMap<>();
		payOrdersSend.put("title", "买家下单");
		payOrdersSend.put("time_list", List.of(5, 10, 15, 20, 30, 60, 120, 180));
		payOrdersSend.put("value", 10);
		payOrdersSend.put("end_title", "未付款触发");
		ORDERED.put(
				"payOrdersRemind",
				new AliTemplateSceneDefinition(
						"订单待支付提醒",
						"订单待支付提醒",
						"交易提醒",
						List.of(
								row("order_id", "订单号", "character_string"),
								row("pay_money", "待支付金额", "amount"),
								row("item_name", "商品名称", "thing"),
								row("created", "下单时间", "date"),
								row("remarks", "温馨提示", "thing")),
						Collections.unmodifiableMap(payOrdersSend)));
		ORDERED.put(
				"orderDeliverySucc",
				new AliTemplateSceneDefinition(
						"订单发货提醒",
						"订单发货提醒",
						"交易提醒",
						List.of(
								row("order_id", "订单号", "character_string"),
								row("delivery_corp", "物流公司", "thing"),
								row("delivery_code", "快递单号", "character_string"),
								row("item_name", "商品信息", "thing")),
						sendDesc("商家发货后立即触发")));
		ORDERED.put(
				"aftersalesRefuse",
				new AliTemplateSceneDefinition(
						"售后通知",
						"售后通知",
						"交易提醒",
						List.of(
								row("order_id", "订单编号", "character_string"),
								row("refund_fee", "退款金额", "amount"),
								row("remarks", "备注", "thing")),
						sendDesc("商家售后操作后触发")));
		ORDERED.put(
				"userGetCardSucc",
				new AliTemplateSceneDefinition(
						"优惠券领取通知",
						"优惠券状态通知",
						"交易提醒",
						List.of(
								row("title", "优惠券名称", "thing"),
								row("active_date", "有效期", "character_string"),
								row("used_action", "使用方式", "thing"),
								row("remarks", "温馨提醒", "thing")),
						sendDesc("优惠券领取触发")));
		ORDERED.put(
				"registrationResultNotice",
				new AliTemplateSceneDefinition(
						"报名结果通知",
						"报名结果通知",
						"交易提醒",
						List.of(
								row("activity_name", "报名活动", "thing"),
								row("review_result", "报名结果", "thing")),
						sendDesc("报名审核结果通知")));
		ORDERED.put(
				"goodsArrivalNotice",
				new AliTemplateSceneDefinition(
						"商品到货通知",
						"商品到货通知",
						"会员提醒",
						List.of(
								row("item_name", "商品名称", "thing"),
								row("notice", "温馨提示", "thing")),
						sendDesc("缺货商品到货通知到会员")));
	}

	private AliTemplateSceneDefinitions() {}

	private static Map<String, String> row(String column, String title, String keyword) {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("column", column);
		m.put("title", title);
		m.put("keyword", keyword);
		return Collections.unmodifiableMap(m);
	}

	private static Map<String, Object> sendDesc(String title) {
		return Map.of("title", title);
	}

	public static Iterable<Map.Entry<String, AliTemplateSceneDefinition>> orderedEntries() {
		return ORDERED.entrySet();
	}

	public static final class AliTemplateSceneDefinition {
		private final String title;
		private final String sceneDesc;
		private final String tmplType;
		private final List<Map<String, String>> value;
		private final Map<String, Object> sendTimeDesc;

		public AliTemplateSceneDefinition(
				String title,
				String sceneDesc,
				String tmplType,
				List<Map<String, String>> value,
				Map<String, Object> sendTimeDesc) {
			this.title = title;
			this.sceneDesc = sceneDesc;
			this.tmplType = tmplType;
			this.value = value;
			this.sendTimeDesc = sendTimeDesc;
		}

		public String getTitle() {
			return title;
		}

		public String getSceneDesc() {
			return sceneDesc;
		}

		public String getTmplType() {
			return tmplType;
		}

		public List<Map<String, String>> getValue() {
			return value;
		}

		public Map<String, Object> getSendTimeDesc() {
			return sendTimeDesc;
		}
	}
}
