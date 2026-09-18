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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.DistributorGeofence;
import cn.shopex.ecshopx.distribution.mapper.DistributorGeofenceMapper;
import cn.shopex.ecshopx.distribution.service.dto.DistributorGeofenceJoinRow;
import cn.shopex.ecshopx.thirdparty.service.map.amap.AmapTrackRestClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class DistributorGeofenceDeleteService {

	private final DistributorGeofenceMapper distributorGeofenceMapper;
	private final AmapTrackRestClient amapTrackRestClient;
	private final TransactionTemplate transactionTemplate;

	public DistributorGeofenceDeleteService(
			DistributorGeofenceMapper distributorGeofenceMapper,
			AmapTrackRestClient amapTrackRestClient,
			PlatformTransactionManager platformTransactionManager) {
		this.distributorGeofenceMapper = distributorGeofenceMapper;
		this.amapTrackRestClient = amapTrackRestClient;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
		this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public Map<String, Object> delete(long companyId, long distributorId, Long distributorGeofenceIdOrNull) {
		boolean filterByGeofenceId = distributorGeofenceIdOrNull != null;
		Long geofenceIdParam = distributorGeofenceIdOrNull;
		List<DistributorGeofenceJoinRow> rows =
				distributorGeofenceMapper.selectJoinList(companyId, distributorId, filterByGeofenceId, geofenceIdParam);
		if (rows == null || rows.isEmpty()) {
			return Map.of("status", Integer.valueOf(1));
		}
		for (DistributorGeofenceJoinRow row : rows) {
			final DistributorGeofenceJoinRow r = row;
			transactionTemplate.executeWithoutResult(status -> {
				Long id = r.getId();
				String gfid = r.getGeofenceId();
				String serviceSid = r.getServiceId();
				String appKey = r.getAppKey();
				String configType = r.getConfigType();

				LambdaQueryWrapper<DistributorGeofence> w = new LambdaQueryWrapper<DistributorGeofence>()
						.eq(DistributorGeofence::getId, id)
						.eq(DistributorGeofence::getCompanyId, companyId);
				int deleted = distributorGeofenceMapper.delete(w);
				if (deleted <= 0 || !StringUtils.hasText(gfid)) {
					return;
				}
				if (!"amap".equals(configType == null ? null : configType.trim())) {
					return;
				}
				boolean ok = amapTrackRestClient.deleteGeofenceByGfid(appKey, serviceSid, gfid);
				if (!ok) {
					throw new ResourceException("删除围栏失败！");
				}
			});
		}
		return Map.of("status", Integer.valueOf(1));
	}
}
