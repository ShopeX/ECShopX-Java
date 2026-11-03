package cn.shopex.ecshopx.espier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.espier.storage.FileStorageService;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ExportCsvFileServiceTest {

	@Test
	void exportsHtmlContainingQuotesAndCommasAsOneCsvCell() {
		FileStorageService fileStorageService = Mockito.mock(FileStorageService.class);
		when(fileStorageService.privateDownloadUrl(eq("file"), any(), eq(3600))).thenReturn("https://example.test/export.csv");
		ExportCsvFileService service = new ExportCsvFileService(fileStorageService);
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("intro", "详情图");
		titles.put("status", "商品状态");
		String intro = "<img src=\"https://example.test/a.jpg\"><img src=\"https://example.test/b.jpg?tag=a,b\">\\path";

		service.exportCsv("items", titles, List.of(Map.of("intro", intro, "status", "前台可销售")));

		ArgumentCaptor<byte[]> content = ArgumentCaptor.forClass(byte[].class);
		Mockito.verify(fileStorageService).put(eq("file"), eq("export/csv/items.csv"), content.capture());
		String csv = new String(content.getValue(), StandardCharsets.UTF_8);
		assertThat(csv).contains("\"<img src=\"\"https://example.test/a.jpg\"\"><img src=\"\"https://example.test/b.jpg?tag=a,b\"\">\\path\",前台可销售");
	}
}
