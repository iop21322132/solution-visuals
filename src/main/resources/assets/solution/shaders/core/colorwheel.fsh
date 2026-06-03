#version 150

// Modern rounded-square HSB Saturation/Brightness fragment shader
// Saturation mapped to horizontal axis (texCoord.x)
// Brightness mapped to vertical axis (1.0 - texCoord.y)
// Hue is passed as the uniform "brightness" for perfect backward compatibility.

uniform vec2  size;       // pixel size of the quad
uniform vec2  location;   // bottom-left in screen coords
uniform float brightness; // Hue (passed from CPU)
uniform float alpha;      // GUI fade alpha

in vec2 texCoord;
out vec4 fragColor;

vec3 hsb2rgb(float h, float s, float b) {
    float c = b * s;
    float x = c * (1.0 - abs(mod(h * 6.0, 2.0) - 1.0));
    float m = b - c;
    vec3 rgb;
    if      (h < 1.0/6.0) rgb = vec3(c, x, 0);
    else if (h < 2.0/6.0) rgb = vec3(x, c, 0);
    else if (h < 3.0/6.0) rgb = vec3(0, c, x);
    else if (h < 4.0/6.0) rgb = vec3(0, x, c);
    else if (h < 5.0/6.0) rgb = vec3(x, 0, c);
    else                  rgb = vec3(c, 0, x);
    return rgb + m;
}

float roundedBoxSDF(vec2 center, vec2 size, float radius) {
    vec2 q = abs(center) - size + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

void main() {
    float s = clamp(texCoord.x, 0.0, 1.0);
    float b = clamp(1.0 - texCoord.y, 0.0, 1.0);
    float h = clamp(brightness, 0.0, 1.0); // Hue

    vec3 rgb = hsb2rgb(h, s, b);

    // Compute rounded corner discard for 4px corner roundness
    vec2 center = (texCoord - 0.5) * size;
    float dist = roundedBoxSDF(center, size / 2.0, 4.0);
    float boxAlpha = 1.0 - smoothstep(-0.5, 0.5, dist);

    if (boxAlpha < 0.001) discard;

    fragColor = vec4(rgb, boxAlpha * alpha);
}
