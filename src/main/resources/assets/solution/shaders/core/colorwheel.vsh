#version 150

#moj_import <solution:common.glsl>

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord;

void main() {
    texCoord = rvertexcoord(gl_VertexID);
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
