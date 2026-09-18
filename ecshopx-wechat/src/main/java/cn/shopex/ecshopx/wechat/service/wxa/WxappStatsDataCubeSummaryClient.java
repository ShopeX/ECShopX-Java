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

package cn.shopex.ecshopx.wechat.service.wxa;

import static cn.binarywang.wx.miniapp.constant.WxMaApiUrlConstants.Analysis.GET_DAILY_SUMMARY_TREND_URL;
import static cn.binarywang.wx.miniapp.constant.WxMaApiUrlConstants.Analysis.GET_DAILY_VISIT_TREND_URL;
import static cn.binarywang.wx.miniapp.constant.WxMaApiUrlConstants.Analysis.GET_MONTHLY_VISIT_TREND_URL;
import static cn.binarywang.wx.miniapp.constant.WxMaApiUrlConstants.Analysis.GET_WEEKLY_VISIT_TREND_URL;

import cn.shopex.ecshopx.wechat.wxjava.WxMaDataCubePosts;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

@Service
public class WxappStatsDataCubeSummaryClient {

	private final WxMaDataCubePosts posts;

	public WxappStatsDataCubeSummaryClient(WxMaDataCubePosts posts) {
		this.posts = posts;
	}

	public JsonNode postDailySummaryTrend(String wxaAppId, String beginYmd, String endYmd) {
		return posts.postDateRange(wxaAppId, GET_DAILY_SUMMARY_TREND_URL, beginYmd, endYmd);
	}

	public JsonNode postDailyVisitTrend(String wxaAppId, String beginYmd, String endYmd) {
		return posts.postDateRange(wxaAppId, GET_DAILY_VISIT_TREND_URL, beginYmd, endYmd);
	}

	public JsonNode postWeeklyVisitTrend(String wxaAppId, String beginYmd, String endYmd) {
		return posts.postDateRange(wxaAppId, GET_WEEKLY_VISIT_TREND_URL, beginYmd, endYmd);
	}

	public JsonNode postMonthlyVisitTrend(String wxaAppId, String beginYmd, String endYmd) {
		return posts.postDateRange(wxaAppId, GET_MONTHLY_VISIT_TREND_URL, beginYmd, endYmd);
	}
}
