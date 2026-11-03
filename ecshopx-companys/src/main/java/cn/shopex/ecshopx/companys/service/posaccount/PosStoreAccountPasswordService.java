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

package cn.shopex.ecshopx.companys.service.posaccount;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PosStoreAccountPasswordService {

	private final PosStoreAccountMapper mapper;
	private final SensitiveFieldEncryptor encryptor;
	private final PosStoreAccountPasswordGenerator generator;

	public PosStoreAccountPasswordService(
			PosStoreAccountMapper mapper,
			SensitiveFieldEncryptor encryptor,
			PosStoreAccountPasswordGenerator generator) {
		this.mapper = mapper;
		this.encryptor = encryptor;
		this.generator = generator;
	}

	public Map<String, Object> reset(Map<String, Object> jwt, long id) {
		PosStoreAccount row = requireWritable(jwt, id);
		GeneratedPassword generated = generateEncrypted();
		row.setPasswordCipher(generated.cipher());
		row.setUpdated((int) (System.currentTimeMillis() / 1000L));
		mapper.updateById(row);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", row.getId());
		out.put("password", generated.plain());
		return out;
	}

	public GeneratedPassword generateEncrypted() {
		String plain = generator.generate();
		try {
			return new GeneratedPassword(plain, encryptor.encryptAlways(plain));
		} catch (RuntimeException e) {
			throw new BadRequestException("密码生成失败请重试");
		}
	}

	public String decryptAlways(String cipher) {
		return encryptor.decryptAlways(cipher);
	}

	PosStoreAccount requireWritable(Map<String, Object> jwt, long id) {
		PosStoreAccount row = mapper.selectById(id);
		long companyId = PosStoreAccountScope.companyId(jwt);
		if (row == null || row.getCompanyId() == null || row.getCompanyId() != companyId) {
			throw new ForbiddenException("无访问权限");
		}
		PosStoreAccountScope.assertShopAllowed(jwt, row.getDistributorId());
		if ("resigned".equals(row.getStatus())) {
			throw new BadRequestException("账号已离职，无法重置密码");
		}
		return row;
	}

	public record GeneratedPassword(String plain, String cipher) {}
}
