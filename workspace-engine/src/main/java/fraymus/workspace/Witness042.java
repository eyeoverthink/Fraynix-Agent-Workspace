package fraymus.workspace;

import brain_architecture.kernel.MerkleAppendOnlyJournal;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Segmented use of the existing owned integrity journal, not an oracle of physical truth. */
final class Witness042 {
    private final Path root;
    private final List<MerkleAppendOnlyJournal> segments=new ArrayList<>();
    private final int segmentSize=256;
    private final JSONArray recoveredAppends=new JSONArray();
    Witness042(Path root)throws IOException{
        this.root=root;Files.createDirectories(root);
        try(var paths=Files.list(root)){
            for(Path p:paths.filter(Files::isDirectory).sorted().toList()){
                String expected=String.format(Locale.ROOT,"segment-%06d",segments.size());
                if(!p.getFileName().toString().equals(expected))throw new IOException("Witness segment sequence changed; original retained");
                segments.add(new MerkleAppendOnlyJournal(p));
            }
        }
        // Validate every committed segment before considering the final unfinished atomic append.
        String parent="GENESIS";
        for(int i=0;i<segments.size();i++){
            var journal=segments.get(i);var check=journal.verify();var entries=journal.getEntries();
            final int ordinal=i;final String expectedParent=parent;
            if(!check.pendingFiles().isEmpty()){
                if(i!=segments.size()-1)throw new IOException("Pending append in a sealed segment; original retained");
                Path retained=journal.recoverPendingAppend(candidate->validateSegment(candidate,ordinal,expectedParent));
                recoveredAppends.put(new JSONObject().put("segment",i).put("evidenceFile",retained.getFileName().toString()).put("meaning","Recovered one complete hash-verified append, not a replayed world or browser action"));
                entries=journal.getEntries();
            }
            try{validateSegment(entries,i,parent);}catch(RuntimeException invalid){throw new IOException("Witness segment evidence differs; original retained",invalid);}
            if(i<segments.size()-1&&entries.size()!=segmentSize)throw new IOException("An incomplete witness segment was sealed");
            parent=journal.getLastMerkleRoot();
        }
        verify();
    }
    private void validateSegment(List<MerkleAppendOnlyJournal.JournalEntry> entries,int ordinal,String parent){
        if(entries.isEmpty()||entries.size()>segmentSize)throw new IllegalStateException("Invalid witness segment size");
        var first=entries.getFirst();if(!first.type().equals("segment")||!parent.equals(first.metadata().get("parentRoot"))||!Integer.toString(ordinal).equals(first.metadata().get("segment")))throw new IllegalStateException("Witness parent linkage differs");
        for(var e:entries.subList(1,entries.size())){var data=new JSONObject(e.metadata().get("evidence"));if(!WorldFrames.hash(data).equals(e.metadata().get("sha256")))throw new IllegalStateException("Witness evidence hash differs");}
    }
    private void next()throws IOException{
        if(segments.size()>=1024)throw new IOException("Witness retention budget reached; preserve/archive before more background work");
        String parent=segments.isEmpty()?"GENESIS":segments.getLast().getLastMerkleRoot();
        var journal=new MerkleAppendOnlyJournal(root.resolve(String.format(Locale.ROOT,"segment-%06d",segments.size())));
        journal.append("segment","Witness segment opened",Map.of("parentRoot",parent,"segment",Integer.toString(segments.size())));segments.add(journal);
    }
    synchronized void append(String kind,JSONObject evidence)throws IOException{
        if(evidence.toString().length()>12000)throw new IOException("Witness event exceeds bounded numeric evidence format");
        if(segments.isEmpty()||segments.getLast().size()>=segmentSize)next();
        segments.getLast().append(kind,"Java agent evidence",Map.of("evidence",evidence.toString(),"sha256",WorldFrames.hash(evidence)));
    }
    synchronized JSONObject verify()throws IOException{
        String parent="GENESIS";int total=0;
        for(int i=0;i<segments.size();i++){
            var journal=segments.get(i);var check=journal.verify();var entries=journal.getEntries();
            if(!check.pendingFiles().isEmpty()||entries.isEmpty()||entries.size()>segmentSize)throw new IOException("Witness has pending or invalid segment evidence; original retained");
            var first=entries.getFirst();if(!first.type().equals("segment")||!parent.equals(first.metadata().get("parentRoot"))||!Integer.toString(i).equals(first.metadata().get("segment")))throw new IOException("Witness parent linkage differs");
            for(var e:entries.subList(1,entries.size())){var data=new JSONObject(e.metadata().get("evidence"));if(!WorldFrames.hash(data).equals(e.metadata().get("sha256")))throw new IOException("Witness evidence hash differs");}
            if(i<segments.size()-1&&entries.size()!=segmentSize)throw new IOException("An incomplete witness segment was sealed");
            total+=entries.size();parent=check.merkleRoot();
        }
        return summary().put("verified",true).put("records",total);
    }
    synchronized JSONObject summary(){int total=segments.stream().mapToInt(MerkleAppendOnlyJournal::size).sum();return new JSONObject().put("segments",segments.size()).put("records",total).put("head",segments.isEmpty()?"GENESIS":segments.getLast().getLastMerkleRoot()).put("recoveredAppends126",new JSONArray(recoveredAppends.toString())).put("meaning","Hash/sequence integrity, not a signature or proof that a source claim is true");}
    synchronized boolean hasAnchor(JSONObject anchor)throws IOException{
        verify();int target=anchor.getInt("records"),offset=0;
        if(target==0)return anchor.getInt("segments")==0&&anchor.getString("head").equals("GENESIS");
        for(int i=0;i<segments.size();i++){var entries=segments.get(i).getEntries();if(target>offset&&target<=offset+entries.size())return anchor.getInt("segments")==i+1&&entries.get(target-offset-1).merkleRoot().equals(anchor.getString("head"));offset+=entries.size();}return false;
    }
    synchronized JSONArray recent(){var out=new JSONArray();var rows=new ArrayList<JSONObject>();for(int s=segments.size()-1;s>=0&&rows.size()<16;s--){var entries=segments.get(s).getEntries();for(int i=entries.size()-1;i>=0&&rows.size()<16;i--){var e=entries.get(i);if(!e.type().equals("segment"))rows.add(new JSONObject().put("kind",e.type()).put("hash",e.entryHash()).put("evidence",new JSONObject(e.metadata().get("evidence"))));}}Collections.reverse(rows);rows.forEach(out::put);return out;}
}
