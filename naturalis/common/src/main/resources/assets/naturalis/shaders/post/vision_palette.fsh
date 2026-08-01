#version 150

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

/** Matches {@code VisionPaletteConfig} in morph {@code post_effect/*.json} and {@link MorphPostEffectUniformWriter}. */
layout(std140) uniform VisionPaletteConfig {
    vec3 AxisA;
    vec3 AxisB;
    vec3 AxisC;
    vec3 ColorA;
    vec3 ColorB;
    vec3 ColorC;
    float Strength;
    float ShadowLift;
    float LumaPreserve;
    float ChromaticMode;
    float PhotoStress;
    float KaleidoStrength;
    float KaleidoFoldCount;
    float SpectralProfile;
    float MotionTrail;
    float MotionUx;
    float MotionUz;
};

in vec2 texCoord;
out vec4 fragColor;

vec3 srgbToLinear(vec3 c) {
    vec3 cutoff = step(vec3(0.04045), c);
    vec3 low = c / 12.92;
    vec3 high = pow((c + 0.055) / 1.055, vec3(2.4));
    return mix(low, high, cutoff);
}

vec3 linearToSrgb(vec3 c) {
    vec3 cutoff = step(vec3(0.0031308), c);
    vec3 low = 12.92 * c;
    vec3 high = 1.055 * pow(max(c, 0.0), vec3(1.0 / 2.4)) - 0.055;
    return clamp(mix(low, high, cutoff), 0.0, 1.0);
}

bool paletteBound() {
    return length(ColorA) + length(ColorB) > 0.12;
}

/** Built-in mammal dichrome when the UBO block is empty (common on 1.21.x if CPU upload misses). */
void defaultPalette(
    out vec3 axisA, out vec3 axisB, out vec3 axisC,
    out vec3 colorA, out vec3 colorB, out vec3 colorC,
    out float strength, out float shadowLift, out float lumaPreserve
) {
    axisA = vec3(0.10, 0.80, 0.10);
    axisB = vec3(0.65, 0.30, 0.05);
    axisC = vec3(0.15, 0.20, 0.65);
    colorA = vec3(0.56, 0.78, 0.28);
    colorB = vec3(0.84, 0.82, 0.32);
    colorC = vec3(0.30, 0.46, 0.86);
    strength = 0.92;
    shadowLift = 0.22;
    lumaPreserve = 0.45;
}

vec2 kaleidoUv(vec2 uv, float foldCount, float strength) {
    float st = clamp(strength, 0.0, 1.0);
    if (st < 0.002 || foldCount < 1.6) {
        return uv;
    }
    float n = max(floor(foldCount + 0.5), 2.0);
    vec2 p = uv - 0.5;
    float r = length(p);
    float a = atan(p.y, p.x);
    float twoPi = 6.2831853;
    float seg = twoPi / n;
    a += seg * (0.18 + 0.07 * sin(uv.x * 6.2831853) * st);
    a = mod(a + seg * 0.5, seg) - seg * 0.5;
    vec2 q = vec2(cos(a), sin(a)) * r + 0.5;
    return mix(uv, clamp(q, vec2(0.001), vec2(0.999)), st);
}

vec3 dichromaticPalette(
    vec3 lin,
    float paletteStrength,
    vec3 axisA, vec3 axisB, vec3 axisC,
    vec3 colorA, vec3 colorB, vec3 colorC
) {
    float warmSignal = max(dot(lin, axisA), 0.0);
    float coolSignal = max(dot(lin, axisB), 0.0);
    float contrast = coolSignal - warmSignal;
    float contrastAbs = abs(contrast);

    float luma = dot(lin, vec3(0.2126, 0.7152, 0.0722));
    float cMax = max(lin.r, max(lin.g, lin.b));
    float cMin = min(lin.r, min(lin.g, lin.b));
    float saturation = cMax - cMin;
    float satNorm = saturation / (cMax + 0.06);

    float normContrast = contrastAbs / (luma + 0.08);

    float warmPower = clamp(warmSignal * 1.05, 0.0, 1.0);
    float coolPower = clamp(coolSignal * 1.35, 0.0, 1.0);

    vec3 warmColor = clamp(colorA, 0.0, 1.0) * warmPower;
    vec3 coolColor = clamp(colorB, 0.0, 1.0) * coolPower;
    vec3 byColor = mix(warmColor, coolColor, step(0.0, contrast));

    float cheatSignal = clamp(dot(lin, axisC), 0.0, 1.0);
    vec3 cheatColor = clamp(colorC, 0.0, 1.0) * cheatSignal;

    float byWeight = smoothstep(0.10, 0.55, normContrast);
    float cheatWeight = smoothstep(0.05, 0.35, cheatSignal) * (1.0 - byWeight * 0.65);
    vec3 projected = mix(byColor, cheatColor, cheatWeight);

    vec3 gray = vec3(luma);

    float gate = smoothstep(0.04, 0.22, satNorm) * clamp(paletteStrength, 0.0, 1.0);
    return mix(gray, projected, gate);
}

vec3 monochromeScotopic(vec3 lin, float srcLuma, float paletteStrength) {
    float rod = dot(lin, vec3(0.06, 0.62, 0.32));
    vec3 mono = vec3(rod);
    mono *= vec3(0.88, 0.94, 1.10);
    float gate = clamp(paletteStrength, 0.0, 1.0);
    return mix(vec3(srcLuma), mono, gate);
}

vec3 tetrachromatic(
    vec3 lin,
    float paletteStrength,
    vec3 axisA, vec3 axisB, vec3 axisC,
    vec3 colorA, vec3 colorB, vec3 colorC
) {
    float uvProxy = max(lin.b - max(lin.r, lin.g) * 0.52, 0.0);
    vec3 q = lin;
    q += vec3(-0.07, 0.14, 0.28) * uvProxy * 1.65;
    q.r += (lin.r - lin.g) * 0.10;
    q.g += (lin.g - lin.b) * -0.06;
    q.b += uvProxy * 0.42;
    q = clamp(q, 0.0, 1.0);
    return dichromaticPalette(q, paletteStrength, axisA, axisB, axisC, colorA, colorB, colorC);
}

void main() {
    float kf = KaleidoFoldCount;
    float ks = KaleidoStrength;
    vec2 uvK = kaleidoUv(texCoord, kf, ks);

    vec4 src = texture(InSampler, uvK);
    vec3 lin = srgbToLinear(src.rgb);
    float srcLuma = dot(lin, vec3(0.2126, 0.7152, 0.0722));

    vec3 axisA;
    vec3 axisB;
    vec3 axisC;
    vec3 colorA;
    vec3 colorB;
    vec3 colorC;
    float paletteStrength;
    float shadowLift;
    float lumaPreserve;

    if (paletteBound()) {
        axisA = AxisA;
        axisB = AxisB;
        axisC = AxisC;
        colorA = ColorA;
        colorB = ColorB;
        colorC = ColorC;
        paletteStrength = Strength < 0.12 ? 0.92 : Strength;
        shadowLift = ShadowLift;
        lumaPreserve = LumaPreserve;
    } else {
        defaultPalette(axisA, axisB, axisC, colorA, colorB, colorC, paletteStrength, shadowLift, lumaPreserve);
    }
    float chromaticMode = paletteBound() ? clamp(ChromaticMode, 0.0, 2.0) : 0.0;

    vec3 dichOut = dichromaticPalette(lin, paletteStrength, axisA, axisB, axisC, colorA, colorB, colorC);
    vec3 monoOut = monochromeScotopic(lin, srcLuma, paletteStrength);
    vec3 quadOut = tetrachromatic(lin, paletteStrength, axisA, axisB, axisC, colorA, colorB, colorC);

    vec3 outLin = dichOut;
    if (chromaticMode >= 1.75) {
        outLin = quadOut;
    } else if (chromaticMode >= 0.45) {
        outLin = monoOut;
    }

    float outLuma = max(dot(outLin, vec3(0.2126, 0.7152, 0.0722)), 0.0001);
    vec3 lumPreserved = outLin * (srcLuma / outLuma);
    outLin = mix(outLin, lumPreserved, clamp(lumaPreserve, 0.0, 1.0));

    outLin = max(outLin, vec3(srcLuma * shadowLift));

    vec2 px = 1.0 / max(InSize, vec2(1.0));

    float sp = clamp(SpectralProfile, 0.0, 2.0);
    if (sp > 1.18) {
        float uvAmt = (sp - 1.18) / 0.82;
        float uvP = max(outLin.b - max(outLin.r, outLin.g) * 0.48, 0.0);
        outLin += vec3(-0.035, 0.085, 0.22) * uvP * uvAmt;
        outLin.r += uvP * 0.065 * uvAmt;
        outLin = clamp(outLin, 0.0, 1.0);
    } else if (sp > 0.06) {
        float therm = smoothstep(0.10, 0.93, srcLuma);
        vec3 warm = outLin * vec3(1.12, 0.89, 0.66);
        vec3 cool = outLin * vec3(0.74, 0.93, 1.07);
        float irAmt = clamp(sp, 0.0, 1.18) * 0.52;
        outLin = mix(outLin, warm, therm * irAmt);
        outLin = mix(outLin, cool, (1.0 - therm) * irAmt * 0.38);
    }

    float mt = clamp(MotionTrail, 0.0, 1.0);
    vec2 mv = vec2(MotionUx, MotionUz);
    float mLen = length(mv);
    float trailAmt = mt * smoothstep(0.006, 0.88, mLen);
    if (trailAmt > 0.003) {
        vec2 p = texCoord - 0.5;
        float rad = length(p);
        float ang = atan(p.y, p.x);
        float twoPi = 6.2831853;
        float nWedge = 8.0;
        float phase = fract(ang / twoPi * nWedge + 1.0);
        vec2 dir = mv / max(mLen, 1e-4);
        float wedgeBoost = 0.35 + 0.65 * phase;
        vec2 delta = dir * px * (34.0 + wedgeBoost * 22.0) * (0.35 + rad * 1.35);
        vec2 tailUV = kaleidoUv(texCoord - delta, kf, ks);
        tailUV = clamp(tailUV, vec2(0.002), vec2(0.998));
        vec3 tailLin = srgbToLinear(texture(InSampler, tailUV).rgb);
        outLin = mix(outLin, tailLin, trailAmt * 0.62 * smoothstep(0.04, 1.0, rad));
    }

    float ph = clamp(PhotoStress, 0.0, 1.45);
    float hi = smoothstep(0.38, 0.94, srcLuma);
    float midBand = smoothstep(0.10, 0.48, srcLuma) * (1.0 - smoothstep(0.48, 0.88, srcLuma));
    float veil = ph * (0.52 * hi + 0.46 * midBand);

    vec3 sunTint = vec3(1.08, 0.96, 0.78);
    outLin = mix(outLin, outLin * sunTint, min(1.0, veil * 0.58));

    float sep = min(ph * 3.6, 7.0);
    vec3 sampR = srgbToLinear(texture(InSampler, kaleidoUv(texCoord + vec2(sep * px.x, 0.0), kf, ks)).rgb);
    vec3 sampB = srgbToLinear(texture(InSampler, kaleidoUv(texCoord + vec2(-sep * px.x * 0.88, sep * px.y * 0.28), kf, ks)).rgb);
    vec3 fringe = outLin;
    fringe.r = mix(fringe.r, sampR.r, min(1.0, ph * 0.44));
    fringe.b = mix(fringe.b, sampB.b, min(1.0, ph * 0.44));
    outLin = mix(outLin, fringe, min(1.0, veil * 1.02));

    float mids = midBand * ph * 0.34;
    outLin *= (1.0 - mids);

    float bleach = smoothstep(0.58, 1.0, srcLuma) * ph * 0.40;
    outLin = mix(outLin, outLin * sunTint, bleach);

    fragColor = vec4(linearToSrgb(clamp(outLin, 0.0, 1.0)), 1.0);
}
