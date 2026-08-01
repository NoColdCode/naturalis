#version 150

uniform sampler2D DiffuseSampler;
uniform vec3 AxisA;
uniform vec3 AxisB;
uniform vec3 AxisC;
uniform vec3 ColorA;
uniform vec3 ColorB;
uniform vec3 ColorC;
uniform float Strength;
uniform float ShadowLift;
uniform float LumaPreserve;
/** 0 = dichromatic opponent (mammal-like), 1 = rod-heavy monochrome, 2 = tetrachromatic / UV-augmented (avian-like). */
uniform float ChromaticMode;
/** Photophobia: species-specific glare / chromatic veil — keep visually distinct from vanilla NV / blindness mush. */
uniform float PhotoStress;
/** 0–1 kaleidoscope / wedge-fold blend toward compound-eye tiling (non-spider multi-eye morphs). */
uniform float KaleidoStrength;
/** Approximate wedge count (∝ ommatidia bands); folded into symmetric sectors for sampling. */
uniform float KaleidoFoldCount;
/** 0 = neutral, ~0.2–1.1 IR / pit-style emphasis, >1.2 UV accent on top of chromatic mode. */
uniform float SpectralProfile;
/** 0–1 wedge-modulated motion streak (temporal predator / fast mover vision). */
uniform float MotionTrail;
/** Horizontal camera-plane motion weights (Java: strafe / forward vs look). */
uniform float MotionUx;
uniform float MotionUz;
uniform vec2 InSize;
uniform vec2 OutSize;
uniform float Time;
uniform mat4 ProjMat;

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

/** Fold UV into one wedge — kaleidoscope crop/orientation vs facet count. */
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
    // Orientation offset: stagger dominant sector like rotated ommatidium rows.
    a += seg * (0.18 + 0.07 * sin(uv.x * 6.2831853) * st);
    a = mod(a + seg * 0.5, seg) - seg * 0.5;
    vec2 q = vec2(cos(a), sin(a)) * r + 0.5;
    return mix(uv, clamp(q, vec2(0.001), vec2(0.999)), st);
}

vec3 dichromaticPalette(vec3 lin) {
    float warmSignal = max(dot(lin, AxisA), 0.0);
    float coolSignal = max(dot(lin, AxisB), 0.0);
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

    vec3 warmColor = clamp(ColorA, 0.0, 1.0) * warmPower;
    vec3 coolColor = clamp(ColorB, 0.0, 1.0) * coolPower;
    vec3 byColor = mix(warmColor, coolColor, step(0.0, contrast));

    float cheatSignal = clamp(dot(lin, AxisC), 0.0, 1.0);
    vec3 cheatColor = clamp(ColorC, 0.0, 1.0) * cheatSignal;

    float byWeight = smoothstep(0.10, 0.55, normContrast);
    float cheatWeight = smoothstep(0.05, 0.35, cheatSignal) * (1.0 - byWeight * 0.65);
    vec3 projected = mix(byColor, cheatColor, cheatWeight);

    vec3 gray = vec3(luma);

    float gate = smoothstep(0.04, 0.22, satNorm) * clamp(Strength, 0.0, 1.0);
    return mix(gray, projected, gate);
}

vec3 monochromeScotopic(vec3 lin, float srcLuma) {
    float rod = dot(lin, vec3(0.06, 0.62, 0.32));
    vec3 mono = vec3(rod);
    mono *= vec3(0.88, 0.94, 1.10);
    float gate = clamp(Strength, 0.0, 1.0);
    return mix(vec3(srcLuma), mono, gate);
}

vec3 tetrachromatic(vec3 lin) {
    float uvProxy = max(lin.b - max(lin.r, lin.g) * 0.52, 0.0);
    vec3 q = lin;
    q += vec3(-0.07, 0.14, 0.28) * uvProxy * 1.65;
    q.r += (lin.r - lin.g) * 0.10;
    q.g += (lin.g - lin.b) * -0.06;
    q.b += uvProxy * 0.42;
    q = clamp(q, 0.0, 1.0);
    return dichromaticPalette(q);
}

void main() {
    float kf = KaleidoFoldCount;
    float ks = KaleidoStrength;
    vec2 uvK = kaleidoUv(texCoord, kf, ks);

    vec4 src = texture(DiffuseSampler, uvK);
    vec3 lin = srgbToLinear(src.rgb);
    float srcLuma = dot(lin, vec3(0.2126, 0.7152, 0.0722));

    vec3 dichOut = dichromaticPalette(lin);
    vec3 monoOut = monochromeScotopic(lin, srcLuma);
    vec3 quadOut = tetrachromatic(lin);

    vec3 outLin = dichOut;
    if (ChromaticMode >= 1.75) {
        outLin = quadOut;
    } else if (ChromaticMode >= 0.45) {
        outLin = monoOut;
    }

    float outLuma = max(dot(outLin, vec3(0.2126, 0.7152, 0.0722)), 0.0001);
    vec3 lumPreserved = outLin * (srcLuma / outLuma);
    outLin = mix(outLin, lumPreserved, clamp(LumaPreserve, 0.0, 1.0));

    outLin = max(outLin, vec3(srcLuma * ShadowLift));

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
        vec3 tailLin = srgbToLinear(texture(DiffuseSampler, tailUV).rgb);
        outLin = mix(outLin, tailLin, trailAmt * 0.62 * smoothstep(0.04, 1.0, rad));
    }

    float ph = clamp(PhotoStress, 0.0, 1.45);
    float hi = smoothstep(0.38, 0.94, srcLuma);
    float midBand = smoothstep(0.10, 0.48, srcLuma) * (1.0 - smoothstep(0.48, 0.88, srcLuma));
    float veil = ph * (0.52 * hi + 0.46 * midBand);

    vec3 sunTint = vec3(1.08, 0.96, 0.78);
    outLin = mix(outLin, outLin * sunTint, min(1.0, veil * 0.58));

    float sep = min(ph * 3.6, 7.0);
    vec3 sampR = srgbToLinear(texture(DiffuseSampler, kaleidoUv(texCoord + vec2(sep * px.x, 0.0), kf, ks)).rgb);
    vec3 sampB = srgbToLinear(texture(DiffuseSampler, kaleidoUv(texCoord + vec2(-sep * px.x * 0.88, sep * px.y * 0.28), kf, ks)).rgb);
    vec3 fringe = outLin;
    fringe.r = mix(fringe.r, sampR.r, min(1.0, ph * 0.44));
    fringe.b = mix(fringe.b, sampB.b, min(1.0, ph * 0.44));
    outLin = mix(outLin, fringe, min(1.0, veil * 1.02));

    float mids = midBand * ph * 0.34;
    outLin *= (1.0 - mids);

    float bleach = smoothstep(0.58, 1.0, srcLuma) * ph * 0.40;
    outLin = mix(outLin, outLin * sunTint, bleach);

    // Hold uniforms wired by vanilla EffectPass even when visually negligible (avoids stripped-uniform warnings).
    float hold = ProjMat[3][3] * 1e-8 + OutSize.y * 1e-8 + fract(Time * 0.002) * 1e-8 + InSize.x * 1e-8;
    outLin += vec3(hold);

    fragColor = vec4(linearToSrgb(clamp(outLin, 0.0, 1.0)), 1.0);
}
