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

package cn.shopex.ecshopx.companys.service.posaccount;

import cn.shopex.ecshopx.common.dispatch.PosStoreAccountCreateEventDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.port.digios.PosStoreAccountDigiOsCreatePort;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class PosStoreAccountImportService {

	private final PosStoreAccountMapper mapper;
	private final DistributionDistributorSelfReadMapper shopMapper;
	private final PosStoreAccountPasswordService passwordService;
	private final OperatorsMapper operatorsMapper;
	private final PosStoreAccountCreateEventDispatchPublisher createEventDispatchPublisher;
	private final PosStoreAccountDigiOsCreatePort digiOsCreatePort;
	private final Set<String> acceptedKeys = ConcurrentHashMap.newKeySet();

	public PosStoreAccountImportService(
			PosStoreAccountMapper mapper,
			DistributionDistributorSelfReadMapper shopMapper,
			PosStoreAccountPasswordService passwordService,
			OperatorsMapper operatorsMapper,
			PosStoreAccountCreateEventDispatchPublisher createEventDispatchPublisher,
			PosStoreAccountDigiOsCreatePort digiOsCreatePort) {
		this.mapper = mapper;
		this.shopMapper = shopMapper;
		this.passwordService = passwordService;
		this.operatorsMapper = operatorsMapper;
		this.createEventDispatchPublisher = createEventDispatchPublisher;
		this.digiOsCreatePort = digiOsCreatePort;
	}

	public void acceptRow(long companyId, long operatorId, String operatorType, Map<String, Object> row) {
		String name = PosStoreAccountRules.requireName(str(row, "name"));
		String employeeNo = PosStoreAccountRules.requireEmployeeNo(str(row, "employee_no"));
		String email = requireEmail(row);
		String mobile = requireMobile(row);
		String role = mapRole(str(row, "role"));
		String shopCode = requireText(row, "shop_code", "请选择调入门店");
		String status = mapStatus(str(row, "status"));
		Map<String, Object> jwt = jwt(companyId, operatorId, operatorType);
		PosStoreAccountScope.requireReadableShops(jwt);
		Map<String, Object> shop = shopMapper.selectShopByCompanyAndShopCode(companyId, shopCode);
		if (shop == null || shop.isEmpty()) {
			throw new BadRequestException("请选择调入门店");
		}
		Long shopId = toLong(shop.get("distributor_id"));
		PosStoreAccountScope.assertShopAllowed(jwt, shopId);
		String key = employeeNo.toLowerCase(Locale.ROOT);
		if (!acceptedKeys.add(key)) {
			throw new BadRequestException("员工号已存在，请更换员工号");
		}
		try {
			// 新增与更新均先 upsert DigiOS；失败本行失败、不写库（ECX-10668）
			digiOsCreatePort.ensureSyncedOnCreate(companyId, employeeNo, name, mobile, email);
			LambdaQueryWrapper<PosStoreAccount> w = new LambdaQueryWrapper<>();
			w.eq(PosStoreAccount::getEmployeeNo, employeeNo);
			PosStoreAccount existing = mapper.selectOne(w);
			int now = (int) (System.currentTimeMillis() / 1000L);
			if (existing != null) {
				existing.setName(name);
				existing.setEmail(email);
				existing.setMobile(mobile);
				existing.setRole(role);
				existing.setDistributorId(shopId);
				existing.setStatus(status);
				existing.setUpdated(now);
				mapper.updateById(existing);
				return;
			}
			PosStoreAccountPasswordService.GeneratedPassword pwd = passwordService.generateEncrypted();
			PosStoreAccount created = new PosStoreAccount();
			created.setCompanyId(companyId);
			created.setEmployeeNo(employeeNo);
			created.setName(name);
			created.setEmail(email);
			created.setMobile(mobile);
			created.setRole(role);
			created.setDistributorId(shopId);
			created.setStatus(status);
			created.setPasswordCipher(pwd.cipher());
			created.setCreated(now);
			created.setUpdated(now);
			try {
				mapper.insert(created);
			} catch (DuplicateKeyException e) {
				throw new BadRequestException("员工号已存在，请更换员工号");
			}
			scheduleCreateDispatch(created);
		} catch (RuntimeException e) {
			acceptedKeys.remove(key);
			throw e;
		}
	}

	private void scheduleCreateDispatch(PosStoreAccount row) {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("id", row.getId());
		entities.put("company_id", row.getCompanyId());
		entities.put("employee_no", row.getEmployeeNo());
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			createEventDispatchPublisher.publish(entities);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						createEventDispatchPublisher.publish(entities);
					}
				});
	}

	private Map<String, Object> jwt(long companyId, long operatorId, String operatorType) {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", companyId);
		jwt.put("operator_type", operatorType == null ? "" : operatorType);
		if (!"admin".equals(operatorType) && operatorId > 0) {
			Operators op = operatorsMapper.selectById(operatorId);
			if (op != null) {
				jwt.put("distributor_ids", op.getDistributorIds());
			}
		}
		return jwt;
	}

	private static String mapRole(String raw) {
		return switch (raw.trim()) {
			case "店长" -> "store_manager";
			case "主管" -> "supervisor";
			case "店员" -> "clerk";
			default -> throw new BadRequestException("请选择新角色");
		};
	}

	private static String mapStatus(String raw) {
		return switch (raw.trim()) {
			case "启用" -> "enabled";
			case "禁用" -> "disabled";
			case "离职" -> "resigned";
			default -> throw new BadRequestException("状态无效");
		};
	}

	private static String requireText(Map<String, Object> row, String key, String message) {
		String v = str(row, key);
		if (!StringUtils.hasText(v)) {
			throw new BadRequestException(message);
		}
		return v.trim();
	}

	private static String requireEmail(Map<String, Object> row) {
		String v = requireText(row, "email", "邮箱必填");
		return PosStoreAccountRules.optionalEmail(v);
	}

	private static String requireMobile(Map<String, Object> row) {
		String v = requireText(row, "mobile", "手机号必填");
		return PosStoreAccountRules.optionalMobile(v);
	}

	private static String str(Map<String, Object> row, String key) {
		Object v = row == null ? null : row.get(key);
		return v == null ? "" : v.toString();
	}

	private static Long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v));
	}
}
