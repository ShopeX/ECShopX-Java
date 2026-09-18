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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.Monitors;
import cn.shopex.ecshopx.datacube.mapper.MonitorsMapper;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.util.Base64;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MonitorsWxaCode64Service {

	private final MonitorsMapper monitorsMapper;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public MonitorsWxaCode64Service(
			MonitorsMapper monitorsMapper, WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.monitorsMapper = monitorsMapper;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public Map<String, Object> buildBase64Image(long monitorId, String monitorIdForScene, String sourceIdForScene) {
		Monitors row = monitorsMapper.selectById(monitorId);
		if (row == null) {
			throw new ResourceException("monitor_id=" + monitorId + "的跟踪链接不存在");
		}
		String wxappid = row.getWxappid() == null ? "" : row.getWxappid();
		String page = row.getMonitorPath();
		if (page == null) {
			page = "";
		}
		String params = row.getMonitorPathParams() == null ? "" : row.getMonitorPathParams();
		String paramsStr = params + "&s=" + sourceIdForScene + "&m=" + monitorIdForScene;
		String scene = trimLeadingTrailingAmpersands(paramsStr);
		byte[] jpeg = wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxappid, scene, page);
		String b64 = Base64.getEncoder().encodeToString(jpeg);
		return Map.of("base64Image", "data:image/jpg;base64," + b64);
	}

	public byte[] buildJpegBytesWithWidth1280(long monitorId, String monitorIdForScene, String sourceIdForScene) {
		Monitors row = monitorsMapper.selectById(monitorId);
		if (row == null) {
			throw new ResourceException("monitor_id=" + monitorId + "的跟踪链接不存在");
		}
		String wxappid = row.getWxappid() == null ? "" : row.getWxappid();
		String page = row.getMonitorPath();
		if (page == null) {
			page = "";
		}
		String params = row.getMonitorPathParams() == null ? "" : row.getMonitorPathParams();
		String paramsStr = params + "&s=" + sourceIdForScene + "&m=" + monitorIdForScene;
		String scene = trimLeadingTrailingAmpersands(paramsStr);
		return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxappid, scene, page, 1280);
	}

	static String trimLeadingTrailingAmpersands(String str) {
		if (str == null || str.isEmpty()) {
			return "";
		}
		int start = 0;
		int end = str.length();
		while (start < end && str.charAt(start) == '&') {
			start++;
		}
		while (end > start && str.charAt(end - 1) == '&') {
			end--;
		}
		return str.substring(start, end);
	}
}
