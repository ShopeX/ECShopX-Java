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

import cn.shopex.ecshopx.point.domain.PointMember;
import cn.shopex.ecshopx.point.domain.PointMemberLog;
import cn.shopex.ecshopx.point.mapper.PointMemberLogMapper;
import cn.shopex.ecshopx.point.mapper.PointMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class PointMemberDepositExchangeWriteService {

	private static final int JOURNAL_TYPE_DEPOSIT_TO_POINT = 16;

	private final PointMemberMapper pointMemberMapper;
	private final PointMemberLogMapper pointMemberLogMapper;

	public PointMemberDepositExchangeWriteService(
			PointMemberMapper pointMemberMapper, PointMemberLogMapper pointMemberLogMapper) {
		this.pointMemberMapper = pointMemberMapper;
		this.pointMemberLogMapper = pointMemberLogMapper;
	}

	public void addPointForDepositExchange(long userId, long companyId, long pointDelta, long moneyFen) {
		String yuan =
				BigDecimal.valueOf(moneyFen)
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN)
						.stripTrailingZeros()
						.toPlainString();
		String pointDesc = yuan + "储值兑换积分" + pointDelta;

		PointMember existing =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getCompanyId, companyId)
								.eq(PointMember::getUserId, userId)
								.last("LIMIT 1"));
		int now = (int) Instant.now().getEpochSecond();
		if (existing != null) {
			long base = existing.getPoint() != null ? existing.getPoint() : 0L;
			pointMemberMapper.update(
					null,
					new LambdaUpdateWrapper<PointMember>()
							.eq(PointMember::getUserId, userId)
							.eq(PointMember::getCompanyId, companyId)
							.set(PointMember::getPoint, base + pointDelta));
		} else {
			PointMember row = new PointMember();
			row.setUserId(userId);
			row.setCompanyId(companyId);
			row.setPoint(pointDelta);
			pointMemberMapper.insert(row);
		}

		PointMemberLog logRow = new PointMemberLog();
		logRow.setUserId(userId);
		logRow.setCompanyId(companyId);
		logRow.setJournalType(JOURNAL_TYPE_DEPOSIT_TO_POINT);
		logRow.setPointDesc(pointDesc);
		logRow.setIncome((int) pointDelta);
		logRow.setOutcome(0);
		logRow.setOrderId(null);
		logRow.setCreated(now);
		logRow.setUpdated(now);
		pointMemberLogMapper.insert(logRow);
	}
}
