#version 150

uniform vec2 size;
uniform vec2 location;
uniform vec4 radius;
uniform float softness;
uniform vec4 color1;

uniform sampler2D TextureSampler;
uniform vec2 InputResolution;

in vec2 texCoord;
out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, vec4 radius) {
    radius.xy = (center.x > 0.0) ? radius.xy : radius.zw;
    radius.x  = (center.y > 0.0) ? radius.x : radius.y;
    vec2 q = abs(center) - size + radius.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius.x;
}

void main() {
    // UV координаты текстуры
    vec2 uv = (gl_FragCoord.xy - location) / size;
    uv.y = 1.0 - uv.y; // flip Y

    // Сэмплируем текстуру
    vec4 texColor = texture(TextureSampler, uv) * color1;

    // SDF для закруглённых углов
    float dist = roundedBoxSDF(gl_FragCoord.xy - location - (size / 2.0), size / 2.0, radius);
    float alpha = 1.0 - smoothstep(-softness, softness, dist);

    fragColor = vec4(texColor.rgb, texColor.a * alpha);
}
