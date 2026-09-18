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

package cn.shopex.ecshopx.wechat.mp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaExceptions;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import java.util.ArrayList;
import java.util.List;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.bean.result.WxMpUserList;
import org.springframework.stereotype.Service;

@Service
public class OfficialAccountUserGetService {

	private final WxJavaMpRuntime wxJavaMpRuntime;

	public OfficialAccountUserGetService(WxJavaMpRuntime wxJavaMpRuntime) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
	}

	public UserGetPage listOpenIdsForSync(String authorizerAppId, String nextOpenidOrNull) {
		try {
			WxMpUserList list =
					nextOpenidOrNull == null || nextOpenidOrNull.isBlank()
							? wxJavaMpRuntime.mp(authorizerAppId).getUserService().userList()
							: wxJavaMpRuntime.mp(authorizerAppId).getUserService().userList(nextOpenidOrNull);
			long total = list.getTotal();
			int count = list.getCount();
			List<String> openids = list.getOpenids() != null ? list.getOpenids() : new ArrayList<>();
			String nextOpenid = list.getNextOpenid();
			if (nextOpenid != null && nextOpenid.isEmpty()) {
				nextOpenid = null;
			}
			return new UserGetPage(total, count, openids, nextOpenid);
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		}
	}

	public record UserGetPage(long total, int count, List<String> openids, String nextOpenid) {}
}
