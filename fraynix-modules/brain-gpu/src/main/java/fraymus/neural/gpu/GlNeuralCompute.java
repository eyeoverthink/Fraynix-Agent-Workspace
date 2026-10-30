package fraymus.neural.gpu;
import fraymus.neural.NeuralCompute;
import org.lwjgl.opengl.GL;
import java.util.concurrent.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL43.*;

/** Real GLSL compute dispatch, with a hidden context owned by one worker. */
public final class GlNeuralCompute implements NeuralCompute {
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"neural-gpu-context");t.setDaemon(true);return t;});
    private long window;private int program;private String device;
    public GlNeuralCompute(){
        try{call(()->{
            if(!glfwInit())throw new IllegalStateException("GLFW unavailable");
            glfwDefaultWindowHints();glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,4);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
            window=glfwCreateWindow(64,64,"Fraynix neural compute",0,0);if(window==0)throw new IllegalStateException("OpenGL 4.3 compute context unavailable");
            glfwMakeContextCurrent(window);GL.createCapabilities();device=glGetString(GL_RENDERER)+" / "+glGetString(GL_VERSION);
            String source="""
                #version 430
                layout(local_size_x=64) in;
                layout(std430,binding=0) readonly buffer W { float w[]; };
                layout(std430,binding=1) readonly buffer X { float x[]; };
                layout(std430,binding=2) writeonly buffer Y { float y[]; };
                uniform int rows; uniform int columns;
                void main(){uint r=gl_GlobalInvocationID.x;if(r>=uint(rows))return;precise float s=0.0;for(int c=0;c<columns;c++)s=s+w[r*uint(columns)+uint(c)]*x[c];y[r]=s;}
                """;
            int shader=glCreateShader(GL_COMPUTE_SHADER);glShaderSource(shader,source);glCompileShader(shader);
            if(glGetShaderi(shader,GL_COMPILE_STATUS)==0)throw new IllegalStateException(glGetShaderInfoLog(shader));
            program=glCreateProgram();glAttachShader(program,shader);glLinkProgram(program);glDeleteShader(shader);
            if(glGetProgrami(program,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(program));
            return null;
        });}catch(RuntimeException error){close();throw error;}
    }
    private <T>T call(Callable<T> fn){
        try{return worker.submit(fn).get(15,TimeUnit.SECONDS);}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
        catch(Exception e){throw new IllegalStateException("GPU backend: "+(e.getCause()==null?e:e.getCause()),e);}
    }
    public float[] matvec(float[] weights,float[] input,int rows,int columns){
        NeuralCompute.validate(weights,input,rows,columns);
        return call(()->{
            int[] buffers={glGenBuffers(),glGenBuffers(),glGenBuffers()};
            try{
                glBindBuffer(GL_SHADER_STORAGE_BUFFER,buffers[0]);glBufferData(GL_SHADER_STORAGE_BUFFER,weights,GL_STREAM_DRAW);glBindBufferBase(GL_SHADER_STORAGE_BUFFER,0,buffers[0]);
                glBindBuffer(GL_SHADER_STORAGE_BUFFER,buffers[1]);glBufferData(GL_SHADER_STORAGE_BUFFER,input,GL_STREAM_DRAW);glBindBufferBase(GL_SHADER_STORAGE_BUFFER,1,buffers[1]);
                glBindBuffer(GL_SHADER_STORAGE_BUFFER,buffers[2]);glBufferData(GL_SHADER_STORAGE_BUFFER,(long)rows*4,GL_STREAM_READ);glBindBufferBase(GL_SHADER_STORAGE_BUFFER,2,buffers[2]);
                glUseProgram(program);glUniform1i(glGetUniformLocation(program,"rows"),rows);glUniform1i(glGetUniformLocation(program,"columns"),columns);
                glDispatchCompute((rows+63)/64,1,1);glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT|GL_BUFFER_UPDATE_BARRIER_BIT);
                float[] result=new float[rows];glGetBufferSubData(GL_SHADER_STORAGE_BUFFER,0,result);
                int error=glGetError();if(error!=GL_NO_ERROR)throw new IllegalStateException("GPU error "+error);return result;
            }finally{glUseProgram(0);for(int b:buffers)glDeleteBuffers(b);}
        });
    }
    public String device(){return device;}
    public void close(){if(!worker.isShutdown()){try{call(()->{if(window!=0){if(program!=0)glDeleteProgram(program);GL.setCapabilities(null);glfwMakeContextCurrent(0);glfwDestroyWindow(window);window=0;}glfwTerminate();return null;});}finally{worker.shutdownNow();}}}
}
