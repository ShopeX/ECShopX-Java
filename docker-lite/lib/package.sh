#!/usr/bin/env bash

release_archive_basename() {
  printf 'ecshopx-java-%s' "$1"
}

release_build_tarball() {
  local version=$1
  local output_dir=$2
  local name stage exclude_file
  name=$(release_archive_basename "$version")
  mkdir -p "$output_dir"
  stage=$(mktemp -d)
  exclude_file=$(mktemp)

  cat > "$exclude_file" <<'EOF'
/node_modules/
.git/
*.log
.DS_Store
._*
dist-release
ecshopx-*.tar.gz
ecshopx-java-*.tar.gz
.env
/target/
EOF

  mkdir -p "$stage/$name"
  local projects=(ECShopX-Java ECShopX-Java_Admin ECShopX-Java_Mobile ECShopX-Java_Web)
  local p
  for p in "${projects[@]}"; do
    if [ ! -d "$RELEASE_PARENT_DIR/$p" ]; then
      release_log_error "missing project for package: $RELEASE_PARENT_DIR/$p"
      rm -rf "$stage" "$exclude_file"
      return 1
    fi
    if command -v rsync >/dev/null 2>&1; then
      mkdir -p "$stage/$name/$p"
      # /node_modules/ is rooted — keeps Nitro .output/server/node_modules
      rsync -a --exclude-from="$exclude_file" \
        "$RELEASE_PARENT_DIR/$p"/ "$stage/$name/$p"/
    else
      cp -a "$RELEASE_PARENT_DIR/$p" "$stage/$name/$p"
      # Only strip project-root deps — NEVER remove .output/server/node_modules
      rm -rf "$stage/$name/$p/node_modules"
      rm -rf "$stage/$name/$p/target"
      rm -rf "$stage/$name/$p/.git"
      rm -f "$stage/$name/$p/.env"
      rm -f "$stage/$name/$p"/ecshopx-*.tar.gz "$stage/$name/$p"/ecshopx-java-*.tar.gz
      # Nested Maven module targets under ECShopX-Java
      find "$stage/$name/$p" -mindepth 2 -type d -name target -prune -exec rm -rf {} + 2>/dev/null || true
      find "$stage/$name/$p" -mindepth 2 -type d -name .git -prune -exec rm -rf {} + 2>/dev/null || true
    fi
  done

  # Drop transient admin/mobile dist (keep dist-b2c / dist-bbc only)
  rm -rf "$stage/$name/ECShopX-Java_Admin/dist"
  rm -rf "$stage/$name/ECShopX-Java_Mobile/dist"

  # Nitro production deps must ship with the tarball
  if [ ! -d "$stage/$name/ECShopX-Java_Web/.output/server/node_modules" ]; then
    release_log_error "missing ECShopX-Java_Web/.output/server/node_modules in package (Nuxt SSR will fail)"
    rm -rf "$stage" "$exclude_file"
    return 1
  fi

  # Ship bootstrap jar inside the tarball (deploy uses packaged jar only)
  local staged_jar="$stage/$name/ECShopX-Java/docker-lite/app/ecshopx-bootstrap.jar"
  local staged_app_jar="$stage/$name/ECShopX-Java/docker-lite/app/app.jar"
  mkdir -p "$stage/$name/ECShopX-Java/docker-lite/app"
  if [ ! -f "$staged_jar" ] || [ ! -s "$staged_jar" ]; then
    if [ -f "$staged_app_jar" ] && [ -s "$staged_app_jar" ]; then
      cp -f "$staged_app_jar" "$staged_jar"
    else
      release_log_error "missing packaged jar: docker-lite/app/ecshopx-bootstrap.jar (run pack.sh jar build first)"
      rm -rf "$stage" "$exclude_file"
      return 1
    fi
  fi
  # compose mounts ./app/app.jar — keep both names in sync
  cp -f "$staged_jar" "$staged_app_jar"

  if [ ! -f "$stage/$name/ECShopX-Java/docker-lite/docker-compose.yml" ]; then
    release_log_error "docker-lite/docker-compose.yml missing from package staging tree"
    rm -rf "$stage" "$exclude_file"
    return 1
  fi

  find "$stage/$name" -name '._*' -delete 2>/dev/null || true

  COPYFILE_DISABLE=1 tar -czf "$output_dir/$name.tar.gz" -C "$stage" "$name"
  rm -rf "$stage" "$exclude_file"
  release_log_success "wrote $output_dir/$name.tar.gz"
}
