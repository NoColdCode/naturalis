#version 150

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

const vec3 AxisA = vec3(0.10, 0.80, 0.10);
const vec3 AxisB = vec3(0.65, 0.30, 0.05);
const vec3 AxisC = vec3(0.15, 0.20, 0.65);
const vec3 ColorA = vec3(0.56, 0.78, 0.28);
const vec3 ColorB = vec3(0.84, 0.82, 0.32);
const vec3 ColorC = vec3(0.30, 0.46, 0.86);
const float Strength = 0.92;
const float ShadowLift = 0.22;
const float LumaPreserve = 0.45;

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

void main() {
    vec4 src = texture(InSampler, texCoord);
    vec3 lin = srgbToLinear(src.rgb);

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

    float gate = max(0.38, smoothstep(0.04, 0.22, satNorm)) * clamp(Strength, 0.0, 1.0);
    vec3 outLin = mix(gray, projected, gate);

    float outLuma = max(dot(outLin, vec3(0.2126, 0.7152, 0.0722)), 0.0001);
    vec3 lumPreserved = outLin * (luma / outLuma);
    outLin = mix(outLin, lumPreserved, clamp(LumaPreserve, 0.0, 1.0));

    outLin = max(outLin, vec3(luma * ShadowLift));

    fragColor = vec4(linearToSrgb(clamp(outLin, 0.0, 1.0)), 1.0);
}
