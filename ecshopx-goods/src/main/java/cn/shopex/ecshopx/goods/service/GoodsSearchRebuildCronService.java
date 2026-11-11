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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.dispatch.SearchRebuildJobDispatchPublisher;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** XXL 定时：按有效公司逐个入队搜索索引全量 upsert。 */
@Service
public class GoodsSearchRebuildCronService {

	private static final Logger log = LoggerFactory.getLogger(GoodsSearchRebuildCronService.class);

	private final CompanysMapper companysMapper;
	private final SearchRebuildJobDispatchPublisher searchRebuildJobDispatchPublisher;
	private final SearchEsProperties searchEsProperties;

	public GoodsSearchRebuildCronService(
			CompanysMapper companysMapper,
			SearchRebuildJobDispatchPublisher searchRebuildJobDispatchPublisher,
			SearchEsProperties searchEsProperties) {
		this.companysMapper = companysMapper;
		this.searchRebuildJobDispatchPublisher = searchRebuildJobDispatchPublisher;
		this.searchEsProperties = searchEsProperties;
	}

	/** @return 入队的公司数量；ES 未启用时返回 0 且不入队 */
	public int enqueueRebuildForActiveCompanies() {
		if (!searchEsProperties.isEnabled()) {
			log.info("skip goods search rebuild cron: ecshopx.search.es.enabled=false");
			return 0;
		}
		List<Long> companyIds = listActiveCompanyIds();
		for (Long companyId : companyIds) {
			searchRebuildJobDispatchPublisher.enqueueCompany(companyId);
		}
		log.info("enqueued goods search rebuild for {} companies", companyIds.size());
		return companyIds.size();
	}

	List<Long> listActiveCompanyIds() {
		long nowSec = System.currentTimeMillis() / 1000L;
		LambdaQueryWrapper<Companys> w = new LambdaQueryWrapper<>();
		w.select(Companys::getCompanyId)
				.and(q -> q.isNull(Companys::getIsDisabled).or().eq(Companys::getIsDisabled, false))
				.and(q -> q.isNull(Companys::getExpiredAt).or().gt(Companys::getExpiredAt, nowSec))
				.orderByAsc(Companys::getCompanyId);
		List<Companys> rows = companysMapper.selectList(w);
		List<Long> ids = new ArrayList<>();
		if (rows != null) {
			for (Companys row : rows) {
				if (row.getCompanyId() != null && row.getCompanyId() > 0L) {
					ids.add(row.getCompanyId());
				}
			}
		}
		return ids;
	}
}
