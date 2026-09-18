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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.members.service.h5.dto.H5LoginAttemptResult;
import com.nimbusds.jose.JOSEException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class H5LoginOrchestrator {

	private final EspierLocalUserResolver espierLocalUserResolver;

	private final H5JwtIssuer h5JwtIssuer;

	public H5LoginOrchestrator(EspierLocalUserResolver espierLocalUserResolver, H5JwtIssuer h5JwtIssuer) {
		this.espierLocalUserResolver = espierLocalUserResolver;
		this.h5JwtIssuer = h5JwtIssuer;
	}

	public H5LoginAttemptResult attempt(Map<String, Object> credentials) {
		Optional<H5GenericUser> user = espierLocalUserResolver.retrieve(credentials);
		if (user.isEmpty()) {
			return new H5LoginAttemptResult(false, null, true);
		}
		try {
			String token = h5JwtIssuer.issue(user.get());
			return new H5LoginAttemptResult(true, token, false);
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}
}
