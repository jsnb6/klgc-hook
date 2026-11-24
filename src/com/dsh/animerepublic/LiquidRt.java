package com.dsh.animerepublic;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RuntimeShader;
import android.graphics.Shader;

/**
 * 液态玻璃药丸：把 yuwu2007/liquid-glass-dock 的折射着色器移植成 AGSL（Android 13+）。
 * 逐像素做的事：圆角矩形 SDF -> 法线 -> Kyant 折射剖面位移 -> 背景重采样（带 RGB 色散）
 * -> 提鲜/着色 -> 上下厚度 -> 边缘取色染色 + 菲涅尔亮线 -> 外阴影。
 * 背景图由调用方截取并模糊后传入，作为 uniform shader。
 */
final class LiquidRt {

    private static final String AGSL =
            "uniform shader bg;\n"
            + "uniform float2 size;\n"
            + "uniform float radius;\n"
            + "uniform float refrH;\n"
            + "uniform float refrAmt;\n"
            + "uniform float disp;\n"
            + "uniform float fresnel;\n"
            + "uniform float edgeTint;\n"
            + "uniform float sheen;\n"
            + "uniform float tint;\n"
            + "uniform float vibrancy;\n"
            + "uniform float shadow;\n"
            + "float sdRB(float2 p, float2 b, float r) {\n"
            + "  float2 q = abs(p) - b + r;\n"
            + "  return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n"
            + "}\n"
            + "half4 main(float2 p) {\n"
            + "  float2 c = size * 0.5;\n"
            + "  float2 b = max(size * 0.5 - float2(1.0), float2(1.0));\n"
            + "  float rr = min(radius, min(b.x, b.y));\n"
            + "  float d = sdRB(p - c, b, rr);\n"
            + "  float e = 0.75;\n"
            + "  float2 g = float2(sdRB(p - c + float2(e, 0.0), b, rr) - sdRB(p - c - float2(e, 0.0), b, rr),\n"
            + "                    sdRB(p - c + float2(0.0, e), b, rr) - sdRB(p - c - float2(0.0, e), b, rr));\n"
            + "  float2 n = g / max(length(g), 0.00001);\n"
            + "  float alpha = smoothstep(0.8, -0.8, d);\n"
            + "  float2 off = float2(0.0);\n"
            + "  if (d < 0.0 && -d < refrH) {\n"
            + "    float x = 1.0 + d / max(refrH, 0.001);\n"
            + "    float prof = 1.0 - sqrt(max(0.0, 1.0 - x * x));\n"
            + "    off = n * (prof * refrAmt);\n"
            + "  }\n"
            + "  float3 col;\n"
            + "  col.r = bg.eval(p + off * (1.0 + disp)).r;\n"
            + "  col.g = bg.eval(p + off).g;\n"
            + "  col.b = bg.eval(p + off * (1.0 - disp)).b;\n"
            + "  float luma = dot(col, float3(0.2126, 0.7152, 0.0722));\n"
            + "  col = mix(float3(luma), col, vibrancy);\n"
            + "  col = mix(col, float3(1.0), tint);\n"
            + "  col *= 0.955;\n"
            + "  float vy = clamp(p.y / max(size.y, 1.0), 0.0, 1.0);\n"
            + "  col *= mix(1.0 + sheen, 1.0 - sheen, vy);\n"
            + "  float3 picked = bg.eval(c).rgb;\n"
            + "  float el = dot(picked, float3(0.2126, 0.7152, 0.0722));\n"
            + "  float3 pickedTint = clamp(mix(float3(el), picked, 1.35), 0.0, 1.0) * 1.08;\n"
            + "  pickedTint = clamp(pickedTint, 0.0, 1.0);\n"
            + "  float2 L = normalize(float2(-0.35, -0.94));\n"
            + "  float lit = clamp(dot(n, L), 0.0, 1.0);\n"
            + "  float band = exp(-pow((d + 4.0) / 5.0, 2.0));\n"
            + "  float line = pow(band, 3.0);\n"
            + "  col += pickedTint * (edgeTint * band * 0.55 * (0.55 + 0.45 * lit));\n"
            + "  col += float3(fresnel * line * (0.30 + 0.70 * pow(lit, 1.2)));\n"
            + "  float inner = exp(-pow((d + 1.6) / 2.2, 2.0));\n"
            + "  col -= float3(0.16 * inner * (0.6 + 0.4 * (1.0 - lit)));\n"
            + "  float frost = smoothstep(0.0, -18.0, d);\n"
            + "  col = mix(col, float3(0.965, 0.972, 0.985), 0.16 * frost);\n"
            + "  float topHi = exp(-pow((p.y - 2.0) / 6.0, 2.0)) * alpha;\n"
            + "  col += float3(0.55 * topHi);\n"
            + "  float botLo = exp(-pow((size.y - 2.0 - p.y) / 7.0, 2.0)) * alpha;\n"
            + "  col += pickedTint * (0.35 * botLo);\n"
            + "  col = clamp(col, 0.0, 1.0);\n"
            + "  float sh = (1.0 - alpha) * exp(-max(d, 0.0) / 9.0) * shadow;\n"
            + "  float a = alpha + sh * (1.0 - alpha);\n"
            + "  return half4(half3(col) * half(alpha), half(a));\n"
            + "}\n";

    private LiquidRt() {
    }

    private static String mode = "未检测";

    static String mode() {
        try {
            if (android.os.Build.VERSION.SDK_INT < 33) {
                return "基础（系统 < 13）";
            }
            get();
            return "AGSL 折射";
        } catch (Throwable t) {
            return "基础（着色器不可用）";
        }
    }

    static boolean available() {
        try {
            return android.os.Build.VERSION.SDK_INT >= 33;
        } catch (Throwable t) {
            return false;
        }
    }

    private static RuntimeShader shader;

    private static RuntimeShader get() {
        if (shader == null) {
            shader = new RuntimeShader(AGSL);
        }
        return shader;
    }

    static boolean draw(Canvas c, Bitmap bg, float w, float h, float radius, float density) {
        boolean ok = false;
        try {
            RuntimeShader s = get();
            BitmapShader bs = new BitmapShader(bg, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.setScale(w / Math.max(bg.getWidth(), 1), h / Math.max(bg.getHeight(), 1));
            bs.setLocalMatrix(m);
            s.setInputShader("bg", bs);
            s.setFloatUniform("size", w, h);
            s.setFloatUniform("radius", Math.max(radius, 1f));
            s.setFloatUniform("refrH", 14f * density);
            s.setFloatUniform("refrAmt", 30f * density);
            s.setFloatUniform("disp", 0.02f);
            s.setFloatUniform("fresnel", 1.55f);
            s.setFloatUniform("edgeTint", 1.0f);
            s.setFloatUniform("sheen", 0.07f);
            s.setFloatUniform("tint", 0.04f);
            s.setFloatUniform("vibrancy", 1.35f);
            s.setFloatUniform("shadow", 0.22f);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(s);
            RectF rf = new RectF(0f, 0f, w, h);
            c.drawRoundRect(rf, radius, radius, p);
            // 探针：着色器真的绘制成功时，左上角出现一个品红小方块
            Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
            mark.setColor(0xFFFF00FF);
            c.drawRect(0f, 0f, 6f, 6f, mark);
            ok = true;
        } catch (Throwable ignored) {
        }
        return ok;
    }
}
