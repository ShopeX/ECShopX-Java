package cn.shopex.ecshopx.orders.dispatch.printer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class YilianyunShippingTicketFormatterTest {

	private final YilianyunShippingTicketFormatter formatter = new YilianyunShippingTicketFormatter();

	@Test
	void formatsScreenshotStyleWithoutBarcodeSellerRemarkOrWeightDiff() {
		YilianyunShippingTicketModel model =
				new YilianyunShippingTicketModel(
						"测试店铺名称",
						"TESTORDER20250805001",
						"2025-08-05 10:30:00",
						"测试门店",
						"测试操作员",
						List.of(
								new YilianyunShippingTicketModel.Item(
										"测试商品A(测试规格)", "1.00", "1KG * 10", "10.00"),
								new YilianyunShippingTicketModel.Item(
										"测试商品B(标准装)", "5.00", "1件 * 3", "15.00")),
						"25.00",
						"测试支付方式",
						"测试张三",
						"13800000000",
						"打印测试地址",
						"2025-08-05 10:00",
						"测试买家留言: 请尽快发货",
						"13800000002",
						"测试门店地址");

		String content = formatter.format(model);

		assertThat(content)
				.contains("<FS2><center>发货小票</center></FS2>\n")
				.contains("<center>测试店铺名称</center>\n")
				.contains("单号:TESTORDER20250805001\n")
				.contains("时间:2025-08-05 10:30:00\n")
				.contains("下单店铺:测试门店\n")
				.contains("打印人员:测试操作员\n")
				.contains("品名\n")
				.contains("<td>单价</td><td>数量</td><td>实付金额</td>")
				.contains("测试商品A(测试规格)\n")
				.contains("<td>1.00</td><td>1KG * 10</td><td>10.00</td>")
				.contains("测试商品B(标准装)\n")
				.contains("<td>5.00</td><td>1件 * 3</td><td>15.00</td>")
				.contains("总数:2\n")
				.contains("已付:25.00\n")
				.contains("支付方式:测试支付方式\n")
				.contains("<FS2>实付:25.00</FS2>\n")
				.contains("收件人:测试张三\n")
				.contains("收件电话:13800000000\n")
				.contains("收货地址:打印测试地址\n")
				.contains("送达时间:2025-08-05 10:00\n")
				.contains("买家备注:测试买家留言: 请尽快发货\n")
				.contains("店铺电话:13800000002\n")
				.contains("店铺地址:测试门店地址\n");
		assertThat(content).endsWith("店铺地址:测试门店地址\n\n\n\n");
		assertThat(content).doesNotContain("BARCODE", "QR", "卖家备注", "已退差价", "实发重量", "应退差价", "<BR>", "<LR>");
	}

	@Test
	void omitsBlankOptionalLines() {
		YilianyunShippingTicketModel model =
				new YilianyunShippingTicketModel(
						"门店A",
						"1001",
						"2025-08-05 10:30:00",
						"门店A",
						"",
						List.of(new YilianyunShippingTicketModel.Item("商品", "5.00", "x1", "5.00")),
						"5.00",
						"微信支付",
						"李四",
						"13900000000",
						"地址A",
						"",
						"",
						"",
						"");

		String content = formatter.format(model);

		assertThat(content)
				.contains("单号:1001\n")
				.contains("收件人:李四")
				.doesNotContain("打印人员:")
				.doesNotContain("送达时间:")
				.doesNotContain("买家备注:")
				.doesNotContain("店铺电话:")
				.doesNotContain("店铺地址:")
				.doesNotContain("<BR>")
				.endsWith("收货地址:地址A\n\n\n\n");
	}
}
