package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.IIOImage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static fraymus.workspace.MotionMediaContainers.*;

final class AvatarWorkshop {
    private static final String FORMAT = "fraynix.avatar-v1";
    private static final int IMAGE_LIMIT = 1_800_000, AUDIO_LIMIT = HZ * 2 * 30 + 4096, RECIPE_LIMIT = 4_500_000;
    private static final int POSES = 5, SPRITE_W = 96, SPRITE_H = 112;
    private final Path root;
    private final TileWorkshop tiles;

    AvatarWorkshop(Path project, TileWorkshop tiles) {
        root = project.resolve("media/avatars");
        this.tiles = tiles;
    }

    synchronized JSONObject save(JSONObject input) throws Exception {
        SpatialScene.keys(input, "name", "image", "rig", "wav");
        String name = SpatialScene.text(input, "name", 64).strip();
        String png = input.getString("image");
        String wav = input.optString("wav");
        Portrait portrait = portrait(png, input.getJSONObject("rig"), wav);
        var recipe = new JSONObject().put("format", FORMAT).put("name", name).put("image", png)
                .put("rig", portrait.rig.json()).put("wav", wav).put("hashes", portrait.hashes());
        byte[] bytes = recipe.toString().getBytes(StandardCharsets.UTF_8);
        return retain(bytes, recipe, portrait);
    }

    synchronized JSONObject importRecipe(JSONObject input) throws Exception {
        SpatialScene.keys(input, "base64");
        byte[] bytes = SpriteWorkshop.decode(input.getString("base64"), RECIPE_LIMIT);
        var recipe = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        return retain(bytes, recipe, checkedPortrait(recipe));
    }

    private JSONObject retain(byte[] bytes, JSONObject recipe, Portrait portrait) throws IOException {
        if (bytes.length > RECIPE_LIMIT) throw new IllegalArgumentException("Avatar exceeds the project size limit.");
        String id = MapDefinition.sha(bytes);
        Files.createDirectories(root);
        Path path = root.resolve(id + ".json");
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) SpriteWorkshop.read(path, RECIPE_LIMIT, id);
        else SpriteWorkshop.replace(path, bytes);
        SpriteWorkshop.replace(root.resolve("latest"), id.getBytes(StandardCharsets.US_ASCII));
        return response(id, recipe, portrait);
    }

    synchronized JSONObject open(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        var latest = root.resolve("latest");
        if (!input.has("id") && !Files.exists(latest, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        String id = input.has("id") ? input.getString("id") : new String(SpriteWorkshop.read(latest, 64), StandardCharsets.US_ASCII);
        var saved = load(id);
        return response(id, saved.recipe, saved.portrait);
    }

    synchronized JSONObject library(JSONObject input) throws IOException {
        SpatialScene.keys(input);
        var rows = new JSONArray();
        if (!Files.isDirectory(root)) return new JSONObject().put("avatars", rows);
        List<Path> files;
        try (var paths = Files.list(root)) {
            files = paths.filter(p -> p.getFileName().toString().matches("[0-9a-f]{64}\\.json")).sorted().toList();
        }
        for (Path path : files) {
            String id = path.getFileName().toString().substring(0,64);
            try { rows.put(new JSONObject().put("id", id).put("name", recipe(id).getString("name"))); }
            catch (IOException | IllegalArgumentException | org.json.JSONException damaged) {
                rows.put(new JSONObject().put("id", id).put("name", "Unavailable avatar"));
            }
        }
        return new JSONObject().put("avatars", rows);
    }

    synchronized JSONObject tile(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        var saved = load(input.getString("id"));
        var source = new JSONObject().put("filename", "Avatar " + input.getString("id").substring(0,12) + ".png")
                .put("png", b64(saved.portrait.sprite));
        return tiles.add(new JSONObject().put("source", source));
    }

    synchronized JSONObject player(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        String id = input.getString("id");
        var saved = load(id);
        return tiles.addPlayer(id, saved.recipe.getString("name"), saved.portrait.sprite, saved.portrait.walk);
    }

    synchronized JSONObject export(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "kind");
        String id = input.getString("id");
        var saved = load(id);
        Portrait p = saved.portrait;
        String kind = input.getString("kind");
        byte[] bytes = switch (kind) {
            case "sprite" -> p.sprite;
            case "walk" -> p.walk;
            case "portrait" -> p.poses.getFirst();
            case "avi" -> video(p);
            case "manifest" -> manifest(id, saved.recipe, p).toString(2).getBytes(StandardCharsets.UTF_8);
            case "project" -> SpriteWorkshop.read(root.resolve(id + ".json"), RECIPE_LIMIT, id);
            default -> throw new IllegalArgumentException("Unknown avatar export.");
        };
        return new JSONObject().put("base64", b64(bytes)).put("sha256", MapDefinition.sha(bytes))
                .put("sourceSha256", p.hashes().getString("imageSha256"));
    }

    private JSONObject recipe(String id) throws IOException {
        if (!id.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid avatar ID.");
        return new JSONObject(new String(SpriteWorkshop.read(root.resolve(id + ".json"), RECIPE_LIMIT, id), StandardCharsets.UTF_8));
    }

    private Saved load(String id) throws Exception {
        var recipe = recipe(id);
        return new Saved(recipe, checkedPortrait(recipe));
    }

    private static Portrait checkedPortrait(JSONObject recipe) throws Exception {
        SpatialScene.keys(recipe, "format", "name", "image", "rig", "wav", "hashes");
        SpatialScene.text(recipe, "name", 64);
        if (!FORMAT.equals(recipe.getString("format"))) throw new IllegalArgumentException("Unsupported avatar recipe.");
        var portrait = portrait(recipe.getString("image"), recipe.getJSONObject("rig"), recipe.getString("wav"));
        if (!portrait.hashes().similar(recipe.getJSONObject("hashes")))
            throw new IllegalArgumentException("Rebuilt avatar differs from the saved outputs.");
        return portrait;
    }

    private static JSONObject response(String id, JSONObject recipe, Portrait p) {
        return manifest(id, recipe, p).put("poses", b64List(p.poses)).put("sprite", b64(p.sprite))
                .put("walk", b64(p.walk)).put("image", recipe.getString("image")).put("wav", recipe.getString("wav"));
    }

    private static JSONObject manifest(String id, JSONObject recipe, Portrait p) {
        return new JSONObject().put("id", id).put("format", FORMAT).put("name", recipe.getString("name"))
                .put("rig", p.rig.json()).put("hashes", p.hashes()).put("spriteWidth", SPRITE_W).put("spriteHeight", SPRITE_H)
                .put("walkFrames", 4).put("videoFps", FPS).put("mouth", new JSONArray(p.envelope));
    }

    private record Saved(JSONObject recipe, Portrait portrait) {}
    private record Rig(double x, double y, double w, double h, double mouthX, double mouthY) {
        static Rig parse(JSONObject input) {
            SpatialScene.keys(input, "x", "y", "w", "h", "mouthX", "mouthY");
            var rig = new Rig(SpatialScene.number(input,"x",0,.99), SpatialScene.number(input,"y",0,.99),
                    SpatialScene.number(input,"w",.08,1), SpatialScene.number(input,"h",.12,1),
                    SpatialScene.number(input,"mouthX",0,1), SpatialScene.number(input,"mouthY",0,1));
            double mx = (rig.mouthX-rig.x)/rig.w, my = (rig.mouthY-rig.y)/rig.h;
            if (rig.x+rig.w>1.0000001 || rig.y+rig.h>1.0000001 || mx<.2 || mx>.8 || my<.45 || my>.93)
                throw new IllegalArgumentException("Draw a head crop and place the mouth inside its lower half.");
            return rig;
        }
        JSONObject json() {
            return new JSONObject().put("x",x).put("y",y).put("w",w).put("h",h)
                    .put("mouthX",mouthX).put("mouthY",mouthY);
        }
    }

    private record Portrait(Rig rig, String imageSha, String wavSha, List<byte[]> poses, byte[] sprite,
                            byte[] walk, int[] envelope, byte[] pcm) {
        JSONObject hashes() {
            var frames = new JSONArray();
            for (byte[] png : poses) frames.put(MapDefinition.sha(png));
            return new JSONObject().put("imageSha256",imageSha).put("audioSha256",wavSha)
                    .put("poseSha256",frames).put("spriteSha256",MapDefinition.sha(sprite))
                    .put("walkSha256",MapDefinition.sha(walk));
        }
    }

    private static Portrait portrait(String png, JSONObject marks, String wav) throws Exception {
        byte[] imageBytes = SpriteWorkshop.decode(png, IMAGE_LIMIT);
        BufferedImage image = BrowserShapeTrace.decode(imageBytes, 8);
        if (image.getWidth() > 800 || image.getHeight() > 800)
            throw new IllegalArgumentException("Import an image at up to 800 pixels per side.");
        Rig rig = Rig.parse(marks);
        byte[] wave = SpriteWorkshop.decode(wav, AUDIO_LIMIT);
        byte[] pcm = pcm(wave);
        var poses = new ArrayList<byte[]>(POSES);
        for (int i=0; i<POSES; i++) poses.add(png(frame(image, rig, i)));
        BufferedImage head = head(image, rig);
        BufferedImage idle = sprite(head, 0, 0);
        var walk = new BufferedImage(SPRITE_W*4, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
        var g = walk.createGraphics();
        for (int i=0; i<4; i++)
            g.drawImage(sprite(head, new int[]{-10,0,10,0}[i], (i%2)*2), i*SPRITE_W, 0, null);
        g.dispose();
        return new Portrait(rig, MapDefinition.sha(imageBytes), wave.length==0?"":MapDefinition.sha(wave),
                poses, png(idle), png(walk), envelope(pcm), pcm);
    }

    private static byte[] pcm(byte[] wav) throws Exception {
        if (wav.length==0) return wav;
        if (wav.length<44 || !"RIFF".equals(new String(wav,0,4,StandardCharsets.US_ASCII))
                || !"WAVE".equals(new String(wav,8,4,StandardCharsets.US_ASCII)))
            throw new IllegalArgumentException("Choose a WAV recording.");
        try (var stream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
            var f = stream.getFormat();
            long count = stream.getFrameLength();
            if (f.getChannels()!=1 || f.getSampleRate()!=HZ || f.getSampleSizeInBits()!=16 || f.isBigEndian()
                    || !AudioFormat.Encoding.PCM_SIGNED.equals(f.getEncoding()) || count < HZ/8 || count > HZ*30)
                throw new IllegalArgumentException("Audio must be 0.125–30 seconds of mono 24 kHz PCM.");
            byte[] data = stream.readNBytes(Math.toIntExact(count*2));
            if (data.length != count*2) throw new IllegalArgumentException("Audio data is incomplete.");
            int size = (data.length+PCM_PER_FRAME-1)/PCM_PER_FRAME*PCM_PER_FRAME;
            return Arrays.copyOf(data, size);
        }
    }

    private static int[] envelope(byte[] pcm) {
        var energy = new double[pcm.length/PCM_PER_FRAME];
        for (int f=0; f<energy.length; f++) {
            double sum = 0;
            for (int i=0; i<PCM_PER_FRAME; i+=2) {
                int offset = f*PCM_PER_FRAME+i;
                double sample = (short)((pcm[offset]&255)|(pcm[offset+1]<<8))/32768.0;
                sum += sample*sample;
            }
            energy[f] = Math.sqrt(sum/(PCM_PER_FRAME/2));
        }
        if (energy.length==0) return new int[0];
        double[] ordered = energy.clone();
        Arrays.sort(ordered);
        double peak = Math.max(.055, ordered[(ordered.length-1)*9/10]);
        int[] mouth = new int[energy.length];
        for (int f=0; f<energy.length; f++)
            if (energy[f]>.012) mouth[f] = Math.min(POSES-1, (int)Math.ceil(energy[f]/peak*(POSES-1)));
        return mouth;
    }

    private static BufferedImage frame(BufferedImage image, Rig rig, int pose) {
        var frame = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var g = graphics(frame);
        g.setColor(new Color(16,25,37));
        g.fillRect(0,0,WIDTH,HEIGHT);
        double scale = Math.min((double)WIDTH/image.getWidth(), (double)HEIGHT/image.getHeight());
        int w = (int)Math.round(image.getWidth()*scale), h = (int)Math.round(image.getHeight()*scale);
        int x = (WIDTH-w)/2, y = (HEIGHT-h)/2;
        g.drawImage(image,x,y,w,h,null);
        g.dispose();
        if (pose==0) return frame;
        double cx = x+rig.mouthX*w, cy = y+rig.mouthY*h;
        double rx = Math.max(3,rig.w*w*.21), ry = Math.max(3,rig.h*h*.105);
        int top = Math.max(0,(int)(cy-ry)-1), bottom = Math.min(HEIGHT-1,(int)(cy+ry)+1);
        int left = Math.max(0,(int)(cx-rx)-1), right = Math.min(WIDTH-1,(int)(cx+rx)+1);
        int[] original = frame.getRGB(left,top,right-left+1,bottom-top+1,null,0,right-left+1);
        for (int py=top; py<=bottom; py++) {
            for (int px=left; px<=right; px++) {
                double dx=(px-cx)/rx, dy=(py-cy)/ry, weight=Math.max(0,1-dx*dx-dy*dy);
                int from=(int)Math.round(py-(py-cy)*weight*pose*.19);
                frame.setRGB(px,py,original[(from-top)*(right-left+1)+px-left]);
            }
        }
        return frame;
    }

    private static BufferedImage head(BufferedImage image, Rig rig) {
        var out = new BufferedImage(62,66,BufferedImage.TYPE_INT_ARGB);
        var g = graphics(out);
        var shape = new Path2D.Double();
        shape.moveTo(31,0);
        shape.curveTo(55,0,61,13,59,31);
        shape.curveTo(65,40,56,50,51,53);
        shape.curveTo(45,63,39,65,31,66);
        shape.curveTo(23,65,17,63,11,53);
        shape.curveTo(6,50,-3,40,3,31);
        shape.curveTo(1,13,7,0,31,0);
        shape.closePath();
        g.setClip(shape);
        int x=(int)Math.round(rig.x*image.getWidth()), y=(int)Math.round(rig.y*image.getHeight());
        int r=(int)Math.round((rig.x+rig.w)*image.getWidth()), b=(int)Math.round((rig.y+rig.h)*image.getHeight());
        g.drawImage(image,0,0,62,66,x,y,r,b,null);
        g.dispose();
        return out;
    }

    private static BufferedImage sprite(BufferedImage head, int stride, int bob) {
        var out = new BufferedImage(SPRITE_W,SPRITE_H,BufferedImage.TYPE_INT_ARGB);
        var g = graphics(out);
        g.translate(0,-bob);
        line(g, new Color(21,31,53), 13,40,91,36+stride,102);
        line(g, new Color(34,48,72), 13,55,91,60-stride,102);
        line(g, new Color(12,21,32), 7,31+stride,105,42+stride,105);
        line(g, new Color(12,21,32), 7,56-stride,105,67-stride,105);
        line(g, new Color(57,107,151), 11,31,71,25-stride/3,92);
        line(g, new Color(80,132,174), 11,64,71,69+stride/3,92);
        line(g, new Color(192,120,91), 6,25-stride/3,92,25-stride/3,94);
        line(g, new Color(192,120,91), 6,69+stride/3,92,69+stride/3,94);
        g.setColor(new Color(16,26,43)); g.fillRoundRect(29,63,38,34,14,14);
        g.setColor(new Color(92,155,195)); g.fillRoundRect(32,64,32,29,12,12);
        g.setColor(new Color(147,47,64)); g.fillRoundRect(43,64,10,28,5,5);
        g.setColor(new Color(116,219,208)); g.fillRect(55,75,5,3);
        g.drawImage(head,16,4,null);
        g.dispose();
        return out;
    }

    private static void line(Graphics2D g, Color color, int width, int x, int y, int x2, int y2) {
        g.setColor(color);
        g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.drawLine(x,y,x2,y2);
    }

    private static Graphics2D graphics(BufferedImage image) {
        var g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return g;
    }

    private static byte[] png(BufferedImage image) throws IOException {
        var out = new ByteArrayOutputStream();
        if (!ImageIO.write(image,"png",out)) throw new IOException("PNG encoder unavailable.");
        return out.toByteArray();
    }

    private static byte[] video(Portrait p) throws IOException {
        if (p.envelope.length==0) throw new IllegalArgumentException("Add audio before exporting a talking video.");
        var frames = videoFrames(p,.76f);
        if (aviSize(frames,p.pcm) > 19_900_000) frames = videoFrames(p,.53f);
        if (aviSize(frames,p.pcm) > 19_900_000) frames = videoFrames(p,.32f);
        if (aviSize(frames,p.pcm) > 19_900_000)
            throw new IOException("The talking video exceeds 20 MB. Use a shorter recording.");
        byte[] avi = avi(frames,p.pcm);
        checkAvi(avi,frames,p.pcm);
        return avi;
    }

    private static long aviSize(List<byte[]> frames, byte[] pcm) {
        return frames.stream().mapToLong(frame->frame.length+56L).sum()+pcm.length+1024;
    }

    private static List<byte[]> videoFrames(Portrait p, float quality) throws IOException {
        var jpeg = new ArrayList<byte[]>(POSES);
        var writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IOException("JPEG encoder unavailable.");
        var writer = writers.next();
        try {
            var settings = writer.getDefaultWriteParam();
            settings.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            settings.setCompressionQuality(quality);
            for (byte[] pose : p.poses) {
                var image = ImageIO.read(new ByteArrayInputStream(pose));
                var out = new ByteArrayOutputStream();
                try (var stream = ImageIO.createImageOutputStream(out)) {
                    writer.setOutput(stream);
                    writer.write(null,new IIOImage(image,null,null),settings);
                }
                jpeg.add(out.toByteArray());
            }
        } finally {
            writer.dispose();
        }
        var frames = new ArrayList<byte[]>(p.envelope.length);
        for (int pose : p.envelope) frames.add(jpeg.get(pose));
        return frames;
    }

    private static String b64(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }
    private static JSONArray b64List(List<byte[]> bytes) {
        var out = new JSONArray();
        for (byte[] item : bytes) out.put(b64(item));
        return out;
    }
}
