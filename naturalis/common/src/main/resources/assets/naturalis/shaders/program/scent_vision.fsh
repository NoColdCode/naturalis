#version 150
/** Legacy {@code loadEffect} path (1.20–1.21.1); 1.21.8+ uses {@code shaders/post/scent_vision.fsh}. */

uniform sampler2D DiffuseSampler;

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

vec3 sampleLin(vec2 uv) {
    return srgbToLinear(texture(DiffuseSampler, clamp(uv, 0.0, 1.0)).rgb);
}

void main() {
    vec2 uv = texCoord;
    vec3 src = sampleLin(uv);
    float luma = dot(src, vec3(0.2126, 0.7152, 0.0722));

    vec2 centered = uv * 2.0 - 1.0;
    float dist = length(centered);
    float edgeMask = smoothstep(0.22, 1.05, dist);
    vec3 mixed = src;

    float mono = dot(mixed, vec3(0.2126, 0.7152, 0.0722));
    vec3 nightBlue = vec3(mono * 0.22, mono * 0.30, mono * 0.58);
    vec3 pale = vec3(mono * 0.72, mono * 0.78, mono * 0.92);
    float centerGlow = 1.0 - smoothstep(0.0, 0.38, dist);
    vec3 outLin = mix(nightBlue, pale, centerGlow * 0.38);

    vec3 deepBlue = vec3(0.01, 0.02, 0.06);
    outLin = mix(outLin, deepBlue, edgeMask * 0.78);

    float vig = 1.0 - smoothstep(0.28, 1.2, dist) * 0.62;
    outLin *= vig;

    outLin = max(outLin, vec3(luma * 0.05));

    // Keep saturated scent ribbons and mob outline rims visible through the blue filter.
    float satIn = max(abs(src.r - src.g), max(abs(src.g - src.b), abs(src.r - src.b)));
    float brightIn = max(src.r, max(src.g, src.b));
    float ribbonMask = smoothstep(0.025, 0.12, satIn) * smoothstep(0.12, 0.45, brightIn);
    vec3 ribbonLin = mix(outLin, src, ribbonMask * 0.92);

    fragColor = vec4(linearToSrgb(clamp(ribbonLin, 0.0, 1.0)), 1.0);
}
