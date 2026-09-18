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

package cn.shopex.ecshopx.espier.service.offline;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.domain.OfflineBankAccount;
import cn.shopex.ecshopx.espier.mapper.OfflineBankAccountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfflineBankAccountApplicationService {

	private final OfflineBankAccountMapper offlineBankAccountMapper;
	private final OfflineBankAccountMultiLangWriteService offlineBankAccountMultiLangWriteService;
	private final OfflineBankAccountMultiLangReadService offlineBankAccountMultiLangReadService;

	public OfflineBankAccountApplicationService(
			OfflineBankAccountMapper offlineBankAccountMapper,
			OfflineBankAccountMultiLangWriteService offlineBankAccountMultiLangWriteService,
			OfflineBankAccountMultiLangReadService offlineBankAccountMultiLangReadService) {
		this.offlineBankAccountMapper = offlineBankAccountMapper;
		this.offlineBankAccountMultiLangWriteService = offlineBankAccountMultiLangWriteService;
		this.offlineBankAccountMultiLangReadService = offlineBankAccountMultiLangReadService;
	}

	public Map<String, Object> lists(long companyId, int pageOneBasedIndex, int pageSize, String requestLangTag) {
		LambdaQueryWrapper<OfflineBankAccount> base = new LambdaQueryWrapper<>();
		base.eq(OfflineBankAccount::getCompanyId, companyId);
		long totalCount = offlineBankAccountMapper.selectCount(base);
		List<Map<String, Object>> list;
		if (totalCount == 0L) {
			list = new ArrayList<>();
		} else {
			@SuppressWarnings("unchecked")
			LambdaQueryWrapper<OfflineBankAccount> q =
					(LambdaQueryWrapper<OfflineBankAccount>) base.clone();
			q.orderByDesc(OfflineBankAccount::getCreated);
			if (pageSize > 0) {
				// 页码为 0 或更小时将 OFFSET 置为 0，避免驱动层对负 OFFSET 报错。
				long offset = (long) pageOneBasedIndex - 1L;
				if (offset < 0L) {
					offset = 0L;
				}
				q.last("LIMIT " + pageSize + " OFFSET " + offset);
			}
			List<OfflineBankAccount> entities = offlineBankAccountMapper.selectList(q);
			list = new ArrayList<>(entities.size());
			for (OfflineBankAccount entity : entities) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("id", entity.getId());
				row.put("company_id", entity.getCompanyId());
				row.put(
						"bank_account_name",
						entity.getBankAccountName() == null ? "" : entity.getBankAccountName());
				row.put("bank_account_no", entity.getBankAccountNo());
				row.put("bank_name", entity.getBankName());
				row.put("china_ums_no", entity.getChinaUmsNo());
				row.put("pic", entity.getPic());
				row.put("remark", entity.getRemark());
				row.put("is_default", entity.getIsDefault());
				row.put("created", entity.getCreated());
				row.put("updated", entity.getUpdated());
				list.add(row);
			}
			offlineBankAccountMultiLangReadService.applyToRows(list, requestLangTag);
			for (Map<String, Object> row : list) {
				boolean def = Objects.equals(row.get("is_default"), Boolean.TRUE)
						|| Objects.equals(row.get("is_default"), 1)
						|| Objects.equals(row.get("is_default"), 1L)
						|| "1".equals(String.valueOf(row.get("is_default")).trim());
				row.put("is_default", def ? "true" : "false");
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", list);
		return out;
	}

	public List<Map<String, Object>> getOfflineAccount(long companyId, String requestLangTag) {
		LambdaQueryWrapper<OfflineBankAccount> q = new LambdaQueryWrapper<>();
		q.eq(OfflineBankAccount::getCompanyId, companyId);
		q.orderByDesc(OfflineBankAccount::getIsDefault).orderByDesc(OfflineBankAccount::getCreated);
		List<OfflineBankAccount> entities = offlineBankAccountMapper.selectList(q);
		if (entities == null || entities.isEmpty()) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> list = new ArrayList<>(entities.size());
		for (OfflineBankAccount entity : entities) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", entity.getId());
			row.put("company_id", entity.getCompanyId());
			row.put(
					"bank_account_name",
					entity.getBankAccountName() == null ? "" : entity.getBankAccountName());
			row.put("bank_account_no", entity.getBankAccountNo());
			row.put("bank_name", entity.getBankName());
			row.put("china_ums_no", entity.getChinaUmsNo());
			row.put("pic", entity.getPic());
			row.put("remark", entity.getRemark());
			row.put("is_default", entity.getIsDefault());
			row.put("created", entity.getCreated());
			row.put("updated", entity.getUpdated());
			list.add(row);
		}
		offlineBankAccountMultiLangReadService.applyToRows(list, requestLangTag);
		return list;
	}

	public Map<String, Object> getInfo(long companyId, long accountId, String requestLangTag) {
		LambdaQueryWrapper<OfflineBankAccount> q = new LambdaQueryWrapper<>();
		q.eq(OfflineBankAccount::getId, accountId).eq(OfflineBankAccount::getCompanyId, companyId);
		OfflineBankAccount entity = offlineBankAccountMapper.selectOne(q);
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", entity.getId());
		row.put("company_id", entity.getCompanyId());
		row.put(
				"bank_account_name",
				entity.getBankAccountName() == null ? "" : entity.getBankAccountName());
		row.put("bank_account_no", entity.getBankAccountNo());
		row.put("bank_name", entity.getBankName());
		row.put("china_ums_no", entity.getChinaUmsNo());
		row.put("pic", entity.getPic());
		row.put("remark", entity.getRemark());
		row.put("is_default", entity.getIsDefault());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		List<Map<String, Object>> one = new ArrayList<>();
		one.add(row);
		offlineBankAccountMultiLangReadService.applyToRows(one, requestLangTag);
		boolean def = Objects.equals(row.get("is_default"), Boolean.TRUE)
				|| Objects.equals(row.get("is_default"), 1)
				|| Objects.equals(row.get("is_default"), 1L)
				|| "1".equals(String.valueOf(row.get("is_default")).trim());
		row.put("is_default", def ? "true" : "false");
		return row;
	}

	@Transactional(rollbackFor = Exception.class)
	public void delete(long companyId, String idPath) {
		String path = idPath == null ? "" : idPath;
		LambdaQueryWrapper<OfflineBankAccount> w = new LambdaQueryWrapper<>();
		w.eq(OfflineBankAccount::getCompanyId, companyId);
		if (path.matches("^-?\\d+$")) {
			try {
				w.eq(OfflineBankAccount::getId, Long.parseLong(path));
			} catch (NumberFormatException e) {
				w.apply("CAST(id AS CHAR) = {0}", path);
			}
		} else {
			w.apply("CAST(id AS CHAR) = {0}", path);
		}
		offlineBankAccountMapper.delete(w);
	}

	@Transactional(rollbackFor = Exception.class)
	public void create(long companyId, Map<String, Object> normalizedParams, String requestLangTag) {
		Integer isDefault = null;
		Object rawIsDefault = normalizedParams.get("is_default");
		if (rawIsDefault instanceof Number n) {
			isDefault = n.intValue();
		}

		if (Objects.equals(isDefault, 1)) {
			LambdaUpdateWrapper<OfflineBankAccount> u = new LambdaUpdateWrapper<>();
			u.eq(OfflineBankAccount::getCompanyId, companyId)
					.eq(OfflineBankAccount::getIsDefault, Boolean.TRUE)
					.set(OfflineBankAccount::getIsDefault, Boolean.FALSE);
			offlineBankAccountMapper.update(null, u);
		}

		OfflineBankAccount entity = new OfflineBankAccount();
		entity.setCompanyId(companyId);
		entity.setBankAccountName(String.valueOf(normalizedParams.get("bank_account_name")).trim());
		entity.setBankAccountNo(String.valueOf(normalizedParams.get("bank_account_no")).trim());
		entity.setBankName(String.valueOf(normalizedParams.get("bank_name")).trim());
		entity.setChinaUmsNo(String.valueOf(normalizedParams.get("china_ums_no")).trim());
		entity.setPic(String.valueOf(normalizedParams.get("pic")));
		entity.setRemark(String.valueOf(normalizedParams.get("remark")));
		entity.setIsDefault(Objects.equals(isDefault, 1));
		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreated((long) now);
		entity.setUpdated((long) now);
		offlineBankAccountMapper.insert(entity);
		if (entity.getId() == null) {
			throw new ResourceException("创建失败");
		}

		Map<String, Object> createDataSnapshot = new LinkedHashMap<>();
		createDataSnapshot.put("company_id", companyId);
		createDataSnapshot.put("bank_account_name", normalizedParams.get("bank_account_name"));
		createDataSnapshot.put("bank_name", normalizedParams.get("bank_name"));
		createDataSnapshot.put("remark", normalizedParams.get("remark"));
		offlineBankAccountMultiLangWriteService.syncAfterInsert(
				entity.getId().longValue(), createDataSnapshot, requestLangTag);
	}

	public void update(long companyId, Long accountId, Map<String, Object> normalizedParams, String requestLangTag) {
		Integer isDefault = null;
		Object rawIsDefault = normalizedParams.get("is_default");
		if (rawIsDefault instanceof Number n) {
			isDefault = n.intValue();
		}

		if (Objects.equals(isDefault, 1)) {
			LambdaUpdateWrapper<OfflineBankAccount> u = new LambdaUpdateWrapper<>();
			u.eq(OfflineBankAccount::getCompanyId, companyId)
					.eq(OfflineBankAccount::getIsDefault, Boolean.TRUE)
					.set(OfflineBankAccount::getIsDefault, Boolean.FALSE);
			offlineBankAccountMapper.update(null, u);
		}

		if (accountId == null) {
			throw new ResourceException("未查询到更新数据");
		}

		LambdaQueryWrapper<OfflineBankAccount> q = new LambdaQueryWrapper<>();
		q.eq(OfflineBankAccount::getId, accountId).eq(OfflineBankAccount::getCompanyId, companyId);
		OfflineBankAccount row = offlineBankAccountMapper.selectOne(q);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}

		row.setBankAccountName(String.valueOf(normalizedParams.get("bank_account_name")).trim());
		row.setBankAccountNo(String.valueOf(normalizedParams.get("bank_account_no")).trim());
		row.setBankName(String.valueOf(normalizedParams.get("bank_name")).trim());
		row.setChinaUmsNo(String.valueOf(normalizedParams.get("china_ums_no")).trim());
		row.setPic(String.valueOf(normalizedParams.get("pic")));
		row.setRemark(String.valueOf(normalizedParams.get("remark")));
		row.setIsDefault(Objects.equals(isDefault, 1));
		row.setUpdated((long) (System.currentTimeMillis() / 1000L));

		int affected = offlineBankAccountMapper.updateById(row);
		if (affected == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		Map<String, Object> langSnap = new LinkedHashMap<>();
		langSnap.put("bank_account_name", normalizedParams.get("bank_account_name"));
		langSnap.put("bank_name", normalizedParams.get("bank_name"));
		langSnap.put("remark", normalizedParams.get("remark"));
		offlineBankAccountMultiLangWriteService.syncAfterUpdate(
				row.getId(), intFromCompany(companyId), langSnap, requestLangTag);
	}

	private static int intFromCompany(long companyId) {
		return (int) companyId;
	}
}
