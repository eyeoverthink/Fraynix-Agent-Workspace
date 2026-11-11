package renderer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class ShaderPortability116Test {
    @Test public void bothNativeShadersCompileAndInvalidShaderFailsExplicitly() throws Exception {
        assertTrue(glfwInit());long window=0;
        try {
            glfwDefaultWindowHints();glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
            window=glfwCreateWindow(32,32,"Shader portability check",0,0);assertNotEquals(0,window);glfwMakeContextCurrent(window);org.lwjgl.opengl.GL.createCapabilities();
            for(String name:new String[]{"default.glsl","pickingShader.glsl"}){var shader=new Shader("assets/shaders/"+name);shader.compile();shader.use();assertEquals(GL_NO_ERROR,glGetError());shader.detach();}
            Path invalid=Files.createTempFile("invalid-shader116",".glsl");Files.writeString(invalid,"#type vertex\n#version 330 core\nvoid main(){gl_Position=vec4(0.0);}\n#type fragment\n#version 330 core\ninvalid shader source\n");
            assertThrows(IllegalStateException.class,()->new Shader(invalid.toString()).compile());
            Files.createDirectories(Path.of("build/renderer116"));Files.writeString(Path.of("build/renderer116/driver.txt"),glGetString(GL_VENDOR)+"\n"+glGetString(GL_RENDERER)+"\n"+glGetString(GL_VERSION));
        } finally {if(window!=0)glfwDestroyWindow(window);glfwTerminate();}
    }
}
