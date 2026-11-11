#type vertex
#version 330 core
layout (location=0) in vec3 aPos;
layout (location=1) in vec4 aColor;
layout (location=2) in vec2 aTexCoords;
layout (location=3) in float aTexId;
layout (location=4) in float aEntityId;

uniform mat4 uProjection;
uniform mat4 uView;

out vec4 fColor;
out vec2 fTexCoords;
out float fTexId;
out float fEntityId;

void main()
{
    fColor = aColor;
    fTexCoords = aTexCoords;
    fTexId = aTexId;
    fEntityId = aEntityId;

    gl_Position = uProjection * uView * vec4(aPos, 1.0);
}

    #type fragment
    #version 330 core

in vec4 fColor;
in vec2 fTexCoords;
in float fTexId;
in float fEntityId;

uniform sampler2D uTextures[8];

out vec3 color;

vec4 sampleSprite(int id, vec2 uv) {
    // GLSL 3.30 requires constant sampler-array indices (including Mesa).
    switch (id) {
        case 0: return texture(uTextures[0], uv);
        case 1: return texture(uTextures[1], uv);
        case 2: return texture(uTextures[2], uv);
        case 3: return texture(uTextures[3], uv);
        case 4: return texture(uTextures[4], uv);
        case 5: return texture(uTextures[5], uv);
        case 6: return texture(uTextures[6], uv);
        case 7: return texture(uTextures[7], uv);
        default: return vec4(0.0);
    }
}

void main()
{
    vec4 texColor = vec4(1, 1, 1, 1);
    if (fTexId > 0) {
        int id = int(fTexId);
        texColor = fColor * sampleSprite(id, fTexCoords);
    }

    if (texColor.a < 0.5) {
        discard;
    }
    color = vec3(fEntityId, fEntityId, fEntityId);
}