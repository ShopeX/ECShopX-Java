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

package cn.shopex.ecshopx.reservation.service;

import cn.shopex.ecshopx.common.dispatch.ReservationExpireCancelDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ReservationFinishDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.reservation.domain.ReservationRecord;
import cn.shopex.ecshopx.reservation.domain.ReservationSetting;
import cn.shopex.ecshopx.reservation.mapper.ReservationRecordMapper;
import cn.shopex.ecshopx.reservation.port.ReservationShopDetailPort;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReservationCreateService {

	private final ReservationSettingQueryService reservationSettingQueryService;

	private final ReservationShopDetailPort reservationShopDetailPort;

	private final ReservationCheckService reservationCheckService;

	private final ReservationRecordMapper reservationRecordMapper;

	private final ReservationFinishDispatchPublisher reservationFinishDispatchPublisher;

	private final ReservationAsyncNotifier reservationAsyncNotifier;

	private final ReservationExpireCancelDispatchPublisher reservationExpireCancelDispatchPublisher;

	public ReservationCreateService(
			ReservationSettingQueryService reservationSettingQueryService,
			ReservationShopDetailPort reservationShopDetailPort,
			ReservationCheckService reservationCheckService,
			ReservationRecordMapper reservationRecordMapper,
			ReservationFinishDispatchPublisher reservationFinishDispatchPublisher,
			ReservationAsyncNotifier reservationAsyncNotifier,
			ReservationExpireCancelDispatchPublisher reservationExpireCancelDispatchPublisher) {
		this.reservationSettingQueryService = reservationSettingQueryService;
		this.reservationShopDetailPort = reservationShopDetailPort;
		this.reservationCheckService = reservationCheckService;
		this.reservationRecordMapper = reservationRecordMapper;
		this.reservationFinishDispatchPublisher = reservationFinishDispatchPublisher;
		this.reservationAsyncNotifier = reservationAsyncNotifier;
		this.reservationExpireCancelDispatchPublisher = reservationExpireCancelDispatchPublisher;
	}

	public boolean createReservation(Map<String, Object> paramsData) {
		long companyId = longVal(paramsData.get("company_id"));
		ReservationSetting setting =
				reservationSettingQueryService.findByCompanyId(companyId).orElse(null);
		if (setting == null || setting.getReservationMode() == null) {
			throw new ResourceException("预约未配置");
		}

		reservationShopDetailPort.getShopsDetail(longVal(paramsData.get("shop_id")), companyId);

		if (setting.getReservationMode() == 1) {
			reservationCheckService.checkAndMaybeAssignResource(companyId, paramsData);
		}

		ReservationRecord record = toRecord(paramsData);
		int now = (int) (System.currentTimeMillis() / 1000L);
		record.setCreated(now);
		record.setUpdated(now);
		int ins = reservationRecordMapper.insert(record);
		if (ins <= 0) {
			throw new ResourceException("预约失败");
		}

		Map<String, Object> resultMap = recordToEventResult(record);
		Map<String, Object> postCopy = new LinkedHashMap<>(paramsData);
		Map<String, Object> settingLite = settingToLiteMap(setting);
		Map<String, Object> eventPayload = new LinkedHashMap<>();
		eventPayload.put("postdata", postCopy);
		eventPayload.put("result", resultMap);
		eventPayload.put("setting_data", settingLite);
		reservationFinishDispatchPublisher.publish(eventPayload);

		Map<String, Object> smsNoticePayload = buildReservationSendSmsNoticePayload(record, postCopy, settingLite);
		reservationAsyncNotifier.afterReservationCreated(companyId, smsNoticePayload, setting);
		reservationExpireCancelDispatchPublisher.publishExpireCancelAfterCreate(companyId, smsNoticePayload);
		return true;
	}

	private static Map<String, Object> buildReservationSendSmsNoticePayload(
			ReservationRecord record, Map<String, Object> paramsData, Map<String, Object> settingLite) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", record.getCompanyId());
		m.put("shop_id", record.getShopId());
		m.put("to_shop_time", record.getToShopTime());
		m.put("shop_name", record.getShopName());
		m.put("rights_name", record.getRightsName());
		m.put("mobile", record.getMobile());
		m.put("record_id", record.getRecordId());
		m.put("status", record.getStatus());
		if (record.getUserName() != null) {
			m.put("user_name", record.getUserName());
		}
		String[] passthroughKeys = {"shop_address", "telephone", "address", "shop_tel"};
		for (String k : passthroughKeys) {
			if (paramsData.containsKey(k) && paramsData.get(k) != null) {
				m.put(k, paramsData.get(k));
			}
		}
		m.put("setting_data", new LinkedHashMap<>(settingLite));
		return m;
	}

	private static Map<String, Object> settingToLiteMap(ReservationSetting s) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("reservationMode", s.getReservationMode());
		m.put("timeInterval", s.getTimeInterval());
		m.put("smsDelay", s.getSmsDelay());
		return m;
	}

	private ReservationRecord toRecord(Map<String, Object> p) {
		ReservationRecord r = new ReservationRecord();
		r.setCompanyId(longVal(p.get("company_id")));
		r.setShopId(longVal(p.get("shop_id")));
		if (p.containsKey("shop_name")) {
			r.setShopName(Objects.toString(p.get("shop_name"), null));
		}
		String dateDay = Objects.toString(p.get("date_day"), "");
		int agreement = parseAgreementDateEpoch(dateDay);
		r.setAgreementDate(agreement);
		String beginTime = Objects.toString(p.get("begin_time"), "");
		r.setBeginTime(beginTime);
		if (p.containsKey("end_time")) {
			r.setEndTime(Objects.toString(p.get("end_time"), null));
		}
		r.setStatus(Objects.toString(p.get("status"), "system"));
		r.setNum(1);
		if (p.containsKey("user_id") && p.get("user_id") != null) {
			Object uid = p.get("user_id");
			if (uid instanceof Number n) {
				r.setUserId(n.longValue());
			} else if (!uid.toString().isBlank()) {
				r.setUserId(Long.parseLong(uid.toString().trim()));
			}
		}
		if (p.containsKey("user_name")) {
			r.setUserName(Objects.toString(p.get("user_name"), null));
		}
		if (p.containsKey("sex")) {
			Object sx = p.get("sex");
			if (sx instanceof Number n) {
				r.setSex(n.intValue());
			} else if (sx != null && !sx.toString().isBlank()) {
				r.setSex(Integer.parseInt(sx.toString().trim()));
			}
		}
		if (p.containsKey("mobile")) {
			r.setMobile(Objects.toString(p.get("mobile"), null));
		}
		r.setResourceLevelId(longVal(p.get("resource_level_id")));
		if (p.containsKey("resource_level_name")) {
			r.setResourceLevelName(Objects.toString(p.get("resource_level_name"), null));
		}
		if (p.containsKey("label_id") && p.get("label_id") != null) {
			r.setLabelId(longValOrNull(p.get("label_id")));
		}
		if (p.containsKey("label_name")) {
			r.setLabelName(Objects.toString(p.get("label_name"), null));
		}
		if (p.containsKey("rights_id") && p.get("rights_id") != null) {
			r.setRightsId(longValOrNull(p.get("rights_id")));
		}
		if (p.containsKey("rights_name")) {
			r.setRightsName(Objects.toString(p.get("rights_name"), null));
		}
		r.setToShopTime((int) toShopTimeUnix(dateDay, beginTime));
		return r;
	}

	private static Map<String, Object> recordToEventResult(ReservationRecord rec) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("recordId", rec.getRecordId());
		m.put("companyId", rec.getCompanyId());
		m.put("shopId", rec.getShopId());
		m.put("agreementDate", rec.getAgreementDate());
		m.put("beginTime", rec.getBeginTime());
		m.put("endTime", rec.getEndTime());
		m.put("to_shop_time", rec.getToShopTime());
		m.put("status", rec.getStatus());
		m.put("num", rec.getNum());
		m.put("userId", rec.getUserId());
		m.put("resourceLevelId", rec.getResourceLevelId());
		m.put("resourceLevelName", rec.getResourceLevelName());
		m.put("rightsId", rec.getRightsId());
		m.put("labelId", rec.getLabelId());
		return m;
	}

	private static int parseAgreementDateEpoch(String ymd) {
		LocalDate d = LocalDate.parse(ymd);
		return (int) d.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
	}

	private static long toShopTimeUnix(String ymd, String beginTime) {
		LocalDate d = LocalDate.parse(ymd);
		LocalTime t = parseTimeFlexible(beginTime.trim());
		return d.atTime(t).atZone(ZoneId.systemDefault()).toEpochSecond();
	}

	private static LocalTime parseTimeFlexible(String s) {
		String[] patterns = {"H:mm", "HH:mm", "H:m", "HH:m"};
		for (String p : patterns) {
			try {
				return LocalTime.parse(s, DateTimeFormatter.ofPattern(p));
			} catch (Exception ignored) {
				// try next
			}
		}
		throw new ResourceException("时段必选");
	}

	private static long longVal(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(Objects.toString(v, "0").trim());
	}

	private static Long longValOrNull(Object v) {
		if (v == null) {
			return null;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		return Long.parseLong(s);
	}
}
