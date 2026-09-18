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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorCheckSmsVcodeService {

	private static final String MSG_VALIDATION_REQUIRED = "validation.required，";

	private final DistributorMapper distributorMapper;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final ObjectMapper objectMapper;
	private final StringRedisTemplate companysRedisTemplate;

	public DistributorCheckSmsVcodeService(
			DistributorMapper distributorMapper,
			DistributorListRowFormatService distributorListRowFormatService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			ObjectMapper objectMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.distributorMapper = distributorMapper;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.objectMapper = objectMapper;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public Map<String, Object> checkSmsVcode(long companyId, String distributorIdRaw, String vcode) {
		boolean missingDistributorId = distributorIdRaw == null || distributorIdRaw.isBlank();
		boolean missingVcode = vcode == null || !StringUtils.hasText(vcode.trim());
		if (missingDistributorId && missingVcode) {
			throw new ResourceException(MSG_VALIDATION_REQUIRED + MSG_VALIDATION_REQUIRED);
		}
		if (missingDistributorId) {
			throw new ResourceException(MSG_VALIDATION_REQUIRED);
		}
		if (missingVcode) {
			throw new ResourceException(MSG_VALIDATION_REQUIRED);
		}

		String t = distributorIdRaw.trim();
		boolean explicitBranch;
		long explicitId = 0L;
		try {
			long parsed = Long.parseLong(t);
			explicitBranch = parsed > 0L;
			explicitId = parsed;
		} catch (NumberFormatException e) {
			explicitBranch = false;
			explicitId = 0L;
		}

		Distributor chosen = null;
		boolean clientVirtualMainStoreId = false;
		if (explicitBranch) {
			Distributor entity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.eq(Distributor::getDistributorId, explicitId)
					.last("LIMIT 1"));
			if (entity != null) {
				chosen = entity;
			}
		}
		if (chosen == null) {
			DefaultOrMainResolution resolution = resolveDefaultOrMainFallback(companyId);
			chosen = resolution.distributor;
			clientVirtualMainStoreId = resolution.useVirtualMainStoreIdInClientMap;
		}
		if (chosen == null) {
			throw new ResourceException("店铺或店铺联系手机不存在");
		}

		int distributorSelf = chosen.getDistributorSelf() == null ? 0 : chosen.getDistributorSelf();
		long rowDistributorId = chosen.getDistributorId() == null ? 0L : chosen.getDistributorId();
		Map<String, Object> selfRule =
				selfDeliverySettingReadService.getSetting(companyId, rowDistributorId, distributorSelf);
		Map<String, Object> row = distributorListRowFormatService.formatStoreRow(chosen, selfRule, objectMapper);

		if (isEmptyMobileForBindAction(row.get("mobile"))) {
			throw new ResourceException("店铺或店铺联系手机不存在");
		}

		Object mobileObj = row.get("mobile");
		String mobileForRedis = mobileObj == null ? null : normalize(String.valueOf(mobileObj));
		String key = "distributor-bind:company" + companyId + ":" + mobileForRedis;
		String stored = companysRedisTemplate.opsForValue().get(key);
		if (!Objects.equals(normalize(stored), normalize(vcode))) {
			throw new ResourceException("验证码错误");
		}
		companysRedisTemplate.delete(key);

		if (clientVirtualMainStoreId) {
			row.put("distributor_id", 0L);
		}
		return row;
	}

	/**
	 * Same resolution order as {@link DistributorRepositoryGetInfoSimpleService#getDistributorInfoForCompanyShop}
	 * (default row then main self row), without the positive-id branch; operates on entities only.
	 */
	private DefaultOrMainResolution resolveDefaultOrMainFallback(long companyId) {
		Distributor defaultEntity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getIsDefault, 1)
				.last("LIMIT 1"));

		Distributor chosen = defaultEntity;
		boolean defaultStoreNonEmpty = defaultEntity != null;

		if (chosen == null) {
			Distributor mainEntity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.eq(Distributor::getDistributorSelf, 1)
					.last("LIMIT 1"));
			if (mainEntity != null) {
				return new DefaultOrMainResolution(mainEntity, true);
			}
			return new DefaultOrMainResolution(null, false);
		}
		if (defaultStoreNonEmpty && shouldUseMainDistributorInsteadOfDefault(chosen)) {
			Distributor mainEntity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.eq(Distributor::getDistributorSelf, 1)
					.last("LIMIT 1"));
			if (mainEntity != null) {
				return new DefaultOrMainResolution(mainEntity, true);
			}
			// Default row must be replaced by a self-operated main store; absent main store means no resolution.
			return new DefaultOrMainResolution(null, false);
		}
		return new DefaultOrMainResolution(chosen, false);
	}

	private static final class DefaultOrMainResolution {
		private final Distributor distributor;
		private final boolean useVirtualMainStoreIdInClientMap;

		private DefaultOrMainResolution(Distributor distributor, boolean useVirtualMainStoreIdInClientMap) {
			this.distributor = distributor;
			this.useVirtualMainStoreIdInClientMap = useVirtualMainStoreIdInClientMap;
		}
	}

	private static boolean shouldUseMainDistributorInsteadOfDefault(Distributor d) {
		String isValid = d.getIsValid();
		if ("false".equals(isValid) || Boolean.FALSE.equals(isValid)) {
			return true;
		}
		return distributorSelfIsStrictZero(d.getDistributorSelf());
	}

	/** True when {@code distributor_self} is numeric zero or the literal string {@code "0"} (strict). */
	private static boolean distributorSelfIsStrictZero(Object self) {
		if (self == null) {
			return false;
		}
		if ("0".equals(self)) {
			return true;
		}
		if (self instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		return false;
	}

	private static boolean isEmptyMobileForBindAction(Object mobile) {
		if (mobile == null) {
			return true;
		}
		if (mobile instanceof String s) {
			String trimmed = s.trim();
			return trimmed.isEmpty() || "0".equals(trimmed);
		}
		if (mobile instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		if (Boolean.FALSE.equals(mobile)) {
			return true;
		}
		String trimmed = String.valueOf(mobile).trim();
		return trimmed.isEmpty() || "0".equals(trimmed);
	}

	private static String normalize(String s) {
		return s == null ? null : s.trim();
	}
}
