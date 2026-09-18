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

package cn.shopex.ecshopx.point.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.point.PointMemberDmPointMemberInfoReadPort;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformPointPort;
import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.point.domain.PointMember;
import cn.shopex.ecshopx.point.domain.PointMemberLog;
import cn.shopex.ecshopx.point.mapper.PointMemberLogMapper;
import cn.shopex.ecshopx.point.mapper.PointMemberMapper;
import cn.shopex.ecshopx.point.service.shuyun.ShuyunMemberPointSyncPort;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmManualPointChangePort;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmManualPointChangeRequest;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PointMemberAddPointService {

	private static final Logger log = LoggerFactory.getLogger(PointMemberAddPointService.class);

	private static final int JOURNAL_TYPE_MANUAL = 12;

	private static final int JOURNAL_TYPE_OPENAPI = 13;

	private static final String OPENAPI_OPERATER = "外部开发者";

	/** Register gift points (注册赠送), aligned with {@link PointMemberJournalTypeDescriptions}. */
	private static final int JOURNAL_TYPE_REGISTER_GIFT = 1;

	private static final int JOURNAL_TYPE_REGISTRATION_ACTIVITY = 16;

	private static final int JOURNAL_TYPE_TURNTABLE_WIN = 11;

	/** 订单消费送积分 */
	private static final int JOURNAL_TYPE_ORDER_BONUS = 7;

	/** 取消订单返还积分 */
	private static final int JOURNAL_TYPE_ORDER_CANCEL_RETURN = 9;

	/** 售后/退款返还积分 */
	private static final int JOURNAL_TYPE_REFUND_RETURN = 10;

	/** 分佣结算（与 PHP PointMemberService::JOURNAL_TYPE_PROMOTER 一致） */
	private static final int JOURNAL_TYPE_PROMOTER = 14;

	/** 商家导入修改（与 PHP JOURNAL_TYPE_MAP 15 / 会员导入加减积分一致） */
	private static final int JOURNAL_TYPE_MEMBER_IMPORT = 15;

	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final DmCrmManualPointChangePort dmCrmManualPointChangePort;
	private final PointMemberDmPointMemberInfoReadPort pointMemberDmPointMemberInfoReadPort;
	private final PointMemberMapper pointMemberMapper;
	private final PointMemberLogMapper pointMemberLogMapper;
	private final ShuyunMemberPointSyncPort shuyunMemberPointSyncPort;
	private final ShuyunOpenPlatformPointPort openPlatformPointPort;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	public PointMemberAddPointService(
			DmCrmSettingReadPort dmCrmSettingReadPort,
			DmCrmManualPointChangePort dmCrmManualPointChangePort,
			PointMemberDmPointMemberInfoReadPort pointMemberDmPointMemberInfoReadPort,
			PointMemberMapper pointMemberMapper,
			PointMemberLogMapper pointMemberLogMapper,
			ShuyunMemberPointSyncPort shuyunMemberPointSyncPort,
			ShuyunOpenPlatformPointPort openPlatformPointPort) {
		this.dmCrmSettingReadPort = dmCrmSettingReadPort;
		this.dmCrmManualPointChangePort = dmCrmManualPointChangePort;
		this.pointMemberDmPointMemberInfoReadPort = pointMemberDmPointMemberInfoReadPort;
		this.pointMemberMapper = pointMemberMapper;
		this.pointMemberLogMapper = pointMemberLogMapper;
		this.shuyunMemberPointSyncPort = shuyunMemberPointSyncPort;
		this.openPlatformPointPort = openPlatformPointPort;
	}

	/** 达摩与开放平台互斥：OPEN 启用时不走达摩（对齐 PHP）。 */
	private boolean shouldUseDmPointIntegration(long companyId) {
		return dmCrmSettingReadPort.isPointIntegrationOpen(companyId)
				&& !openPlatformPointPort.isOpenPlatformPointEnabled(companyId);
	}

	/**
	 * OPEN 启用时先调 point.change（对齐 PHP「先网关后本地」）。
	 *
	 * @return true 表示扣减场景应跳过本地余额闸门
	 */
	private boolean syncOpenPlatformPointFirst(
			long companyId,
			long userId,
			int point,
			boolean plus,
			int journalType,
			String record,
			String orderId,
			Map<String, Object> otherParams) {
		if (!openPlatformPointPort.isOpenPlatformPointEnabled(companyId)) {
			return false;
		}
		Map<String, Object> extras = otherParams == null ? Map.of() : otherParams;
		String safeRecord = record == null ? "" : record;
		String safeOrderId = orderId == null ? "" : orderId;
		if (point != 0) {
			if (!openPlatformPointPort.changePoint(
					companyId, userId, point, plus, journalType, safeRecord, safeOrderId, extras)) {
				throw new ResourceException("数云开放网关积分同步失败，请稍后重试");
			}
		}
		return !plus && point != 0;
	}

	/** OPEN 已在本地前同步；此处仅 OEM UAPI 双写。 */
	private void maybePushShuyunPoint(
			long companyId,
			long userId,
			int point,
			boolean plus,
			int journalType,
			String record,
			String orderId,
			Map<String, Object> otherParams) {
		if (openPlatformPointPort.isOpenPlatformPointEnabled(companyId)) {
			return;
		}
		Map<String, Object> extras = otherParams == null ? Map.of() : otherParams;
		String safeRecord = record == null ? "" : record;
		String safeOrderId = orderId == null ? "" : orderId;
		if (oemShuyun) {
			shuyunMemberPointSyncPort.shuyunAddPoint(point, plus, safeRecord, safeOrderId, extras);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void addPointForManualAdjustment(long userId, long companyId, int point, boolean plus, String record) {
		applyLocalOrDmPointChange(userId, companyId, point, plus, record, JOURNAL_TYPE_MANUAL);
	}

	/**
	 * Member Excel import/update point change (PHP journal_type=15「商家导入修改」).
	 */
	@Transactional(rollbackFor = Exception.class)
	public void addPointForMemberImport(long userId, long companyId, int point, boolean plus, String record) {
		applyLocalOrDmPointChange(userId, companyId, point, plus, record, JOURNAL_TYPE_MEMBER_IMPORT);
	}

	private void applyLocalOrDmPointChange(
			long userId, long companyId, int point, boolean plus, String record, int journalType) {
		if (shouldUseDmPointIntegration(companyId)) {
			Map<String, Object> memberInfo = pointMemberDmPointMemberInfoReadPort.getMemberInfo(userId, companyId);
			Object uidObj = memberInfo.get("user_id");
			if (uidObj == null || !StringUtils.hasText(String.valueOf(uidObj))) {
				throw new ResourceException("未查询到相关会员信息");
			}
			String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
			if (!StringUtils.hasText(mobile)) {
				throw new ResourceException("未查询到相关会员信息");
			}
			Object cardNoObj = memberInfo.get("dm_card_no");
			String cardNo = cardNoObj == null ? null : String.valueOf(cardNoObj);

			long flowTs = Instant.now().getEpochSecond();
			String integralFlow = mobile + "_" + flowTs;
			DmCrmManualPointChangeRequest request;
			if (!plus) {
				request =
						DmCrmManualPointChangeRequest.builder()
								.mobile(mobile)
								.cardNo(cardNo)
								.integral(-point)
								.type(0)
								.changeType("1240")
								.remark("积分扣减:" + record)
								.integralFlow(integralFlow)
								.sourceChannel("c_brand_mall")
								.build();
			} else {
				request =
						DmCrmManualPointChangeRequest.builder()
								.mobile(mobile)
								.cardNo(cardNo)
								.integral(point)
								.type(1)
								.changeType("1140")
								.remark("积分增加:" + record)
								.integralFlow(integralFlow)
								.sourceChannel("c_brand_mall")
								.build();
			}
			try {
				dmCrmManualPointChangePort.changePoint(request);
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				String masked = DataMasking.maskMobile(mobile);
				log.error(
						"Dam manual point change failed companyId={} userId={} mobile={}",
						companyId,
						userId,
						masked,
						e);
				String msg = e.getMessage();
				if (!StringUtils.hasText(msg)) {
					msg = "达摩CRM积分变更失败";
				}
				throw new ResourceException(msg);
			}
			return;
		}

		boolean skipLocalBalance =
				syncOpenPlatformPointFirst(companyId, userId, point, plus, journalType, record, "", Map.of());

		if (point != 0 && !skipLocalBalance) {
			if (plus) {
				pointMemberMapper.addPointDelta(userId, companyId, point);
			} else {
				int rows = pointMemberMapper.subtractPointIfEnough(userId, companyId, point);
				if (rows == 0) {
					throw new ResourceException("积分不足");
				}
			}
		}

		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null && !skipLocalBalance) {
			if (point == 0) {
				maybePushShuyunPoint(companyId, userId, point, plus, journalType, record, "", Map.of());
				return;
			}
			throw new ResourceException("积分不足");
		}
		String pointDescPrefix = StringUtils.hasText(record) ? record : "无记录";
		String pointDesc;
		if (skipLocalBalance) {
			pointDesc = pointDescPrefix + "，剩余积分以数云端为准";
		} else {
			long remainingLong = row.getPoint() != null ? row.getPoint() : 0L;
			if (remainingLong > Integer.MAX_VALUE) {
				remainingLong = Integer.MAX_VALUE;
			}
			int remaining = (int) remainingLong;
			pointDesc = pointDescPrefix + "，当前剩余积分：" + remaining;
		}

		int now = (int) Instant.now().getEpochSecond();
		PointMemberLog logRow = new PointMemberLog();
		logRow.setUserId(userId);
		logRow.setCompanyId(companyId);
		logRow.setJournalType(journalType);
		logRow.setPointDesc(pointDesc);
		logRow.setIncome(plus ? point : 0);
		logRow.setOutcome(plus ? 0 : point);
		logRow.setOrderId("");
		logRow.setExternalId("");
		logRow.setOperater("");
		logRow.setOperaterRemark("");
		logRow.setCreated(now);
		logRow.setUpdated(now);
		pointMemberLogMapper.insert(logRow);

		maybePushShuyunPoint(companyId, userId, point, plus, journalType, record, "", Map.of());
	}

	@Transactional(rollbackFor = Exception.class)
	public void addPointForOpenapi(
			long userId,
			long companyId,
			int point,
			boolean plus,
			String externalId,
			String operaterRemark) {
		String safeExternalId = externalId == null ? "" : externalId;
		String safeOperaterRemark = operaterRemark == null ? "" : operaterRemark;
		if (shouldUseDmPointIntegration(companyId)) {
			Map<String, Object> memberInfo = pointMemberDmPointMemberInfoReadPort.getMemberInfo(userId, companyId);
			Object uidObj = memberInfo.get("user_id");
			if (uidObj == null || !StringUtils.hasText(String.valueOf(uidObj))) {
				throw new ResourceException("未查询到相关会员信息");
			}
			String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
			if (!StringUtils.hasText(mobile)) {
				throw new ResourceException("未查询到相关会员信息");
			}
			Object cardNoObj = memberInfo.get("dm_card_no");
			String cardNo = cardNoObj == null ? null : String.valueOf(cardNoObj);

			long flowTs = Instant.now().getEpochSecond();
			String integralFlow = mobile + "_" + flowTs;
			DmCrmManualPointChangeRequest request;
			if (!plus) {
				request =
						DmCrmManualPointChangeRequest.builder()
								.mobile(mobile)
								.cardNo(cardNo)
								.integral(-point)
								.type(0)
								.changeType("1240")
								.remark("积分扣减:" + safeOperaterRemark)
								.integralFlow(integralFlow)
								.sourceChannel("c_brand_mall")
								.build();
			} else {
				request =
						DmCrmManualPointChangeRequest.builder()
								.mobile(mobile)
								.cardNo(cardNo)
								.integral(point)
								.type(1)
								.changeType("1140")
								.remark("积分增加:" + safeOperaterRemark)
								.integralFlow(integralFlow)
								.sourceChannel("c_brand_mall")
								.build();
			}
			try {
				dmCrmManualPointChangePort.changePoint(request);
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				String masked = DataMasking.maskMobile(mobile);
				log.error(
						"Dam manual point change failed companyId={} userId={} mobile={}",
						companyId,
						userId,
						masked,
						e);
				String msg = e.getMessage();
				if (!StringUtils.hasText(msg)) {
					msg = "达摩CRM积分变更失败";
				}
				throw new ResourceException(msg);
			}
			return;
		}

		boolean skipLocalBalance =
				syncOpenPlatformPointFirst(companyId, userId, point, plus, JOURNAL_TYPE_OPENAPI, "", "", Map.of());

		if (point != 0 && !skipLocalBalance) {
			if (plus) {
				pointMemberMapper.addPointDelta(userId, companyId, point);
			} else {
				int rows = pointMemberMapper.subtractPointIfEnough(userId, companyId, point);
				if (rows == 0) {
					throw new ResourceException("积分不足");
				}
			}
		}

		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null && !skipLocalBalance) {
			if (point == 0) {
				maybePushShuyunPoint(companyId, userId, point, plus, JOURNAL_TYPE_OPENAPI, "", "", Map.of());
				return;
			}
			throw new ResourceException("积分不足");
		}
		String pointDesc;
		if (skipLocalBalance) {
			pointDesc = "无记录，剩余积分以数云端为准";
		} else {
			long remainingLong = row.getPoint() != null ? row.getPoint() : 0L;
			if (remainingLong > Integer.MAX_VALUE) {
				remainingLong = Integer.MAX_VALUE;
			}
			int remaining = (int) remainingLong;
			pointDesc = "无记录，当前剩余积分：" + remaining;
		}

		int now = (int) Instant.now().getEpochSecond();
		PointMemberLog logRow = new PointMemberLog();
		logRow.setUserId(userId);
		logRow.setCompanyId(companyId);
		logRow.setJournalType(JOURNAL_TYPE_OPENAPI);
		logRow.setPointDesc(pointDesc);
		logRow.setIncome(plus ? point : 0);
		logRow.setOutcome(plus ? 0 : point);
		logRow.setOrderId("");
		logRow.setExternalId(safeExternalId);
		logRow.setOperater(OPENAPI_OPERATER);
		logRow.setOperaterRemark(safeOperaterRemark);
		logRow.setCreated(now);
		logRow.setUpdated(now);
		pointMemberLogMapper.insert(logRow);

		maybePushShuyunPoint(companyId, userId, point, plus, JOURNAL_TYPE_OPENAPI, "", "", Map.of());
	}

	@Transactional(rollbackFor = Exception.class)
	public void addPointForPopularizeSettle(long userId, long companyId, int pointSigned, String orderId) {
		int pointAbs = Math.abs(pointSigned);
		boolean plus = pointSigned > 0;
		String record = plus ? "分佣获取积分" : "分佣获取积分返回";
		if (shouldUseDmPointIntegration(companyId)) {
			Map<String, Object> memberInfo = pointMemberDmPointMemberInfoReadPort.getMemberInfo(userId, companyId);
			Object uidObj = memberInfo.get("user_id");
			if (uidObj == null || !StringUtils.hasText(String.valueOf(uidObj))) {
				throw new ResourceException("未查询到相关会员信息");
			}
			String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
			if (!StringUtils.hasText(mobile)) {
				throw new ResourceException("未查询到相关会员信息");
			}
			Object cardNoObj = memberInfo.get("dm_card_no");
			String cardNo = cardNoObj == null ? null : String.valueOf(cardNoObj);

			long flowTs = Instant.now().getEpochSecond();
			String integralFlow = mobile + "_" + flowTs;
			DmCrmManualPointChangeRequest request;
			if (!plus) {
				request =
						DmCrmManualPointChangeRequest.builder()
								.mobile(mobile)
								.cardNo(cardNo)
								.integral(-pointAbs)
								.type(0)
								.changeType("1240")
								.remark("积分扣减:" + record)
								.integralFlow(integralFlow)
								.sourceChannel("c_brand_mall")
								.build();
			} else {
				request =
						DmCrmManualPointChangeRequest.builder()
								.mobile(mobile)
								.cardNo(cardNo)
								.integral(pointAbs)
								.type(1)
								.changeType("1140")
								.remark("积分增加:" + record)
								.integralFlow(integralFlow)
								.sourceChannel("c_brand_mall")
								.build();
			}
			try {
				dmCrmManualPointChangePort.changePoint(request);
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				String masked = DataMasking.maskMobile(mobile);
				log.error(
						"Dam manual point change failed companyId={} userId={} mobile={}",
						companyId,
						userId,
						masked,
						e);
				String msg = e.getMessage();
				if (!StringUtils.hasText(msg)) {
					msg = "达摩CRM积分变更失败";
				}
				throw new ResourceException(msg);
			}
			return;
		}

		String orderIdSafe = StringUtils.hasText(orderId) ? orderId : "";
		boolean skipLocalBalance =
				syncOpenPlatformPointFirst(
						companyId, userId, pointAbs, plus, JOURNAL_TYPE_PROMOTER, record, orderIdSafe, Map.of());

		if (pointAbs != 0 && !skipLocalBalance) {
			if (plus) {
				pointMemberMapper.addPointDelta(userId, companyId, pointAbs);
			} else {
				int rows = pointMemberMapper.subtractPointIfEnough(userId, companyId, pointAbs);
				if (rows == 0) {
					throw new ResourceException("积分不足");
				}
			}
		}

		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null && !skipLocalBalance) {
			if (pointAbs == 0) {
				maybePushShuyunPoint(
						companyId,
						userId,
						pointSigned,
						plus,
						JOURNAL_TYPE_PROMOTER,
						record,
						orderIdSafe,
						Map.of());
				return;
			}
			throw new ResourceException("积分不足");
		}
		String pointDescPrefix = StringUtils.hasText(record) ? record : "无记录";
		String pointDesc;
		if (skipLocalBalance) {
			pointDesc = pointDescPrefix + "，剩余积分以数云端为准";
		} else {
			long remainingLong = row.getPoint() != null ? row.getPoint() : 0L;
			if (remainingLong > Integer.MAX_VALUE) {
				remainingLong = Integer.MAX_VALUE;
			}
			int remaining = (int) remainingLong;
			pointDesc = pointDescPrefix + "，当前剩余积分：" + remaining;
		}

		int now = (int) Instant.now().getEpochSecond();
		PointMemberLog logRow = new PointMemberLog();
		logRow.setUserId(userId);
		logRow.setCompanyId(companyId);
		logRow.setJournalType(JOURNAL_TYPE_PROMOTER);
		logRow.setPointDesc(pointDesc);
		logRow.setIncome(plus ? pointAbs : 0);
		logRow.setOutcome(plus ? 0 : pointAbs);
		logRow.setOrderId(StringUtils.hasText(orderId) ? orderId : "");
		logRow.setExternalId("");
		logRow.setOperater("");
		logRow.setOperaterRemark("");
		logRow.setCreated(now);
		logRow.setUpdated(now);
		pointMemberLogMapper.insert(logRow);

		maybePushShuyunPoint(
				companyId,
				userId,
				pointSigned,
				plus,
				JOURNAL_TYPE_PROMOTER,
				record,
				StringUtils.hasText(orderId) ? orderId : "",
				Map.of());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	@SuppressWarnings("unused")
	public void addPointForSelfserviceRegistration(long userId, long companyId, int point, Locale locale) {
		boolean plus = true;
		String record = "活动报名送积分";
		if (shouldUseDmPointIntegration(companyId)) {
			Map<String, Object> memberInfo = pointMemberDmPointMemberInfoReadPort.getMemberInfo(userId, companyId);
			Object uidObj = memberInfo.get("user_id");
			if (uidObj == null || !StringUtils.hasText(String.valueOf(uidObj))) {
				throw new ResourceException("未查询到相关会员信息");
			}
			String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
			if (!StringUtils.hasText(mobile)) {
				throw new ResourceException("未查询到相关会员信息");
			}
			Object cardNoObj = memberInfo.get("dm_card_no");
			String cardNo = cardNoObj == null ? null : String.valueOf(cardNoObj);

			long flowTs = Instant.now().getEpochSecond();
			String integralFlow = mobile + "_" + flowTs;
			DmCrmManualPointChangeRequest request =
					DmCrmManualPointChangeRequest.builder()
							.mobile(mobile)
							.cardNo(cardNo)
							.integral(point)
							.type(1)
							.changeType("1140")
							.remark("积分增加:" + record)
							.integralFlow(integralFlow)
							.sourceChannel("c_brand_mall")
							.build();
			try {
				dmCrmManualPointChangePort.changePoint(request);
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				String masked = DataMasking.maskMobile(mobile);
				log.error(
						"Dam manual point change failed companyId={} userId={} mobile={}",
						companyId,
						userId,
						masked,
						e);
				String msg = e.getMessage();
				if (!StringUtils.hasText(msg)) {
					msg = "达摩CRM积分变更失败";
				}
				throw new ResourceException(msg);
			}
			return;
		}

		syncOpenPlatformPointFirst(companyId, userId, point, plus, JOURNAL_TYPE_REGISTRATION_ACTIVITY, record, "", Map.of());

		if (point != 0) {
			pointMemberMapper.addPointDelta(userId, companyId, point);
		}

		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			if (point == 0) {
				maybePushShuyunPoint(companyId, userId, point, plus, JOURNAL_TYPE_REGISTRATION_ACTIVITY, record, "", Map.of());
				return;
			}
			throw new ResourceException("积分不足");
		}
		long remainingLong = row.getPoint() != null ? row.getPoint() : 0L;
		if (remainingLong > Integer.MAX_VALUE) {
			remainingLong = Integer.MAX_VALUE;
		}
		int remaining = (int) remainingLong;

		String pointDescPrefix = StringUtils.hasText(record) ? record : "无记录";
		String pointDesc = pointDescPrefix + "，当前剩余积分：" + remaining;

		int now = (int) Instant.now().getEpochSecond();
		PointMemberLog logRow = new PointMemberLog();
		logRow.setUserId(userId);
		logRow.setCompanyId(companyId);
		logRow.setJournalType(JOURNAL_TYPE_REGISTRATION_ACTIVITY);
		logRow.setPointDesc(pointDesc);
		logRow.setIncome(point);
		logRow.setOutcome(0);
		logRow.setOrderId("");
		logRow.setExternalId("");
		logRow.setOperater("");
		logRow.setOperaterRemark("");
		logRow.setCreated(now);
		logRow.setUpdated(now);
		pointMemberLogMapper.insert(logRow);

		maybePushShuyunPoint(companyId, userId, point, plus, JOURNAL_TYPE_REGISTRATION_ACTIVITY, record, "", Map.of());
	}

	@Transactional(rollbackFor = Exception.class)
	public void addPointForMemberRegisterGift(long userId, long companyId, int point) {
		boolean plus = true;
		String record = "注册赠送";
		if (shouldUseDmPointIntegration(companyId)) {
			Map<String, Object> memberInfo = pointMemberDmPointMemberInfoReadPort.getMemberInfo(userId, companyId);
			Object uidObj = memberInfo.get("user_id");
			if (uidObj == null || !StringUtils.hasText(String.valueOf(uidObj))) {
				throw new ResourceException("未查询到相关会员信息");
			}
			String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
			if (!StringUtils.hasText(mobile)) {
				throw new ResourceException("未查询到相关会员信息");
			}
			Object cardNoObj = memberInfo.get("dm_card_no");
			String cardNo = cardNoObj == null ? null : String.valueOf(cardNoObj);

			long flowTs = Instant.now().getEpochSecond();
			String integralFlow = mobile + "_" + flowTs;
			DmCrmManualPointChangeRequest request =
					DmCrmManualPointChangeRequest.builder()
							.mobile(mobile)
							.cardNo(cardNo)
							.integral(point)
							.type(1)
							.changeType("1140")
							.remark("积分增加:" + record)
							.integralFlow(integralFlow)
							.sourceChannel("c_brand_mall")
							.build();
			try {
				dmCrmManualPointChangePort.changePoint(request);
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				String masked = DataMasking.maskMobile(mobile);
				log.error(
						"Dam manual point change failed companyId={} userId={} mobile={}",
						companyId,
						userId,
						masked,
						e);
				String msg = e.getMessage();
				if (!StringUtils.hasText(msg)) {
					msg = "达摩CRM积分变更失败";
				}
				throw new ResourceException(msg);
			}
			return;
		}

		syncOpenPlatformPointFirst(companyId, userId, point, plus, JOURNAL_TYPE_REGISTER_GIFT, record, "", Map.of());

		if (point != 0) {
			pointMemberMapper.addPointDelta(userId, companyId, point);
		}

		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			if (point == 0) {
				maybePushShuyunPoint(companyId, userId, point, plus, JOURNAL_TYPE_REGISTER_GIFT, record, "", Map.of());
				return;
			}
			throw new ResourceException("积分不足");
		}
		long remainingLong = row.getPoint() != null ? row.getPoint() : 0L;
		if (remainingLong > Integer.MAX_VALUE) {
			remainingLong = Integer.MAX_VALUE;
		}
		int remaining = (int) remainingLong;

		String pointDescPrefix = StringUtils.hasText(record) ? record : "无记录";
		String pointDesc = pointDescPrefix + "，当前剩余积分：" + remaining;

		int now = (int) Instant.now().getEpochSecond();
		PointMemberLog logRow = new PointMemberLog();
		logRow.setUserId(userId);
		logRow.setCompanyId(companyId);
		logRow.setJournalType(JOURNAL_TYPE_REGISTER_GIFT);
		logRow.setPointDesc(pointDesc);
		logRow.setIncome(point);
		logRow.setOutcome(0);
		logRow.setOrderId("");
		logRow.setExternalId("");
		logRow.setOperater("");
		logRow.setOperaterRemark("");
		logRow.setCreated(now);
		logRow.setUpdated(now);
		pointMemberLogMapper.insert(logRow);

		maybePushShuyunPoint(companyId, userId, point, plus, JOURNAL_TYPE_REGISTER_GIFT, record, "", Map.of());
	}

	@Transactional(rollbackFor = Exception.class)
	public void addPointForTurntableWin(long userId, long companyId, int point) {
		addPointForTurntableWin(userId, companyId, point, null);
	}

	@Transactional(rollbackFor = Exception.class)
	public void addPointForTurntableWin(long userId, long companyId, int point, String requestId) {
		if (StringUtils.hasText(requestId)
				&& queryTurntableWinResult(companyId, userId, requestId) == TurntableDrawCostQueryResult.SUCCESS) {
			return;
		}
		boolean plus = true;
		String record = "大转盘抽奖获得";
		String orderIdForLog = normalizeTurntableRequestOrderId(requestId);
		if (shouldUseDmPointIntegration(companyId)) {
			changeDmPoint(userId, companyId, point, true, record);
			insertTurntablePointLog(userId, companyId, point, 0, orderIdForLog, record, null);
			return;
		}

		syncOpenPlatformPointFirst(companyId, userId, point, plus, JOURNAL_TYPE_TURNTABLE_WIN, record, "", Map.of());

		if (point != 0) {
			pointMemberMapper.addPointDelta(userId, companyId, point);
		}

		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			if (point == 0) {
				maybePushShuyunPoint(companyId, userId, point, plus, JOURNAL_TYPE_TURNTABLE_WIN, record, "", Map.of());
				return;
			}
			throw new ResourceException("积分不足");
		}
		insertTurntablePointLog(userId, companyId, point, 0, orderIdForLog, record, remainingOf(row));
		maybePushShuyunPoint(companyId, userId, point, plus, JOURNAL_TYPE_TURNTABLE_WIN, record, "", Map.of());
	}

	@Transactional(rollbackFor = Exception.class)
	public void addPointForNormalOrderBonus(long userId, long companyId, int bonusPoints, long orderId) {
		addPointForNormalOrderBonus(userId, companyId, bonusPoints, orderId, null);
	}

	@Transactional(rollbackFor = Exception.class)
	public void addPointForNormalOrderBonus(
			long userId, long companyId, int bonusPoints, long orderId, String markOverride) {
		boolean plus = true;
		String record =
				StringUtils.hasText(markOverride)
						? markOverride
						: ("订单号：" + orderId + " 消费送积分");
		if (shouldUseDmPointIntegration(companyId)) {
			Map<String, Object> memberInfo = pointMemberDmPointMemberInfoReadPort.getMemberInfo(userId, companyId);
			Object uidObj = memberInfo.get("user_id");
			if (uidObj == null || !StringUtils.hasText(String.valueOf(uidObj))) {
				throw new ResourceException("未查询到相关会员信息");
			}
			String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
			if (!StringUtils.hasText(mobile)) {
				throw new ResourceException("未查询到相关会员信息");
			}
			Object cardNoObj = memberInfo.get("dm_card_no");
			String cardNo = cardNoObj == null ? null : String.valueOf(cardNoObj);

			long flowTs = Instant.now().getEpochSecond();
			String integralFlow = mobile + "_" + flowTs;
			DmCrmManualPointChangeRequest request =
					DmCrmManualPointChangeRequest.builder()
							.mobile(mobile)
							.cardNo(cardNo)
							.integral(bonusPoints)
							.type(1)
							.changeType("1140")
							.remark("积分增加:" + record)
							.integralFlow(integralFlow)
							.sourceChannel("c_brand_mall")
							.build();
			try {
				dmCrmManualPointChangePort.changePoint(request);
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				String masked = DataMasking.maskMobile(mobile);
				log.error(
						"Dam manual point change failed companyId={} userId={} mobile={}",
						companyId,
						userId,
						masked,
						e);
				String msg = e.getMessage();
				if (!StringUtils.hasText(msg)) {
					msg = "达摩CRM积分变更失败";
				}
				throw new ResourceException(msg);
			}
			return;
		}

		syncOpenPlatformPointFirst(
				companyId, userId, bonusPoints, plus, JOURNAL_TYPE_ORDER_BONUS, record, "", Map.of());

		if (bonusPoints != 0) {
			pointMemberMapper.addPointDelta(userId, companyId, bonusPoints);
		}

		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			if (bonusPoints == 0) {
				maybePushShuyunPoint(companyId, userId, bonusPoints, plus, JOURNAL_TYPE_ORDER_BONUS, record, "", Map.of());
				return;
			}
			throw new ResourceException("积分不足");
		}

		int now = (int) Instant.now().getEpochSecond();
		PointMemberLog logRow = new PointMemberLog();
		logRow.setUserId(userId);
		logRow.setCompanyId(companyId);
		logRow.setJournalType(JOURNAL_TYPE_ORDER_BONUS);
		logRow.setPointDesc(record);
		logRow.setIncome(bonusPoints);
		logRow.setOutcome(0);
		logRow.setOrderId(String.valueOf(orderId));
		logRow.setExternalId("");
		logRow.setOperater("");
		logRow.setOperaterRemark("");
		logRow.setCreated(now);
		logRow.setUpdated(now);
		pointMemberLogMapper.insert(logRow);

		maybePushShuyunPoint(companyId, userId, bonusPoints, plus, JOURNAL_TYPE_ORDER_BONUS, record, "", Map.of());
	}

	@Transactional(rollbackFor = Exception.class)
	public void addPointForOrderCancelReturn(long userId, long companyId, int points, long orderId) {
		boolean plus = true;
		String record = "取消订单" + orderId + "返还";
		if (shouldUseDmPointIntegration(companyId)) {
			Map<String, Object> memberInfo = pointMemberDmPointMemberInfoReadPort.getMemberInfo(userId, companyId);
			Object uidObj = memberInfo.get("user_id");
			if (uidObj == null || !StringUtils.hasText(String.valueOf(uidObj))) {
				throw new ResourceException("未查询到相关会员信息");
			}
			String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
			if (!StringUtils.hasText(mobile)) {
				throw new ResourceException("未查询到相关会员信息");
			}
			Object cardNoObj = memberInfo.get("dm_card_no");
			String cardNo = cardNoObj == null ? null : String.valueOf(cardNoObj);

			long flowTs = Instant.now().getEpochSecond();
			String integralFlow = mobile + "_" + flowTs;
			DmCrmManualPointChangeRequest request =
					DmCrmManualPointChangeRequest.builder()
							.mobile(mobile)
							.cardNo(cardNo)
							.integral(points)
							.type(1)
							.changeType("1140")
							.remark("积分增加:" + record)
							.integralFlow(integralFlow)
							.sourceChannel("c_brand_mall")
							.build();
			try {
				dmCrmManualPointChangePort.changePoint(request);
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				String masked = DataMasking.maskMobile(mobile);
				log.error(
						"Dam manual point change failed companyId={} userId={} mobile={}",
						companyId,
						userId,
						masked,
						e);
				String msg = e.getMessage();
				if (!StringUtils.hasText(msg)) {
					msg = "达摩CRM积分变更失败";
				}
				throw new ResourceException(msg);
			}
			return;
		}

		syncOpenPlatformPointFirst(companyId, userId, points, plus, JOURNAL_TYPE_ORDER_CANCEL_RETURN, record, "", Map.of());

		if (points != 0) {
			pointMemberMapper.addPointDelta(userId, companyId, points);
		}

		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			if (points == 0) {
				return;
			}
			throw new ResourceException("积分不足");
		}

		int now = (int) Instant.now().getEpochSecond();
		PointMemberLog logRow = new PointMemberLog();
		logRow.setUserId(userId);
		logRow.setCompanyId(companyId);
		logRow.setJournalType(JOURNAL_TYPE_ORDER_CANCEL_RETURN);
		logRow.setPointDesc(record);
		logRow.setIncome(points);
		logRow.setOutcome(0);
		logRow.setOrderId(String.valueOf(orderId));
		logRow.setExternalId("");
		logRow.setOperater("");
		logRow.setOperaterRemark("");
		logRow.setCreated(now);
		logRow.setUpdated(now);
		pointMemberLogMapper.insert(logRow);

		maybePushShuyunPoint(
				companyId, userId, points, plus, JOURNAL_TYPE_ORDER_CANCEL_RETURN, record, String.valueOf(orderId), Map.of());
	}

	@Transactional(rollbackFor = Exception.class)
	public void addPointForAftersalesRefund(
			long userId,
			long companyId,
			int refundPoint,
			long orderId,
			long refundBn,
			@SuppressWarnings("unused") Long aftersalesBn) {
		if (refundPoint <= 0) {
			return;
		}
		boolean plus = true;
		String record = "退款单号:" + refundBn;
		if (shouldUseDmPointIntegration(companyId)) {
			Map<String, Object> memberInfo = pointMemberDmPointMemberInfoReadPort.getMemberInfo(userId, companyId);
			Object uidObj = memberInfo.get("user_id");
			if (uidObj == null || !StringUtils.hasText(String.valueOf(uidObj))) {
				throw new ResourceException("未查询到相关会员信息");
			}
			String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
			if (!StringUtils.hasText(mobile)) {
				throw new ResourceException("未查询到相关会员信息");
			}
			Object cardNoObj = memberInfo.get("dm_card_no");
			String cardNo = cardNoObj == null ? null : String.valueOf(cardNoObj);

			long flowTs = Instant.now().getEpochSecond();
			String integralFlow = mobile + "_" + flowTs;
			DmCrmManualPointChangeRequest request =
					DmCrmManualPointChangeRequest.builder()
							.mobile(mobile)
							.cardNo(cardNo)
							.integral(refundPoint)
							.type(1)
							.changeType("1140")
							.remark("积分增加:" + record)
							.integralFlow(integralFlow)
							.sourceChannel("c_brand_mall")
							.build();
			try {
				dmCrmManualPointChangePort.changePoint(request);
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				String masked = DataMasking.maskMobile(mobile);
				log.error(
						"Dam manual point change failed companyId={} userId={} mobile={}",
						companyId,
						userId,
						masked,
						e);
				String msg = e.getMessage();
				if (!StringUtils.hasText(msg)) {
					msg = "达摩CRM积分变更失败";
				}
				throw new ResourceException(msg);
			}
			return;
		}

		syncOpenPlatformPointFirst(
				companyId,
				userId,
				refundPoint,
				plus,
				JOURNAL_TYPE_REFUND_RETURN,
				record,
				String.valueOf(orderId),
				Map.of());

		pointMemberMapper.addPointDelta(userId, companyId, refundPoint);

		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("积分不足");
		}
		long remainingLong = row.getPoint() != null ? row.getPoint() : 0L;
		if (remainingLong > Integer.MAX_VALUE) {
			remainingLong = Integer.MAX_VALUE;
		}
		int remaining = (int) remainingLong;

		String pointDescPrefix = StringUtils.hasText(record) ? record : "无记录";
		String pointDesc = pointDescPrefix + "，当前剩余积分：" + remaining;

		int now = (int) Instant.now().getEpochSecond();
		PointMemberLog logRow = new PointMemberLog();
		logRow.setUserId(userId);
		logRow.setCompanyId(companyId);
		logRow.setJournalType(JOURNAL_TYPE_REFUND_RETURN);
		logRow.setPointDesc(pointDesc);
		logRow.setIncome(refundPoint);
		logRow.setOutcome(0);
		logRow.setOrderId(String.valueOf(orderId));
		logRow.setExternalId("");
		logRow.setOperater("");
		logRow.setOperaterRemark("");
		logRow.setCreated(now);
		logRow.setUpdated(now);
		pointMemberLogMapper.insert(logRow);

		maybePushShuyunPoint(
				companyId,
				userId,
				refundPoint,
				plus,
				JOURNAL_TYPE_REFUND_RETURN,
				record,
				String.valueOf(orderId),
				Map.of());
	}

	public void addPointForTurntableDrawCost(long userId, long companyId, int point) {
		addPointForTurntableDrawCost(userId, companyId, point, null);
	}

	@Transactional(rollbackFor = Exception.class)
	public void addPointForTurntableDrawCost(long userId, long companyId, int point, String requestId) {
		if (StringUtils.hasText(requestId)
				&& queryTurntableDrawCostResult(companyId, userId, requestId) == TurntableDrawCostQueryResult.SUCCESS) {
			return;
		}
		boolean plus = false;
		String record = "大转盘抽奖扣除";
		String orderIdForLog = normalizeTurntableRequestOrderId(requestId);
		if (shouldUseDmPointIntegration(companyId)) {
			changeDmPoint(userId, companyId, point, false, record);
			insertTurntablePointLog(userId, companyId, 0, point, orderIdForLog, record, null);
			return;
		}

		boolean skipLocalBalance =
				syncOpenPlatformPointFirst(companyId, userId, point, plus, JOURNAL_TYPE_TURNTABLE_WIN, record, "", Map.of());

		if (point != 0 && !skipLocalBalance) {
			int rows = pointMemberMapper.subtractPointIfEnough(userId, companyId, point);
			if (rows == 0) {
				throw new ResourceException("积分不足");
			}
		}

		PointMember row =
				pointMemberMapper.selectOne(
						new LambdaQueryWrapper<PointMember>()
								.eq(PointMember::getUserId, userId)
								.eq(PointMember::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null && !skipLocalBalance) {
			if (point == 0) {
				maybePushShuyunPoint(companyId, userId, point, plus, JOURNAL_TYPE_TURNTABLE_WIN, record, "", Map.of());
				return;
			}
			throw new ResourceException("积分不足");
		}
		Integer remaining = skipLocalBalance || row == null ? null : remainingOf(row);
		insertTurntablePointLog(userId, companyId, 0, point, orderIdForLog, record, remaining);
		maybePushShuyunPoint(companyId, userId, point, plus, JOURNAL_TYPE_TURNTABLE_WIN, record, "", Map.of());
	}

	/**
	 * 按 requestId 查询大转盘扣积分是否已成功落本地流水（PRD §6.4.1）。
	 *
	 * @see TurntableDrawCostQueryResult 补偿语义说明
	 */
	public TurntableDrawCostQueryResult queryTurntableDrawCostResult(
			long companyId, long userId, String requestId) {
		return queryTurntableJournal(companyId, userId, requestId, true);
	}

	public TurntableDrawCostQueryResult queryTurntableWinResult(
			long companyId, long userId, String requestId) {
		return queryTurntableJournal(companyId, userId, requestId, false);
	}

	private TurntableDrawCostQueryResult queryTurntableJournal(
			long companyId, long userId, String requestId, boolean cost) {
		if (!StringUtils.hasText(requestId)) {
			return TurntableDrawCostQueryResult.ABSENT;
		}
		String orderId = normalizeTurntableRequestOrderId(requestId);
		LambdaQueryWrapper<PointMemberLog> w =
				new LambdaQueryWrapper<PointMemberLog>()
						.eq(PointMemberLog::getCompanyId, companyId)
						.eq(PointMemberLog::getUserId, userId)
						.eq(PointMemberLog::getOrderId, orderId)
						.eq(PointMemberLog::getJournalType, JOURNAL_TYPE_TURNTABLE_WIN);
		if (cost) {
			w.gt(PointMemberLog::getOutcome, 0);
		} else {
			w.gt(PointMemberLog::getIncome, 0);
		}
		PointMemberLog row = pointMemberLogMapper.selectOne(w.last("LIMIT 1"));
		return row != null ? TurntableDrawCostQueryResult.SUCCESS : TurntableDrawCostQueryResult.ABSENT;
	}

	private void changeDmPoint(long userId, long companyId, int point, boolean plus, String record) {
		Map<String, Object> memberInfo = pointMemberDmPointMemberInfoReadPort.getMemberInfo(userId, companyId);
		Object uidObj = memberInfo.get("user_id");
		if (uidObj == null || !StringUtils.hasText(String.valueOf(uidObj))) {
			throw new ResourceException("未查询到相关会员信息");
		}
		String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
		if (!StringUtils.hasText(mobile)) {
			throw new ResourceException("未查询到相关会员信息");
		}
		Object cardNoObj = memberInfo.get("dm_card_no");
		String cardNo = cardNoObj == null ? null : String.valueOf(cardNoObj);
		long flowTs = Instant.now().getEpochSecond();
		String integralFlow = mobile + "_" + flowTs;
		DmCrmManualPointChangeRequest request =
				DmCrmManualPointChangeRequest.builder()
						.mobile(mobile)
						.cardNo(cardNo)
						.integral(plus ? point : -point)
						.type(plus ? 1 : 0)
						.changeType(plus ? "1140" : "1240")
						.remark((plus ? "积分增加:" : "积分扣减:") + record)
						.integralFlow(integralFlow)
						.sourceChannel("c_brand_mall")
						.build();
		try {
			dmCrmManualPointChangePort.changePoint(request);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			String masked = DataMasking.maskMobile(mobile);
			log.error(
					"Dam manual point change failed companyId={} userId={} mobile={}",
					companyId,
					userId,
					masked,
					e);
			String msg = e.getMessage();
			if (!StringUtils.hasText(msg)) {
				msg = "达摩CRM积分变更失败";
			}
			throw new ResourceException(msg);
		}
	}

	private void insertTurntablePointLog(
			long userId,
			long companyId,
			int income,
			int outcome,
			String orderId,
			String record,
			Integer remaining) {
		String pointDescPrefix = StringUtils.hasText(record) ? record : "无记录";
		String pointDesc =
				remaining == null
						? pointDescPrefix + "，剩余积分以外部账户为准"
						: pointDescPrefix + "，当前剩余积分：" + remaining;
		int now = (int) Instant.now().getEpochSecond();
		PointMemberLog logRow = new PointMemberLog();
		logRow.setUserId(userId);
		logRow.setCompanyId(companyId);
		logRow.setJournalType(JOURNAL_TYPE_TURNTABLE_WIN);
		logRow.setPointDesc(pointDesc);
		logRow.setIncome(income);
		logRow.setOutcome(outcome);
		logRow.setOrderId(orderId == null ? "" : orderId);
		logRow.setExternalId("");
		logRow.setOperater("");
		logRow.setOperaterRemark("");
		logRow.setCreated(now);
		logRow.setUpdated(now);
		pointMemberLogMapper.insert(logRow);
	}

	private static int remainingOf(PointMember row) {
		long remainingLong = row.getPoint() != null ? row.getPoint() : 0L;
		if (remainingLong > Integer.MAX_VALUE) {
			remainingLong = Integer.MAX_VALUE;
		}
		return (int) remainingLong;
	}

	private static String normalizeTurntableRequestOrderId(String requestId) {
		if (!StringUtils.hasText(requestId)) {
			return "";
		}
		String trimmed = requestId.trim();
		return trimmed.length() <= 64 ? trimmed : trimmed.substring(0, 64);
	}
}
