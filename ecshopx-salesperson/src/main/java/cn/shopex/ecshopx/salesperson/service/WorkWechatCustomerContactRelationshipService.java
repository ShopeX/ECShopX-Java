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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.wechat.WorkWechatCustomerContactRelationshipPort;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRelLogs;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelLogsMapper;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import cn.shopex.ecshopx.workwechat.service.WorkWechatCorpUserApiService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WorkWechatCustomerContactRelationshipService implements WorkWechatCustomerContactRelationshipPort {

	private final WorkWechatCorpUserApiService workWechatCorpUserApiService;
	private final WorkWechatRelMapper workWechatRelMapper;
	private final WorkWechatRelLogsMapper workWechatRelLogsMapper;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ShopSalespersonFriendCountWriteService shopSalespersonFriendCountWriteService;

	public WorkWechatCustomerContactRelationshipService(
			WorkWechatCorpUserApiService workWechatCorpUserApiService,
			WorkWechatRelMapper workWechatRelMapper,
			WorkWechatRelLogsMapper workWechatRelLogsMapper,
			MembersAssociationsMapper membersAssociationsMapper,
			ShopSalespersonMapper shopSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ShopSalespersonFriendCountWriteService shopSalespersonFriendCountWriteService) {
		this.workWechatCorpUserApiService = workWechatCorpUserApiService;
		this.workWechatRelMapper = workWechatRelMapper;
		this.workWechatRelLogsMapper = workWechatRelLogsMapper;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.shopSalespersonFriendCountWriteService = shopSalespersonFriendCountWriteService;
	}

	@Override
	public void handleCustomerContactEvent(Long companyId, Map<String, Object> eventMap) {
		if (eventMap == null || eventMap.isEmpty()) {
			return;
		}
		Object ct = eventMap.get("ChangeType");
		String changeType = ct == null ? "" : String.valueOf(ct);
		switch (changeType) {
			case "add_external_contact" -> {
				log.info("导购员外部联系人添加好友关系");
				change(
						companyId,
						stringVal(eventMap.get("UserID")),
						stringVal(eventMap.get("ExternalUserID")),
						true,
						"导购员外部联系人添加好友关系");
			}
			case "add_half_external_contact" -> {
				log.info("外部联系人请求添加好友");
				change(
						companyId,
						stringVal(eventMap.get("UserID")),
						stringVal(eventMap.get("ExternalUserID")),
						false,
						"外部联系人请求添加好友");
			}
			case "del_external_contact" -> log.info("导购员删除外部联系人");
			case "del_follow_user" -> log.info("外部联系人删除导购员");
			default -> {}
		}
	}

	private void change(
			Long companyId, String workUserid, String externalUserid, boolean isFriend, String message) {
		long cid = companyId == null ? 0L : companyId;
		Map<String, Object> externalUserInfo =
				workWechatCorpUserApiService.getExternalContact(cid, externalUserid);
		Object ecObj = externalUserInfo.get("external_contact");
		if (!(ecObj instanceof Map<?, ?> ecMap)) {
			log.warn("unionid 获取失败, 需要企业或第三方服务商绑定了微信开发者ID");
			return;
		}
		Object unionObj = ecMap.get("unionid");
		if (unionObj == null || !StringUtils.hasText(String.valueOf(unionObj))) {
			log.warn("unionid 获取失败, 需要企业或第三方服务商绑定了微信开发者ID");
			return;
		}
		String unionid = String.valueOf(unionObj).trim();
		Map<String, Object> workUserMap = workWechatCorpUserApiService.getUser(cid, workUserid);
		Object mob = workUserMap.get("mobile");
		String mobilePlain = mob == null ? "" : String.valueOf(mob).trim();
		String encMobile = sensitiveFieldEncryptor.encrypt(mobilePlain);
		ShopSalesperson sp = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, cid)
				.eq(ShopSalesperson::getMobile, encMobile)
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.last("LIMIT 1"));
		long salespersonId = sp != null && sp.getSalespersonId() != null ? sp.getSalespersonId() : 0L;
		List<MembersAssociations> membersAssocInfo = membersAssociationsMapper.selectList(
				new LambdaQueryWrapper<MembersAssociations>().eq(MembersAssociations::getUnionid, unionid));
		long nowEpoch = Instant.now().getEpochSecond();
		if (membersAssocInfo != null && !membersAssocInfo.isEmpty()) {
			for (MembersAssociations v : membersAssocInfo) {
				if (v == null) {
					continue;
				}
				Long uid = v.getUserId();
				long userIdVal = uid == null ? 0L : uid;
				WorkWechatRel existing = workWechatRelMapper.selectOne(new LambdaQueryWrapper<WorkWechatRel>()
						.eq(WorkWechatRel::getSalespersonId, salespersonId)
						.eq(WorkWechatRel::getUserId, userIdVal)
						.last("LIMIT 1"));
				Long rowCompanyId = v.getCompanyId() == null ? 0L : v.getCompanyId();
				boolean isBind = false;
				long boundTime = 0L;
				if (existing != null) {
					isBind = Boolean.TRUE.equals(existing.getIsBind());
					if (isBind) {
						boundTime = existing.getBoundTime() == null ? 0L : existing.getBoundTime();
					}
				}
				WorkWechatRel row = new WorkWechatRel();
				row.setCompanyId(rowCompanyId);
				row.setWorkUserid(workUserid == null ? "" : workUserid);
				row.setSalespersonId(salespersonId);
				row.setExternalUserid(externalUserid == null ? "" : externalUserid);
				row.setUnionid(unionid);
				row.setUserId(userIdVal);
				row.setIsFriend(isFriend);
				row.setIsBind(isBind);
				row.setBoundTime(boundTime);
				row.setAddFriendTime(isFriend ? nowEpoch : 0L);
				if (existing != null) {
					row.setId(existing.getId());
					int n = workWechatRelMapper.updateById(row);
					if (n == 0) {
						throw new ResourceException("未查询到更新数据");
					}
				} else {
					workWechatRelMapper.insert(row);
				}
				WorkWechatRelLogs logRow = new WorkWechatRelLogs();
				logRow.setCompanyId(rowCompanyId);
				logRow.setWorkUserid(row.getWorkUserid());
				logRow.setSalespersonId(salespersonId);
				logRow.setExternalUserid(row.getExternalUserid());
				logRow.setUnionid(unionid);
				logRow.setUserId(userIdVal);
				logRow.setIsFriend(isFriend);
				logRow.setRemarks(message);
				int nowInt = (int) nowEpoch;
				logRow.setCreated(nowInt);
				logRow.setUpdated(nowInt);
				workWechatRelLogsMapper.insert(logRow);
				List<String> extIds = workWechatCorpUserApiService.listExternalUserIds(cid, workUserid);
				shopSalespersonFriendCountWriteService.updateFriendCount(salespersonId, extIds.size());
			}
		} else {
			WorkWechatRel existing = workWechatRelMapper.selectOne(new LambdaQueryWrapper<WorkWechatRel>()
					.eq(WorkWechatRel::getSalespersonId, salespersonId)
					.eq(WorkWechatRel::getUnionid, unionid)
					.last("LIMIT 1"));
			WorkWechatRel row = new WorkWechatRel();
			row.setCompanyId(0L);
			row.setWorkUserid(workUserid == null ? "" : workUserid);
			row.setSalespersonId(salespersonId);
			row.setExternalUserid(externalUserid == null ? "" : externalUserid);
			row.setUnionid(unionid);
			row.setUserId(0L);
			row.setIsFriend(isFriend);
			row.setIsBind(false);
			row.setBoundTime(0L);
			row.setAddFriendTime(isFriend ? nowEpoch : 0L);
			if (existing != null) {
				row.setId(existing.getId());
				int n = workWechatRelMapper.updateById(row);
				if (n == 0) {
					throw new ResourceException("未查询到更新数据");
				}
			} else {
				workWechatRelMapper.insert(row);
			}
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
