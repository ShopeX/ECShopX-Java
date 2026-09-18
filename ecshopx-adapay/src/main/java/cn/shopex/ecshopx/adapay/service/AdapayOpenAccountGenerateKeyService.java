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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdapayOpenAccountGenerateKeyService {

	public Map<String, String> generateKey() {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(1024);
			KeyPair pair = kpg.generateKeyPair();

			if (pair.getPrivate() == null || pair.getPublic() == null) {
				throw new ResourceException("密钥生成失败");
			}
			byte[] privateEncoded = pair.getPrivate().getEncoded();
			byte[] publicEncoded = pair.getPublic().getEncoded();
			if (privateEncoded == null || publicEncoded == null) {
				throw new ResourceException("密钥生成失败");
			}

			String privateKey = Base64.getEncoder().encodeToString(privateEncoded);
			String publicKey = Base64.getEncoder().encodeToString(publicEncoded);
			if (privateKey == null || publicKey == null) {
				throw new ResourceException("密钥生成失败");
			}

			Map<String, String> result = new LinkedHashMap<>();
			result.put("private_key", privateKey);
			result.put("public_key", publicKey);
			return result;
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			throw new ResourceException("密钥生成失败");
		}
	}
}
