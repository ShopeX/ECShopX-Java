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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.CardPackageTrigger;
import cn.shopex.ecshopx.kaquan.domain.CardPackageReceiveRecord;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageReceiveRecordMapper;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageTriggerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CardPackageTriggerGradeBindService {

	private final CardPackageTriggerMapper cardPackageTriggerMapper;
	private final CardPackageReceiveRecordMapper cardPackageReceiveRecordMapper;
	private final TransactionTemplate requiresNewTemplate;

	public CardPackageTriggerGradeBindService(CardPackageTriggerMapper cardPackageTriggerMapper,
			CardPackageReceiveRecordMapper cardPackageReceiveRecordMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.cardPackageTriggerMapper = cardPackageTriggerMapper;
		this.cardPackageReceiveRecordMapper = cardPackageReceiveRecordMapper;
		this.requiresNewTemplate = new TransactionTemplate(platformTransactionManager);
		this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public void deleteTriggersForGrades(long companyId, List<Long> associationIds, String triggerType) {
		if (associationIds == null || associationIds.isEmpty()) {
			return;
		}
		cardPackageTriggerMapper.delete(new LambdaQueryWrapper<CardPackageTrigger>()
				.eq(CardPackageTrigger::getCompanyId, companyId)
				.eq(CardPackageTrigger::getTriggerType, triggerType)
				.in(CardPackageTrigger::getAssociationId, associationIds));
	}

	public void setTriggersByPackageSet(long companyId, List<?> packageIdSet, long associationId, String triggerType) {
		assertTriggerType(triggerType);
		if (packageIdSet == null || packageIdSet.isEmpty()) {
			return;
		}
		List<Long> uniqueIds = normalizePackageIds(packageIdSet);
		if (uniqueIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		requiresNewTemplate.executeWithoutResult(status -> {
			try {
				cardPackageTriggerMapper.delete(new LambdaQueryWrapper<CardPackageTrigger>()
						.eq(CardPackageTrigger::getCompanyId, companyId)
						.eq(CardPackageTrigger::getTriggerType, triggerType)
						.eq(CardPackageTrigger::getAssociationId, associationId));
				for (Long packageId : uniqueIds) {
					CardPackageTrigger row = new CardPackageTrigger();
					row.setPackageId(packageId);
					row.setCompanyId(companyId);
					row.setTriggerType(triggerType);
					row.setAssociationId(associationId);
					row.setCreated(now);
					row.setUpdated(now);
					cardPackageTriggerMapper.insert(row);
				}
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				String msg = e.getMessage();
				throw new ResourceException(msg == null || msg.isEmpty() ? "卡券包触发关联失败" : msg);
			}
		});
	}

	public void clearReceiveRecords(long companyId, long gradeId, String triggerType) {
		assertTriggerType(triggerType);
		cardPackageReceiveRecordMapper.delete(new LambdaQueryWrapper<CardPackageReceiveRecord>()
				.eq(CardPackageReceiveRecord::getCompanyId, companyId)
				.eq(CardPackageReceiveRecord::getGradeId, gradeId)
				.eq(CardPackageReceiveRecord::getTriggerType, triggerType));
	}

	private static void assertTriggerType(String triggerType) {
		if (!"grade".equals(triggerType) && !"vip_grade".equals(triggerType)) {
			throw new ResourceException("trigger_type 无效");
		}
	}

	private static List<Long> normalizePackageIds(List<?> packageIdSet) {
		if (packageIdSet.isEmpty()) {
			return List.of();
		}
		Object first = packageIdSet.get(0);
		boolean mapMode = !isNumericScalar(first) && first instanceof Map<?, ?> m && m.containsKey("package_id");
		Set<Long> ordered = new LinkedHashSet<>();
		if (mapMode) {
			for (Object o : packageIdSet) {
				if (o instanceof Map<?, ?> row) {
					Object pid = row.get("package_id");
					Long id = toLongId(pid);
					if (id != null && id > 0L) {
						ordered.add(id);
					}
				}
			}
		} else {
			for (Object o : packageIdSet) {
				Long id = toLongId(o);
				if (id != null && id > 0L) {
					ordered.add(id);
				}
			}
		}
		return new ArrayList<>(ordered);
	}

	private static boolean isNumericScalar(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof Number) {
			return true;
		}
		if (o instanceof String s) {
			try {
				new BigDecimal(s.trim());
				return true;
			} catch (NumberFormatException ignored) {
				return false;
			}
		}
		return false;
	}

	private static Long toLongId(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}
}
