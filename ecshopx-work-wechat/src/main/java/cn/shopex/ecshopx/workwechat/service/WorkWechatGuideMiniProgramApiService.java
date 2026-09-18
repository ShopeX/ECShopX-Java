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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.thirdparty.service.workwechat.WorkWechatAccessTokenProvider;
import cn.shopex.ecshopx.workwechat.wxjava.WorkWechatWxCpRuntime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.WxCpMaJsCode2SessionResult;
import me.chanjar.weixin.cp.bean.external.WxCpContactWayInfo;
import me.chanjar.weixin.cp.bean.external.WxCpContactWayResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WorkWechatGuideMiniProgramApiService {

	private final WorkWechatAccessTokenProvider workWechatAccessTokenProvider;
	private final WorkWechatWxCpRuntime workWechatWxCpRuntime;

	public WorkWechatGuideMiniProgramApiService(
			WorkWechatAccessTokenProvider workWechatAccessTokenProvider,
			WorkWechatWxCpRuntime workWechatWxCpRuntime) {
		this.workWechatAccessTokenProvider = workWechatAccessTokenProvider;
		this.workWechatWxCpRuntime = workWechatWxCpRuntime;
	}

	public Map<String, Object> miniprogramJscode2session(long companyId, String jsCode) {
		String token = workWechatAccessTokenProvider
				.getAccessToken(companyId)
				.filter(StringUtils::hasText)
				.orElseThrow(() -> new BadRequestException("导购登陆失败！"));
		String code = jsCode == null ? "" : jsCode.trim();
		if (!StringUtils.hasText(code)) {
			throw new BadRequestException("导购登陆失败！");
		}
		WxCpService cp = workWechatWxCpRuntime.cpBearer(token);
		try {
			WxCpMaJsCode2SessionResult r = cp.jsCode2Session(code);
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("userid", r.getUserId());
			map.put("session_key", r.getSessionKey());
			if (StringUtils.hasText(r.getCorpId())) {
				map.put("corpid", r.getCorpId());
			}
			return map;
		} catch (WxErrorException e) {
			log.warn(
					"work wechat jscode2session wx err companyId={} errcode={}",
					companyId,
					e.getError() != null ? e.getError().getErrorCode() : null);
			throw new BadRequestException("导购登陆解析失败！");
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			log.warn("work wechat jscode2session request failed companyId={}", companyId, e);
			throw new BadRequestException("导购登陆失败！");
		}
	}

	public String addContactWay(long companyId, int scene, int style, String plainUserid) {
		String token = workWechatAccessTokenProvider
				.getAccessToken(companyId)
				.filter(StringUtils::hasText)
				.orElseThrow(() -> new ResourceException("企业微信 access_token 不可用，请检查配置"));
		String uid = plainUserid == null ? "" : plainUserid.trim();
		if (!StringUtils.hasText(uid)) {
			throw new ResourceException("企业微信接口调用失败：userid 为空");
		}
		WxCpService cp = workWechatWxCpRuntime.cpBearer(token);
		WxCpContactWayInfo wrap = new WxCpContactWayInfo();
		WxCpContactWayInfo.ContactWay cw = new WxCpContactWayInfo.ContactWay();
		cw.setType(WxCpContactWayInfo.TYPE.SINGLE);
		cw.setScene(scene == 2 ? WxCpContactWayInfo.SCENE.QRCODE : WxCpContactWayInfo.SCENE.MINIPROGRAM);
		cw.setStyle(style);
		cw.setSkipVerify(true);
		cw.setUsers(java.util.List.of(uid));
		wrap.setContactWay(cw);
		try {
			WxCpContactWayResult resp = cp.getExternalContactService().addContactWay(wrap);
			if (resp == null || !resp.success()) {
				long errcode = resp != null && resp.getErrcode() != null ? resp.getErrcode() : -1L;
				String errmsg = resp != null ? resp.getErrmsg() : "";
				log.warn("work wechat add_contact_way errcode={} errmsg={}", errcode, errmsg);
				throw new ResourceException("企业微信错误: errcode=" + errcode + ", errmsg=" + errmsg);
			}
			String configId = resp.getConfigId() == null ? "" : resp.getConfigId().trim();
			if (!StringUtils.hasText(configId)) {
				throw new ResourceException("企业微信接口调用失败");
			}
			return configId;
		} catch (WxErrorException e) {
			log.warn(
					"work wechat add_contact_way wx err companyId={} errcode={}",
					companyId,
					e.getError() != null ? e.getError().getErrorCode() : null);
			throw new ResourceException("企业微信接口调用失败");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("work wechat add_contact_way request failed companyId={}", companyId, e);
			throw new ResourceException("企业微信接口调用失败");
		}
	}
}
