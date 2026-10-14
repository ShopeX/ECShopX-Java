package cn.shopex.ecshopx.promotions.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MemberTaskExportCsvTemplatesTest {

	@Test
	void overviewHeaders_matchProductMdOrder() {
		assertThat(MemberTaskExportCsvTemplates.overviewDataColumnHeadersInOrder())
				.containsExactly(
						"任务ID",
						"任务名称",
						"任务类型",
						"任务时间",
						"任务奖励",
						"任务完成次数",
						"任务完成人数",
						"通过任务累计消费",
						"通过任务累计入会");
	}

	@Test
	void completionHeaders_matchProductMdOrder() {
		assertThat(MemberTaskExportCsvTemplates.completionDataColumnHeadersInOrder())
				.containsExactly(
						"完成会员",
						"会员编号",
						"完成时间",
						"奖励发放状态",
						"发放失败原因");
	}

	@Test
	void completionPreambleLines_matchProductMdShape() {
		assertThat(
						MemberTaskExportCsvTemplates.completionPreambleLines(
								"新人首单", 1001L, "2025-01-01 00:00:00 ~ 2025-02-01 00:00:00", 28115L, 12115L))
				.containsExactly(
						"新人首单_1001 2025-01-01 00:00:00 ~ 2025-02-01 00:00:00",
						"任务完成总次数：28115，任务完成总人数：12115");
	}
}
