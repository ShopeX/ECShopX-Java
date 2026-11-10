# Uses pre-extracted dist/elasticsearch-8.15.5 + dist/analysis-ik (host unpack).
# Bundled JDK inside the ES tarball — no apt / no extra JRE pull.
FROM ubuntu:22.04

ENV ES_HOME=/usr/share/elasticsearch \
    ES_JAVA_OPTS="-Xms512m -Xmx512m" \
    TZ=Asia/Shanghai

RUN groupadd --system --gid 1000 elasticsearch \
  && useradd --system --uid 1000 --gid 1000 --home-dir ${ES_HOME} elasticsearch \
  && mkdir -p ${ES_HOME}

COPY --chown=elasticsearch:elasticsearch dist/elasticsearch-8.15.5/ ${ES_HOME}/
COPY --chown=elasticsearch:elasticsearch dist/analysis-ik/ ${ES_HOME}/plugins/analysis-ik/
COPY --chmod=755 entrypoint.sh /entrypoint.sh

WORKDIR ${ES_HOME}
EXPOSE 9200 9300

ENTRYPOINT ["/entrypoint.sh"]
CMD ["bin/elasticsearch", \
     "-E", "discovery.type=single-node", \
     "-E", "network.host=0.0.0.0", \
     "-E", "http.port=9200", \
     "-E", "xpack.security.enabled=false", \
     "-E", "xpack.security.http.ssl.enabled=false", \
     "-E", "xpack.security.enrollment.enabled=false"]
