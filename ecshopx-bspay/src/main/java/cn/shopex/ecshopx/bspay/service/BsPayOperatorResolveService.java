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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BsPayOperatorResolveService {

	private final OperatorsMapper operatorsMapper;

	public BsPayOperatorResolveService(OperatorsMapper operatorsMapper) {
		this.operatorsMapper = operatorsMapper;
	}

	public long resolveMerchantIdForBspayTradeExportOrThrow(long jwtOperatorId) {
		Operators op = operatorsMapper.selectById(jwtOperatorId);
		if (op == null) {
			throw new ResourceException("没有账号信息");
		}
		long merchantFilterId = jwtOperatorId;
		if (Boolean.TRUE.equals(op.getIsMerchantMain())
				&& op.getMerchantId() != null
				&& op.getMerchantId() > 0) {
			merchantFilterId = op.getMerchantId();
		}
		if (merchantFilterId <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		return merchantFilterId;
	}

	public Optional<Long> resolveMerchantFilterIdForTradeListOrThrow(long jwtOperatorId) {
		Operators op = operatorsMapper.selectById(jwtOperatorId);
		if (op == null) {
			throw new ResourceException("没有账号信息");
		}
		long merchantFilterId = jwtOperatorId;
		if (Boolean.TRUE.equals(op.getIsMerchantMain())
				&& op.getMerchantId() != null
				&& op.getMerchantId() > 0) {
			merchantFilterId = op.getMerchantId();
		}
		if (merchantFilterId <= 0) {
			return Optional.empty();
		}
		return Optional.of(merchantFilterId);
	}

	public OperatorContext resolve(Map<String, Object> jwtMap) {
		String operatorType = stringOrEmpty(jwtMap.get("operator_type"));
		long operatorId = parseOperatorLong(jwtMap.get("operator_id"), "operator_id");
		if ("distributor".equals(operatorType)) {
			operatorId = parseOperatorLong(jwtMap.get("distributor_id"), "distributor_id");
		} else if ("merchant".equals(operatorType)) {
			Operators op = operatorsMapper.selectById(operatorId);
			if (op == null) {
				throw new ResourceException("没有账号信息");
			}
			if (Boolean.TRUE.equals(op.getIsMerchantMain())
					&& op.getMerchantId() != null
					&& op.getMerchantId() > 0) {
				operatorId = parseOperatorLong(op.getMerchantId(), "merchant_id");
			}
		}
		int id = mustFitInt(operatorId);
		return new OperatorContext(id, operatorType);
	}

	private static String stringOrEmpty(Object raw) {
		return raw == null ? "" : Objects.toString(raw, "").trim();
	}

	private long parseOperatorLong(Object raw, String fieldLabel) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new ResourceException(
						"操作员标识格式无效（非数字），字段：" + fieldLabel + "，字面量：" + t);
			}
		}
		String text = Objects.toString(raw, "").trim();
		if (text.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException e) {
			throw new ResourceException(
					"操作员标识格式无效（非数字），字段：" + fieldLabel + "，字面量：" + text);
		}
	}

	private static int mustFitInt(long value) {
		if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
			throw new ResourceException("操作员标识超出整数范围，字面量：" + value);
		}
		return (int) value;
	}

	public record OperatorContext(int operatorId, String operatorType) {
	}
}
