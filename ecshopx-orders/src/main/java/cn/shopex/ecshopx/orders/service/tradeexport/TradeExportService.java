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

package cn.shopex.ecshopx.orders.service.tradeexport;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.tradeexport.support.TradeExportQuerySupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Service
public class TradeExportService {

	private final TradeExportFilterAssembler tradeExportFilterAssembler;
	private final TradeMapper tradeMapper;
	private final TradeExportFileJobHandler tradeExportFileJobHandler;
	private final TradeExportAsyncExecutor tradeExportAsyncExecutor;
	private final Environment environment;

	public TradeExportService(
			TradeExportFilterAssembler tradeExportFilterAssembler,
			TradeMapper tradeMapper,
			TradeExportFileJobHandler tradeExportFileJobHandler,
			TradeExportAsyncExecutor tradeExportAsyncExecutor,
			Environment environment) {
		this.tradeExportFilterAssembler = tradeExportFilterAssembler;
		this.tradeMapper = tradeMapper;
		this.tradeExportFileJobHandler = tradeExportFileJobHandler;
		this.tradeExportAsyncExecutor = tradeExportAsyncExecutor;
		this.environment = environment;
	}

	public void exportTradeData(
			long companyId,
			long operatorId,
			String operatorType,
			Long merchantIdOrNull,
			List<Long> distributorIdsFromJwt,
			List<Long> shopIdsFromJwt,
			HttpServletRequest request) {
		LinkedHashMap<String, Object> filter =
				tradeExportFilterAssembler.assemble(
						companyId, operatorType, merchantIdOrNull, distributorIdsFromJwt, shopIdsFromJwt, request);
		filter.put("company_id", Long.valueOf(companyId));
		long count = tradeMapper.selectCount(TradeExportQuerySupport.toCountWrapper(companyId, filter));
		if (count <= 0L) {
			throw new ResourceException("导出有误,暂无数据导出");
		}
		TradeExportJobContext ctx =
				new TradeExportJobContext(companyId, operatorId, "tradedata", new LinkedHashMap<>(filter));
		if (environment.acceptsProfiles(Profiles.of("local"))) {
			tradeExportFileJobHandler.run(ctx);
		} else {
			tradeExportAsyncExecutor.executeAsync(ctx);
		}
	}
}
