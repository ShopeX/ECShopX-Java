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

package cn.shopex.ecshopx.wechat.service.openplatform;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WechatOfficialAccountKfAccountClient {

	private static final Logger log = LoggerFactory.getLogger(WechatOfficialAccountKfAccountClient.class);

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final WxJavaMpRuntime wxJavaMpRuntime;

	public WechatOfficialAccountKfAccountClient(WxJavaMpRuntime wxJavaMpRuntime) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
	}

	public boolean addKfAccount(String accessToken, String kfAccount, String nickName) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("kf_account", kfAccount == null ? "" : kfAccount);
		body.put("nickname", nickName == null ? "" : nickName);
		try {
			String raw =
					wxJavaMpRuntime.mpBearer(accessToken).post(WxMpApiUrl.Kefu.KFACCOUNT_ADD, body);
			JsonNode root = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
			if (!root.hasNonNull("errcode")) {
				return true;
			}
			return root.get("errcode").asInt() == 0;
		} catch (WxErrorException e) {
			throw new IllegalStateException("微信客服接口调用失败", e);
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("解析微信客服添加接口响应失败", e);
		}
	}

	public void inviteWorker(String accessToken, String kfAccount, String inviteWx) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("kf_account", kfAccount == null ? "" : kfAccount);
		body.put("invite_wx", inviteWx == null ? "" : inviteWx);
		try {
			String raw =
					wxJavaMpRuntime
							.mpBearer(accessToken)
							.post(WxMpApiUrl.Kefu.KFACCOUNT_INVITE_WORKER, body);
			JsonNode root = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
			if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
				if (log.isDebugEnabled()) {
					log.debug(
							"微信 inviteworker 业务返回 errcode={} errmsg={}",
							root.get("errcode").asInt(),
							root.path("errmsg").asText(""));
				}
			}
		} catch (WxErrorException e) {
			throw new IllegalStateException("微信客服邀请接口调用失败", e);
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("解析微信客服邀请接口响应失败", e);
		}
	}

	public void updateKfNickname(String accessToken, String kfAccount, String nickName) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("kf_account", kfAccount == null ? "" : kfAccount);
		body.put("nickname", nickName == null ? "" : nickName);
		try {
			String raw =
					wxJavaMpRuntime.mpBearer(accessToken).post(WxMpApiUrl.Kefu.KFACCOUNT_UPDATE, body);
			byte[] rawBytes = raw.getBytes(StandardCharsets.UTF_8);
			JsonNode root = objectMapper.readTree(new String(rawBytes, StandardCharsets.UTF_8));
			if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
				if (log.isDebugEnabled()) {
					log.debug(
							"微信 kfaccount/update 业务返回 errcode={} errmsg={}",
							root.get("errcode").asInt(),
							root.path("errmsg").asText(""));
				}
			}
		} catch (WxErrorException e) {
			throwWechatWxFailure(e, "微信客服修改账号请求失败");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("微信客服修改账号接口处理异常", e);
			throw new ResourceException("微信修改客服账号失败");
		}
	}

	public void deleteKfAccount(String accessToken, String kfAccount) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("kf_account", kfAccount == null ? "" : kfAccount);
		try {
			String raw = wxJavaMpRuntime.mpBearer(accessToken).post(WxMpApiUrl.Kefu.KFACCOUNT_DEL, body);
			JsonNode root = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
			if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
				if (log.isDebugEnabled()) {
					log.debug(
							"微信 kfaccount/del 业务返回 errcode={} errmsg={}",
							root.get("errcode").asInt(),
							root.path("errmsg").asText(""));
				}
			}
		} catch (WxErrorException e) {
			throwWechatWxFailure(e, "微信客服删除账号请求失败");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("微信客服删除账号接口处理异常", e);
			throw new ResourceException("微信删除客服账号失败");
		}
	}

	public void uploadHeadImg(String accessToken, String kfAccount, Path imagePath) {
		try {
			wxJavaMpRuntime
					.mpBearer(accessToken)
					.getKefuService()
					.kfAccountUploadHeadImg(kfAccount == null ? "" : kfAccount, imagePath.toFile());
		} catch (WxErrorException e) {
			log.warn("微信客服上传头像 wx error", e);
			throw new ResourceException("微信客服上传头像接口返回无法解析");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("微信客服上传头像接口处理异常", e);
			throw new ResourceException("微信客服上传头像接口返回无法解析");
		}
	}

	private void throwWechatWxFailure(WxErrorException e, String logMessage) {
		log.warn(logMessage, e);
		String m = e.getError() != null ? e.getError().getErrorMsg() : e.getMessage();
		String suffix = (m != null && !m.isBlank()) ? (": " + m) : "";
		throw new ResourceException("调用微信客服接口失败" + suffix);
	}

	/**
	 * Fetches full customer-service list and merges online flags when the online list is present and non-empty.
	 */
	public List<Map<String, Object>> listKfWithOnlineFlags(String accessToken) {
		try {
			String listRaw =
					wxJavaMpRuntime.mpBearer(accessToken).get(WxMpApiUrl.Kefu.GET_KF_LIST, "");
			if (listRaw == null || listRaw.isEmpty()) {
				throw new ResourceException("微信接口返回为空");
			}
			JsonNode listRoot = objectMapper.readTree(listRaw.getBytes(StandardCharsets.UTF_8));
			if (listRoot.hasNonNull("errcode") && listRoot.get("errcode").asInt() != 0) {
				String errmsg = listRoot.path("errmsg").asText("");
				throw new ResourceException(
						(errmsg != null && !errmsg.isEmpty()) ? errmsg : "微信获取客服列表失败");
			}
			List<Map<String, Object>> kfLists = parseKfListArray(listRoot.get("kf_list"));
			if (kfLists.isEmpty()) {
				return kfLists;
			}
			try {
				String onlineRaw =
						wxJavaMpRuntime.mpBearer(accessToken).get(WxMpApiUrl.Kefu.GET_ONLINE_KF_LIST, "");
				if (onlineRaw == null || onlineRaw.isEmpty()) {
					return kfLists;
				}
				JsonNode onlineRoot = objectMapper.readTree(onlineRaw.getBytes(StandardCharsets.UTF_8));
				if (onlineRoot.hasNonNull("errcode") && onlineRoot.get("errcode").asInt() != 0) {
					return kfLists;
				}
				JsonNode kfOnlineList = onlineRoot.get("kf_online_list");
				if (kfOnlineList == null || !kfOnlineList.isArray() || kfOnlineList.isEmpty()) {
					return kfLists;
				}
				Set<String> onlineKfIds = new HashSet<>();
				for (JsonNode row : kfOnlineList) {
					if (row != null && row.has("kf_id") && !row.get("kf_id").isNull()) {
						onlineKfIds.add(normalizeKfIdValue(row.get("kf_id")));
					}
				}
				for (Map<String, Object> row : kfLists) {
					Object kfId = row.get("kf_id");
					row.put("is_online", onlineKfIds.contains(normalizeKfIdValue(kfId)));
				}
			} catch (WxErrorException e) {
				return kfLists;
			} catch (Exception e) {
				return kfLists;
			}
			return kfLists;
		} catch (WxErrorException e) {
			throw new IllegalStateException("微信客服列表请求失败", e);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("微信客服列表请求处理失败", e);
		}
	}

	private List<Map<String, Object>> parseKfListArray(JsonNode kfListNode) {
		if (kfListNode == null || !kfListNode.isArray()) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (JsonNode el : kfListNode) {
			if (el != null && el.isObject()) {
				out.add(objectMapper.convertValue(el, new TypeReference<LinkedHashMap<String, Object>>() {}));
			}
		}
		return out;
	}

	private static String normalizeKfIdValue(Object kfId) {
		if (kfId == null) {
			return "";
		}
		return String.valueOf(kfId);
	}

	private static String normalizeKfIdValue(JsonNode kfIdNode) {
		if (kfIdNode == null || kfIdNode.isNull()) {
			return "";
		}
		return kfIdNode.asText("");
	}
}
