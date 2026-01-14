#version 120

uniform sampler2D textureIn;
uniform vec2 texelSize;
uniform vec2 direction;
uniform vec3 color;
uniform float radius;
uniform float weights[256];

void main() {
    vec3 current = vec3(0.0);
    float total = 0.0;

    for (float i = -radius; i <= radius; i++) {
        vec2 coord = gl_TexCoord[0].st + i * texelSize * direction;
        // Check if pixel is alpha (part of entity mask)
        float alpha = texture2D(textureIn, coord).a;

        current += color * alpha * weights[int(abs(i))];
        total += weights[int(abs(i))];
    }

    gl_FragColor = vec4(current / total, 1.0);
}