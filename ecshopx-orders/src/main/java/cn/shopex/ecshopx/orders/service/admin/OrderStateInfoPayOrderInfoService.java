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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.chinaumspay.service.query.ChinaumsPayOrderInfoQueryService;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.payment.OrdersExternalPayParamBuildService;
import cn.shopex.ecshopx.payment.service.orderquery.AdapayPayOrderInfoQueryService;
import cn.shopex.ecshopx.payment.service.orderquery.BsPayPayOrderInfoQueryService;
import cn.shopex.ecshopx.payment.service.orderquery.PaypalPayOrderInfoQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Service
public class OrderStateInfoPayOrderInfoService {

	private static final String WX_ORDER_QUERY_URL = "https://api.mch.weixin.qq.com/pay/orderquery";
	private static final String WX_REFUND_QUERY_URL = "https://api.mch.weixin.qq.com/pay/refundquery";

	private final TradeMapper tradeMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final OrdersExternalPayParamBuildService ordersExternalPayParamBuildService;
	private final AdapayPayOrderInfoQueryService adapayPayOrderInfoQueryService;
	private final BsPayPayOrderInfoQueryService bsPayPayOrderInfoQueryService;
	private final ChinaumsPayOrderInfoQueryService chinaumsPayOrderInfoQueryService;
	private final PaypalPayOrderInfoQueryService paypalPayOrderInfoQueryService;
	private final RestTemplate restTemplate;

	@Value("${ecshopx.ums.order-id-prefix:}")
	private String umsOrderIdPrefix;

	public OrderStateInfoPayOrderInfoService(
			TradeMapper tradeMapper,
			AftersalesRefundMapper aftersalesRefundMapper,
			OrdersExternalPayParamBuildService ordersExternalPayParamBuildService,
			AdapayPayOrderInfoQueryService adapayPayOrderInfoQueryService,
			BsPayPayOrderInfoQueryService bsPayPayOrderInfoQueryService,
			ChinaumsPayOrderInfoQueryService chinaumsPayOrderInfoQueryService,
			PaypalPayOrderInfoQueryService paypalPayOrderInfoQueryService) {
		this.tradeMapper = tradeMapper;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.ordersExternalPayParamBuildService = ordersExternalPayParamBuildService;
		this.adapayPayOrderInfoQueryService = adapayPayOrderInfoQueryService;
		this.bsPayPayOrderInfoQueryService = bsPayPayOrderInfoQueryService;
		this.chinaumsPayOrderInfoQueryService = chinaumsPayOrderInfoQueryService;
		this.paypalPayOrderInfoQueryService = paypalPayOrderInfoQueryService;
		this.restTemplate = new RestTemplate();
	}

	public Object getPayOrderInfo(HttpServletRequest request, String tradeId, String payTypeRaw) {
		long companyId = readCompanyIdFromJwt(request);
		if (tradeId == null || tradeId.isBlank() || "0".equals(tradeId)) {
			throw new ResourceException("无单号!", 422);
		}
		Trade row = tradeMapper.selectById(tradeId);
		long distributorId = parseDistributorId(row);

		String pt = payTypeRaw == null ? "" : payTypeRaw.trim().toLowerCase(Locale.ROOT);
		if (pt.isEmpty()) {
			throw new BadRequestException("无此类型支付！", 400);
		}
		if (!isKnownPayType(pt)) {
			throw new BadRequestException("无此类型支付！", 400);
		}

		if (isWechatChannel(pt)) {
			return queryWechatPayOrder(companyId, distributorId, tradeId, row);
		}
		if (returnsEmptyListChannel(pt)) {
			return Collections.emptyList();
		}
		if ("adapay".equals(pt)) {
			Trade t2 = requireTradeForCompany(tradeId, companyId);
			String tid = t2.getTransactionId() == null ? "" : t2.getTransactionId().trim();
			if (!StringUtils.hasText(tid)) {
				return Collections.emptyList();
			}
			return adapayPayOrderInfoQueryService.queryPayOrderInfoJson(companyId, tradeId, tid);
		}
		if ("bspay".equals(pt)) {
			Trade t2 = requireTradeForCompany(tradeId, companyId);
			String tid = t2.getTransactionId() == null ? "" : t2.getTransactionId().trim();
			if (!StringUtils.hasText(tid)) {
				return Collections.emptyList();
			}
			return bsPayPayOrderInfoQueryService.queryPayOrderInfoJson(
					companyId, tradeId, tid, t2.getBspayReqDate());
		}
		if ("chinaums".equals(pt)) {
			Trade t2 = requireTradeForCompany(tradeId, companyId);
			chinaumsPayOrderInfoQueryService.requireChinaumsPaymentSetting(companyId);
			String tid = t2.getTransactionId() == null ? "" : t2.getTransactionId().trim();
			if (!StringUtils.hasText(tid)) {
				throw new BadRequestException("交易缺少 transaction_id", 400);
			}
			return chinaumsPayOrderInfoQueryService.queryPayOrderInfo(
					companyId, t2.getTradeId(), umsOrderIdPrefix == null ? "" : umsOrderIdPrefix);
		}
		if ("paypal".equals(pt)) {
			return paypalPayOrderInfoQueryService.queryPayOrderInfo(companyId, tradeId);
		}
		throw new BadRequestException("无此类型支付！", 400);
	}

	public Object getRefundOrderInfo(HttpServletRequest request, String refundBnRaw, String payTypeRaw) {
		long companyId = readCompanyIdFromJwt(request);
		if (refundBnRaw == null || refundBnRaw.isBlank() || "0".equals(refundBnRaw.trim())) {
			throw new ResourceException("无单号!", 422);
		}
		String trimmed = refundBnRaw.trim();

		AftersalesRefund row =
				aftersalesRefundMapper.selectOne(
						new LambdaQueryWrapper<AftersalesRefund>()
								.apply("refund_bn = {0}", trimmed)
								.last("LIMIT 1"));
		long distributorId =
				row == null || row.getDistributorId() == null ? 0L : row.getDistributorId();

		String pt = payTypeRaw == null ? "" : payTypeRaw.trim().toLowerCase(Locale.ROOT);
		if (pt.isEmpty()) {
			throw new BadRequestException("无此类型支付！", 500);
		}
		if (!isKnownPayType(pt)) {
			throw new BadRequestException("无此类型支付！", 500);
		}

		if (isWechatChannel(pt)) {
			return queryWechatRefundOrder(companyId, distributorId, trimmed);
		}
		if (returnsEmptyListChannel(pt)) {
			return Collections.emptyList();
		}
		if ("adapay".equals(pt) || "bspay".equals(pt)) {
			return Collections.emptyList();
		}
		if ("chinaums".equals(pt)) {
			chinaumsPayOrderInfoQueryService.requireChinaumsPaymentSetting(companyId);
			AftersalesRefund umsRow =
					aftersalesRefundMapper.selectOne(
							new LambdaQueryWrapper<AftersalesRefund>()
									.eq(AftersalesRefund::getCompanyId, companyId)
									.apply("refund_bn = {0}", trimmed)
									.last("LIMIT 1"));
			if (umsRow == null) {
				throw new BadRequestException("退款单不存在", 400);
			}
			String rid = umsRow.getRefundId() == null ? "" : umsRow.getRefundId().trim();
			if (!StringUtils.hasText(rid)) {
				throw new BadRequestException("退款缺少 refund_id", 400);
			}
			return chinaumsPayOrderInfoQueryService.queryRefundOrderInfo(
					companyId, trimmed, rid, umsOrderIdPrefix == null ? "" : umsOrderIdPrefix);
		}
		if ("paypal".equals(pt)) {
			String rid = row == null || row.getRefundId() == null ? "" : row.getRefundId().trim();
			if (!StringUtils.hasText(rid)) {
				return paypalPayOrderInfoQueryService.paypalQueryErrorEnvelope(
						trimmed, "缺少 refund_id，无法查询 PayPal 退款");
			}
			return paypalPayOrderInfoQueryService.queryRefundOrderInfo(companyId, rid);
		}
		throw new BadRequestException("无此类型支付！", 500);
	}

	private Map<String, String> queryWechatRefundOrder(long companyId, long distributorId, String outRefundNo) {
		String xml;
		try {
			xml = ordersExternalPayParamBuildService.buildWxpayRefundQueryXml(companyId, distributorId, outRefundNo);
		} catch (ResourceException e) {
			String msg = e.getMessage();
			throw new BadRequestException(StringUtils.hasText(msg) ? msg : "不支持支付服务，请联系商家", 400);
		}
		ResponseEntity<String> resp;
		try {
			resp = restTemplate.postForEntity(WX_REFUND_QUERY_URL, wxEntity(xml), String.class);
		} catch (Exception e) {
			throw new BadRequestException("支付失败", 400);
		}
		String respXml = resp.getBody();
		if (!StringUtils.hasText(respXml)) {
			throw new BadRequestException("支付失败", 400);
		}
		return parseWechatOrderQueryXmlToMap(respXml);
	}

	private static long readCompanyIdFromJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}

	private static long parseDistributorId(Trade row) {
		if (row == null || row.getDistributorId() == null || row.getDistributorId().isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(row.getDistributorId().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private Trade requireTradeForCompany(String tradeId, long companyId) {
		Trade t =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getTradeId, tradeId)
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.last("LIMIT 1"));
		if (t == null) {
			throw new BadRequestException("交易ID不存在", 400);
		}
		return t;
	}

	private Map<String, String> queryWechatPayOrder(long companyId, long distributorId, String tradeId, Trade row) {
		Trade stubOrRow = row;
		if (stubOrRow == null) {
			stubOrRow = new Trade();
			stubOrRow.setTradeId(tradeId);
		}
		String xml;
		try {
			xml = ordersExternalPayParamBuildService.buildWxpayOrderQueryXml(companyId, distributorId, stubOrRow);
		} catch (ResourceException e) {
			String msg = e.getMessage();
			throw new BadRequestException(StringUtils.hasText(msg) ? msg : "不支持支付服务，请联系商家", 400);
		}
		ResponseEntity<String> resp;
		try {
			resp = restTemplate.postForEntity(WX_ORDER_QUERY_URL, wxEntity(xml), String.class);
		} catch (Exception e) {
			throw new BadRequestException("支付失败", 400);
		}
		String respXml = resp.getBody();
		if (!StringUtils.hasText(respXml)) {
			throw new BadRequestException("支付失败", 400);
		}
		return parseWechatOrderQueryXmlToMap(respXml);
	}

	private static HttpEntity<String> wxEntity(String xml) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_XML);
		return new HttpEntity<>(xml, headers);
	}

	private static Map<String, String> parseWechatOrderQueryXmlToMap(String xml) {
		Map<String, String> map = new LinkedHashMap<>();
		try {
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			f.setNamespaceAware(false);
			Document doc = f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
			Element root = doc.getDocumentElement();
			if (root == null) {
				throw new BadRequestException("支付失败", 400);
			}
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					String tag = ((Element) n).getTagName();
					String text = n.getTextContent() == null ? "" : n.getTextContent().trim();
					map.put(tag, text);
				}
			}
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("支付失败", 400);
		}
		return map;
	}

	private static boolean isKnownPayType(String pt) {
		return isWechatChannel(pt)
				|| returnsEmptyListChannel(pt)
				|| "adapay".equals(pt)
				|| "bspay".equals(pt)
				|| "chinaums".equals(pt)
				|| "paypal".equals(pt);
	}

	private static boolean isWechatChannel(String pt) {
		return switch (pt) {
			case "wxpay", "wxpaypc", "wxpayh5", "wxpayjs", "wxpayapp", "wxpaypos" -> true;
			default -> false;
		};
	}

	private static boolean returnsEmptyListChannel(String pt) {
		return switch (pt) {
			case "alipay",
					"alipayh5",
					"alipayapp",
					"alipaypos",
					"alipaymini",
					"point",
					"deposit",
					"hfpay",
					"offline_pay",
					"pos" -> true;
			default -> false;
		};
	}
}
