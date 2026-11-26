package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Durable local desk mission with source, projection, ray, media and calendar checks. */
final class DeskRecordingMission {
    static final int VIDEO_LIMIT = 24 * 1024 * 1024;
    private static final int FRAME_LIMIT = 100_000, SAMPLES = 80, SECONDS = 10, SOURCE_FPS = 8;
    private static final int AVI_LIMIT = 20_000_000;
    private static final DateTimeFormatter ICAL = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'",Locale.ROOT)
            .withZone(ZoneOffset.UTC);
    private final Path root;
    private final EnvironmentStore environments;

    DeskRecordingMission(Path project, EnvironmentStore environments) {
        this.root = project.resolve("desk-recordings");
        this.environments = environments;
    }

    synchronized JSONObject start(JSONObject args) throws Exception {
        SpatialScene.keys(args,"environmentId","setup","video","frames","sourceName","startS","anchorSample");
        String scene = args.getString("environmentId");
        environment(scene);
        var setup = args.getJSONObject("setup");
        SpatialScene.keys(setup,"fit","heldOut","deskM","doorway","availability","originalRecording");
        available(setup.getJSONObject("availability"));
        double startS=SpatialScene.number(args,"startS",0,14400);
        String name=SpatialScene.text(args,"sourceName",150);
        require(!name.matches(".*[/\\\\].*"),"Choose a local source file.");
        byte[] video=SpriteWorkshop.decode(args.getString("video"),VIDEO_LIMIT);
        require(validContainer(video),"Choose a decoded local MP4, MOV or WebM recording.");
        JSONArray frames=args.getJSONArray("frames");
        require(frames.length()==SAMPLES,"The recording needs 80 decoded frames covering ten seconds.");
        int anchorSample=args.has("anchorSample")?SpriteWorkshop.integer(args,"anchorSample",0,SAMPLES-1):0;
        var source=new JSONObject().put("videoSha256",MapDefinition.sha(video)).put("startS",startS)
                .put("anchorSample",anchorSample)
                .put("durationS",SECONDS).put("samplesPerSecond",SOURCE_FPS).put("frameSha256",new JSONArray());
        if(setup.has("originalRecording")) {
            var original=setup.getJSONObject("originalRecording");
            SpatialScene.keys(original,"name","sha256","preparedFromS");
            SpatialScene.text(original,"name",150);
            require(original.getString("sha256").matches("[a-f0-9]{64}"),"Invalid original recording checksum.");
            SpatialScene.number(original,"preparedFromS",0,14400);
            source.put("declaredOriginal",original);
        }
        var parts=new ArrayList<byte[]>();
        for (Object value : frames) {
            byte[] frame = SpriteWorkshop.decode((String)value,FRAME_LIMIT);
            checkFrame(frame);
            parts.add(frame);
            source.getJSONArray("frameSha256").put(MapDefinition.sha(frame));
        }
        String bound = WorldFrames.hash(new JSONObject().put("environmentId",scene).put("setup",setup).put("source",source));
        var mission = scope(bound);
        if (Files.exists(mission.resolve("mission.json"),LinkOption.NOFOLLOW_LINKS)) {
            var previous=verified(bound);
            latest(bound);
            return view(previous);
        }
        Files.createDirectories(mission);
        SpriteWorkshop.replace(mission.resolve("source.video"),video);
        for(int i=0;i<parts.size();i++) SpriteWorkshop.replace(mission.resolve("frame-"+i+".jpg"),parts.get(i));
        var record = new JSONObject().put("format","fraynix.desk-recording-mission-v1").put("id",bound)
                .put("environmentId",scene).put("setup",setup).put("source",source.put("name",name)).put("stage","prepared");
        AgentFiles042.saveChecked(mission.resolve("mission.json"),record);
        latest(bound);
        return view(record);
    }

    synchronized JSONObject read(JSONObject args) throws Exception {
        SpatialScene.keys(args,"id");
        String id=args.optString("id");
        if (id.isEmpty()) {
            Path latest=root.resolve("latest");
            if (!Files.exists(latest,LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
            id=new String(SpriteWorkshop.read(latest,64),StandardCharsets.US_ASCII);
        }
        return view(verified(id));
    }

    synchronized JSONObject advance(JSONObject args) throws Exception {
        SpatialScene.keys(args,"id","footageOnly");
        String id=args.getString("id");
        JSONObject record=verified(id);
        Path folder=scope(id);
        switch(record.getString("stage")) {
            case "prepared" -> {
                try {
                    record.put("anchor",anchor(record)).put("stage","anchored");
                } catch(IllegalArgumentException failure) {
                    if(!Boolean.TRUE.equals(args.opt("footageOnly"))) throw failure;
                    record.put("anchorFailure",failure.getMessage()).put("stage","anchor-unresolved");
                }
            }
            case "anchored", "anchor-unresolved" -> record.put("recording",new JSONObject().put("frameCount",SAMPLES).put("framesPerSecond",SOURCE_FPS)
                    .put("durationS",SECONDS).put("audio","silent preview").put("anchorFrame",
                            record.has("anchor") ? record.getJSONObject("source").optInt("anchorSample") : JSONObject.NULL)
                    .put("description","Ten-second excerpt from the existing recording."))
                    .put("stage","recorded");
            case "recorded" -> {
                List<byte[]> frames=frames(record);
                var repeated=new ArrayList<byte[]>();
                for(byte[] image:frames) for(int i=0;i<MotionMediaContainers.FPS/SOURCE_FPS;i++) repeated.add(image);
                byte[] silence=new byte[MotionMediaContainers.PCM_PER_FRAME*repeated.size()];
                byte[] avi=MotionMediaContainers.avi(repeated,silence);
                require(avi.length<=AVI_LIMIT,"The encoded preview exceeds 20 MB.");
                MotionMediaContainers.checkAvi(avi,repeated,silence);
                SpriteWorkshop.replace(folder.resolve("preview.avi"),avi);
                record.put("preview",new JSONObject().put("sha256",MapDefinition.sha(avi)).put("outputFrames",repeated.size())
                        .put("fps",MotionMediaContainers.FPS).put("durationS",SECONDS).put("sourceSamples",SAMPLES))
                        .put("stage","previewed");
            }
            case "previewed" -> {
                var availability=record.getJSONObject("setup").getJSONObject("availability");
                Slot slot=available(availability);
                byte[] ics=draft(id,slot);
                SpriteWorkshop.replace(folder.resolve("draft.ics"),ics);
                record.put("draft",new JSONObject().put("start",slot.start.toString()).put("end",slot.end.toString())
                        .put("timezone",slot.zone.getId()).put("availabilitySource",availability.getString("source"))
                        .put("availabilityReadAt",slot.observed.toString()).put("sha256",MapDefinition.sha(ics))
                        .put("state","local-calendar-draft")).put("stage",record.has("anchor")?
                                "draft-ready":"draft-anchor-unresolved");
            }
            case "draft-ready", "draft-anchor-unresolved" -> { return view(record); }
            default -> throw new IllegalStateException("Unknown desk mission stage.");
        }
        AgentFiles042.saveChecked(folder.resolve("mission.json"),record);
        return view(verified(id));
    }

    synchronized JSONObject artifact(JSONObject args) throws Exception {
        SpatialScene.keys(args,"id","format");
        var record=verified(args.getString("id"));
        String id=record.getString("id"), format=args.getString("format");
        Path dir=scope(id);
        if (format.equals("frames")) {
            require(record.has("preview"),"Generate the preview first.");
            var images=new JSONArray();
            for(byte[] image:frames(record)) images.put(Base64.getEncoder().encodeToString(image));
            return new JSONObject().put("jpegFrames",images).put("framesPerSecond",SOURCE_FPS);
        }
        if (format.equals("avi")) {
            require(record.has("preview"),"Generate the preview first.");
            return artifact(dir.resolve("preview.avi"),AVI_LIMIT,record.getJSONObject("preview"));
        }
        if (format.equals("ics")) {
            require(record.has("draft"),"Prepare the calendar draft first.");
            return artifact(dir.resolve("draft.ics"),4096,record.getJSONObject("draft"));
        }
        throw new IllegalArgumentException("Choose the preview AVI or calendar draft ICS.");
    }

    private JSONObject anchor(JSONObject record) throws Exception {
        var source=environment(record.getString("environmentId"));
        var setup=record.getJSONObject("setup");
        var camera=DeskCameraAnchor.fit(setup.getJSONArray("fit"),setup.getJSONArray("heldOut"));
        double[] desk=SpatialScene.vector(setup.getJSONArray("deskM"),1000);
        double[] image=camera.project(desk);
        require(image[0]>=0 && image[0]<DeskCameraAnchor.WIDTH && image[1]>=0 && image[1]<DeskCameraAnchor.HEIGHT,
                "The proposed desk target is outside the camera frame.");
        var door = setup.getJSONObject("doorway");
        SpatialScene.keys(door,"fromM","toM","halfWidthM");
        double[] from=SpatialScene.vector(door.getJSONArray("fromM"),1000);
        double[] to=SpatialScene.vector(door.getJSONArray("toM"),1000);
        double half=SpatialScene.number(door,"halfWidthM",.1,.75);
        double[] d=DeskCameraAnchor.minus(to,from);
        double distance=DeskCameraAnchor.length(d);
        require(distance>=.2 && distance<=4 && Math.abs(d[1])<1e-6,"Use a level doorway segment of 0.2–4 m.");
        double[] sideways={-d[2]/distance,0,d[0]/distance};
        var probes=new GlbSection.Ray[4];
        double[] view=DeskCameraAnchor.minus(desk,camera.center());
        double viewDistance=DeskCameraAnchor.length(view);
        require(viewDistance>=.35 && viewDistance<=30,"The desk position is outside the supported viewing distance.");
        probes[0]=ray(camera.center(),view,viewDistance-.035);
        for(int i=0;i<3;i++) {
            double offset=(i-1)*half;
            double[] origin={from[0]+sideways[0]*offset,from[1],from[2]+sideways[2]*offset};
            probes[i+1]=ray(origin,d,distance);
        }
        byte[] glb=SpriteWorkshop.decode(source.getString("base64"),EnvironmentStore.MAX_BYTES);
        var hits=GlbSection.probe(glb,source.getJSONArray("nodes"),probes);
        require(hits[0]==null,"Source geometry occludes the proposed desk target.");
        require(Arrays.stream(hits,1,4).allMatch(hit->hit==null),"A centre or edge ray through the proposed doorway is blocked.");
        var recordingSource=record.getJSONObject("source");
        int sample=recordingSource.optInt("anchorSample");
        return camera.evidence().put("anchorSample",sample)
                .put("anchorTimeS",recordingSource.getDouble("startS")+(double)sample/SOURCE_FPS)
                .put("anchorFrameSha256",recordingSource.getJSONArray("frameSha256").getString(sample))
                .put("deskPixel",new JSONArray(image)).put("deskSightlineM",viewDistance)
                .put("doorwayProbes",3).put("doorwayProbeWidthM",2*half).put("doorwayLengthM",distance)
                .put("basis","Source GLB model units and labelled pixels in the selected recording frame. Doorway rays test clearance at one declared height.");
    }

    private JSONObject environment(String id) throws Exception {
        require(id.matches("[0-9a-f]{64}"),"Select a retained environment.");
        return environments.open(new JSONObject().put("id",id),true);
    }

    private static GlbSection.Ray ray(double[] origin,double[] d,double length) {
        double magnitude=DeskCameraAnchor.length(d);
        return new GlbSection.Ray(origin,Arrays.stream(d).map(v->v/magnitude).toArray(),length);
    }

    private record Busy(Instant start,Instant end) {}
    private record Slot(Instant start,Instant end,Instant observed,ZoneId zone) {}

    private static Slot available(JSONObject data) {
        SpatialScene.keys(data,"windowStart","windowEnd","capturedAt","timezone","source","busy");
        SpatialScene.text(data,"source",80);
        var first=Instant.parse(data.getString("windowStart"));
        var last=Instant.parse(data.getString("windowEnd"));
        var observed=Instant.parse(data.getString("capturedAt"));
        var zone=ZoneId.of(data.getString("timezone"));
        require(first.isBefore(last) && Duration.between(first,last).toHours()<=72 && observed.isBefore(last),
                "Supply a bounded calendar availability window after the observation.");
        var claimed=data.getJSONArray("busy");
        require(claimed.length()<=200,"Too many calendar busy intervals.");
        var spans=new ArrayList<Busy>();
        for(Object value:claimed) {
            var row=(JSONObject)value;
            SpatialScene.keys(row,"start","end");
            var start=Instant.parse(row.getString("start"));
            var end=Instant.parse(row.getString("end"));
            require(start.isBefore(end),"A busy interval needs an end after its start.");
            spans.add(new Busy(start,end));
        }
        spans.sort(Comparator.comparing(Busy::start));
        var minimum=first.isAfter(observed.plusSeconds(300))?first:observed.plusSeconds(300);
        Instant candidate=quarter(minimum);
        for(Busy busy:spans) {
            if(!candidate.isBefore(busy.end)) continue;
            if(!candidate.plusSeconds(1800).isAfter(busy.start)) break;
            candidate=quarter(busy.end);
        }
        Instant end=candidate.plusSeconds(1800);
        require(!end.isAfter(last),"No thirty-minute free period was found in the calendar window.");
        return new Slot(candidate,end,observed,zone);
    }

    private static Instant quarter(Instant time) {
        long seconds=time.getEpochSecond()+(time.getNano()>0?1:0);
        return Instant.ofEpochSecond(Math.floorDiv(seconds+899,900)*900);
    }

    private static byte[] draft(String id,Slot slot) {
        String text=String.join("\r\n","BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//Fraynix//Desk recording//EN",
                "CALSCALE:GREGORIAN","BEGIN:VEVENT","UID:"+id.substring(0,32)+"@fraynix.local",
                "DTSTAMP:"+ICAL.format(slot.observed),"DTSTART:"+ICAL.format(slot.start),"DTEND:"+ICAL.format(slot.end),
                "SUMMARY:Desk recording","STATUS:TENTATIVE","TRANSP:OPAQUE","CLASS:PRIVATE","END:VEVENT","END:VCALENDAR","");
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private JSONObject verified(String id) throws Exception {
        Path dir=scope(id);
        JSONObject record=AgentFiles042.loadChecked(dir.resolve("mission.json"));
        require(id.equals(record.getString("id")),"The saved mission identity changed.");
        var source=record.getJSONObject("source");
        SpriteWorkshop.read(dir.resolve("source.video"),VIDEO_LIMIT,source.getString("videoSha256"));
        frames(record);
        environment(record.getString("environmentId"));
        if(record.has("preview")) artifact(dir.resolve("preview.avi"),AVI_LIMIT,record.getJSONObject("preview"));
        if(record.has("draft")) artifact(dir.resolve("draft.ics"),4096,record.getJSONObject("draft"));
        return record;
    }

    private List<byte[]> frames(JSONObject record) throws IOException {
        Path dir=scope(record.getString("id"));
        var expected=record.getJSONObject("source").getJSONArray("frameSha256");
        var images=new ArrayList<byte[]>();
        for(int i=0;i<SAMPLES;i++) images.add(SpriteWorkshop.read(dir.resolve("frame-"+i+".jpg"),FRAME_LIMIT,expected.getString(i)));
        return images;
    }

    private static void checkFrame(byte[] frame) throws Exception {
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(frame))) {
            var readers=ImageIO.getImageReaders(input);
            require(readers.hasNext(),"A recording sample cannot be decoded.");
            var reader=readers.next();
            try {
                reader.setInput(input,true,true);
                require(reader.getFormatName().equalsIgnoreCase("jpeg") && reader.getWidth(0)==DeskCameraAnchor.WIDTH
                        && reader.getHeight(0)==DeskCameraAnchor.HEIGHT,"Recording samples must be 640 × 360 JPEG.");
                reader.read(0);
            } finally {reader.dispose();}
        }
    }

    private static boolean validContainer(byte[] bytes) {
        if(bytes.length<16) return false;
        return new String(bytes,4,4,StandardCharsets.US_ASCII).equals("ftyp")
                || (bytes[0]&255)==0x1a && (bytes[1]&255)==0x45 && (bytes[2]&255)==0xdf && (bytes[3]&255)==0xa3;
    }

    private static JSONObject artifact(Path path,int cap,JSONObject evidence) throws IOException {
        String sha=evidence.getString("sha256");
        return new JSONObject().put("sha256",sha).put("base64",Base64.getEncoder().encodeToString(SpriteWorkshop.read(path,cap,sha)));
    }

    private void latest(String id) throws IOException {
        SpriteWorkshop.replace(root.resolve("latest"),id.getBytes(StandardCharsets.US_ASCII));
    }

    private Path scope(String id) {
        require(id.matches("[a-f0-9]{64}"),"Choose a valid desk mission.");
        return root.resolve(id);
    }

    private static JSONObject view(JSONObject record) {
        var result=new JSONObject(record.toString());
        result.remove("setup");
        result.getJSONObject("source").remove("frameSha256");
        return result;
    }

    private static void require(boolean value,String message) {
        if(!value) throw new IllegalArgumentException(message);
    }
}
