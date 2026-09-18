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

package cn.shopex.ecshopx.youshu.service;

import cn.shopex.ecshopx.common.cron.wechat.WxappDataCubeVisitDistributionPort;
import cn.shopex.ecshopx.common.cron.wechat.WxappDataCubeVisitPagePort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddOrderSumPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddWxappVisitDistributionPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuAnalysisAddWxappVisitPagePort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuDataSourceApiPort;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOpenApiCredentials;
import cn.shopex.ecshopx.common.cron.youshu.YoushuOrderSumRow;
import cn.shopex.ecshopx.youshu.domain.YoushuSetting;
import cn.shopex.ecshopx.youshu.mapper.YoushuSettingMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class YoushuTaskService {

	private static final String TEMPLATE_NAME_YYKWEISHOP = "yykweishop";
	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
	private static final int DATA_SOURCE_TYPE_WXAPP_PAGE = 0;
	/** 与 PHP data_source_type=0、订单/下单口径一致。 */
	private static final int DATA_SOURCE_TYPE_ORDER = 0;
	private static final int DATA_SOURCE_TYPE_WXAPP_VISIT_DISTRIBUTION = 8;

	private final YoushuSettingMapper youshuSettingMapper;
	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final WxappDataCubeVisitPagePort visitPagePort;
	private final WxappDataCubeVisitDistributionPort visitDistributionPort;
	private final YoushuDataSourceApiPort youshuDataSourceApiPort;
	private final YoushuAnalysisAddWxappVisitPagePort youshuAnalysisAddWxappVisitPagePort;
	private final YoushuAnalysisAddWxappVisitDistributionPort youshuAnalysisAddWxappVisitDistributionPort;
	private final NormalOrdersMapper normalOrdersMapper;
	private final TradeMapper tradeMapper;
	private final YoushuAnalysisAddOrderSumPort youshuAnalysisAddOrderSumPort;
	private final Clock clock;

	public YoushuTaskService(
			YoushuSettingMapper youshuSettingMapper,
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			WxappDataCubeVisitPagePort visitPagePort,
			WxappDataCubeVisitDistributionPort visitDistributionPort,
			YoushuDataSourceApiPort youshuDataSourceApiPort,
			YoushuAnalysisAddWxappVisitPagePort youshuAnalysisAddWxappVisitPagePort,
			YoushuAnalysisAddWxappVisitDistributionPort youshuAnalysisAddWxappVisitDistributionPort,
			NormalOrdersMapper normalOrdersMapper,
			TradeMapper tradeMapper,
			YoushuAnalysisAddOrderSumPort youshuAnalysisAddOrderSumPort,
			@Autowired(required = false) @Nullable Clock clock) {
		this.youshuSettingMapper = youshuSettingMapper;
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.visitPagePort = visitPagePort;
		this.visitDistributionPort = visitDistributionPort;
		this.youshuDataSourceApiPort = youshuDataSourceApiPort;
		this.youshuAnalysisAddWxappVisitPagePort = youshuAnalysisAddWxappVisitPagePort;
		this.youshuAnalysisAddWxappVisitDistributionPort = youshuAnalysisAddWxappVisitDistributionPort;
		this.normalOrdersMapper = normalOrdersMapper;
		this.tradeMapper = tradeMapper;
		this.youshuAnalysisAddOrderSumPort = youshuAnalysisAddOrderSumPort;
		this.clock = clock != null ? clock : Clock.systemDefaultZone();
	}

	public void scheduleAddWxappVisitPage() {
		List<YoushuSetting> list = youshuSettingMapper.selectList(new QueryWrapper<>());
		if (list == null || list.isEmpty()) {
			return;
		}
		for (YoushuSetting row : list) {
			try {
				runOneRow(row);
			} catch (Exception e) {
				log.debug("addWxappVisitPage_error: {}", e.getMessage());
			}
		}
	}

	private void runOneRow(YoushuSetting row) {
		long companyId = row.getCompanyId();
		String wxaAppId = weappAuthorizerAppidRepository
				.findAuthorizerAppid(companyId, TEMPLATE_NAME_YYKWEISHOP)
				.orElse(null);
		if (wxaAppId == null || wxaAppId.isBlank()) {
			return;
		}
		LocalDate yesterday = LocalDate.now(clock).minusDays(1);
		String ymd = yesterday.format(YMD);
		JsonNode visitData = visitPagePort.postVisitPage(wxaAppId.trim(), ymd, ymd);
		YoushuOpenApiCredentials creds = toCredentials(row);
		String merchantId = row.getMerchantId() == null ? "" : row.getMerchantId();
		String dataSourceId =
				youshuDataSourceApiPort.getOrCreateDataSourceId(merchantId, DATA_SOURCE_TYPE_WXAPP_PAGE, creds);
		youshuAnalysisAddWxappVisitPagePort.addWxappVisitPage(dataSourceId, visitData, creds);
	}

	public void scheduleAddWxappVisitDistribution() {
		List<YoushuSetting> list = youshuSettingMapper.selectList(new QueryWrapper<>());
		if (list == null || list.isEmpty()) {
			return;
		}
		for (YoushuSetting row : list) {
			try {
				runOneRowVisitDistribution(row);
			} catch (Exception e) {
				log.debug("addWxappVisitDistribution_error: {}", e.getMessage());
			}
		}
	}

	private void runOneRowVisitDistribution(YoushuSetting row) {
		long companyId = row.getCompanyId();
		String wxaAppId = weappAuthorizerAppidRepository
				.findAuthorizerAppid(companyId, TEMPLATE_NAME_YYKWEISHOP)
				.orElse(null);
		if (wxaAppId == null || wxaAppId.isBlank()) {
			return;
		}
		LocalDate yesterday = LocalDate.now(clock).minusDays(1);
		String ymd = yesterday.format(YMD);
		JsonNode visitData = visitDistributionPort.postVisitDistribution(wxaAppId.trim(), ymd, ymd);
		YoushuOpenApiCredentials creds = toCredentials(row);
		String merchantId = row.getMerchantId() == null ? "" : row.getMerchantId();
		String dataSourceId =
				youshuDataSourceApiPort.getOrCreateDataSourceId(
						merchantId, DATA_SOURCE_TYPE_WXAPP_VISIT_DISTRIBUTION, creds);
		youshuAnalysisAddWxappVisitDistributionPort.addWxappVisitDistribution(dataSourceId, visitData, creds);
	}

	public void scheduleAddOrderSum() {
		List<YoushuSetting> list = youshuSettingMapper.selectList(new QueryWrapper<>());
		if (list == null || list.isEmpty()) {
			return;
		}
		ZoneId zone = clock.getZone();
		LocalDate today = LocalDate.now(clock);
		LocalDate yesterday = today.minusDays(1);
		long startTimeSec = yesterday.atStartOfDay(zone).toEpochSecond();
		long endTimeSec = today.atStartOfDay(zone).toEpochSecond();
		String refDate = startTimeSec + "000";
		for (YoushuSetting row : list) {
			try {
				runOneRowAddOrderSum(row, startTimeSec, endTimeSec, refDate);
			} catch (Exception e) {
				log.debug("addOrderSum_error: {}", e.getMessage());
			}
		}
	}

	private void runOneRowAddOrderSum(YoushuSetting row, long startTimeSec, long endTimeSec, String refDate) {
		long companyId = row.getCompanyId();
		Long orderSumFen = normalOrdersMapper.sumOrderTotalFeeCentsForYoushu(companyId, startTimeSec, endTimeSec);
		long orderCount = normalOrdersMapper.countOrdersForYoushu(companyId, startTimeSec, endTimeSec);
		Long paySumFen = tradeMapper.sumTradeTotalFeeCentsForYoushu(companyId, startTimeSec, endTimeSec);
		long payCount = tradeMapper.countTradesForYoushu(companyId, startTimeSec, endTimeSec);
		double giveOrderAmountSum = fenToYuan(orderSumFen);
		double paymentAmountSum = fenToYuan(paySumFen);
		YoushuOrderSumRow sumRow =
				new YoushuOrderSumRow(
						refDate, giveOrderAmountSum, orderCount, paymentAmountSum, payCount);
		YoushuOpenApiCredentials creds = toCredentials(row);
		String merchantId = row.getMerchantId() == null ? "" : row.getMerchantId();
		String dataSourceId =
				youshuDataSourceApiPort.getOrCreateDataSourceId(merchantId, DATA_SOURCE_TYPE_ORDER, creds);
		youshuAnalysisAddOrderSumPort.addOrderSum(dataSourceId, sumRow, creds);
	}

	private static double fenToYuan(Long fen) {
		if (fen == null) {
			return 0.0;
		}
		return BigDecimal.valueOf(fen)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.doubleValue();
	}

	private static YoushuOpenApiCredentials toCredentials(YoushuSetting v) {
		String base = firstNonBlank(v.getApiUrl(), v.getSandboxApiUrl());
		String appId = firstNonBlank(v.getAppId(), v.getSandboxAppId());
		String appSecret = firstNonBlank(v.getAppSecret(), v.getSandboxAppSecret());
		return new YoushuOpenApiCredentials(base, appId, appSecret);
	}

	private static String firstNonBlank(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) {
			return primary;
		}
		if (fallback != null && !fallback.isBlank()) {
			return fallback;
		}
		return "";
	}
}
