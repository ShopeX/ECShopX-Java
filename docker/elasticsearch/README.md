# 本地 Elasticsearch（商品搜索）

- 镜像：Elasticsearch **8.15.5** + **analysis-ik**
- 端口：`9200`（可被 `ES_HOST_PORT` 覆盖）
- 安全：关闭（仅本地开发）
- 配置对齐：`ecshopx.search.es.hosts=http://127.0.0.1:9200`

## 准备发行包（仅首次 / arm64 Mac）

官方镜像大层拉取易中断时，改用本地 tarball + 解压后 COPY 构建：

```bash
cd docker/elasticsearch
mkdir -p dist
curl -fL --retry 8 --continue-at - \
  -o dist/elasticsearch-8.15.5-linux-aarch64.tar.gz \
  https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.15.5-linux-aarch64.tar.gz
curl -fL -o dist/elasticsearch-analysis-ik-8.15.5.zip \
  https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.15.5.zip
tar -xzf dist/elasticsearch-8.15.5-linux-aarch64.tar.gz -C dist
mkdir -p dist/analysis-ik
unzip -q dist/elasticsearch-analysis-ik-8.15.5.zip -d dist/analysis-ik
```

> `dist/` 已加入 `.gitignore`，勿提交大包。

## 启动

```bash
cd docker/elasticsearch
docker compose up -d --build
docker compose ps
curl http://127.0.0.1:9200
```

验证 IK：

```bash
curl -s -X POST 'http://127.0.0.1:9200/_analyze' \
  -H 'Content-Type: application/json' \
  -d '{"analyzer":"ik_smart","text":"中华人民共和国"}'
```

## 停止

```bash
docker compose down          # 保留数据卷
docker compose down -v       # 同时删除数据
```
