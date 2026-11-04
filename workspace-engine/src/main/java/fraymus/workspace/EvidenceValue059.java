package fraymus.workspace;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.math.*;

/** Private in-memory snapshots preserve numeric types and signed zero across copies. */
final class EvidenceValue059 {
 private EvidenceValue059(){}
 static byte[] encode(JSONObject value)throws IOException{var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){write(value,out);}return bytes.toByteArray();}
 static JSONObject decode(byte[] bytes)throws IOException{try(var in=new DataInputStream(new ByteArrayInputStream(bytes))){var value=(JSONObject)read(in);if(in.available()!=0)throw new IOException("Unexpected snapshot tail");return value;}}
 private static void text(String s,DataOutputStream out)throws IOException{byte[] bytes=s.getBytes(StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);}
 private static String text(DataInputStream in)throws IOException{int size=in.readInt();if(size<0||size>in.available())throw new IOException("Invalid snapshot text");return new String(in.readNBytes(size),StandardCharsets.UTF_8);}
 private static void write(Object value,DataOutputStream out)throws IOException{
  if(value==JSONObject.NULL){out.writeByte(0);}
  else if(value instanceof JSONObject o){out.writeByte(1);out.writeInt(o.length());for(String key:o.keySet()){text(key,out);write(o.get(key),out);}}
  else if(value instanceof JSONArray a){out.writeByte(2);out.writeInt(a.length());for(Object item:a)write(item,out);}
  else if(value instanceof String s){out.writeByte(3);text(s,out);}
  else if(value instanceof Boolean b){out.writeByte(b?4:5);}
  else if(value instanceof Double n){out.writeByte(6);out.writeDouble(n);}
  else if(value instanceof Float n){out.writeByte(7);out.writeFloat(n);}
  else if(value instanceof Integer n){out.writeByte(8);out.writeInt(n);}
  else if(value instanceof Long n){out.writeByte(9);out.writeLong(n);}
  else if(value instanceof BigDecimal n){out.writeByte(10);text(n.toString(),out);}
  else if(value instanceof BigInteger n){out.writeByte(11);text(n.toString(),out);}
  else if(value instanceof Short n){out.writeByte(12);out.writeShort(n);}
  else if(value instanceof Byte n){out.writeByte(13);out.writeByte(n);}
  else throw new IOException("Unsupported evidence value "+value.getClass().getName());
 }
 private static Object read(DataInputStream in)throws IOException{return switch(in.readUnsignedByte()){
  case 0->JSONObject.NULL;case 1->{var o=new JSONObject();int n=in.readInt();if(n<0||n>in.available())throw new IOException("Invalid object count");for(int i=0;i<n;i++)o.put(text(in),read(in));yield o;}
  case 2->{var a=new JSONArray();int n=in.readInt();if(n<0||n>in.available())throw new IOException("Invalid array count");for(int i=0;i<n;i++)a.put(read(in));yield a;}
  case 3->text(in);case 4->true;case 5->false;case 6->in.readDouble();case 7->in.readFloat();case 8->in.readInt();case 9->in.readLong();case 10->new BigDecimal(text(in));case 11->new BigInteger(text(in));case 12->in.readShort();case 13->in.readByte();default->throw new IOException("Invalid evidence tag");};}
}
