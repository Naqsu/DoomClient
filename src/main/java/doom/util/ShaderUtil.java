package doom.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ShaderUtil {
    private final int programID;

    public ShaderUtil(String fragmentShaderLoc) {
        int program = GL20.glCreateProgram();
        try {
            int fragmentShaderID = createShader(Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(fragmentShaderLoc)).getInputStream(), GL20.GL_FRAGMENT_SHADER);
            GL20.glAttachShader(program, fragmentShaderID);

            // W MC 1.8.9 vertex shader jest zazwyczaj standardowy, ale dla rounded rects potrzebujemy prostego pass-through
            int vertexShaderID = createShader(Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("doom/shaders/vertex.vert")).getInputStream(), GL20.GL_VERTEX_SHADER);
            GL20.glAttachShader(program, vertexShaderID);

        } catch (Exception e) {
            e.printStackTrace();
        }

        GL20.glLinkProgram(program);
        int status = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS);

        if (status == 0) {
            throw new IllegalStateException("Shader failed to link!");
        }
        this.programID = program;
    }

    // Konstruktor przyjmujący kod źródłowy bezpośrednio (dla wygody)
    public ShaderUtil(String fragmentSource, boolean isSource) {
        int program = GL20.glCreateProgram();
        int fragmentShaderID = createShaderFromSource(fragmentSource, GL20.GL_FRAGMENT_SHADER);
        int vertexShaderID = createShaderFromSource(VERTEX_SHADER, GL20.GL_VERTEX_SHADER); // Używamy wbudowanego vertexa

        GL20.glAttachShader(program, fragmentShaderID);
        GL20.glAttachShader(program, vertexShaderID);
        GL20.glLinkProgram(program);
        this.programID = program;
    }

    public void init() {
        GL20.glUseProgram(programID);
    }

    public void unload() {
        GL20.glUseProgram(0);
    }

    public int getUniform(String name) {
        return GL20.glGetUniformLocation(programID, name);
    }

    public void setUniformf(String name, float... args) {
        int loc = getUniform(name);
        switch (args.length) {
            case 1: GL20.glUniform1f(loc, args[0]); break;
            case 2: GL20.glUniform2f(loc, args[0], args[1]); break;
            case 3: GL20.glUniform3f(loc, args[0], args[1], args[2]); break;
            case 4: GL20.glUniform4f(loc, args[0], args[1], args[2], args[3]); break;
        }
    }

    private int createShaderFromSource(String source, int shaderType) {
        int shader = GL20.glCreateShader(shaderType);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println(GL20.glGetShaderInfoLog(shader, 4096));
            throw new IllegalStateException("Shader failed to compile!");
        }
        return shader;
    }

    private int createShader(java.io.InputStream inputStream, int shaderType) {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return createShaderFromSource(stringBuilder.toString(), shaderType);
    }

    // Standardowy Vertex Shader dla renderingu 2D w MC
    private static final String VERTEX_SHADER =
            "#version 120\n" +
                    "void main() {\n" +
                    "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                    "}";

    // Dodaj to do klasy ShaderUtil:

    public void setUniform1f(String name, float f) {
        GL20.glUniform1f(getUniform(name), f);
    }

    public void setUniform2f(String name, float f1, float f2) {
        GL20.glUniform2f(getUniform(name), f1, f2);
    }

    public void setUniform3f(String name, float f1, float f2, float f3) {
        GL20.glUniform3f(getUniform(name), f1, f2, f3);
    }

    public void setUniform4f(String name, float f1, float f2, float f3, float f4) {
        GL20.glUniform4f(getUniform(name), f1, f2, f3, f4);
    }

    public void setUniform1i(String name, int i) {
        GL20.glUniform1i(getUniform(name), i);
    }
}