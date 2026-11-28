#!/usr/bin/env bash
# 一键发布到 GitHub（需要 Personal Access Token，classic 勾 repo，或 fine-grained 勾
# Administration:Read and write + Contents:Read and write）
#
#   bash scripts/publish.sh <TOKEN> [仓库名] [描述]
#
set -euo pipefail
TOKEN="${1:-}"; NAME="${2:-klgc-hook}"; DESC="${3:-动漫共和国增强模块（Xposed / libxposed API 102）}"
[ -n "$TOKEN" ] || { echo "用法：bash scripts/publish.sh <TOKEN> [仓库名]"; exit 1; }
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
API="https://api.github.com"

LOGIN=$(curl -fsSL -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" "$API/user" \
        | sed -n 's/.*"login": *"\([^"]*\)".*/\1/p' | head -1)
[ -n "$LOGIN" ] || { echo "Token 无效或缺少 user:read 权限"; exit 1; }
echo "账号：$LOGIN"

CODE=$(curl -s -o /tmp/repo.json -w '%{http_code}' -X POST \
  -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" \
  "$API/user/repos" -d "{\"name\":\"$NAME\",\"description\":\"$DESC\",\"homepage\":\"\",\"private\":false,\"has_issues\":true,\"has_projects\":false,\"has_wiki\":false,\"auto_init\":false}")
case "$CODE" in
  201) echo "已创建仓库 $LOGIN/$NAME";;
  422) echo "仓库已存在，直接推送";;
  *) echo "创建失败 HTTP $CODE"; cat /tmp/repo.json; exit 1;;
esac

git remote remove origin 2>/dev/null || true
git remote add origin "https://x-access-token:$TOKEN@github.com/$LOGIN/$NAME.git"
git branch -M main
git push -u origin main

git remote set-url origin "https://github.com/$LOGIN/$NAME.git"   # 移除 remote 里的 token
git remote -v
echo
echo "完成：https://github.com/$LOGIN/$NAME"
echo "下一步（可选）：在仓库 Settings → Releases 里发一个 v1.4.1 tag，并上传 out/animrep-module.apk"

# ---- 发布后自检：确认 version.json 与下载资产都能 200，避免客户端检查更新拿到 404 ----
VNAME=$(python3 -c "import json;print(json.load(open('version.json'))['versionName'])" 2>/dev/null || echo "")
if [ -n "$VNAME" ]; then
  echo "== 自检 v$VNAME"
  for U in \
    "https://raw.githubusercontent.com/jsnb6/klgc-hook/main/version.json" \
    "https://cdn.jsdelivr.net/gh/jsnb6/klgc-hook@main/version.json" \
    "https://github.com/jsnb6/klgc-hook/releases/download/v$VNAME/klgc-hook-v$VNAME.apk"; do
    C=$(curl -sL -o /dev/null -w '%{http_code}' --max-time 40 "$U")
    echo "  $C  $U"
    [ "$C" = "200" ] || echo "  !! 这个地址不是 200，客户端会拿到错误"
  done
fi
