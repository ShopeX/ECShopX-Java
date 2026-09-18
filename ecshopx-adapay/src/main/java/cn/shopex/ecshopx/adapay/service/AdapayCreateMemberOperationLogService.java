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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayCreateMemberOperationLogService {

	private final AdapayOperationLogMapper adapayOperationLogMapper;
	private final OperatorsQueryService operatorsQueryService;
	private final DistributorMapper distributorMapper;

	public AdapayCreateMemberOperationLogService(
			AdapayOperationLogMapper adapayOperationLogMapper,
			OperatorsQueryService operatorsQueryService,
			DistributorMapper distributorMapper) {
		this.adapayOperationLogMapper = adapayOperationLogMapper;
		this.operatorsQueryService = operatorsQueryService;
		this.distributorMapper = distributorMapper;
	}

	public void recordCreateMemberLog(
			long companyId,
			Map<String, Object> jwtOperator,
			String displayNameForSubject,
			Map<String, Object> operatorContext) {
		String operatorType = stringVal(jwtOperator.get("operator_type")).toLowerCase(Locale.ROOT);
		String sourceType;
		if ("distributor".equals(operatorType)) {
			sourceType = "distributor";
		} else if ("dealer".equals(operatorType)) {
			sourceType = "dealer";
		} else {
			sourceType = "merchant";
		}

		long jwtOperatorId = toLong(jwtOperator.get("operator_id"));
		long relId;
		if ("distributor".equals(operatorType)) {
			long distributorId = toLong(jwtOperator.get("distributor_id"));
			relId = distributorId;
			Distributor d =
					distributorMapper.selectOne(
							new LambdaQueryWrapper<Distributor>()
									.eq(Distributor::getCompanyId, companyId)
									.eq(Distributor::getDistributorId, distributorId));
			if (d == null || !StringUtils.hasText(d.getName()) || d.getName().trim().isEmpty()) {
				throw new ResourceException("店铺信息不存在");
			}
		} else {
			relId = toLong(operatorContext.get("operator_id"));
		}

		Map<String, Object> filter = new HashMap<>(8);
		filter.put("company_id", companyId);
		filter.put("operator_id", jwtOperatorId);
		filter.put("operator_type", jwtOperator.get("operator_type"));
		Map<String, Object> opRow = operatorsQueryService.getInfo(filter);
		String username = "";
		if (opRow != null) {
			Object u = opRow.get("username");
			if (u != null && StringUtils.hasText(u.toString())) {
				username = u.toString();
			} else {
				Object m = opRow.get("mobile");
				username = m != null ? m.toString() : "";
			}
		}

		String subjectLabel;
		if ("distributor".equals(operatorType)) {
			subjectLabel = "店铺";
		} else if ("dealer".equals(operatorType)) {
			subjectLabel = "经销商";
		} else {
			subjectLabel = "主商户";
		}

		String content =
				"用户" + username + "提交了" + subjectLabel + displayNameForSubject + "的开户信息的创建";

		int now = (int) (System.currentTimeMillis() / 1000L);
		AdapayOperationLog row = new AdapayOperationLog();
		row.setCompanyId(companyId);
		row.setLogType(sourceType);
		row.setOperatorId(jwtOperatorId);
		row.setRelId(relId);
		row.setContent(content);
		row.setCreateTime(now);
		row.setUpdateTime(now);
		adapayOperationLogMapper.insert(row);
	}

	public void recordUpdateMemberLog(
			long companyId,
			Map<String, Object> jwtOperator,
			String displayNameForSubject,
			Map<String, Object> operatorContext) {
		String operatorType = stringVal(jwtOperator.get("operator_type")).toLowerCase(Locale.ROOT);
		String sourceType;
		if ("distributor".equals(operatorType)) {
			sourceType = "distributor";
		} else if ("dealer".equals(operatorType)) {
			sourceType = "dealer";
		} else {
			sourceType = "merchant";
		}

		long jwtOperatorId = toLong(jwtOperator.get("operator_id"));
		long relId;
		if ("distributor".equals(operatorType)) {
			long distributorId = toLong(jwtOperator.get("distributor_id"));
			relId = distributorId;
			Distributor d =
					distributorMapper.selectOne(
							new LambdaQueryWrapper<Distributor>()
									.eq(Distributor::getCompanyId, companyId)
									.eq(Distributor::getDistributorId, distributorId));
			if (d == null || !StringUtils.hasText(d.getName()) || d.getName().trim().isEmpty()) {
				throw new ResourceException("店铺信息不存在");
			}
		} else {
			relId = toLong(operatorContext.get("operator_id"));
		}

		Map<String, Object> filter = new HashMap<>(8);
		filter.put("company_id", companyId);
		filter.put("operator_id", jwtOperatorId);
		filter.put("operator_type", jwtOperator.get("operator_type"));
		Map<String, Object> opRow = operatorsQueryService.getInfo(filter);
		String username = "";
		if (opRow != null) {
			Object u = opRow.get("username");
			if (u != null && StringUtils.hasText(u.toString())) {
				username = u.toString();
			} else {
				Object m = opRow.get("mobile");
				username = m != null ? m.toString() : "";
			}
		}

		String subjectLabel;
		if ("distributor".equals(operatorType)) {
			subjectLabel = "店铺";
		} else if ("dealer".equals(operatorType)) {
			subjectLabel = "经销商";
		} else {
			subjectLabel = "主商户";
		}

		String content =
				"用户" + username + "提交了" + subjectLabel + displayNameForSubject + "的开户信息修改";

		int now = (int) (System.currentTimeMillis() / 1000L);
		AdapayOperationLog row = new AdapayOperationLog();
		row.setCompanyId(companyId);
		row.setLogType(sourceType);
		row.setOperatorId(jwtOperatorId);
		row.setRelId(relId);
		row.setContent(content);
		row.setCreateTime(now);
		row.setUpdateTime(now);
		adapayOperationLogMapper.insert(row);
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
