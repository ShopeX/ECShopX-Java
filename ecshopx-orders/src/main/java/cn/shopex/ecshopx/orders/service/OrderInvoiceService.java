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

package cn.shopex.ecshopx.orders.service;

import cn.shopex.ecshopx.common.dispatch.InvoiceCreateJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.InvoiceQueryJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.InvoiceRedQueryJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.orders.OrderInvoiceRedFromAftersalesPort;
import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import cn.shopex.ecshopx.orders.service.invoice.InvoiceSettingMaps;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 周期开票入队。内层对 {@code applyNodeArr} 的迭代复用并累加查询条件、未在分支间互斥清理对端时间键；内层 list 不单独按
 * 「当前公司 id」收窄。与 ScheduleCreateInvoice 分析契约一致。不得首版自行改为按租户单查，除非有独立产品决策。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderInvoiceService {

	private final OrderInvoiceMapper orderInvoiceMapper;
	private final InvoiceSettingRedisService invoiceSettingRedisService;
	private final InvoiceCreateJobDispatchPublisher invoiceCreateJobDispatchPublisher;
	private final InvoiceQueryJobDispatchPublisher invoiceQueryJobDispatchPublisher;
	private final InvoiceRedQueryJobDispatchPublisher invoiceRedQueryJobDispatchPublisher;
	private final OrderInvoiceRedFromAftersalesPort orderInvoiceRedFromAftersalesPort;

	/** @return 本执行中入队成功次数。 */
	public int scheduleCreateInvoice() {
		log.info("[OrderInvoiceService][scheduleCreateInvoice] 开始执行定时开票任务");

		LambdaQueryWrapper<OrderInvoice> base =
				new LambdaQueryWrapper<OrderInvoice>()
						.eq(OrderInvoice::getInvoiceStatus, "pending")
						.eq(OrderInvoice::getInvoiceMethod, "online");
		List<OrderInvoice> firstBatch = orderInvoiceMapper.selectList(base);
		List<Long> companyIds = firstBatch.stream()
				.map(OrderInvoice::getCompanyId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();

		Map<Long, Object> applyNodeArr = new LinkedHashMap<>();
		for (Long companyId : companyIds) {
			Map<String, Object> setting = InvoiceSettingMaps.asSettingMap(invoiceSettingRedisService.getInvoiceSetting(companyId));
			if (setting.containsKey("apply_node")) {
				applyNodeArr.put(companyId, setting.get("apply_node"));
			}
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		boolean endConstraint = false;
		int endLt = 0;
		boolean closeConstraint = false;
		int closeLt = 0;

		int dispatched = 0;
		for (Map.Entry<Long, Object> e : applyNodeArr.entrySet()) {
			Object node = e.getValue();
			if (isApplyNodeOne(node)) {
				endConstraint = true;
				endLt = now;
			} else {
				closeConstraint = true;
				closeLt = now;
			}
			LambdaQueryWrapper<OrderInvoice> w =
					new LambdaQueryWrapper<OrderInvoice>()
							.eq(OrderInvoice::getInvoiceStatus, "pending")
							.eq(OrderInvoice::getInvoiceMethod, "online");
			if (endConstraint) {
				w.lt(OrderInvoice::getEndTime, endLt).gt(OrderInvoice::getEndTime, 0);
			}
			if (closeConstraint) {
				w.lt(OrderInvoice::getCloseAftersalesTime, closeLt)
						.gt(OrderInvoice::getCloseAftersalesTime, 0);
			}
			List<OrderInvoice> pending = orderInvoiceMapper.selectList(w);
			log.info(
					"[OrderInvoiceService][scheduleCreateInvoice] 找到 {} 个待处理发票申请", pending.size());
			for (OrderInvoice inv : pending) {
				try {
					invoiceCreateJobDispatchPublisher.publish(toJobDataMap(inv));
					dispatched++;
					log.info(
							"[OrderInvoiceService][scheduleCreateInvoice] 发票申请 ID: {} 已推送到队列", inv.getId());
				} catch (Exception ex) {
					log.error(
							"[OrderInvoiceService][scheduleCreateInvoice] 处理发票申请失败 ID: {}, 错误: {}",
							inv.getId(),
							ex.getMessage());
				}
			}
		}

		log.info("[OrderInvoiceService][scheduleCreateInvoice] 定时开票任务执行完成");
		return dispatched;
	}

	/** @return 本执行中入队成功次数。 */
	public int scheduleQueryInvoice() {
		log.info("[OrderInvoiceService][scheduleQueryInvoice] 开始执行定时查询开票结果任务");
		// Stale threshold: update_time must be at least 180 seconds before now (3-minute window for in-progress rows).
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		int staleBeforeEpochSecond = nowSec - 180;
		LambdaQueryWrapper<OrderInvoice> w = new LambdaQueryWrapper<OrderInvoice>()
				.eq(OrderInvoice::getInvoiceStatus, "inProgress")
				.lt(OrderInvoice::getUpdateTime, staleBeforeEpochSecond);
		List<OrderInvoice> list = orderInvoiceMapper.selectList(w);
		if (list == null || list.isEmpty()) {
			log.info("[OrderInvoiceService][scheduleQueryInvoice] 没有需要查询的发票申请");
			return 0;
		}
		log.info("[OrderInvoiceService][scheduleQueryInvoice] 找到 {} 个需要查询的发票申请", list.size());
		int dispatched = 0;
		for (OrderInvoice inv : list) {
			try {
				invoiceQueryJobDispatchPublisher.publish(toQueryJobDataMap(inv));
				dispatched++;
				log.info(
						"[OrderInvoiceService][scheduleQueryInvoice] jobData invoice_id={} company_id={} invoice_apply_bn={} order_id={} 已推送到队列",
						inv.getId(),
						inv.getCompanyId(),
						inv.getInvoiceApplyBn(),
						inv.getOrderId());
			} catch (Exception ex) {
				log.error(
						"[OrderInvoiceService][scheduleQueryInvoice] 处理发票查询入队失败 ID: {}, 错误: {}",
						inv.getId(),
						ex.getMessage());
			}
		}
		log.info("[OrderInvoiceService][scheduleQueryInvoice] 定时查询开票结果任务执行完成");
		return dispatched;
	}

	/** @return 本执行中入队成功次数。 */
	public int scheduleQueryInvoiceRed() {
		log.info("[OrderInvoiceService][scheduleQueryInvoiceRed] 开始执行红冲定时查询任务");
		LambdaQueryWrapper<OrderInvoice> w = new LambdaQueryWrapper<OrderInvoice>()
				.eq(OrderInvoice::getInvoiceStatus, "waste")
				.isNull(OrderInvoice::getInvoiceFileUrlRed);
		log.info(
				"[OrderInvoiceService][scheduleQueryInvoiceRed] 查询条件: invoice_status=waste, invoice_file_url_red IS NULL");
		List<OrderInvoice> list = orderInvoiceMapper.selectList(w);
		log.info("[OrderInvoiceService][scheduleQueryInvoiceRed] 查询结果: 共 {} 条", list == null ? 0 : list.size());
		if (list == null || list.isEmpty()) {
			log.info("[OrderInvoiceService][scheduleQueryInvoiceRed] 没有待处理的红冲发票");
			return 0;
		}
		log.info("[OrderInvoiceService][scheduleQueryInvoiceRed] 找到 {} 个待处理的红冲发票", list.size());
		int dispatched = 0;
		for (OrderInvoice inv : list) {
			try {
				if (!StringUtils.hasText(inv.getRedSerialNo())) {
					log.warn(
							"[OrderInvoiceService][scheduleQueryInvoiceRed] 发票缺少红冲流水号，跳过处理, invoice_id={}",
							inv.getId());
					continue;
				}
				invoiceRedQueryJobDispatchPublisher.publish(toInvoiceRedQueryJobDataMap(inv));
				dispatched++;
				log.info("[OrderInvoiceService][scheduleQueryInvoiceRed] 红冲发票 ID: {} 已推送到队列", inv.getId());
			} catch (Exception ex) {
				log.error(
						"[OrderInvoiceService][scheduleQueryInvoiceRed] 处理红冲发票失败 ID: {}, 错误: {}",
						inv.getId(),
						ex.getMessage());
			}
		}
		log.info("[OrderInvoiceService][scheduleQueryInvoiceRed] 红冲定时查询任务执行完成");
		return dispatched;
	}

	/** 对位售后退款成功后异步冲红 Job：按发票当前状态分支处理。 */
	public void executeInvoiceRedJob(Map<String, Object> jobData) {
		if (jobData == null || jobData.isEmpty()) {
			log.warn("[InvoiceRedJob][handle] empty jobData");
			return;
		}
		Long companyId = extractLongNullable(jobData.get("company_id"));
		String orderIdStr = normalizeOrderId(jobData.get("order_id"));
		if (companyId == null || !StringUtils.hasText(orderIdStr)) {
			log.warn("[InvoiceRedJob][handle] missing company_id or order_id keys={}", jobData.keySet());
			return;
		}
		OrderInvoice inv =
				orderInvoiceMapper.selectOne(
						new LambdaQueryWrapper<OrderInvoice>()
								.eq(OrderInvoice::getCompanyId, companyId)
								.eq(OrderInvoice::getOrderId, orderIdStr)
								.last("LIMIT 1"));
		if (inv == null) {
			log.info("[InvoiceRedJob][handle] invoice not found company_id={} order_id={}", companyId, orderIdStr);
			return;
		}
		String status = inv.getInvoiceStatus();
		if (status == null) {
			log.debug("[InvoiceRedJob][handle] invoice_id={} null invoice_status", inv.getId());
			return;
		}
		switch (status) {
			case "cancel" ->
					log.info("[InvoiceRedJob][handle] invoice_status=cancel skip invoice_id={}", inv.getId());
			case "pending" -> cancelInvoicePendingBranch(inv);
			case "success" -> orderInvoiceRedFromAftersalesPort.redInvoice(jobData);
			default ->
					log.debug(
							"[InvoiceRedJob][handle] invoice_status={} no explicit branch invoice_id={}",
							status,
							inv.getId());
		}
	}

	private void cancelInvoicePendingBranch(OrderInvoice inv) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<OrderInvoice> uw =
				new LambdaUpdateWrapper<OrderInvoice>()
						.eq(OrderInvoice::getId, inv.getId())
						.set(OrderInvoice::getInvoiceStatus, "cancel")
						.set(OrderInvoice::getUpdateTime, now);
		orderInvoiceMapper.update(null, uw);
		log.info("[InvoiceRedJob][handle] invoice_status pending→cancel invoice_id={}", inv.getId());
	}

	private static Long extractLongNullable(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String normalizeOrderId(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof String s) {
			return s.trim();
		}
		return String.valueOf(raw).trim();
	}

	private static Map<String, Object> toInvoiceRedQueryJobDataMap(OrderInvoice inv) {
		Map<String, Object> m = new LinkedHashMap<>(8);
		m.put("company_id", inv.getCompanyId());
		m.put("order_id", inv.getOrderId());
		m.put("id", inv.getId());
		m.put("red_confirm_serial_no", inv.getRedSerialNo());
		m.put("entry_identity", "0");
		m.put("type", "red");
		return m;
	}

	private static Map<String, Object> toQueryJobDataMap(OrderInvoice inv) {
		Map<String, Object> m = new LinkedHashMap<>(8);
		m.put("invoice_id", inv.getId());
		m.put("company_id", inv.getCompanyId());
		m.put("invoice_apply_bn", inv.getInvoiceApplyBn());
		m.put("order_id", inv.getOrderId());
		return m;
	}

	/** 与 <code>apply_node</code> 为 {@code 1} 或 {@code "1"} 的判定方式一致。 */
	private static boolean isApplyNodeOne(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.intValue() == 1;
		}
		return "1".equals(String.valueOf(v).trim());
	}

	private static Map<String, Object> toJobDataMap(OrderInvoice inv) {
		Map<String, Object> m = new LinkedHashMap<>(16);
		m.put("invoice_id", inv.getId());
		m.put("company_id", inv.getCompanyId());
		m.put("order_id", inv.getOrderId());
		m.put("invoice_type", inv.getInvoiceType());
		m.put("invoice_type_code", inv.getInvoiceTypeCode());
		m.put("company_title", inv.getCompanyTitle());
		m.put("company_tax_number", inv.getCompanyTaxNumber());
		m.put("company_address", inv.getCompanyAddress());
		m.put("company_telephone", inv.getCompanyTelephone());
		m.put("bank_name", inv.getBankName());
		m.put("bank_account", inv.getBankAccount());
		m.put("email", inv.getEmail());
		m.put("mobile", inv.getMobile());
		return m;
	}
}
