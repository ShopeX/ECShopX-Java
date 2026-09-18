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

package cn.shopex.ecshopx.members.service.h5.bind;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobDispatchPublisher;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxappBindSalespersonService {

	private final WorkWechatRelMapper workWechatRelMapper;
	private final ShoppingGuideForH5BindLookup shoppingGuideForH5BindLookup;
	private final BindSalsepersonJobDispatchPublisher bindSalsepersonJobDispatchPublisher;

	public Map<String, Object> bindSalesperson(long companyId, long userId, String unionid, String mobileOrEmpty,
			String workUserid) {
		if (workUserid == null || !StringUtils.hasText(workUserid.trim())) {
			throw new ResourceException("导购员工编号不能为空");
		}
		String wu = workUserid.trim();
		log.info("wxapp bindSalesperson companyId={} userId={}", companyId, userId);

		Map<String, Object> guide = shoppingGuideForH5BindLookup.getShoppingGuideDetailForH5Bind(companyId, wu);
		if (guide == null || guide.isEmpty()) {
			return softFail("导购不存在");
		}

		Object sidObj = guide.get("salesperson_id");
		long salespersonId;
		try {
			salespersonId = Long.parseLong(String.valueOf(sidObj).trim());
			if (salespersonId <= 0L) {
				return softFail("导购不存在");
			}
		} catch (NumberFormatException e) {
			return softFail("导购不存在");
		}

		WorkWechatRel bound = workWechatRelMapper.selectOne(new LambdaQueryWrapper<WorkWechatRel>()
				.eq(WorkWechatRel::getUserId, userId)
				.eq(WorkWechatRel::getCompanyId, companyId)
				.eq(WorkWechatRel::getIsBind, Boolean.TRUE)
				.last("LIMIT 1"));
		if (bound != null) {
			return softFail("已与当前导购员绑定");
		}

		WorkWechatRel row = workWechatRelMapper.selectOne(new LambdaQueryWrapper<WorkWechatRel>()
				.eq(WorkWechatRel::getUserId, userId)
				.eq(WorkWechatRel::getCompanyId, companyId)
				.eq(WorkWechatRel::getSalespersonId, salespersonId)
				.last("LIMIT 1"));

		if (row != null) {
			LambdaUpdateWrapper<WorkWechatRel> uw = new LambdaUpdateWrapper<>();
			uw.eq(WorkWechatRel::getUserId, userId)
					.eq(WorkWechatRel::getCompanyId, companyId)
					.eq(WorkWechatRel::getSalespersonId, salespersonId)
					.set(WorkWechatRel::getIsBind, true);
			int n = workWechatRelMapper.update(null, uw);
			if (n == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		} else {
			WorkWechatRel ins = new WorkWechatRel();
			ins.setCompanyId(companyId);
			ins.setSalespersonId(salespersonId);
			ins.setUserId(userId);
			ins.setUnionid(unionid == null ? "" : unionid);
			ins.setWorkUserid("");
			ins.setExternalUserid("");
			ins.setIsFriend(false);
			ins.setIsBind(true);
			ins.setBoundTime(System.currentTimeMillis() / 1000L);
			ins.setAddFriendTime(0L);
			workWechatRelMapper.insert(ins);
		}

		String u = unionid == null ? "" : unionid;
		String m = mobileOrEmpty == null ? "" : mobileOrEmpty;
		bindSalsepersonJobDispatchPublisher.enqueueBindSalsepersonAfterWxappBindSalesperson(
				companyId, u, wu, 2, m, userId);

		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", Boolean.TRUE);
		return ok;
	}

	private static Map<String, Object> softFail(String msg) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("status", Boolean.FALSE);
		m.put("msg", msg);
		return m;
	}
}
