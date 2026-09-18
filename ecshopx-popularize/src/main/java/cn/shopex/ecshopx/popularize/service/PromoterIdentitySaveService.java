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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.domain.PromoterIdentity;
import cn.shopex.ecshopx.popularize.mapper.PromoterIdentityMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(rollbackFor = Exception.class)
public class PromoterIdentitySaveService {

	private final PromoterIdentityMapper promoterIdentityMapper;
	private final PromoterMapper promoterMapper;

	public PromoterIdentitySaveService(
			PromoterIdentityMapper promoterIdentityMapper, PromoterMapper promoterMapper) {
		this.promoterIdentityMapper = promoterIdentityMapper;
		this.promoterMapper = promoterMapper;
	}

	public void deletePromoteridentity(long companyId, long id) {
		long occupied = promoterMapper.selectCount(new LambdaQueryWrapper<Promoter>()
				.eq(Promoter::getCompanyId, companyId)
				.eq(Promoter::getIdentityId, id));
		if (occupied > 0L) {
			throw new ResourceException("不能删除");
		}
		promoterIdentityMapper.delete(new LambdaQueryWrapper<PromoterIdentity>()
				.eq(PromoterIdentity::getId, id)
				.eq(PromoterIdentity::getCompanyId, companyId));
	}

	public void defaultPromoteridentity(long companyId, long id) {
		PromoterIdentity row = promoterIdentityMapper.selectOne(new LambdaQueryWrapper<PromoterIdentity>()
				.eq(PromoterIdentity::getCompanyId, companyId)
				.eq(PromoterIdentity::getId, id)
				.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("未查询到信息");
		}
		if (Objects.equals(row.getIsSubordinates(), Integer.valueOf(1))) {
			throw new ResourceException("不能设置默认");
		}
		promoterIdentityMapper.update(null, new LambdaUpdateWrapper<PromoterIdentity>()
				.eq(PromoterIdentity::getCompanyId, companyId)
				.set(PromoterIdentity::getIsDefault, 0));
		promoterIdentityMapper.update(null, new LambdaUpdateWrapper<PromoterIdentity>()
				.eq(PromoterIdentity::getCompanyId, companyId)
				.eq(PromoterIdentity::getId, id)
				.set(PromoterIdentity::getIsDefault, 1));
	}

	public void savePromoteridentity(long companyId, String idRaw, String name, String isSubordinatesRaw) {
		boolean createBranch =
				idRaw == null || !StringUtils.hasText(idRaw.trim());
		long idParsed = 0L;
		if (!createBranch) {
			String trimmed = idRaw.trim();
			try {
				idParsed = Long.parseLong(trimmed);
			} catch (NumberFormatException e) {
				throw new BadRequestException("id 格式错误");
			}
			if (idParsed <= 0L) {
				createBranch = true;
			}
		}
		if (!createBranch) {
			updateBranch(companyId, idParsed, name);
			return;
		}
		insertBranch(companyId, name, isSubordinatesRaw);
	}

	private void updateBranch(long companyId, long idParsed, String name) {
		PromoterIdentity row = promoterIdentityMapper.selectOne(new LambdaQueryWrapper<PromoterIdentity>()
				.eq(PromoterIdentity::getId, idParsed)
				.eq(PromoterIdentity::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		int now = (int) Instant.now().getEpochSecond();
		promoterIdentityMapper.update(null, new LambdaUpdateWrapper<PromoterIdentity>()
				.eq(PromoterIdentity::getId, idParsed)
				.eq(PromoterIdentity::getCompanyId, companyId)
				.set(PromoterIdentity::getName, name)
				.set(PromoterIdentity::getUpdated, now));
	}

	private void insertBranch(long companyId, String name, String isSubordinatesRaw) {
		int subCmp;
		int isSubordinatesForInsert;
		if (isSubordinatesRaw == null || !StringUtils.hasText(isSubordinatesRaw.trim())) {
			subCmp = 0;
			isSubordinatesForInsert = 0;
		} else {
			try {
				subCmp = Integer.parseInt(isSubordinatesRaw.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("is_subordinates 格式错误");
			}
			isSubordinatesForInsert = subCmp;
		}
		int isDefault = 0;
		if (subCmp == 0) {
			long cnt = promoterIdentityMapper.selectCount(new LambdaQueryWrapper<PromoterIdentity>()
					.eq(PromoterIdentity::getCompanyId, companyId)
					.eq(PromoterIdentity::getIsDefault, 1)
					.eq(PromoterIdentity::getIsSubordinates, 0));
			if (cnt == 0) {
				isDefault = 1;
			}
		}
		PromoterIdentity ins = new PromoterIdentity();
		ins.setCompanyId(companyId);
		ins.setName(name);
		ins.setIsSubordinates(isSubordinatesForInsert);
		ins.setIsDefault(isDefault);
		int now = (int) Instant.now().getEpochSecond();
		ins.setCreated(now);
		ins.setUpdated(now);
		promoterIdentityMapper.insert(ins);
	}
}
