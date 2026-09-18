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

package cn.shopex.ecshopx.orders.service.rights.export.consume;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.orders.domain.RightsLog;
import cn.shopex.ecshopx.orders.mapper.RightsLogMapper;
import cn.shopex.ecshopx.orders.service.admin.RightsLogsListService;
import cn.shopex.ecshopx.orders.service.admin.support.RightsLogsQueryParseSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RightsConsumeLogExportService {

	private final RightsLogMapper rightsLogMapper;
	private final RightsLogsListService rightsLogsListService;
	private final RightsConsumeLogExportFileJobHandler rightsConsumeLogExportFileJobHandler;
	private final RightsConsumeLogExportAsyncExecutor rightsConsumeLogExportAsyncExecutor;
	private final Environment environment;

	public RightsConsumeLogExportService(
			RightsLogMapper rightsLogMapper,
			RightsLogsListService rightsLogsListService,
			RightsConsumeLogExportFileJobHandler rightsConsumeLogExportFileJobHandler,
			RightsConsumeLogExportAsyncExecutor rightsConsumeLogExportAsyncExecutor,
			Environment environment) {
		this.rightsLogMapper = rightsLogMapper;
		this.rightsLogsListService = rightsLogsListService;
		this.rightsConsumeLogExportFileJobHandler = rightsConsumeLogExportFileJobHandler;
		this.rightsConsumeLogExportAsyncExecutor = rightsConsumeLogExportAsyncExecutor;
		this.environment = environment;
	}

	public void exportRightConsumeData(long companyId, long operatorId, HttpServletRequest request) {
		String mobile = request.getParameter("mobile");
		String name = request.getParameter("name");
		String shopId = request.getParameter("shop_id");
		String tsb = request.getParameter("time_start_begin");
		String tse = request.getParameter("time_start_end");

		LambdaQueryWrapper<RightsLog> w = new LambdaQueryWrapper<>();
		rightsLogsListService.applyCommonRightsLogFilters(companyId, mobile, name, shopId, tsb, tse, w);

		long count = rightsLogMapper.selectCount(w);
		if (count <= 0) {
			throw new ResourceException("导出有误,暂无数据导出");
		}

		LinkedHashMap<String, Object> filterForJob = new LinkedHashMap<>();
		String datapassBlock = request.getParameter("x-datapass-block");
		if (!StringUtils.hasText(datapassBlock)) {
			String h = request.getHeader("x-datapass-block");
			datapassBlock = h != null ? h : "";
		}
		filterForJob.put("datapass_block", datapassBlock);

		int mobileInt = RightsLogsQueryParseSupport.parseIntLooseAsInt(mobile);
		if (mobileInt != 0) {
			filterForJob.put(
					"salesperson_mobile", LegacyFixedMobileEncrypt.fixedEncryptMobile(String.valueOf(mobileInt)));
		}

		int nameInt = RightsLogsQueryParseSupport.parseIntLooseAsInt(name);
		if (nameInt != 0) {
			filterForJob.put("attendant", String.valueOf(nameInt));
		}

		int shopIdInt = RightsLogsQueryParseSupport.parseIntLooseAsInt(shopId);
		if (shopIdInt != 0) {
			filterForJob.put("shop_id", String.valueOf(shopIdInt));
		}

		if (RightsLogsQueryParseSupport.hasTimeToken(tsb)) {
			filterForJob.put("time_start_begin", tsb.trim());
		}
		if (RightsLogsQueryParseSupport.hasTimeToken(tse)) {
			filterForJob.put("time_start_end", tse.trim());
		}

		RightsConsumeLogJobContext ctx = new RightsConsumeLogJobContext(companyId, operatorId, filterForJob);
		if (environment.acceptsProfiles(Profiles.of("local"))) {
			rightsConsumeLogExportFileJobHandler.run(ctx);
		} else {
			rightsConsumeLogExportAsyncExecutor.executeAsync(ctx);
		}
	}
}
