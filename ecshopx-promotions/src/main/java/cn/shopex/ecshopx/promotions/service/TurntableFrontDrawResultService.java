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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.point.domain.PointMember;
import cn.shopex.ecshopx.point.mapper.PointMemberMapper;
import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawResponseMapper;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawStatus;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableErrorCodes;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableErrorMessages;
import cn.shopex.ecshopx.promotions.mapper.TurntableLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TurntableFrontDrawResultService {

	private final TurntableLogMapper turntableLogMapper;
	private final PointMemberMapper pointMemberMapper;
	private final MessageSource messageSource;

	public TurntableFrontDrawResultService(
			TurntableLogMapper turntableLogMapper,
			PointMemberMapper pointMemberMapper,
			MessageSource messageSource) {
		this.turntableLogMapper = turntableLogMapper;
		this.pointMemberMapper = pointMemberMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getDrawResult(
			long userId, long companyId, String requestIdRaw, String recordIdRaw) {
		Locale locale = LocaleContextHolder.getLocale();
		boolean hasReq = StringUtils.hasText(requestIdRaw == null ? null : requestIdRaw.trim());
		boolean hasRec = StringUtils.hasText(recordIdRaw == null ? null : recordIdRaw.trim());
		if (!hasReq && !hasRec) {
			throw new BadRequestException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.REQUEST_ID_INVALID, locale));
		}

		TurntableLog log = null;
		if (hasRec) {
			long recordId;
			try {
				recordId = Long.parseLong(recordIdRaw.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						TurntableErrorMessages.message(messageSource, TurntableErrorCodes.REQUEST_ID_INVALID, locale));
			}
			log =
					turntableLogMapper.selectOne(
							new LambdaQueryWrapper<TurntableLog>()
									.eq(TurntableLog::getId, recordId)
									.eq(TurntableLog::getUserId, userId)
									.eq(TurntableLog::getCompanyId, companyId)
									.last("LIMIT 1"));
		}
		if (log == null && hasReq) {
			String requestId = TurntableDrawResponseMapper.normalizeRequestId(requestIdRaw);
			if (!TurntableDrawResponseMapper.isValidRequestId(requestId)) {
				throw new BadRequestException(
						TurntableErrorMessages.message(messageSource, TurntableErrorCodes.REQUEST_ID_INVALID, locale));
			}
			log =
					turntableLogMapper.selectOne(
							new LambdaQueryWrapper<TurntableLog>()
									.eq(TurntableLog::getCompanyId, companyId)
									.eq(TurntableLog::getUserId, userId)
									.eq(TurntableLog::getRequestId, requestId)
									.last("LIMIT 1"));
		}
		if (log == null) {
			throw new ResourceException(
					TurntableErrorMessages.message(messageSource, TurntableErrorCodes.ACTIVITY_NOT_FOUND, locale));
		}

		Long remainPoints = null;
		PointMember pm =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (pm != null) {
			remainPoints = pm.getPoint();
		}

		if (TurntableDrawStatus.PROCESSING.equals(log.getStatus())) {
			return TurntableDrawResponseMapper.processing(
					log.getActId() == null ? 0L : log.getActId(), log.getId(), log.getRequestId());
		}
		return TurntableDrawResponseMapper.fromLog(log, remainPoints);
	}
}
