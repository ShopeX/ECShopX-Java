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

package cn.shopex.ecshopx.point.service;

import java.util.Map;

public final class PointMemberJournalTypeDescriptions {

	private static final Map<Integer, String> JOURNAL_TYPE_MAP = Map.ofEntries(
			Map.entry(1, "注册赠送"),
			Map.entry(2, "邀请注册赠送"),
			Map.entry(3, "充值赠送"),
			Map.entry(4, "储值兑换"),
			Map.entry(5, "积分换购"),
			Map.entry(6, "消费购物（支出）"),
			Map.entry(7, "消费购物（获取）"),
			Map.entry(8, "会员等级返佣"),
			Map.entry(9, "取消订单返还"),
			Map.entry(10, "退款返还"),
			Map.entry(11, "大转盘"),
			Map.entry(12, "商家手动修改"),
			Map.entry(13, "开放接口"),
			Map.entry(14, "分销佣金（积分）"),
			Map.entry(15, "商家导入修改"),
			Map.entry(16, "活动报名送积分"));

	private PointMemberJournalTypeDescriptions() {
	}

	public static String forType(Integer journalType) {
		if (journalType == null) {
			return "";
		}
		return JOURNAL_TYPE_MAP.getOrDefault(journalType, "");
	}
}
