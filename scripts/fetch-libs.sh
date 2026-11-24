#!/usr/bin/env bash
# 下载构建所需的 libxposed 客户端库（Apache-2.0，来自 Maven Central）。
# 用法：bash scripts/fetch-libs.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
mkdir -p libs build/tmp-libs
MIRRORS=("https://repo1.maven.org/maven2" "https://maven.aliyun.com/repository/central" "https://repo.huaweicloud.com/repository/maven")
for art in api interface service; do
  ver="102.0.0"
  ok=0
  for base in "${MIRRORS[@]}"; do
    for try in 1 2 3; do
      if curl -fsSL --retry 2 --retry-delay 3 "$base/io/github/libxposed/$art/$ver/$art-$ver.aar" -o "build/tmp-libs/$art.aar"; then ok=1; break 2; fi
      sleep $((try*3))
    done
  done
  [ "$ok" = 1 ] || { echo "下载失败：$art（所有镜像均不可用）"; exit 1; }
  unzip -o -q "build/tmp-libs/$art.aar" classes.jar -d "build/tmp-libs/$art"
  cp "build/tmp-libs/$art/classes.jar" "libs/libxposed-$art-102.jar"
  echo "ok  libs/libxposed-$art-102.jar"
done
echo "完成。android.jar 请用本机 Android SDK（ANDROID_JAR 环境变量），r8 请用 R8_JAR 指定。"
