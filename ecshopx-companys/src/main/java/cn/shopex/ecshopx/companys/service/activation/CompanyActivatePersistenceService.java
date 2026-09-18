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

package cn.shopex.ecshopx.companys.service.activation;

import cn.shopex.ecshopx.companys.domain.ActivateLog;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.domain.Resources;
import cn.shopex.ecshopx.companys.mapper.ActivateLogMapper;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.mapper.ResourcesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CompanyActivatePersistenceService {

	private final ResourcesMapper resourcesMapper;
	private final ActivateLogMapper activateLogMapper;
	private final CompanysMapper companysMapper;

	public CompanyActivatePersistenceService(
			ResourcesMapper resourcesMapper,
			ActivateLogMapper activateLogMapper,
			CompanysMapper companysMapper) {
		this.resourcesMapper = resourcesMapper;
		this.activateLogMapper = activateLogMapper;
		this.companysMapper = companysMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> activateAfterLicense(Map<String, Object> params) {
		long companyId = toLong(params.get("company_id"));
		Resources entity = new Resources();
		entity.setCompanyId(companyId);
		entity.setEid(str(params.get("eid")));
		entity.setPassportUid(str(params.get("passport_uid")));
		entity.setResourceName(str(params.get("resource_name")));
		entity.setShopNum(toIntObject(params.get("shop_num")));
		entity.setLeftShopNum(toIntObject(params.get("left_shop_num")));
		entity.setSource(str(params.get("source")));
		entity.setAvailableDays(toIntObject(params.get("available_days")));
		entity.setActiveAt(toLongObject(params.get("active_at")));
		entity.setExpiredAt(toLongObject(params.get("expired_at")));
		entity.setActiveCode(str(params.get("active_code")));
		entity.setIssueId(str(params.get("issue_id")));
		entity.setGoodsCode(str(params.get("goods_code")));
		entity.setProductCode(str(params.get("product_code")));

		resourcesMapper.insert(entity);

		String encActive = str(params.get("active_code"));
		String source = str(params.get("source"));
		if (StringUtils.hasText(encActive) && !"demo".equals(source)) {
			ActivateLog logRow = new ActivateLog();
			logRow.setResourceId(entity.getResourceId());
			logRow.setCompanyId(companyId);
			logRow.setEid(str(params.get("eid")));
			logRow.setPassportUid(str(params.get("passport_uid")));
			logRow.setActiveCode(encActive);
			logRow.setActiveType(str(params.get("active_type")));
			logRow.setActiveStatus(str(params.get("active_status")));
			logRow.setActiveAt(toLongObject(params.get("active_at")));
			logRow.setExpiredAt(toLongObject(params.get("expired_at")));
			activateLogMapper.insert(logRow);
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		LambdaQueryWrapper<Resources> q = new LambdaQueryWrapper<>();
		q.eq(Resources::getCompanyId, companyId)
				.gt(Resources::getExpiredAt, nowSec)
				.orderByDesc(Resources::getExpiredAt)
				.last("LIMIT 1");
		Resources latest = resourcesMapper.selectOne(q);
		if (latest != null && latest.getExpiredAt() != null) {
			LambdaUpdateWrapper<Companys> u = new LambdaUpdateWrapper<>();
			u.eq(Companys::getCompanyId, companyId).set(Companys::getExpiredAt, latest.getExpiredAt());
			companysMapper.update(null, u);
		}

		return resourceRowToSnakeMap(entity);
	}

	private static Map<String, Object> resourceRowToSnakeMap(Resources r) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("resource_id", r.getResourceId());
		m.put("resource_name", r.getResourceName());
		m.put("company_id", r.getCompanyId());
		m.put("eid", r.getEid());
		m.put("passport_uid", r.getPassportUid());
		m.put("shop_num", r.getShopNum());
		m.put("left_shop_num", r.getLeftShopNum());
		m.put("source", r.getSource());
		m.put("available_days", r.getAvailableDays());
		m.put("active_at", r.getActiveAt());
		m.put("expired_at", r.getExpiredAt());
		m.put("active_code", r.getActiveCode());
		m.put("issue_id", r.getIssueId());
		m.put("goods_code", r.getGoodsCode());
		m.put("product_code", r.getProductCode());
		return m;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static Long toLongObject(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		return Long.parseLong(s);
	}

	private static Integer toIntObject(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		return Integer.parseInt(s);
	}
}
