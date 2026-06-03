#version 150

in vec2 FragCoord;

uniform vec2 Resolution;
uniform float Time;
uniform float Fade;
uniform vec3 Color1;
uniform vec3 Color2;

out vec4 fragColor;

void main() {
    // Normalised screen coords (0.0 to 1.0)
    vec2 centralUV = FragCoord;
    
    // Absolute pixel coords based on Resolution
    vec2 uv = FragCoord * Resolution;
    
    // --- WAVE 1 ---
    float wave1 = sin(uv.x * 0.005 + Time * 0.5) * cos(uv.y * 0.005 - Time * 0.3) * 150.0;
    float wave2 = cos(uv.x * 0.008 - Time * 0.4) * sin(uv.y * 0.006 + Time * 0.6) * 100.0;
    
    // Vertical distance to wave 1 (centered horizontally/vertically)
    float distanceToWave = abs(uv.y - (Resolution.y * 0.5 + wave1 + wave2));
    float glow1 = exp(-distanceToWave * 0.015) * 1.2;

    // --- WAVE 2 ---
    float wave3 = sin(uv.x * 0.006 - Time * 0.3) * cos(uv.y * 0.008 + Time * 0.5) * 120.0;
    
    // Vertical distance to wave 2 (placed slightly below center)
    float distanceToWave2 = abs(uv.y - (Resolution.y * 0.46 + wave3));
    float glow2 = exp(-distanceToWave2 * 0.02) * 1.0;

    // Combine glows of both waves
    float intensity = glow1 + glow2;

    // --- VIGNETTE ---
    float vignette = centralUV.x * centralUV.y * (1.0 - centralUV.x) * (1.0 - centralUV.y);
    vignette = clamp(pow(16.0 * vignette, 0.25), 0.0, 1.0);
    intensity *= vignette;
    
    float finalAlpha = clamp(intensity * Fade, 0.0, 1.0);
    
    // Render gradient theme waves with volumetric glow intensity
    vec3 waveColor = mix(Color1, Color2, centralUV.x);
    fragColor = vec4(waveColor, finalAlpha);
}
