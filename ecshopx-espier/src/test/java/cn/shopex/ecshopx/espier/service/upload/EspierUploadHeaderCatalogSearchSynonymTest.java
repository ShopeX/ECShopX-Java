package cn.shopex.ecshopx.espier.service.upload;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EspierUploadHeaderCatalogSearchSynonymTest {

	@Test
	void forFileType_searchSynonym_mapsGroupKeyAndTwentyWordColumns() {
		UploadHeaderTitle title = EspierUploadHeaderCatalog.forFileType("search_synonym");
		assertThat(title.all()).containsKey("同义词组标识");
		assertThat(title.all().get("同义词组标识")).isEqualTo("group_key");
		assertThat(title.all()).containsEntry("词语1", "word_1");
		assertThat(title.all()).containsEntry("词语20", "word_20");
		assertThat(title.isNeed()).containsKey("同义词组标识");
	}

	@Test
	void searchSynonym_factory_matchesForFileType() {
		assertThat(EspierUploadHeaderCatalog.SEARCH_SYNONYM().all())
				.isEqualTo(EspierUploadHeaderCatalog.forFileType("search_synonym").all());
	}
}
