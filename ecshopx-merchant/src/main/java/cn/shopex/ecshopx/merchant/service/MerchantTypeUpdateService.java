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
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MerchantTypeUpdateService {

	private final MerchantTypeMapper merchantTypeMapper;
	private final MerchantTypeOutsideLangWriteService merchantTypeOutsideLangWriteService;

	public MerchantTypeUpdateService(
			MerchantTypeMapper merchantTypeMapper,
			MerchantTypeOutsideLangWriteService merchantTypeOutsideLangWriteService) {
		this.merchantTypeMapper = merchantTypeMapper;
		this.merchantTypeOutsideLangWriteService = merchantTypeOutsideLangWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateMerchantType(long companyId, long typeId, Map<String, Object> params, String acceptLanguage) {
		try {
			validateOptionalName(params);
			validateOptionalSort(params);
			validateOptionalParentId(params);

			MerchantType loaded = merchantTypeMapper.selectOne(
					new LambdaQueryWrapper<MerchantType>()
							.eq(MerchantType::getId, typeId)
							.eq(MerchantType::getCompanyId, companyId));
			if (loaded == null) {
				throw new ResourceException("数据不存在");
			}

			Object rawShow = params.get("is_show");
			boolean normalizedIsShow;
			if (rawShow instanceof Boolean b) {
				normalizedIsShow = Boolean.TRUE.equals(b);
			} else {
				normalizedIsShow = "1".equals(MerchantCreateParamNormalizer.normalizeScalarToString(rawShow));
			}

			LambdaQueryWrapper<MerchantType> dup = new LambdaQueryWrapper<MerchantType>()
					.ne(MerchantType::getId, typeId)
					.eq(MerchantType::getCompanyId, companyId)
					.eq(MerchantType::getParentId, loaded.getParentId());
			if (!params.containsKey("name") || params.get("name") == null) {
				dup.apply("name = NULL");
			} else {
				String trimmedName = String.valueOf(params.get("name")).trim();
				dup.eq(MerchantType::getName, trimmedName);
			}
			if (merchantTypeMapper.selectCount(dup) > 0) {
				throw new ResourceException("名称已存在");
			}

			int now = (int) (System.currentTimeMillis() / 1000L);
			UpdateWrapper<MerchantType> uw = new UpdateWrapper<>();
			uw.eq("id", typeId).eq("company_id", companyId);
			uw.set("is_show", normalizedIsShow);
			uw.set("updated", now);

			String finalNameForLang = null;
			if (params.containsKey("name")) {
				String trimmed = String.valueOf(params.get("name")).trim();
				uw.set("name", trimmed);
				finalNameForLang = trimmed;
			}
			if (params.containsKey("sort")) {
				uw.set("sort", (long) parseSortValue(params.get("sort")));
			}

			int affected = merchantTypeMapper.update(null, uw);
			if (affected == 0) {
				throw new ResourceException("未查询到更新数据");
			}

			if (params.containsKey("name")) {
				merchantTypeOutsideLangWriteService.applyAfterUpdateMerchantTypeName(
						typeId, companyId, finalNameForLang, acceptLanguage);
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.error("更新商户类型失败", e);
			throw new ResourceException("更新商户类型失败");
		}
	}

	private static void validateOptionalName(Map<String, Object> params) {
		if (!params.containsKey("name")) {
			return;
		}
		Object raw = params.get("name");
		if (raw == null) {
			throw new ResourceException("名称必填且不能超过18个字符");
		}
		String name = String.valueOf(raw).trim();
		if (name.isEmpty()) {
			throw new ResourceException("名称必填且不能超过18个字符");
		}
		if (name.length() > 18) {
			throw new ResourceException("名称不能超过18个字符");
		}
	}

	private static void validateOptionalSort(Map<String, Object> params) {
		if (!params.containsKey("sort")) {
			return;
		}
		parseSortValue(params.get("sort"));
	}

	private static int parseSortValue(Object raw) {
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

	private static void validateOptionalParentId(Map<String, Object> params) {
		if (!params.containsKey("parent_id")) {
			return;
		}
		Object raw = params.get("parent_id");
		if (raw == null) {
			throw new ResourceException("父级ID必须大于等于0");
		}
		if (raw instanceof String s) {
			if (s.trim().isEmpty()) {
				throw new ResourceException("父级ID必须大于等于0");
			}
		}
		long effective;
		if (raw instanceof Number n) {
			effective = n.longValue();
		} else {
			String str = String.valueOf(raw).trim();
			try {
				effective = Long.parseLong(str);
			} catch (NumberFormatException e) {
				throw new ResourceException("父级ID必须大于等于0");
			}
		}
		if (effective < 0) {
			throw new ResourceException("父级ID必须大于等于0");
		}
	}
}
