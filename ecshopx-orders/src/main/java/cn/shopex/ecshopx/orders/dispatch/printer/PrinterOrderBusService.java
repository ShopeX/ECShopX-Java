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

package cn.shopex.ecshopx.orders.dispatch.printer;

import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.espier.integration.yilianyun.YilianyunPrintCommand;
import cn.shopex.ecshopx.espier.integration.yilianyun.YilianyunPrintPort;
import cn.shopex.ecshopx.espier.service.printer.PrinterCompanyConfigApplicationService;
import cn.shopex.ecshopx.espier.service.printer.PrinterShopApplicationService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelZiti;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelZitiMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class PrinterOrderBusService {

	private static final String YILIANYUN_TYPE = "yilianyun";
	private static final String RECEIPT_TYPE_LOGISTICS = "logistics";
	private static final int PRINTER_LIST_PAGE_SIZE = 500;

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrdersRelZitiMapper normalOrdersRelZitiMapper;
	private final OperatorsMapper operatorsMapper;
	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;
	private final PrinterCompanyConfigApplicationService printerCompanyConfigApplicationService;
	private final PrinterShopApplicationService printerShopApplicationService;
	private final YilianyunPrintPort yilianyunPrintPort;
	private final YilianyunShippingTicketAssembler ticketAssembler = new YilianyunShippingTicketAssembler();
	private final YilianyunShippingTicketFormatter ticketFormatter = new YilianyunShippingTicketFormatter();

	public PrinterOrderBusService(
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			NormalOrdersRelZitiMapper normalOrdersRelZitiMapper,
			OperatorsMapper operatorsMapper,
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort,
			PrinterCompanyConfigApplicationService printerCompanyConfigApplicationService,
			PrinterShopApplicationService printerShopApplicationService,
			YilianyunPrintPort yilianyunPrintPort) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.normalOrdersRelZitiMapper = normalOrdersRelZitiMapper;
		this.operatorsMapper = operatorsMapper;
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
		this.printerCompanyConfigApplicationService = printerCompanyConfigApplicationService;
		this.printerShopApplicationService = printerShopApplicationService;
		this.yilianyunPrintPort = yilianyunPrintPort;
	}

	public void handleTradeFinishRow(Map<String, Object> tradeRow) {
		Long orderId = parsePositiveLong(first(tradeRow, "order_id", "orderId"));
		Long companyId = parsePositiveLong(first(tradeRow, "company_id", "companyId"));
		Long distributorId = parsePositiveLong(first(tradeRow, "distributor_id", "distributorId"));
		if (orderId == null || companyId == null || distributorId == null) {
			return;
		}

		OrderAssociations orderAssoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getOrderId, orderId)
								.eq(OrderAssociations::getCompanyId, companyId));
		if (orderAssoc == null) {
			return;
		}

		NormalOrders order = loadNormalOrder(companyId, orderId);
		if (isLogisticsReceipt(order)) {
			return;
		}

		Map<String, Object> printerCompanyCfg =
				printerCompanyConfigApplicationService.info(companyId, YILIANYUN_TYPE);
		if (!isYilianyunOpen(printerCompanyCfg)) {
			return;
		}

		String clientId = text(printerCompanyCfg.get("app_id"));
		String clientSecret = text(printerCompanyCfg.get("app_key"));
		if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
			return;
		}

		Map<String, Object> printerListPage =
				printerShopApplicationService.lists(companyId, 1, PRINTER_LIST_PAGE_SIZE, Locale.CHINA.toLanguageTag());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> printers =
				(List<Map<String, Object>>)
						printerListPage.getOrDefault("list", Collections.emptyList());
		String distKey = String.valueOf(distributorId);
		Map<String, Object> deviceRow = null;
		for (Map<String, Object> row : printers) {
			Object did = row.get("distributor_id");
			if (Objects.equals(String.valueOf(did), distKey)) {
				deviceRow = row;
				break;
			}
		}
		if (deviceRow == null) {
			return;
		}
		String machineCode = text(deviceRow.get("app_terminal"));
		if (!StringUtils.hasText(machineCode)) {
			return;
		}

		String content =
				buildTicketContent(orderAssoc, order, tradeRow, printerCompanyCfg, companyId, orderId, distributorId);
		if (!StringUtils.hasText(content)) {
			return;
		}
		String originId = buildOriginId(orderId, companyId);

		YilianyunPrintCommand command =
				new YilianyunPrintCommand(machineCode, content, originId, clientId, clientSecret);
		try {
			yilianyunPrintPort.printTicket(command);
		} catch (RuntimeException e) {
			log.debug(
					"Cloud printer ticket submission failed companyId={} orderId={} distributorId={}",
					companyId,
					orderId,
					distributorId,
					e);
		}
	}

	private String buildTicketContent(
			OrderAssociations orderAssoc,
			NormalOrders order,
			Map<String, Object> tradeRow,
			Map<String, Object> printerCompanyCfg,
			long companyId,
			long orderId,
			long distributorId) {
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getOrderId, orderId)
								.eq(NormalOrdersItems::getCompanyId, companyId));
		NormalOrdersRelZiti ziti = null;
		if (order != null && "ziti".equalsIgnoreCase(text(order.getReceiptType()))) {
			ziti =
					normalOrdersRelZitiMapper.selectOne(
							new LambdaQueryWrapper<NormalOrdersRelZiti>()
									.eq(NormalOrdersRelZiti::getOrderId, orderId)
									.eq(NormalOrdersRelZiti::getCompanyId, companyId)
									.last("LIMIT 1"));
		}
		Map<String, Object> distributor = loadDistributor(companyId, distributorId);
		Operators operator = loadOperator(companyId, order);
		Long payFee = parseLongAllowZero(first(tradeRow, "pay_fee", "payFee"));
		String payType = text(first(tradeRow, "pay_type", "payType"));
		boolean hideReceiver = isHideReceiver(printerCompanyCfg);
		YilianyunShippingTicketModel model =
				ticketAssembler.assemble(
						orderAssoc,
						order,
						items,
						ziti,
						distributor,
						operator,
						payFee,
						payType,
						hideReceiver);
		return ticketFormatter.format(model);
	}

	private NormalOrders loadNormalOrder(long companyId, long orderId) {
		return normalOrdersMapper.selectOne(
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getOrderId, orderId)
						.eq(NormalOrders::getCompanyId, companyId)
						.last("LIMIT 1"));
	}

	private Map<String, Object> loadDistributor(long companyId, long distributorId) {
		try {
			if (distributorId > 0L) {
				return adminOrderDetailDistributionSupportPort.getDistributorInfoFormatted(
						companyId, distributorId);
			}
			return adminOrderDetailDistributionSupportPort.getDistributorSelfSimpleInfo(companyId);
		} catch (RuntimeException e) {
			log.debug("Load distributor for ticket failed companyId={} distributorId={}", companyId, distributorId, e);
			return Collections.emptyMap();
		}
	}

	private Operators loadOperator(long companyId, NormalOrders order) {
		if (order == null || order.getOperatorId() == null || order.getOperatorId() <= 0) {
			return null;
		}
		try {
			return operatorsMapper.selectOne(
					new LambdaQueryWrapper<Operators>()
							.eq(Operators::getOperatorId, order.getOperatorId().longValue())
							.eq(Operators::getCompanyId, companyId)
							.last("LIMIT 1"));
		} catch (RuntimeException e) {
			log.debug("Load operator for ticket failed companyId={} operatorId={}", companyId, order.getOperatorId(), e);
			return null;
		}
	}

	private static String buildOriginId(long orderId, long companyId) {
		String raw = "O" + orderId + "C" + companyId;
		StringBuilder alnum = new StringBuilder();
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
				alnum.append(c);
			}
		}
		if (alnum.isEmpty()) {
			alnum.append('O');
		}
		String s = alnum.toString();
		return s.length() <= 32 ? s : s.substring(0, 32);
	}

	private static boolean isYilianyunOpen(Map<String, Object> cfg) {
		Object v = cfg.get("is_open");
		if (v instanceof Boolean b) {
			return b;
		}
		return "true".equalsIgnoreCase(text(v));
	}

	private static boolean isLogisticsReceipt(NormalOrders order) {
		return order != null && RECEIPT_TYPE_LOGISTICS.equalsIgnoreCase(text(order.getReceiptType()));
	}

	private static boolean isHideReceiver(Map<String, Object> cfg) {
		Object v = cfg.get("is_hide");
		if (v instanceof Boolean b) {
			return b;
		}
		return "true".equalsIgnoreCase(text(v));
	}

	private static Object first(Map<String, Object> m, String a, String b) {
		Object x = m.get(a);
		if (x != null) {
			return x;
		}
		return m.get(b);
	}

	private static String text(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static Long parsePositiveLong(Object raw) {
		Long v = parseLongAllowZero(raw);
		if (v == null || v <= 0L) {
			return null;
		}
		return v;
	}

	private static Long parseLongAllowZero(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
