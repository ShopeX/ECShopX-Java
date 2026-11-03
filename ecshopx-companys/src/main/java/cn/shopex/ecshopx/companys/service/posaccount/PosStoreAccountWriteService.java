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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.digios.PosStoreAccountDigiOsCreatePort;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class PosStoreAccountWriteService {

	private final PosStoreAccountMapper mapper;
	private final DistributionDistributorSelfReadMapper shopMapper;
	private final PosStoreAccountPasswordService passwordService;
	private final PosStoreAccountQueryService queryService;
	private final PosStoreAccountCreateEventDispatchPublisher createEventDispatchPublisher;
	private final PosStoreAccountDigiOsCreatePort digiOsCreatePort;

	public PosStoreAccountWriteService(
			PosStoreAccountMapper mapper,
			DistributionDistributorSelfReadMapper shopMapper,
			PosStoreAccountPasswordService passwordService,
			PosStoreAccountQueryService queryService,
			PosStoreAccountCreateEventDispatchPublisher createEventDispatchPublisher,
			PosStoreAccountDigiOsCreatePort digiOsCreatePort) {
		this.mapper = mapper;
		this.shopMapper = shopMapper;
		this.passwordService = passwordService;
		this.queryService = queryService;
		this.createEventDispatchPublisher = createEventDispatchPublisher;
		this.digiOsCreatePort = digiOsCreatePort;
	}

	public Map<String, Object> create(Map<String, Object> jwt, Map<String, Object> body) {
		long companyId = PosStoreAccountScope.companyId(jwt);
		PosStoreAccountScope.requireReadableShops(jwt);
		String employeeNo = PosStoreAccountRules.requireEmployeeNo(str(body, "employee_no"));
		String role = PosStoreAccountRules.requireRole(str(body, "role"));
		Long shopId = PosStoreAccountRules.requireDistributorId(body.get("distributor_id"));
		PosStoreAccountScope.assertShopAllowed(jwt, shopId);
		Map<String, Object> shop = requireShop(companyId, shopId);
		String name = PosStoreAccountRules.optionalName(str(body, "name"));
		String email = PosStoreAccountRules.optionalEmail(str(body, "email"));
		String mobile = PosStoreAccountRules.optionalMobile(str(body, "mobile"));
		assertEmployeeNoFree(employeeNo);
		// DigiOS 开启时先 upsert；失败抛 BadRequest，阻断本地创建（ECX-10668）
		digiOsCreatePort.ensureSyncedOnCreate(companyId, employeeNo, name, mobile, email);
		PosStoreAccountPasswordService.GeneratedPassword pwd = passwordService.generateEncrypted();
		int now = (int) (System.currentTimeMillis() / 1000L);
		PosStoreAccount row = new PosStoreAccount();
		row.setCompanyId(companyId);
		row.setEmployeeNo(employeeNo);
		row.setName(name);
		row.setEmail(email);
		row.setMobile(mobile);
		row.setRole(role);
		row.setDistributorId(shopId);
		row.setStatus("enabled");
		row.setPasswordCipher(pwd.cipher());
		row.setCreated(now);
		row.setUpdated(now);
		try {
			mapper.insert(row);
		} catch (DuplicateKeyException e) {
			throw new BadRequestException("员工号已存在，请更换员工号");
		}
		scheduleCreateDispatch(row);
		Map<String, Object> out = PosStoreAccountQueryService.toPublicRow(row, shop);
		out.put("password", pwd.plain());
		return out;
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

	public Map<String, Object> update(Map<String, Object> jwt, long id, Map<String, Object> body) {
		PosStoreAccount row = queryService.requireReadable(jwt, id);
		if (body.containsKey("updated") && body.get("updated") != null) {
			Integer expected = toInt(body.get("updated"));
			if (expected != null && row.getUpdated() != null && !expected.equals(row.getUpdated())) {
				throw new ResourceException("数据已变更，请刷新后重试");
			}
		}
		if (body.containsKey("status")) {
			String next = PosStoreAccountRules.requireStatus(str(body, "status"), row.getStatus());
			if ("resigned".equals(row.getStatus()) && !"resigned".equals(next)) {
				throw new BadRequestException("离职账号不可恢复");
			}
			row.setStatus(next);
		}
		long companyId = row.getCompanyId();
		Long shopId =
				body.containsKey("distributor_id")
						? PosStoreAccountRules.requireDistributorId(body.get("distributor_id"))
						: row.getDistributorId();
		PosStoreAccountScope.assertShopAllowed(jwt, shopId);
		Map<String, Object> shop = requireShop(companyId, shopId);
		if (body.containsKey("role")) {
			row.setRole(PosStoreAccountRules.requireRole(str(body, "role")));
		}
		if (body.containsKey("name")) {
			row.setName(PosStoreAccountRules.optionalName(str(body, "name")));
		}
		if (body.containsKey("email")) {
			row.setEmail(PosStoreAccountRules.optionalEmail(str(body, "email")));
		}
		if (body.containsKey("mobile")) {
			row.setMobile(PosStoreAccountRules.optionalMobile(str(body, "mobile")));
		}
		row.setDistributorId(shopId);
		row.setUpdated((int) (System.currentTimeMillis() / 1000L));
		mapper.updateById(row);
		return PosStoreAccountQueryService.toPublicRow(row, shop);
	}

	public Map<String, Object> changeRole(Map<String, Object> jwt, long id, String role) {
		PosStoreAccount row = requireActive(jwt, id);
		row.setRole(PosStoreAccountRules.requireRole(role));
		row.setUpdated((int) (System.currentTimeMillis() / 1000L));
		mapper.updateById(row);
		return PosStoreAccountQueryService.toPublicRow(
				row, requireShop(row.getCompanyId(), row.getDistributorId()));
	}

	public Map<String, Object> changeShop(Map<String, Object> jwt, long id, Object distributorId) {
		PosStoreAccount row = requireActive(jwt, id);
		Long shopId = PosStoreAccountRules.requireDistributorId(distributorId);
		PosStoreAccountScope.assertShopAllowed(jwt, shopId);
		Map<String, Object> shop = requireShop(row.getCompanyId(), shopId);
		row.setDistributorId(shopId);
		row.setUpdated((int) (System.currentTimeMillis() / 1000L));
		mapper.updateById(row);
		return PosStoreAccountQueryService.toPublicRow(row, shop);
	}

	private PosStoreAccount requireActive(Map<String, Object> jwt, long id) {
		PosStoreAccount row = queryService.requireReadable(jwt, id);
		if ("resigned".equals(row.getStatus())) {
			throw new BadRequestException("账号已离职，无法操作");
		}
		return row;
	}

	private void assertEmployeeNoFree(String employeeNo) {
		LambdaQueryWrapper<PosStoreAccount> w = new LambdaQueryWrapper<>();
		w.eq(PosStoreAccount::getEmployeeNo, employeeNo);
		if (mapper.selectOne(w) != null) {
			throw new BadRequestException("员工号已存在，请更换员工号");
		}
	}

	Map<String, Object> requireShop(long companyId, long shopId) {
		Map<String, Object> shop = shopMapper.selectShopByCompanyAndId(companyId, shopId);
		if (shop == null || shop.isEmpty()) {
			throw new BadRequestException("请选择调入门店");
		}
		return shop;
	}

	private static String str(Map<String, Object> body, String key) {
		Object v = body == null ? null : body.get(key);
		return v == null ? "" : v.toString();
	}

	private static Integer toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v == null || !StringUtils.hasText(v.toString())) {
			return null;
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
