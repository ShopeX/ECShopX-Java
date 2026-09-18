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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.domain.MerchantType;
import cn.shopex.ecshopx.merchant.mapper.MerchantTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MerchantTypeCreateService {

	private final MerchantTypeMapper merchantTypeMapper;
	private final MerchantTypeOutsideLangWriteService merchantTypeOutsideLangWriteService;

	public MerchantTypeCreateService(
			MerchantTypeMapper merchantTypeMapper,
			MerchantTypeOutsideLangWriteService merchantTypeOutsideLangWriteService) {
		this.merchantTypeMapper = merchantTypeMapper;
		this.merchantTypeOutsideLangWriteService = merchantTypeOutsideLangWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createMerchantType(long companyId, Map<String, Object> params, String acceptLanguage) {
		try {
			String name = validateAndTrimName(params);
			int sort = parseSort(params.get("sort"));
			long effectiveParentId = resolveEffectiveParentId(params);

			Object rawShow = params.get("is_show");
			boolean isShow;
			if (rawShow instanceof Boolean b) {
				isShow = Boolean.TRUE.equals(b);
			} else {
				isShow = "1".equals(MerchantCreateParamNormalizer.normalizeScalarToString(rawShow));
			}

			LambdaQueryWrapper<MerchantType> dup = new LambdaQueryWrapper<MerchantType>()
					.eq(MerchantType::getCompanyId, companyId)
					.eq(MerchantType::getName, name)
					.eq(MerchantType::getParentId, effectiveParentId);
			if (merchantTypeMapper.selectCount(dup) > 0) {
				throw new ResourceException("名称已存在");
			}

			int now = (int) (System.currentTimeMillis() / 1000L);

			if (effectiveParentId == 0) {
				return insertTopLevel(companyId, name, sort, isShow, now, acceptLanguage);
			}
			return insertUnderParent(companyId, name, sort, isShow, effectiveParentId, now, acceptLanguage);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.error("创建商户类型失败", e);
			throw new ResourceException("创建商户类型失败");
		}
	}

	private static String validateAndTrimName(Map<String, Object> params) {
		Object raw = params.get("name");
		if (raw == null) {
			throw new ResourceException("名称必填且不能超过18个字符");
		}
		String name = String.valueOf(raw).trim();
		if (name.isEmpty()) {
			throw new ResourceException("名称必填且不能超过18个字符");
		}
		if (name.length() > 18) {
			throw new ResourceException("名称必填且不能超过18个字符");
		}
		return name;
	}

	private static int parseSort(Object raw) {
		if (raw == null) {
			throw new ResourceException("排序为0-999999的整数");
		}
		long v;
		if (raw instanceof Number n) {
			v = n.longValue();
		} else {
			String s = String.valueOf(raw).trim();
			if (s.isEmpty()) {
				throw new ResourceException("排序为0-999999的整数");
			}
			try {
				v = Long.parseLong(s);
			} catch (NumberFormatException e) {
				throw new ResourceException("排序为0-999999的整数");
			}
		}
		if (v < 0 || v > 999_999) {
			throw new ResourceException("排序为0-999999的整数");
		}
		return (int) v;
	}

	private static long resolveEffectiveParentId(Map<String, Object> params) {
		Object raw = params.get("parent_id");
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof String s) {
			if (s.trim().isEmpty()) {
				return 0L;
			}
		}
		long effective;
		if (raw instanceof Number n) {
			effective = n.longValue();
		} else {
			String s = String.valueOf(raw).trim();
			try {
				effective = Long.parseLong(s);
			} catch (NumberFormatException e) {
				throw new ResourceException("父级ID必须大于等于0");
			}
		}
		if (effective < 0) {
			throw new ResourceException("父级ID必须大于等于0");
		}
		return effective;
	}

	private Map<String, Object> insertTopLevel(
			long companyId,
			String name,
			int sort,
			boolean isShow,
			int now,
			String acceptLanguage) {
		MerchantType entity = new MerchantType();
		entity.setCompanyId(companyId);
		entity.setName(name);
		entity.setParentId(0L);
		entity.setSort((long) sort);
		entity.setShow(isShow);
		entity.setLevel(1);
		entity.setPath("");
		entity.setCreated(now);
		entity.setUpdated(now);
		merchantTypeMapper.insert(entity);
		Long newId = entity.getId();
		if (newId == null) {
			log.error("创建商户类型失败: insert 未返回主键");
			throw new ResourceException("创建失败");
		}
		merchantTypeOutsideLangWriteService.applyAfterInsert(newId, companyId, name, acceptLanguage);
		String pathVal = String.valueOf(newId);
		int updated = merchantTypeMapper.update(
				null,
				new LambdaUpdateWrapper<MerchantType>()
						.eq(MerchantType::getId, newId)
						.set(MerchantType::getPath, pathVal));
		if (updated == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		return Map.of("status", true);
	}

	private Map<String, Object> insertUnderParent(
			long companyId,
			String name,
			int sort,
			boolean isShow,
			long effectiveParentId,
			int now,
			String acceptLanguage) {
		LambdaQueryWrapper<MerchantType> pq = new LambdaQueryWrapper<MerchantType>()
				.eq(MerchantType::getId, effectiveParentId)
				.eq(MerchantType::getCompanyId, companyId);
		MerchantType parent = merchantTypeMapper.selectOne(pq);
		if (parent == null) {
			throw new ResourceException("父级数据错误，请检查后重新提交");
		}
		int parentLevel = parent.getLevel() == null ? 0 : parent.getLevel().intValue();
		if (parentLevel >= 2) {
			throw new ResourceException("只能添加到二级");
		}
		int newLevel;
		String pathPrefix = parent.getPath() != null ? parent.getPath() : "";
		if (parent.getParentId() == 0L) {
			newLevel = 2;
		} else {
			newLevel = parentLevel + 1;
		}
		MerchantType entity = new MerchantType();
		entity.setCompanyId(companyId);
		entity.setName(name);
		entity.setParentId(effectiveParentId);
		entity.setSort((long) sort);
		entity.setShow(isShow);
		entity.setLevel(newLevel);
		entity.setPath("");
		entity.setCreated(now);
		entity.setUpdated(now);
		merchantTypeMapper.insert(entity);
		Long newId = entity.getId();
		if (newId == null) {
			log.error("创建商户类型失败: insert 未返回主键");
			throw new ResourceException("创建失败");
		}
		merchantTypeOutsideLangWriteService.applyAfterInsert(newId, companyId, name, acceptLanguage);
		String newPath = pathPrefix + "," + newId;
		int updated = merchantTypeMapper.update(
				null,
				new LambdaUpdateWrapper<MerchantType>()
						.eq(MerchantType::getId, newId)
						.set(MerchantType::getPath, newPath));
		if (updated == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		return Map.of("status", true);
	}
}
