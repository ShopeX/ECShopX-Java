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

package cn.shopex.ecshopx.workwechat.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.workwechat.wxjava.WorkWechatWxCpRuntime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpOauth2UserInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WorkWechatWebOAuthService {

	private final WorkWechatWxCpRuntime workWechatWxCpRuntime;

	public WorkWechatWebOAuthService(WorkWechatWxCpRuntime workWechatWxCpRuntime) {
		this.workWechatWxCpRuntime = workWechatWxCpRuntime;
	}

	public String resolveWorkUserIdFromCode(Map<String, Object> workWechatConfig, String code) {
		String trimmedCode = code == null ? "" : code.trim();
		if (!StringUtils.hasText(trimmedCode)) {
			log.warn("work wechat oauth getuserinfo missing code");
			throw new ResourceException("该账号不在企业通讯录中");
		}
		String corpid = trimToEmpty(workWechatConfig.get("corpid"));
		String corpsecret = extractDianwuSecret(workWechatConfig);
		WxCpService cp = workWechatWxCpRuntime.cpDirect(corpid, corpsecret);
		try {
			cp.getAccessToken(false);
			WxCpOauth2UserInfo info = cp.getOauth2Service().getUserInfo(trimmedCode);
			String userId = info == null ? "" : info.getUserId();
			if (!StringUtils.hasText(userId)) {
				log.warn("work wechat getuserinfo empty UserId");
				throw new ResourceException("该账号不在企业通讯录中");
			}
			return userId;
		} catch (WxErrorException e) {
			log.warn(
					"work wechat oauth wx err errcode={} errmsg={}",
					e.getError() != null ? e.getError().getErrorCode() : null,
					e.getError() != null ? e.getError().getErrorMsg() : e.getMessage());
			throw new ResourceException("该账号不在企业通讯录中");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("work wechat oauth request failed", e);
			throw new ResourceException("该账号不在企业通讯录中");
		}
	}

	private static String extractDianwuSecret(Map<String, Object> workWechatConfig) {
		Object agentsObj = workWechatConfig.get("agents");
		if (!(agentsObj instanceof Map<?, ?> agents)) {
			return "";
		}
		Object dianwuObj = agents.get("dianwu");
		if (!(dianwuObj instanceof Map<?, ?> dianwu)) {
			return "";
		}
		Object secretObj = dianwu.get("secret");
		return secretObj != null ? String.valueOf(secretObj).trim() : "";
	}

	private static String trimToEmpty(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}
}
