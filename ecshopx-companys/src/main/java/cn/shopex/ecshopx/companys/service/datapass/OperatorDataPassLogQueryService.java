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

package cn.shopex.ecshopx.companys.service.datapass;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.OperatorDataPassLog;
import cn.shopex.ecshopx.companys.mapper.OperatorDataPassLogMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorDataPassLogQueryService {

	private final OperatorDataPassLogMapper operatorDataPassLogMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final OperatorDataPassLogUserStrPort operatorDataPassLogUserStrPort;

	public OperatorDataPassLogQueryService(
			OperatorDataPassLogMapper operatorDataPassLogMapper,
			OperatorsQueryService operatorsQueryService,
			OperatorDataPassLogUserStrPort operatorDataPassLogUserStrPort) {
		this.operatorDataPassLogMapper = operatorDataPassLogMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.operatorDataPassLogUserStrPort = operatorDataPassLogUserStrPort;
	}

	public Map<String, Object> listDataPassLog(long companyId, int operatorId, int page, int pageSize) {
		LambdaQueryWrapper<OperatorDataPassLog> w = new LambdaQueryWrapper<>();
		w.eq(OperatorDataPassLog::getCompanyId, companyId)
				.eq(OperatorDataPassLog::getOperatorId, operatorId)
				.orderByDesc(OperatorDataPassLog::getCreateTime);
		Page<OperatorDataPassLog> pageReq = new Page<>(page, pageSize);
		Page<OperatorDataPassLog> result = operatorDataPassLogMapper.selectPage(pageReq, w);
		long total = result.getTotal();
		List<OperatorDataPassLog> records = result.getRecords();

		if (records.isEmpty()) {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("total_count", total);
			body.put("list", List.of());
			return body;
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("operator_id", (long) operatorId);
		Map<String, Object> op = operatorsQueryService.getInfo(filter);
		if (op == null || op.isEmpty()) {
			throw new ResourceException("操作员不存在");
		}
		Object ln = op.get("login_name");
		String loginName = ln == null ? "" : ln.toString().trim();
		if (!StringUtils.hasText(loginName)) {
			throw new ResourceException("操作员不存在");
		}

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (OperatorDataPassLog l : records) {
			Map<String, String> args = parseQueryString(l.getUrl());
			String userStr = "";
			if (args.containsKey("user_id") && StringUtils.hasText(args.get("user_id"))) {
				try {
					long uid = Long.parseLong(args.get("user_id").trim());
					userStr = operatorDataPassLogUserStrPort.buildUserStrMiddle(uid, companyId);
				} catch (NumberFormatException ignored) {
					userStr = "";
				}
			}
			String pageName = parseLogPageName(l.getPath(), args);
			String content = loginName + "查看了" + userStr + pageName;

			Map<String, Object> row = new LinkedHashMap<>();
			row.put("log_id", l.getLogId());
			row.put("company_id", l.getCompanyId());
			row.put("operator_id", l.getOperatorId());
			row.put("create_time", l.getCreateTime());
			row.put("path", l.getPath());
			row.put("url", l.getUrl());
			row.put("content", content);
			listMaps.add(row);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", total);
		body.put("list", listMaps);
		return body;
	}

	private static Map<String, String> parseQueryString(String url) {
		LinkedHashMap<String, String> out = new LinkedHashMap<>();
		if (url == null || url.isEmpty()) {
			return out;
		}
		try {
			URI uri = URI.create(url.trim());
			String rawQuery = uri.getRawQuery();
			if (rawQuery == null || rawQuery.isEmpty()) {
				return out;
			}
			for (String part : rawQuery.split("&")) {
				if (part.isEmpty()) {
					continue;
				}
				int eq = part.indexOf('=');
				String rawKey = eq >= 0 ? part.substring(0, eq) : part;
				String rawVal = eq >= 0 ? (eq < part.length() - 1 ? part.substring(eq + 1) : "") : "";
				String k = safeDecode(rawKey);
				String v = safeDecode(rawVal);
				out.put(k, v);
			}
		} catch (IllegalArgumentException ignored) {
			return new LinkedHashMap<>();
		}
		return out;
	}

	private static String safeDecode(String s) {
		try {
			return URLDecoder.decode(s, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			return s;
		}
	}

	private static boolean isDistributionTruthy(Map<String, String> args) {
		String v = args.get("is_distribution");
		if (v == null) {
			return false;
		}
		String t = v.trim();
		if (t.isEmpty()) {
			return false;
		}
		return !"0".equalsIgnoreCase(t);
	}

	private static String parseLogPageName(String path, Map<String, String> args) {
		String p = path == null ? "" : path;
		switch (p) {
			case "order.list.get":
				if (isDistributionTruthy(args)) {
					return "店铺订单";
				}
				if ("normal".equals(args.get("order_type"))) {
					return "实物订单";
				}
				return "服务订单";
			case "order.info.get":
				return "订单详情";
			case "aftersales.list":
				return "售后列表";
			case "order.trade.list":
				return "交易单";
			case "distributor.list":
				return "店铺列表";
			case "member.list":
				return "会员列表";
			case "member.info":
				return "会员详情";
			case "order.rights.transfer.list":
				return "权益转让";
			case "rights.log.list":
				if (args.containsKey("user_id")) {
					return "核销记录";
				}
				return "服务核销单";
			case "vipgrade.order.list":
				if (!args.containsKey("user_id")) {
					return "等级购买记录";
				}
				return "付费会员卡记录";
			case "member.whitelist.list":
				return "白名单列表";
			case "order.rights.list.get":
				return "会员权益";
			case "deposit.trades":
				return "会员储值";
			case "distribution.aftersalesaddress.list":
				return "售后地址";
			case "selfhelp.registrationRecord.list":
				return "报名记录管理";
			case "selfhelp.registrationRecord.info":
				return "报名详情";
			case "member.export":
				return "导出用户信息";
			case "popularize.promoter.list.get":
				return "推广员列表";
			case "":
				return "推广员详情";
			case "adapay.drawcash.getList":
				return "提现申请";
			case "adapay.open_account.step":
				return "开户信息";
			case "adapay.sub_approve.info":
				return "子商户审批详情";
			case "adapay.dealer.list":
				return "经销商列表";
			case "adapay.dealer.info":
				return "经销商详情";
			case "distributor.info":
				return "店铺详情";
			case "popularize.promoter.export":
				return "导出推广员业绩";
			case "popularize.cash_withdrawals.list.get":
				return "佣金提现列表";
			case "popularize.task.brokerage.logs":
				return "任务佣金明细";
			case "popularize.task.brokerage.count.export":
				return "导出任务佣金统计";
			case "popularize.task.brokerage.count":
				return "任务佣金统计";
			case "order.list.export":
				return "导出订单列表";
			case "rights.list.export":
				return "导出权益列表";
			case "trades.list.export":
				return "导出交易单列表";
			case "rights.log.list.export":
				return "导出核销权益列表";
			case "card.detail.list":
				return "卡券领取列表";
			case "voucher.package.receives_log":
				return "卡券包领取日志";
			case "selfhelp.registrationRecord.export":
				return "导出报名记录";
			case "promotions.give.info":
				return "优惠券发送失败详情";
			case "specific.crowd.discount.loglist":
				return "定向促销优惠日志";
			case "adapay.dealer.distributorList":
				return "经销商关联店铺";
			case "account.list":
				return "企业员工信息列表";
			case "shop.salesperson.lists":
				return "门店人员列表";
			case "shop.salesperson.getinfo":
				return "门店人员详情";
			case "popularize.promoter.children.list":
				return "推广员直属下级";
			case "goods.epidemicRegister.list":
				return "疫情商品登记列表";
			case "goods.epidemicRegister.export":
				return "疫情商品登记导出";
			case "order.process.log.get":
				return "订单操作日志";
			case "adapay.member.list":
				return "adapay开户列表(店铺端 经销商端)";
			case "merchant.detail.get":
				return "商户详情";
			case "merchant.list":
				return "商户列表";
			case "merchant.settlement.apply.detail":
				return "商户入驻申请详情";
			default:
				return p;
		}
	}
}
