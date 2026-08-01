#version 150
/** Legacy canid path; {@code wolf_vision.json} now uses {@code vision_palette} with canid-tuned uniforms. */

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

void main() {
    vec4 src = texture(DiffuseSampler, texCoord);
    vec3 lin = srgbToLinear(src.rgb);

    // Opponent blue-yellow channel for canid-style dichromatic mapping.
    float yellowSignal = 0.5 * (lin.r + lin.g);
    float blueSignal = lin.b;
    float byContrast = blueSignal - yellowSignal;
    float byAbs = abs(byContrast);

    // Luma/saturation for fallback and smooth gating.
    float luma = dot(lin, vec3(0.2126, 0.7152, 0.0722));
    float cMax = max(lin.r, max(lin.g, lin.b));
    float cMin = min(lin.r, min(lin.g, lin.b));
    float saturation = cMax - cMin;
    vec3 gray = vec3(luma);

    // Normalize contrast by brightness so dark-but-saturated faces stay colored
    // (prevents cyan side faces from flipping to gray).
    float byNorm = byAbs / (luma + 0.08);

    float yellowPower = clamp(yellowSignal * 1.05, 0.0, 1.0);
    float bluePower = clamp(blueSignal * 1.35, 0.0, 1.0);

    // Keep yellow less washed out and blue stronger/deeper.
    vec3 yellow = vec3(0.92, 0.86, 0.18) * yellowPower;
    vec3 blue = vec3(0.16, 0.36, 1.00) * bluePower;
    vec3 byColor = mix(yellow, blue, step(0.0, byContrast));

    // Gentle "cheat" channel: push ambiguous colors toward green instead of flat gray.
    // This keeps transitions smoother while still primarily blue/yellow.
    float greenSignal = clamp(lin.g * 0.95 + lin.r * 0.20 - lin.b * 0.25, 0.0, 1.0);
    vec3 green = vec3(0.52, 0.80, 0.24) * greenSignal;

    float byWeight = smoothstep(0.10, 0.55, byNorm);
    float greenWeight = smoothstep(0.05, 0.35, greenSignal) * (1.0 - byWeight * 0.65);
    vec3 projected = mix(byColor, green, greenWeight);

    // Low-saturation colors remain grayscale; stronger chroma moves to projected palette.
    float satNorm = saturation / (cMax + 0.06);
    float gate = smoothstep(0.04, 0.22, satNorm);
    vec3 outLin = mix(gray, projected, gate);

    // Soft luminance preservation (not strict) to avoid underwater scenes going pitch dark.
    float outLuma = max(dot(outLin, vec3(0.2126, 0.7152, 0.0722)), 0.0001);
    vec3 lumPreserved = outLin * (luma / outLuma);
    outLin = mix(outLin, lumPreserved, 0.45);

    // Small floor in deep dark tones so water/sky gradients remain visible.
    outLin = max(outLin, vec3(luma * 0.22));

    // Final post target should be opaque to avoid horizon-edge alpha artifacts.
    fragColor = vec4(linearToSrgb(clamp(outLin, 0.0, 1.0)), 1.0);
}
