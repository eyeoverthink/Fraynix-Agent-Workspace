package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Local creative intent, verified references and deliberately scoped editing choices. */
final class CreativeProjects {
    private static final String FORMAT = "fraynix.creative-project-v1";
    private static final int MAX_PROJECTS = 32, MAX_APPROVALS = 64, MAX_CHOICES = 24;
    private static final Pattern DERIVATIVE = Pattern.compile(
            "\\b(another|like (the |that |my )?(last|previous|approved)|new cut|same style)\\b");
    private static final Pattern QUICKER = Pattern.compile("\\b(quicker|faster|snappier|tighter)\\b");
    private static final Pattern SLOWER = Pattern.compile("\\b(slower|longer|unhurried)\\b");
    private static final Pattern LONG_OPENING = Pattern.compile(
            "\\b(keep|make|hold|leave)\\b.{0,35}\\b(opening|intro)\\b.{0,30}\\b(slow|long|longer|slower|unhurried)\\b|\\b(longer|slower) (opening|intro)\\b");
    private static final Pattern SHORT_OPENING = Pattern.compile(
            "\\b(keep|make|cut|trim)\\b.{0,35}\\b(opening|intro)\\b.{0,30}\\b(short|shorter|quick|quicker|faster|snappy)\\b|\\b(shorter|quicker|faster) (opening|intro)\\b");
    private static final Pattern FAST_REST = Pattern.compile(
            "\\b(speed up|shorten|tighten|quicken) (the )?(rest|remainder|second shot)\\b");
    private final Path projects, copies;
    private final MediaEditStore media;

    CreativeProjects(Path base, MediaEditStore media) {
        projects = base.resolve("creative-projects");
        copies = base.resolve("audience-exports");
        this.media = media;
    }

    synchronized JSONObject handle(String command, JSONObject input) throws Exception {
        if (command.equals("list")) {
            SpatialScene.keys(input);
            return new JSONObject().put("projects", list());
        }
        if (command.equals("create")) return create(input);
        JSONObject state = load(input.getString("project"));
        return switch (command) {
            case "open" -> {
                SpatialScene.keys(input, "project");
                yield state;
            }
            case "intent" -> {
                SpatialScene.keys(input, "project", "intent");
                yield save(state.put("intent", SpatialScene.text(input, "intent", 180)));
            }
            case "approve-cut" -> approveCut(state, input);
            case "propose" -> propose(state, input);
            case "correct" -> correct(state, input);
            case "approve-draft" -> approveDraft(state, input);
            case "undo-choice" -> undoChoice(state, input);
            case "review-share" -> reviewShare(state, input);
            case "approve-share" -> approveShare(state, input);
            case "export-share" -> exportShare(state, input);
            case "locate-share" -> locateShare(state, input);
            case "play" -> play(state, input);
            default -> throw new IllegalArgumentException("Unknown creative project action.");
        };
    }

    private JSONArray list() throws IOException {
        JSONArray summaries = new JSONArray();
        if (!Files.isDirectory(projects, LinkOption.NOFOLLOW_LINKS)) return summaries;
        try (var files = Files.list(projects)) {
            for (Path file : files.filter(p -> p.getFileName().toString().matches("[a-f0-9-]{36}[.]json")).sorted().toList()) {
                JSONObject project = AgentFiles042.loadChecked(file);
                String id = project.getString("id");
                if (!file.equals(path(id))) throw new IOException("Creative project filename differs from its record.");
                summaries.put(new JSONObject().put("id", id).put("name", project.getString("name")));
            }
        }
        return summaries;
    }

    private JSONObject create(JSONObject input) throws IOException {
        SpatialScene.keys(input, "name", "intent");
        if (list().length() >= MAX_PROJECTS) throw new IllegalArgumentException("This workspace already has 32 creative projects.");
        return save(new JSONObject().put("format", FORMAT).put("id", newId())
                .put("name", SpatialScene.text(input, "name", 75)).put("intent", SpatialScene.text(input, "intent", 180))
                .put("approved", new JSONArray()).put("choices", new JSONArray()));
    }

    private JSONObject approveCut(JSONObject state, JSONObject input) throws Exception {
        SpatialScene.keys(input, "project", "revision");
        recordCut(state, media.verifiedRevision(input.getString("revision")));
        return save(state);
    }

    private void recordCut(JSONObject state, JSONObject cut) {
        JSONArray approvals = state.getJSONArray("approved");
        if (approved(state, cut.getString("id")) != null) return;
        if (approvals.length() >= MAX_APPROVALS) throw new IllegalArgumentException("This creative project holds 64 approved cuts.");
        var def = cut.getJSONObject("definition");
        approvals.put(new JSONObject().put("id", cut.getString("id")).put("title", def.getString("title"))
                .put("frames", cut.getJSONObject("metrics").getInt("frames")).put("approvedAt", Instant.now().toString()));
    }

    private JSONObject propose(JSONObject state, JSONObject input) throws Exception {
        SpatialScene.keys(input, "project", "request", "opening", "followup", "audio", "reference", "useMemory");
        String request = SpatialScene.text(input, "request", 220);
        String words = words(request);
        if (!DERIVATIVE.matcher(words).find())
            return new JSONObject().put("clarification", "Ask for a new cut like an approved one. You can ask for a quicker or slower cut.");
        String audience = audienceFrom(words);
        if (audience == null)
            return new JSONObject().put("clarification", "Is this cut for family, for public sharing, or for you to keep private?");
        JSONArray approvals = state.getJSONArray("approved");
        if (approvals.isEmpty()) throw new IllegalArgumentException("Approve a saved timeline cut to use as a reference.");
        String reference = input.optString("reference", "");
        if (reference.isBlank() && approvals.length() > 1)
            return new JSONObject().put("clarification", "Which approved cut should I use as the reference?").put("references", approvals);
        if (reference.isBlank()) reference = approvals.getJSONObject(0).getString("id");
        if (approved(state, reference) == null) throw new IllegalArgumentException("Choose an approved cut from this project.");
        JSONObject parent = media.verifiedRevision(reference);
        String opening = input.getString("opening"), followup = input.getString("followup"), audio = input.optString("audio", "");
        if (opening.equals(followup) || opening.isBlank() || followup.isBlank())
            throw new IllegalArgumentException("Choose two different fresh picture sources.");
        JSONObject referenceAssets = parent.getJSONObject("definition").getJSONObject("assets");
        Set<String> previouslyUsedBytes = new HashSet<>();
        for (String id : referenceAssets.keySet())
            previouslyUsedBytes.add(referenceAssets.getJSONObject(id).getJSONObject("source").getString("sha256"));
        for (String id : List.of(opening, followup)) {
            JSONObject source = media.assetSummary(id).getJSONObject("source");
            if (!previouslyUsedBytes.add(source.getString("sha256")))
                throw new IllegalArgumentException("Choose fresh picture sources with distinct bytes from each other and from the approved reference.");
        }
        JSONObject choice = currentChoice(state);
        boolean usedMemory = input.getBoolean("useMemory") && choice != null;
        int bias = usedMemory ? choice.getInt("frames") : 0;
        if (usedMemory) media.verifiedRevision(choice.getString("revision"));
        int requestTrim = QUICKER.matcher(words).find() ? -2 : SLOWER.matcher(words).find() ? 2 : 0;
        JSONObject draft = new JSONObject().put("reference", reference).put("request", request).put("audience", audience)
                .put("openingAsset", opening).put("followupAsset", followup).put("audioAsset", audio)
                .put("bias", bias).put("requestTrim", requestTrim).put("restTrim", 0);
        List<JSONObject> pictures = new ArrayList<>();
        for (Object value : parent.getJSONObject("definition").getJSONArray("strips")) {
            JSONObject strip = (JSONObject) value;
            if (strip.getString("lane").equals("PICTURE")) pictures.add(strip);
        }
        pictures.sort(Comparator.comparingInt(row -> row.getInt("start")));
        draft.put("referenceOpeningFrames", referenceLength(pictures, 0))
                .put("referenceFollowupFrames", referenceLength(pictures, 1));
        if (usedMemory) draft.put("memory", choice);
        renderDraft(state, draft);
        return save(state.put("draft", draft));
    }

    private JSONObject correct(JSONObject state, JSONObject input) throws Exception {
        SpatialScene.keys(input, "project", "draft", "request", "remember");
        JSONObject draft = draft(state, input);
        String request = SpatialScene.text(input, "request", 220);
        String words = words(request);
        boolean longer = LONG_OPENING.matcher(words).find(), shorter = SHORT_OPENING.matcher(words).find();
        boolean rest = FAST_REST.matcher(words).find();
        if (longer && shorter || !longer && !shorter && !rest || input.getBoolean("remember") && !longer && !shorter)
            return new JSONObject().put("clarification", "Say whether to keep the opening slow or cut it shorter. You can also ask to speed up the rest for this cut.");
        if (longer || shorter) {
            draft.put("bias", longer ? 4 : -4);
            draft.remove("memory");
            draft.put("remember", input.getBoolean("remember"));
        }
        if (rest) draft.put("restTrim", -2);
        draft.put("correction", request);
        renderDraft(state, draft);
        return save(state.put("draft", draft));
    }

    private void renderDraft(JSONObject state, JSONObject draft) throws Exception {
        String openingAsset = draft.getString("openingAsset"), followupAsset = draft.getString("followupAsset");
        int openingAvailable = pictureFrames(openingAsset), followupAvailable = pictureFrames(followupAsset);
        int baseline = draft.getInt("referenceOpeningFrames") + draft.getInt("requestTrim");
        int requestedOpening = baseline + draft.getInt("bias");
        int opening = Math.min(requestedOpening, openingAvailable);
        int requestedFollowup = draft.getInt("referenceFollowupFrames") + draft.getInt("requestTrim") + draft.getInt("restTrim");
        int followup = Math.min(requestedFollowup, followupAvailable);
        var strips = new JSONArray().put(strip(openingAsset, "PICTURE", 0, opening))
                .put(strip(followupAsset, "PICTURE", opening, followup));
        String audio = draft.getString("audioAsset");
        if (!audio.isBlank()) {
            int samples = media.assetSummary(audio).getInt("soundFrames");
            if (samples == 0) throw new IllegalArgumentException("Choose a source with checked audio.");
            strips.put(strip(audio, "AUDIO1", 0, Math.min(samples, opening + followup)).put("gain", 0.8));
        }
        var args = new JSONObject().put("title", "New cut · " + draft.getString("audience"))
                .put("parent", draft.getString("reference")).put("strips", strips);
        JSONObject rendered = media.renderPreview(args);
        draft.put("id", newId()).put("revision", rendered.getString("id")).put("openingFrames", opening)
                .put("followupFrames", followup).put("requestOpeningFrames", Math.min(baseline, openingAvailable))
                .put("openingLimited", opening < requestedOpening);
        if (approved(state, draft.getString("reference")) == null) throw new IOException("The reference approval changed.");
    }

    private static int referenceLength(List<JSONObject> pictures, int index) {
        return pictures.size() <= index ? 12 : Math.clamp(pictures.get(index).getInt("length"), 8, 24);
    }

    private int pictureFrames(String id) throws IOException {
        int frames = media.assetSummary(id).getInt("pictureFrames");
        if (frames < 1) throw new IllegalArgumentException("Choose a checked picture source.");
        return frames;
    }

    private static JSONObject strip(String asset, String lane, int start, int length) {
        return new JSONObject().put("assetId", asset).put("lane", lane).put("start", start).put("in", 0).put("length", length);
    }

    private JSONObject approveDraft(JSONObject state, JSONObject input) throws Exception {
        SpatialScene.keys(input, "project", "draft");
        JSONObject draft = draft(state, input);
        JSONObject cut = media.verifiedRevision(draft.getString("revision"));
        recordCut(state, cut);
        if (draft.optBoolean("remember")) {
            if (draft.getInt("openingFrames") == draft.getInt("requestOpeningFrames"))
                throw new IllegalArgumentException("These source lengths cannot show the pacing change. Use longer footage.");
            JSONArray choices = state.getJSONArray("choices");
            if (choices.length() >= MAX_CHOICES) throw new IllegalArgumentException("Undo an earlier pacing choice before saving another.");
            choices.put(new JSONObject().put("frames", draft.getInt("bias")).put("revision", cut.getString("id"))
                    .put("label", draft.getInt("bias") > 0 ? "Longer opening" : "Shorter opening")
                    .put("words", draft.getString("correction")));
        }
        state.put("finished", draft);
        state.remove("draft");
        return save(state);
    }

    private JSONObject undoChoice(JSONObject state, JSONObject input) throws IOException {
        SpatialScene.keys(input, "project");
        JSONArray choices = state.getJSONArray("choices");
        if (!choices.isEmpty()) choices.remove(choices.length() - 1);
        return save(state);
    }

    private JSONObject reviewShare(JSONObject state, JSONObject input) throws Exception {
        SpatialScene.keys(input, "project", "revision", "audience");
        String revision = input.getString("revision");
        if (approved(state, revision) == null) throw new IllegalArgumentException("Approve this cut before preparing a copy.");
        String audience = input.getString("audience");
        if (!List.of("Family", "Public").contains(audience)) throw new IllegalArgumentException("Choose Family or Public for this copy.");
        JSONObject cut = media.verifiedRevision(revision);
        state.put("share", new JSONObject().put("id", newId()).put("revision", revision).put("audience", audience)
                .put("sha256", cut.getJSONObject("files").getJSONObject("avi").getString("sha256")).put("phase", "review"));
        return save(state);
    }

    private JSONObject approveShare(JSONObject state, JSONObject input) throws Exception {
        SpatialScene.keys(input, "project", "review", "cleared");
        JSONObject share = share(state, input);
        JSONObject cut = checkShare(share);
        Set<String> cleared = new HashSet<>();
        for (Object id : input.getJSONArray("cleared"))
            if (!(id instanceof String value) || !cleared.add(value)) throw new IllegalArgumentException("Each source must be cleared once.");
        if (!cut.getJSONObject("definition").getJSONObject("assets").keySet().equals(cleared))
            throw new IllegalArgumentException("Clear each visible or audible source for this audience, or make a cut without it.");
        share.put("phase", "prepared").put("cleared", input.getJSONArray("cleared"));
        return save(state);
    }

    private JSONObject exportShare(JSONObject state, JSONObject input) throws Exception {
        SpatialScene.keys(input, "project", "review");
        JSONObject share = share(state, input);
        if (share.getString("phase").equals("review")) throw new IllegalArgumentException("Approve the source list and preview for this audience first.");
        JSONObject cut = checkShare(share);
        Set<String> cleared = new HashSet<>();
        for (Object id : share.getJSONArray("cleared")) cleared.add((String) id);
        if (!cut.getJSONObject("definition").getJSONObject("assets").keySet().equals(cleared))
            throw new IOException("The approved source list differs from the cut.");
        Path file = copyPath(share);
        byte[] video = media.verifiedVideo(cut);
        Files.createDirectories(file.getParent());
        SpriteWorkshop.replace(file, video);
        SpriteWorkshop.read(file, 16_000_000, share.getString("sha256"));
        share.put("phase", "exported");
        save(state);
        return state;
    }

    private JSONObject locateShare(JSONObject state, JSONObject input) throws IOException {
        SpatialScene.keys(input, "project", "review");
        JSONObject share = share(state, input);
        if (!share.getString("phase").equals("exported")) throw new IllegalArgumentException("Export the prepared copy first.");
        Path file = copyPath(share);
        SpriteWorkshop.read(file, 16_000_000, share.getString("sha256"));
        return new JSONObject().put("path", file.toAbsolutePath().toString());
    }

    private JSONObject checkShare(JSONObject share) throws Exception {
        JSONObject cut = media.verifiedRevision(share.getString("revision"));
        if (!share.getString("sha256").equals(cut.getJSONObject("files").getJSONObject("avi").getString("sha256")))
            throw new IOException("The cut differs from the audience preview.");
        return cut;
    }

    private Path copyPath(JSONObject share) {
        String id = share.getString("id");
        validateId(id);
        return copies.resolve(id).resolve(share.getString("audience").equals("Family") ? "family-cut.avi" : "public-cut.avi");
    }

    private JSONObject play(JSONObject state, JSONObject input) throws Exception {
        SpatialScene.keys(input, "project", "revision");
        String cut = input.getString("revision");
        if (approved(state, cut) == null && !state.optJSONObject("draft", new JSONObject()).optString("revision").equals(cut))
            throw new IllegalArgumentException("Open a draft or approved cut in this project.");
        return media.playback(cut);
    }

    private static JSONObject approved(JSONObject state, String id) {
        for (Object value : state.getJSONArray("approved")) {
            JSONObject cut = (JSONObject) value;
            if (cut.getString("id").equals(id)) return cut;
        }
        return null;
    }

    private static JSONObject currentChoice(JSONObject state) {
        JSONArray history = state.getJSONArray("choices");
        return history.isEmpty() ? null : history.getJSONObject(history.length() - 1);
    }

    private static JSONObject draft(JSONObject state, JSONObject input) {
        return exact(state, "draft", input.getString("draft"), "That draft changed. Review the current cut.");
    }

    private static JSONObject share(JSONObject state, JSONObject input) {
        return exact(state, "share", input.getString("review"), "That audience review changed. Review the current copy.");
    }

    private static JSONObject exact(JSONObject state, String key, String id, String message) {
        JSONObject value = state.optJSONObject(key);
        if (value == null || !value.getString("id").equals(id)) throw new IllegalArgumentException(message);
        return value;
    }

    private static String audienceFrom(String words) {
        boolean family = Pattern.compile("\\b(for (the |my )?family|family cut)\\b").matcher(words).find();
        boolean publicCopy = Pattern.compile("\\b(public|for everyone)\\b").matcher(words).find();
        boolean privateCopy = Pattern.compile("\\b(private|just for me|for myself)\\b").matcher(words).find();
        if ((family ? 1 : 0) + (publicCopy ? 1 : 0) + (privateCopy ? 1 : 0) > 1 ||
                Pattern.compile("\\bfor (my |the )?(team|kids|friends|work|coworkers)\\b").matcher(words).find()) return null;
        return family ? "Family" : publicCopy ? "Public" : "Private";
    }

    private static String words(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", " ").replaceAll(" +", " ").strip();
    }

    private JSONObject load(String id) throws IOException {
        var saved = AgentFiles042.loadChecked(path(id));
        if (!FORMAT.equals(saved.optString("format")) || !id.equals(saved.optString("id")))
            throw new IOException("The creative project identity changed.");
        return saved;
    }

    private JSONObject save(JSONObject state) throws IOException {
        AgentFiles042.saveChecked(path(state.getString("id")), state);
        return state;
    }

    private Path path(String id) {
        validateId(id);
        return projects.resolve(id + ".json");
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    private static void validateId(String id) {
        if (!id.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}"))
            throw new IllegalArgumentException("Invalid creative project reference.");
    }
}
