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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.bind.BindUserSalespersonRelShopReadMapper;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRelLogs;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelLogsMapper;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AdminBindUserSalespersonRelService {

	private final MembersAssociationsMapper membersAssociationsMapper;
	private final WorkWechatRelMapper workWechatRelMapper;
	private final WorkWechatRelLogsMapper workWechatRelLogsMapper;
	private final BindUserSalespersonRelShopReadMapper bindUserSalespersonRelShopReadMapper;
	private final TransactionTemplate transactionTemplate;

	public AdminBindUserSalespersonRelService(
			MembersAssociationsMapper membersAssociationsMapper,
			WorkWechatRelMapper workWechatRelMapper,
			WorkWechatRelLogsMapper workWechatRelLogsMapper,
			BindUserSalespersonRelShopReadMapper bindUserSalespersonRelShopReadMapper,
			TransactionTemplate transactionTemplate) {
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.workWechatRelMapper = workWechatRelMapper;
		this.workWechatRelLogsMapper = workWechatRelLogsMapper;
		this.bindUserSalespersonRelShopReadMapper = bindUserSalespersonRelShopReadMapper;
		this.transactionTemplate = transactionTemplate;
	}

	public Map<String, Object> bindUserSalespersonRel(
			long companyId, List<Long> userIds, long salespersonId, Long distributorShopIdOrNull) {
		int n = bindUserSalespersonRelShopReadMapper.countShopRelSalesperson(
				salespersonId, companyId, distributorShopIdOrNull);
		if (n == 0) {
			throw new ResourceException("导购员不存在");
		}
		String name = bindUserSalespersonRelShopReadMapper.selectSalespersonNameById(salespersonId);
		String salesPersonName = (name == null) ? "" : name.trim();

		if (userIds.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("success", false);
			return empty;
		}

		boolean lastWriteSuccess = false;
		List<Long> inputUserIds = List.copyOf(userIds);

		for (long inputUserId : inputUserIds) {
			MembersAssociations userInfo = membersAssociationsMapper.selectOne(
					new LambdaQueryWrapper<MembersAssociations>()
							.eq(MembersAssociations::getUserId, inputUserId)
							.eq(MembersAssociations::getCompanyId, companyId));
			if (userInfo == null) {
				continue;
			}
			String unionid = userInfo.getUnionid() == null ? "" : userInfo.getUnionid().trim();
			List<MembersAssociations> expanded = membersAssociationsMapper.selectList(
					new LambdaQueryWrapper<MembersAssociations>()
							.eq(MembersAssociations::getUnionid, unionid)
							.orderByDesc(MembersAssociations::getUserId));

			for (MembersAssociations v : expanded) {
				if (v.getUserId() == null) {
					continue;
				}
				long uid = v.getUserId();
				final boolean[] writeOkHolder = new boolean[] {false};
				transactionTemplate.executeWithoutResult(txStatus -> {
					boolean isFirstBind = true;
					LambdaQueryWrapper<WorkWechatRel> cntW = new LambdaQueryWrapper<>();
					cntW.eq(WorkWechatRel::getUserId, uid).eq(WorkWechatRel::getCompanyId, companyId);
					if (workWechatRelMapper.selectCount(cntW) > 0) {
						LambdaUpdateWrapper<WorkWechatRel> unbind = new LambdaUpdateWrapper<>();
						unbind
								.eq(WorkWechatRel::getUserId, uid)
								.eq(WorkWechatRel::getCompanyId, companyId)
								.set(WorkWechatRel::getIsBind, false);
						workWechatRelMapper.update(null, unbind);
						isFirstBind = false;
					}
					LambdaQueryWrapper<WorkWechatRel> bindKey = new LambdaQueryWrapper<>();
					bindKey
							.eq(WorkWechatRel::getUserId, uid)
							.eq(WorkWechatRel::getCompanyId, companyId)
							.eq(WorkWechatRel::getSalespersonId, salespersonId)
							.last("LIMIT 1");
					WorkWechatRel isBound = workWechatRelMapper.selectOne(bindKey);
					long boundEpoch = System.currentTimeMillis() / 1000L;
					boolean writeOk;
					if (isBound != null) {
						LambdaUpdateWrapper<WorkWechatRel> uw = new LambdaUpdateWrapper<>();
						uw.eq(WorkWechatRel::getUserId, uid)
								.eq(WorkWechatRel::getCompanyId, companyId)
								.eq(WorkWechatRel::getSalespersonId, salespersonId)
								.set(WorkWechatRel::getIsBind, true)
								.set(WorkWechatRel::getBoundTime, boundEpoch);
						int st = workWechatRelMapper.update(null, uw);
						if (st == 0) {
							throw new ResourceException("未查询到更新数据");
						}
						writeOk = st > 0;
					} else {
						WorkWechatRel row = new WorkWechatRel();
						row.setCompanyId(companyId);
						row.setSalespersonId(salespersonId);
						row.setUnionid(unionid);
						row.setUserId(uid);
						row.setWorkUserid("");
						row.setExternalUserid("");
						row.setIsFriend(false);
						row.setIsBind(true);
						row.setBoundTime(boundEpoch);
						row.setAddFriendTime(0L);
						int st = workWechatRelMapper.insert(row);
						writeOk = st > 0;
					}
					boolean firstBindFlag = isFirstBind;
					String remarks = firstBindFlag
							? ("初始绑定: 与导购 " + salesPersonName + " 绑定关系")
							: ("绑定变更: 绑定导购变更为 " + salesPersonName);
					WorkWechatRelLogs log = new WorkWechatRelLogs();
					log.setCompanyId(companyId);
					log.setSalespersonId(salespersonId);
					log.setUnionid(unionid);
					log.setUserId(uid);
					log.setWorkUserid("");
					log.setExternalUserid("");
					log.setIsFriend(false);
					log.setRemarks(remarks);
					int ts = (int) boundEpoch;
					log.setCreated(ts);
					log.setUpdated(ts);
					workWechatRelLogsMapper.insert(log);
					writeOkHolder[0] = writeOk;
				});
				lastWriteSuccess = writeOkHolder[0];
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("success", lastWriteSuccess);
		return out;
	}
}
