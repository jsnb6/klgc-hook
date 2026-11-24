#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"; cd "$ROOT"
# ---- 可覆盖的构建环境参数 ----
AJ="${ANDROID_JAR:-${ANDROID_HOME:-$HOME/Android/Sdk}/platforms/android-33/android.jar}"
XPAPI="$ROOT/libs/libxposed-api-102.jar:$ROOT/libs/libxposed-service-102.jar:$ROOT/libs/libxposed-interface-102.jar"
R8_JAR="${R8_JAR:-$HOME/.m2/repository/com/android/tools/r8/8.2.42/r8-8.2.42.jar}"
BT="${BUILD_TOOLS:-$(dirname "$(command -v aapt2 || echo "$HOME/Android/Sdk/build-tools/33.0.2/aapt2")")}"
KS="${KS:-$ROOT/animrep.keystore}"; KSPASS="${KSPASS:-animrep123}"

for f in "$AJ" "$R8_JAR"; do
  [ -f "$f" ] || { echo "缺少依赖：$f"; echo "请设置 ANDROID_JAR / R8_JAR，或先运行 scripts/fetch-libs.sh"; exit 1; }
done
[ -x "$BT/aapt" ] || { echo "找不到 build-tools（aapt）：请设置 BUILD_TOOLS"; exit 1; }

rm -rf build out; mkdir -p build/classes build/dex build/pkg/assets out

echo "== 1 编译"
find src -name '*.java' > build/src.list
javac -nowarn -encoding UTF-8 -source 8 -target 8 -bootclasspath "$AJ" -cp "$XPAPI" \
  -d build/classes @build/src.list

echo "== 1.5 并入 libxposed-service 客户端类"
echo "== 1.5 并入 libxposed service/interface 客户端类"
unzip -o -q "$ROOT/libs/libxposed-service-102.jar" -d build/classes
unzip -o -q "$ROOT/libs/libxposed-interface-102.jar" -d build/classes

echo "== 2 D8"
find build/classes -name '*.class' > build/class.list
java -Xmx2g -cp "$R8_JAR" com.android.tools.r8.D8 --min-api 24 --release \
  --lib "$AJ" --output build/dex @build/class.list
ls -l build/dex

echo "== 3 打包"
cp build/dex/classes.dex build/pkg/
mkdir -p build/pkg/META-INF/xposed
cp src/META-INF/xposed/* build/pkg/META-INF/xposed/
"$BT/aapt" package -f -M AndroidManifest.xml -S res -I "$AJ" -F build/unsigned.apk
(cd build/pkg && zip -q -X ../unsigned.apk classes.dex && zip -q -X ../unsigned.apk META-INF/xposed/module.prop META-INF/xposed/scope.list META-INF/xposed/java_init.list)
unzip -l build/unsigned.apk

echo "== 4 对齐+签名"
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -alias animrep -keyalg RSA -keysize 2048 \
  -validity 10000 -storepass "$KSPASS" -keypass "$KSPASS" -dname "CN=AnimRep,O=DSH,C=CN" >/dev/null 2>&1
"$BT/apksigner" sign --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --ks "$KS" --ks-pass "pass:$KSPASS" --key-pass "pass:$KSPASS" \
  --min-sdk-version 24 --out out/animrep-module.apk build/aligned.apk
"$BT/apksigner" verify --print-certs out/animrep-module.apk | head -3
ls -l out/animrep-module.apk
