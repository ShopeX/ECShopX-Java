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

package cn.shopex.ecshopx.espier.service.upgrade;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class UpgradeExecuteService {

	private static final String MARK_DIR = "ecshopx2_free_patch";

	private final OperatorsQueryService operatorsQueryService;
	private final UpgradeAgreementQueryService upgradeAgreementQueryService;
	private final ObjectMapper objectMapper;
	private final RestTemplate shortTimeoutRestTemplate;
	private final RestTemplate upgradeDownloadRestTemplate;
	private final String storageRootConfig;
	private final String baseUri;
	private final String downloadPath;

	public UpgradeExecuteService(
			OperatorsQueryService operatorsQueryService,
			UpgradeAgreementQueryService upgradeAgreementQueryService,
			ObjectMapper objectMapper,
			@Qualifier("espierShopexUsercenterRestTemplate") RestTemplate shortTimeoutRestTemplate,
			@Qualifier("espierShopexUsercenterUpgradeDownloadRestTemplate") RestTemplate upgradeDownloadRestTemplate,
			@Value("${espier.upgrade.storage-root:}") String storageRootConfig,
			@Value("${espier.shopex-usercenter.base-uri}") String baseUri,
			@Value("${espier.shopex-usercenter.download-path}") String downloadPath) {
		this.operatorsQueryService = operatorsQueryService;
		this.upgradeAgreementQueryService = upgradeAgreementQueryService;
		this.objectMapper = objectMapper;
		this.shortTimeoutRestTemplate = shortTimeoutRestTemplate;
		this.upgradeDownloadRestTemplate = upgradeDownloadRestTemplate;
		this.storageRootConfig = storageRootConfig;
		this.baseUri = baseUri;
		this.downloadPath = downloadPath;
	}

	public void upgrade(long companyId) {
		if (storageRootConfig == null || storageRootConfig.isBlank()) {
			throw new ResourceException("创建目录失败，目录没有写权限！");
		}
		Path storageRoot = Paths.get(storageRootConfig.trim()).toAbsolutePath().normalize();
		if (!Files.isWritable(storageRoot)) {
			throw new ResourceException("创建目录失败，目录没有写权限！");
		}
		Path storageParent = storageRoot.getParent();
		if (storageParent == null) {
			throw new ResourceException("创建目录失败，目录没有写权限！");
		}
		Path workspaceRoot = storageParent.getParent();
		if (workspaceRoot == null) {
			throw new ResourceException("创建目录失败，目录没有写权限！");
		}
		Path upgradeDir = storageRoot.resolve("upgrade");
		Path filesDir = upgradeDir.resolve("files");
		Path baksDir = upgradeDir.resolve("baks");
		ensureWritableDir(upgradeDir);
		ensureWritableDir(filesDir);
		ensureWritableDir(baksDir);

		String bakSuffix = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneId.systemDefault()).format(Instant.now());
		Path currentBakDir = baksDir.resolve("bak" + bakSuffix);
		if (Files.exists(currentBakDir)) {
			throw new ResourceException("备份目录已存在，请稍后再更新");
		}
		try {
			Files.createDirectories(currentBakDir);
		} catch (IOException e) {
			throw new ResourceException("创建目录失败，目录没有写权限！");
		}
		if (!Files.isWritable(currentBakDir)) {
			throw new ResourceException("创建目录失败，目录没有写权限！");
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("operator_type", "admin");
		Map<String, Object> row = operatorsQueryService.getInfo(filter);
		if (row == null || !row.containsKey("passport_uid") || row.get("passport_uid") == null) {
			throw new ResourceException("未找到管理员 passport，无法升级");
		}
		String passportUid = row.get("passport_uid").toString().trim();
		if (!StringUtils.hasText(passportUid)) {
			throw new ResourceException("未找到管理员 passport，无法升级");
		}

		String selfVersion = upgradeAgreementQueryService.resolveLocalPlatformVersionForDetectVersion();
		JsonNode patchRoot = upgradeAgreementQueryService.fetchPatchSignedPostJsonRoot();
		JsonNode dataNode = patchRoot.get("data");
		if (dataNode == null || !dataNode.isObject()) {
			throw new ResourceException("获取升级版本列表失败，请联系管理人员");
		}
		JsonNode patchListNode = dataNode.get("patch_list");
		if (patchListNode == null || !patchListNode.isArray()) {
			throw new ResourceException("获取升级版本列表失败，请联系管理人员");
		}
		Map<String, JsonNode> patchByVersion = new LinkedHashMap<>();
		List<String> rawVersions = new ArrayList<>();
		for (JsonNode item : patchListNode) {
			if (item == null || !item.isObject()) {
				continue;
			}
			JsonNode verNode = item.get("version");
			if (verNode == null || verNode.isNull()) {
				continue;
			}
			String version = verNode.asText();
			if (!StringUtils.hasText(version)) {
				continue;
			}
			patchByVersion.put(version, item);
			rawVersions.add(version);
		}
		if (rawVersions.isEmpty()) {
			throw new ResourceException("获取升级版本列表失败，请联系管理人员");
		}
		List<String> sortedVersions = sortVersionsForUpgrade(rawVersions);
		JsonNode chosenPatch = null;
		for (String v : sortedVersions) {
			if (UpgradeAgreementQueryService.remoteNewerThanLocalForUpgrade(v, selfVersion)) {
				chosenPatch = patchByVersion.get(v);
				break;
			}
		}
		if (chosenPatch == null) {
			throw new ResourceException("已是最新版本，无需升级");
		}

		String patchUuid = firstNonBlankText(chosenPatch, "patch_uuid", "uuid");
		String productType = firstNonBlankText(chosenPatch, "product_type", "productType");
		if (!StringUtils.hasText(patchUuid) || !StringUtils.hasText(productType)) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}

		long timestamp = Instant.now().getEpochSecond();
		Map<String, Object> signParams = new LinkedHashMap<>();
		signParams.put("timestamp", timestamp);
		signParams.put("shopexid", passportUid);
		signParams.put("product_type", productType);
		signParams.put("patch_uuid", patchUuid);
		String sign = upgradeAgreementQueryService.buildSign(signParams);

		MultiValueMap<String, String> downloadForm = new LinkedMultiValueMap<>();
		downloadForm.add("timestamp", Long.toString(timestamp));
		downloadForm.add("shopexid", passportUid);
		downloadForm.add("product_type", productType);
		downloadForm.add("patch_uuid", patchUuid);
		downloadForm.add("sign", sign);

		URI downloadPostUri = URI.create(baseUri.replaceAll("/$", "") + downloadPath);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> downloadEntity = new HttpEntity<>(downloadForm, headers);
		String downloadBody = shortTimeoutRestTemplate.postForObject(downloadPostUri, downloadEntity, String.class);
		if (downloadBody == null || downloadBody.isBlank()) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}
		JsonNode downloadRoot;
		try {
			downloadRoot = objectMapper.readTree(downloadBody);
		} catch (JsonProcessingException e) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}
		if (downloadRoot == null || !downloadRoot.isObject()) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}
		JsonNode st = downloadRoot.get("status");
		if (st == null || st.isNull() || !"success".equals(st.asText())) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}
		JsonNode downloadData = downloadRoot.get("data");
		if (downloadData == null || !downloadData.isObject()) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}
		JsonNode urlNode = downloadData.get("download_url");
		if (urlNode == null || urlNode.isNull() || !StringUtils.hasText(urlNode.asText())) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}
		String downloadUrlRaw = urlNode.asText().trim();
		String normalizedUrl = normalizeDownloadUrl(downloadUrlRaw);
		String zipFileName = resolveZipFileName(URI.create(normalizedUrl), downloadData);
		Path zipPath = upgradeDir.resolve(zipFileName);

		upgradeDownloadRestTemplate.execute(
				URI.create(normalizedUrl),
				HttpMethod.GET,
				null,
				response -> {
					try (InputStream in = response.getBody();
							OutputStream out =
									Files.newOutputStream(zipPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
						if (in == null) {
							return null;
						}
						in.transferTo(out);
					}
					return null;
				});

		try {
			clearAndRecreateDir(filesDir);
			extractTarGz(zipPath, filesDir);
			Path markRoot = filesDir.resolve(MARK_DIR);
			List<String> newFiles = scanDir(markRoot, workspaceRoot);
			checkAccess(workspaceRoot, newFiles);
			mergeFile(storageRoot, workspaceRoot, markRoot, currentBakDir, newFiles);
		} catch (ResourceException ex) {
			throw ex;
		} finally {
			try {
				Files.deleteIfExists(zipPath);
			} catch (IOException ignored) {
				// best-effort cleanup
			}
		}
		try {
			deleteDirRecursive(filesDir);
		} catch (IOException e) {
			throw new ResourceException("清理临时升级目录失败");
		}
	}

	static List<String> sortVersionsForUpgrade(List<String> versions) {
		List<String> paddedForms = new ArrayList<>(versions.size());
		for (String v : versions) {
			paddedForms.add(padVersionForSort(v));
		}
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < versions.size(); i++) {
			indices.add(i);
		}
		indices.sort(Comparator.comparing((Integer i) -> paddedForms.get(i)).thenComparing(i -> versions.get(i)));
		List<String> out = new ArrayList<>();
		for (int i : indices) {
			out.add(intvalNormalizeDottedVersion(paddedForms.get(i)));
		}
		return out;
	}

	private static String padVersionForSort(String version) {
		String[] parts = version.split("\\.", -1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (i > 0) {
				sb.append('.');
			}
			sb.append(zeroPadSegmentMinLengthTwo(parts[i]));
		}
		return sb.toString();
	}

	private static String zeroPadSegmentMinLengthTwo(String seg) {
		String s = seg == null ? "" : seg;
		StringBuilder sb = new StringBuilder(s);
		while (sb.length() < 2) {
			sb.insert(0, '0');
		}
		return sb.toString();
	}

	private static String intvalNormalizeDottedVersion(String paddedDotted) {
		String[] parts = paddedDotted.split("\\.", -1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (i > 0) {
				sb.append('.');
			}
			sb.append(leadingIntegerString(parts[i]));
		}
		return sb.toString();
	}

	private static String leadingIntegerString(String seg) {
		return Integer.toString(parseLeadingInteger(seg));
	}

	private static int parseLeadingInteger(String seg) {
		if (seg == null || seg.isEmpty()) {
			return 0;
		}
		int i = 0;
		int n = seg.length();
		while (i < n && Character.isWhitespace(seg.charAt(i))) {
			i++;
		}
		int sign = 1;
		if (i < n && (seg.charAt(i) == '+' || seg.charAt(i) == '-')) {
			sign = seg.charAt(i) == '-' ? -1 : 1;
			i++;
		}
		int start = i;
		while (i < n && Character.isDigit(seg.charAt(i))) {
			i++;
		}
		if (i == start) {
			return 0;
		}
		try {
			return sign * Integer.parseInt(seg.substring(start, i));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String firstNonBlankText(JsonNode node, String... fieldNames) {
		for (String name : fieldNames) {
			JsonNode child = node.get(name);
			if (child != null && !child.isNull() && StringUtils.hasText(child.asText())) {
				return child.asText().trim();
			}
		}
		return "";
	}

	private static String normalizeDownloadUrl(String url) {
		String u = Objects.requireNonNullElse(url, "").trim();
		if (u.startsWith("//")) {
			return "https:" + u;
		}
		return u;
	}

	private static String resolveZipFileName(URI downloadUri, JsonNode downloadData) {
		JsonNode fn = downloadData.get("filename");
		if (fn != null && !fn.isNull() && StringUtils.hasText(fn.asText())) {
			return Paths.get(fn.asText().trim()).getFileName().toString();
		}
		String path = downloadUri.getPath();
		if (path != null && !path.isBlank()) {
			int slash = path.lastIndexOf('/');
			if (slash >= 0 && slash + 1 < path.length()) {
				return path.substring(slash + 1);
			}
		}
		String q = downloadUri.getQuery();
		if (q != null && q.contains("filename=")) {
			for (String part : q.split("&")) {
				if (part.startsWith("filename=")) {
					String v = part.substring("filename=".length()).trim();
					if (StringUtils.hasText(v)) {
						return Paths.get(v).getFileName().toString();
					}
				}
			}
		}
		return "upgrade-patch.tar.gz";
	}

	private void ensureWritableDir(Path dir) {
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new ResourceException("创建目录失败，目录没有写权限！");
		}
		if (!Files.isWritable(dir)) {
			throw new ResourceException("创建目录失败，目录没有写权限！");
		}
	}

	private static void clearAndRecreateDir(Path dir) {
		try {
			if (Files.exists(dir)) {
				deleteDirRecursive(dir);
			}
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new ResourceException("解压失败");
		}
	}

	private static void deleteDirRecursive(Path root) throws IOException {
		if (!Files.exists(root)) {
			return;
		}
		try (Stream<Path> walk = Files.walk(root)) {
			List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
			for (Path p : paths) {
				Files.deleteIfExists(p);
			}
		}
	}

	private void extractTarGz(Path zipPath, Path destDir) {
		try (InputStream fi = Files.newInputStream(zipPath);
				BufferedInputStream bi = new BufferedInputStream(fi);
				GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi);
				TarArchiveInputStream tin = new TarArchiveInputStream(gzi)) {
			TarArchiveEntry entry;
			while ((entry = tin.getNextTarEntry()) != null) {
				Path outPath = destDir.resolve(entry.getName()).normalize();
				if (!outPath.startsWith(destDir)) {
					throw new IOException("bad tar path");
				}
				if (entry.isDirectory()) {
					Files.createDirectories(outPath);
					continue;
				}
				if (!entry.isFile()) {
					IOUtils.copy(tin, OutputStream.nullOutputStream());
					continue;
				}
				Path parent = outPath.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
				try (OutputStream os = Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
					byte[] buf = new byte[8192];
					int n;
					while ((n = tin.read(buf)) != -1) {
						os.write(buf, 0, n);
					}
				}
			}
		} catch (IOException e) {
			throw new ResourceException("解压失败");
		}
	}

	private static List<String> scanDir(Path markRoot, Path workspaceRoot) {
		if (!Files.isDirectory(markRoot)) {
			throw new ResourceException("解压失败");
		}
		List<String> out = new ArrayList<>();
		try (Stream<Path> stream = Files.walk(markRoot)) {
			stream.filter(Files::isRegularFile).forEach(p -> {
				Path rel = markRoot.relativize(p).normalize();
				if (rel.getNameCount() == 0) {
					return;
				}
				String top = rel.getName(0).toString();
				if (!StringUtils.hasText(top)) {
					return;
				}
				Path workspaceTop = workspaceRoot.resolve(top).normalize();
				if (Files.isDirectory(workspaceTop)) {
					out.add(rel.toString().replace('\\', '/'));
				}
			});
		} catch (IOException e) {
			throw new ResourceException("解压失败");
		}
		return out;
	}

	private void checkAccess(Path workspaceRoot, List<String> newFiles) {
		List<String> errorFiles = new ArrayList<>();
		for (String file : newFiles) {
			if (!StringUtils.hasText(file)) {
				continue;
			}
			Path target = workspaceRoot.resolve(file).normalize();
			if (!target.startsWith(workspaceRoot)) {
				errorFiles.add(file);
				continue;
			}
			if (Files.isRegularFile(target) && Files.isWritable(target)) {
				continue;
			}
			Path fileDir = target.getParent();
			if (fileDir == null) {
				errorFiles.add(file);
				continue;
			}
			boolean mkdirsForProbe = false;
			if (!Files.isDirectory(fileDir)) {
				mkdirsForProbe = true;
				try {
					Files.createDirectories(fileDir);
				} catch (IOException e) {
					errorFiles.add(file);
					continue;
				}
			}
			if (Files.isWritable(fileDir)) {
				if (mkdirsForProbe) {
					delEmptyDirChainFromLeaf(workspaceRoot, fileDir);
				}
				continue;
			}
			errorFiles.add(file);
		}
		if (errorFiles.isEmpty()) {
			return;
		}
		StringBuilder msg = new StringBuilder("error message: \\n");
		for (String f : errorFiles) {
			msg.append("文件无权限写入，请检查权限: ").append(f).append('\n');
		}
		throw new ResourceException(msg.toString());
	}

	private void delEmptyDirChainFromLeaf(Path workspaceRoot, Path leafDir) {
		Path current = leafDir;
		while (current != null && Files.isDirectory(current)) {
			try {
				Files.delete(current);
			} catch (DirectoryNotEmptyException e) {
				break;
			} catch (IOException e) {
				break;
			}
			Path parent = current.getParent();
			if (parent == null || !parent.startsWith(workspaceRoot)) {
				break;
			}
			current = parent;
		}
	}

	private void mergeFile(Path storageRoot, Path workspaceRoot, Path markRoot, Path currentBakDir, List<String> newFiles) {
		Path newfilesList = storageRoot.resolve("upgrade").resolve("newfiles.txt");
		Path oldfilesList = storageRoot.resolve("upgrade").resolve("oldfiles.txt");
		try {
			for (String file : newFiles) {
				if (!StringUtils.hasText(file)) {
					continue;
				}
				Path rel = Paths.get(file);
				Path src = markRoot.resolve(rel).normalize();
				if (!src.startsWith(markRoot)) {
					continue;
				}
				Path target = workspaceRoot.resolve(rel).normalize();
				if (!target.startsWith(workspaceRoot)) {
					continue;
				}
				boolean existed = Files.isRegularFile(target);
				Path backupFile = currentBakDir.resolve(rel).normalize();
				if (!backupFile.startsWith(currentBakDir)) {
					continue;
				}
				Path backupParent = backupFile.getParent();
				if (backupParent != null) {
					Files.createDirectories(backupParent);
				}
				Path targetParent = target.getParent();
				if (targetParent != null) {
					Files.createDirectories(targetParent);
				}
				if (existed) {
					Files.copy(target, backupFile, StandardCopyOption.REPLACE_EXISTING);
					String line = backupFile.toAbsolutePath().normalize().toString();
					Files.writeString(
							oldfilesList,
							line + "\n",
							StandardCharsets.UTF_8,
							StandardOpenOption.CREATE,
							StandardOpenOption.APPEND);
				}
				Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
				if (!existed) {
					Files.writeString(
							newfilesList,
							file.replace('\\', '/') + "\n",
							StandardCharsets.UTF_8,
							StandardOpenOption.CREATE,
							StandardOpenOption.APPEND);
				}
			}
		} catch (IOException e) {
			throw new ResourceException("合并升级文件失败");
		}
	}

}
