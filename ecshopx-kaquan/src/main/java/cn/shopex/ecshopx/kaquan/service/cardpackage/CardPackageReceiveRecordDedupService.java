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

import cn.shopex.ecshopx.kaquan.domain.CardPackageReceiveRecord;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageReceiveRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CardPackageReceiveRecordDedupService {

	private final CardPackageReceiveRecordMapper cardPackageReceiveRecordMapper;

	public CardPackageReceiveRecordDedupService(CardPackageReceiveRecordMapper cardPackageReceiveRecordMapper) {
		this.cardPackageReceiveRecordMapper = cardPackageReceiveRecordMapper;
	}

	public Optional<CardPackageReceiveRecord> find(long companyId, long userId, long gradeId, String triggerType) {
		CardPackageReceiveRecord row = cardPackageReceiveRecordMapper.selectOne(new LambdaQueryWrapper<CardPackageReceiveRecord>()
				.eq(CardPackageReceiveRecord::getCompanyId, companyId)
				.eq(CardPackageReceiveRecord::getUserId, userId)
				.eq(CardPackageReceiveRecord::getGradeId, gradeId)
				.eq(CardPackageReceiveRecord::getTriggerType, triggerType)
				.last("LIMIT 1"));
		return Optional.ofNullable(row);
	}

	public void insert(long companyId, long userId, long gradeId, String triggerType) {
		int now = (int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE);
		CardPackageReceiveRecord row = new CardPackageReceiveRecord();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setGradeId(gradeId);
		row.setTriggerType(triggerType);
		row.setCreated(now);
		row.setUpdated(now);
		cardPackageReceiveRecordMapper.insert(row);
	}
}
