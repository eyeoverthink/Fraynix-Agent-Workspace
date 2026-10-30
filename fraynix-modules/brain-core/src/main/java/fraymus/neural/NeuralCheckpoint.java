package fraymus.neural;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
public final class NeuralCheckpoint {
    private NeuralCheckpoint(){}
    public static String save(NeuromorphicCore core,Path target) throws Exception {
        var s=core.snapshot();var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){
            out.writeInt(0x46524e31);out.writeInt(s.neurons);out.writeInt(s.connections);out.writeLong(s.seed);out.writeLong(s.randomState);out.writeLong(s.tick);
            for(float[] a:new float[][]{s.membrane,s.threshold,s.weights,s.calcium}){out.writeInt(a.length);for(float v:a)out.writeFloat(v);}
            out.writeInt(s.lastSpike.length);for(long v:s.lastSpike)out.writeLong(v);out.writeInt(s.targets.length);for(int v:s.targets)out.writeInt(v);
        }
        byte[] data=bytes.toByteArray(),hash=MessageDigest.getInstance("SHA-256").digest(data);Files.createDirectories(target.getParent());var temp=Files.createTempFile(target.getParent(),"neural-",".pending");
        try(var out=Files.newOutputStream(temp)){out.write(hash);out.write(data);}
        try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
        return HexFormat.of().formatHex(hash);
    }
    public static NeuromorphicCore load(Path target) throws Exception {
        if(Files.size(target)>32_000_000)throw new IOException("Neural checkpoint exceeds limit");byte[] bytes=Files.readAllBytes(target);
        if(bytes.length<80)throw new IOException("Neural checkpoint truncated");byte[] data=Arrays.copyOfRange(bytes,32,bytes.length);
        if(!MessageDigest.isEqual(Arrays.copyOf(bytes,32),MessageDigest.getInstance("SHA-256").digest(data)))throw new IOException("Neural checkpoint integrity mismatch");
        try(var in=new DataInputStream(new ByteArrayInputStream(data))){
            if(in.readInt()!=0x46524e31)throw new IOException("Unsupported neural checkpoint");var s=new NeuromorphicCore.State();s.neurons=in.readInt();s.connections=in.readInt();s.seed=in.readLong();s.randomState=in.readLong();s.tick=in.readLong();
            if(s.neurons<16||s.neurons>32768||s.connections<1||s.connections>64)throw new IOException("Invalid cortex size");
            s.membrane=floats(in,s.neurons);s.threshold=floats(in,s.neurons);s.weights=floats(in,s.neurons*s.connections);s.calcium=floats(in,Math.min(1024,s.neurons));
            if(in.readInt()!=s.neurons)throw new IOException("Invalid spike length");s.lastSpike=new long[s.neurons];for(int i=0;i<s.neurons;i++)s.lastSpike[i]=in.readLong();
            if(in.readInt()!=s.neurons*s.connections)throw new IOException("Invalid target length");s.targets=new int[s.neurons*s.connections];for(int i=0;i<s.targets.length;i++)s.targets[i]=in.readInt();
            if(in.available()!=0)throw new IOException("Trailing neural data");return NeuromorphicCore.restore(s);
        }
    }
    private static float[] floats(DataInputStream in,int length) throws IOException {if(in.readInt()!=length)throw new IOException("Invalid neural array length");float[] a=new float[length];for(int i=0;i<length;i++)a[i]=in.readFloat();return a;}
}
