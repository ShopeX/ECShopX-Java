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

package cn.shopex.ecshopx.members.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.distribution.CompanyMapGeocodePort;
import cn.shopex.ecshopx.members.domain.MembersAddress;
import cn.shopex.ecshopx.members.mapper.MembersAddressMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UpdateAddressLatAndLngJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(UpdateAddressLatAndLngJobHandler.class);

	private static final Duration LOCK_TTL = Duration.ofSeconds(120);

	private final CompanyMapGeocodePort companyMapGeocodePort;
	private final MembersAddressMapper membersAddressMapper;
	private final StringRedisTemplate sharedStringRedisTemplate;

	public UpdateAddressLatAndLngJobHandler(
			CompanyMapGeocodePort companyMapGeocodePort,
			MembersAddressMapper membersAddressMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.companyMapGeocodePort = companyMapGeocodePort;
		this.membersAddressMapper = membersAddressMapper;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = readLong(payload.get("company_id"), 0L);
		long userId = readLong(payload.get("user_id"), 0L);
		long addressId = readLong(payload.get("address_id"), 0L);
		if (companyId <= 0L || userId <= 0L || addressId <= 0L) {
			return;
		}
		String lockKey = buildLockKey(companyId, addressId);
		Boolean locked = sharedStringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
		if (!Boolean.TRUE.equals(locked)) {
			return;
		}
		try {
			MembersAddress row = loadRow(companyId, userId, addressId);
			if (row == null) {
				return;
			}
			String city = row.getCity();
			String detail = row.getAdrdetail();
			if (!StringUtils.hasText(city) || !StringUtils.hasText(detail)) {
				return;
			}
			try {
				CompanyMapGeocodePort.GeocodeLatLng g = companyMapGeocodePort.geocode(companyId, city, detail);
				if (g == null || g.lat() == null || g.lng() == null) {
					return;
				}
				MembersAddress patch = new MembersAddress();
				patch.setLat(g.lat());
				patch.setLng(g.lng());
				membersAddressMapper.update(
						patch,
						new LambdaUpdateWrapper<MembersAddress>()
								.eq(MembersAddress::getAddressId, addressId)
								.eq(MembersAddress::getCompanyId, companyId));
			} catch (RuntimeException ex) {
				log.warn(
						"[update-address-lat-lng] geocode or update failed companyId={} addressId={}: {}",
						companyId,
						addressId,
						ex.getMessage());
			}
		} finally {
			try {
				sharedStringRedisTemplate.delete(lockKey);
			} catch (RuntimeException ex) {
				log.debug("[update-address-lat-lng] lock release failed: {}", ex.getMessage());
			}
		}
	}

	private static String buildLockKey(long companyId, long addressId) {
		return "ecshopx:members:address_lat_lng:" + companyId + ":" + addressId;
	}

	private MembersAddress loadRow(long companyId, long userId, long addressId) {
		return membersAddressMapper.selectOne(
				new LambdaQueryWrapper<MembersAddress>()
						.eq(MembersAddress::getCompanyId, companyId)
						.eq(MembersAddress::getUserId, userId)
						.eq(MembersAddress::getAddressId, addressId)
						.last("LIMIT 1"));
	}

	private static long readLong(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
