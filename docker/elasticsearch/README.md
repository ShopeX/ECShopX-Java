# 本地 Elasticsearch（仅开发）

Elasticsearch **8.15.5** + **analysis-ik**。无认证，勿对公网暴露。

```bash
cd docker/elasticsearch
docker compose up -d --build
curl http://127.0.0.1:9200
```

应用配置：`ecshopx.search.es.hosts=http://127.0.0.1:9200`

验证 IK：

```bash
curl -s -X POST 'http://127.0.0.1:9200/_analyze' \
  -H 'Content-Type: application/json' \
  -d '{"analyzer":"ik_smart","text":"中华人民共和国"}'
```

停止：`docker compose down`（加 `-v` 删数据）。
