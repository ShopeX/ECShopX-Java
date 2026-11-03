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

package cn.shopex.ecshopx.companys.service.posauth;

import cn.shopex.ecshopx.common.auth.PosJwtSessionValidatorPort;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/** G-L9：禁用/离职/调店/改角色后旧 POS token 立刻失效。 */
@Service
public class PosJwtSessionValidator implements PosJwtSessionValidatorPort {

	private final PosStoreAccountMapper mapper;
	private final MessageSource messageSource;

	public PosJwtSessionValidator(PosStoreAccountMapper mapper, MessageSource messageSource) {
		this.mapper = mapper;
		this.messageSource = messageSource;
	}

	@Override
	public void assertSessionStillValid(Map<String, Object> jwtClaims) {
		Long id = longOrNull(jwtClaims.get("pos_store_account_id"));
		if (id == null) {
			id = longOrNull(jwtClaims.get("id"));
		}
		if (id == null || id <= 0L) {
			throw invalid();
		}
		PosStoreAccount row = mapper.selectById(id);
		if (row == null) {
			throw invalid();
		}
		if (!"enabled".equals(row.getStatus())) {
			throw invalid();
		}
		if (!Objects.equals(String.valueOf(jwtClaims.get("status")), row.getStatus())) {
			throw invalid();
		}
		if (!Objects.equals(longOrNull(jwtClaims.get("distributor_id")), row.getDistributorId())) {
			throw invalid();
		}
		if (!Objects.equals(String.valueOf(jwtClaims.get("role")), row.getRole())) {
			throw invalid();
		}
	}

	private UnauthorizedException invalid() {
		return new UnauthorizedException(PosAuthMessages.loginInvalid(messageSource));
	}

	private static Long longOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String text = String.valueOf(raw).trim();
		if (text.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
