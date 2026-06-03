#version 150

uniform sampler2D InputSampler;
uniform vec2 InputResolution;
uniform float Saturation;

out vec4 fragColor;

vec3 adjustSaturation(vec3 color, float sat) {
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    return mix(vec3(luma), color, sat);
}

void main() {
    vec2 uv = gl_FragCoord.xy / InputResolution;
    vec4 sampled = texture(InputSampler, uv);
    fragColor = vec4(adjustSaturation(sampled.rgb, Saturation), sampled.a);
}
