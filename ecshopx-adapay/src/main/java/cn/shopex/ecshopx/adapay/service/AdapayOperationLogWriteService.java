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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayOperationLog;
import cn.shopex.ecshopx.adapay.mapper.AdapayOperationLogMapper;
import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 写入 adapay 操作日志，对齐管理端业务侧记录语义。
 */
@Service
public class AdapayOperationLogWriteService implements AdapayOperationLogRecordPort {

	private static final Set<String> SOURCE_TYPES = Set.of("merchant", "distributor", "dealer");

	private final AdapayOperationLogMapper adapayOperationLogMapper;
	private final JdbcTemplate jdbcTemplate;

	public AdapayOperationLogWriteService(
			AdapayOperationLogMapper adapayOperationLogMapper, JdbcTemplate jdbcTemplate) {
		this.adapayOperationLogMapper = adapayOperationLogMapper;
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * @param logParams 须含 {@code company_id}、{@code name}（被操作对象展示名）
	 * @param relId 关联主键，语义与业务动作一致
	 * @param jwtOperatorId 当前登录操作员，写入 {@code operator_id}
	 */
	@Override
	public void logRecord(
			Map<String, Object> logParams, long relId, String action, String sourceType, long jwtOperatorId) {
		long companyId = requireLong(logParams.get("company_id"));
		String otherName = logParams.get("name") != null ? logParams.get("name").toString() : "";
		String st = sourceType.toLowerCase(Locale.ROOT);
		if (!SOURCE_TYPES.contains(st)) {
			throw new ResourceException("log source type error");
		}
		String actor = resolveActorDisplayName(companyId, jwtOperatorId);
		if (!StringUtils.hasText(actor)) {
			throw new ResourceException("操作者信息为空");
		}
		String content;
		if ("withdraw".equals(action)) {
			content = "用户" + actor + "申请了提现";
		} else if ("withdrawset".equals(action)) {
			content = "用户" + actor + "设置了店铺" + otherName + "提现配置";
		} else if ("sub_approve/save_split_ledger".equals(action)) {
			String actionWord =
					"APPROVED".equalsIgnoreCase(String.valueOf(logParams.get("status"))) ? "通过" : "驳回";
			String base = "用户" + actor + actionWord + otherName + "的审批";
			if (isSmsTruthy(logParams.get("is_sms"))) {
				base += "，开通了短信提醒";
			}
			content = base;
		} else if ("dealer/disable".equals(action)) {
			boolean disabling = isDisableLogTruthy(logParams.get("is_disable"));
			String verb = disabling ? "禁用" : "开启";
			content = "用户" + actor + verb + "了经销商" + otherName;
		} else if ("dealer/rel/dealer".equals(action)) {
			String actionWord =
					Boolean.TRUE.equals(logParams.get("is_rel")) ? "关联" : "解除";
			String dealerName = str(logParams.get("dealer_name"));
			String distributorName = str(logParams.get("distributor_name"));
			content = "用户" + actor + "给" + dealerName + actionWord + distributorName;
		} else if ("dealer/rel/distributor".equals(action)) {
			String actionWord =
					Boolean.TRUE.equals(logParams.get("is_rel")) ? "关联" : "解除";
			String dealerName = str(logParams.get("dealer_name"));
			String distributorName = str(logParams.get("distributor_name"));
			content = "用户" + actor + "给" + distributorName + actionWord + dealerName;
		} else if ("dealer/reset".equals(action)) {
			content = "用户" + actor + "重置了经销商" + otherName + "密码";
		} else if ("trade/exportdata".equals(action)) {
			content = "用户" + actor + "导出了分账列表";
		} else if ("set_payment_setting".equals(action)) {
			content = "用户" + actor + "配置支付配置";
		} else {
			content = "用户" + actor + "新增经销商" + otherName;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		AdapayOperationLog row = new AdapayOperationLog();
		row.setCompanyId(companyId);
		row.setLogType(st);
		row.setOperatorId(jwtOperatorId);
		row.setRelId(relId);
		row.setContent(content);
		row.setCreateTime(now);
		row.setUpdateTime(now);
		adapayOperationLogMapper.insert(row);
	}

	private String resolveActorDisplayName(long companyId, long operatorId) {
		String sql = "SELECT username, mobile FROM operators WHERE company_id = ? AND operator_id = ? LIMIT 1";
		return jdbcTemplate.query(
				sql,
				rs -> {
					if (!rs.next()) {
						return "";
					}
					String u = rs.getString("username");
					if (u != null && !u.isEmpty()) {
						return u;
					}
					String m = rs.getString("mobile");
					return m != null ? m : "";
				},
				companyId,
				operatorId);
	}

	private static long requireLong(Object o) {
		if (o == null) {
			throw new ResourceException("company_id required");
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static boolean isSmsTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(v).trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static boolean isDisableLogTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 1;
		}
		return "1".equals(String.valueOf(raw).trim());
	}
}
