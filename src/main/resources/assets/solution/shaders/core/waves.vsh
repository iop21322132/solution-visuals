#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    
    int id = gl_VertexID % 4;
    if (id == 0) FragCoord = vec2(0.0, 0.0);
    else if (id == 1) FragCoord = vec2(0.0, 1.0);
    else if (id == 2) FragCoord = vec2(1.0, 1.0);
    else FragCoord = vec2(1.0, 0.0);
}
