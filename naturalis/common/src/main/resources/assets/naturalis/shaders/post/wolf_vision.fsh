#version 150

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
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

void main() {
    vec4 src = texture(InSampler, texCoord);
    vec3 lin = srgbToLinear(src.rgb);

    float yellowSignal = 0.5 * (lin.r + lin.g);
    float blueSignal = lin.b;
    float byContrast = blueSignal - yellowSignal;
    float byAbs = abs(byContrast);

    float luma = dot(lin, vec3(0.2126, 0.7152, 0.0722));
    float cMax = max(lin.r, max(lin.g, lin.b));
    float cMin = min(lin.r, min(lin.g, lin.b));
    float saturation = cMax - cMin;
    vec3 gray = vec3(luma);

    float byNorm = byAbs / (luma + 0.08);

    float yellowPower = clamp(yellowSignal * 1.05, 0.0, 1.0);
    float bluePower = clamp(blueSignal * 1.35, 0.0, 1.0);

    vec3 yellow = vec3(0.92, 0.86, 0.18) * yellowPower;
    vec3 blue = vec3(0.16, 0.36, 1.00) * bluePower;
    vec3 byColor = mix(yellow, blue, step(0.0, byContrast));

    float greenSignal = clamp(lin.g * 0.95 + lin.r * 0.20 - lin.b * 0.25, 0.0, 1.0);
    vec3 green = vec3(0.52, 0.80, 0.24) * greenSignal;

    float byWeight = smoothstep(0.10, 0.55, byNorm);
    float greenWeight = smoothstep(0.05, 0.35, greenSignal) * (1.0 - byWeight * 0.65);
    vec3 projected = mix(byColor, green, greenWeight);

    float satNorm = saturation / (cMax + 0.06);
    float gate = smoothstep(0.04, 0.22, satNorm);
    vec3 outLin = mix(gray, projected, gate);

    float outLuma = max(dot(outLin, vec3(0.2126, 0.7152, 0.0722)), 0.0001);
    vec3 lumPreserved = outLin * (luma / outLuma);
    outLin = mix(outLin, lumPreserved, 0.45);

    outLin = max(outLin, vec3(luma * 0.22));

    float brightIn = max(lin.r, max(lin.g, lin.b));
    float ribbonKeep = smoothstep(0.10, 0.24, saturation) * smoothstep(0.28, 0.62, brightIn);
    outLin = mix(outLin, lin, ribbonKeep * 0.88);

    fragColor = vec4(linearToSrgb(clamp(outLin, 0.0, 1.0)), 1.0);
}
