package renderer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ShaderSourceTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test public void loadsLfShaderWithoutAnOpenGlContext() throws IOException {
        loadShader("\n");
    }

    @Test public void loadsCrLfShaderWithoutAnOpenGlContext() throws IOException {
        loadShader("\r\n");
    }

    private void loadShader(String newline) throws IOException {
        Path shaderFile = temporaryFolder.newFile("shader.glsl").toPath();
        String source = String.join(newline,
                "#type vertex", "#version 330 core", "void main() {}",
                "#type fragment", "#version 330 core", "void main() {}", "");
        Files.writeString(shaderFile, source);
        new Shader(shaderFile.toString());
    }
}
