package fraymus.workspace;

import org.junit.Test;
import org.json.*;
import java.nio.file.*;
import static org.junit.Assert.*;

public class WorkspaceStoreTest {
    private JSONObject create(){return new JSONObject().put("title","Counter").put("files",new JSONObject().put("index.html","<h1 id='count'>0</h1>"));}
    @Test public void versionedArtifactRestoresAndStaleEditsCannotOverwrite() throws Exception {
        Path root=Files.createTempDirectory("fraynix-workspace-test");String id;
        try(var store=new WorkspaceStore(root)) {
            var first=store.write(create());id=first.getString("id");
            var edit=create().put("id",id).put("expectedVersion",1);assertEquals(2,store.write(edit).getInt("version"));
            try{store.write(edit);fail("Stale edit accepted");}catch(java.util.ConcurrentModificationException expected){}
            assertEquals(2,store.read(id).getInt("version"));assertTrue(Files.exists(store.asset(id,1,"index.html")));
        }
        try(var restored=new WorkspaceStore(root)){assertEquals(2,restored.read(id).getInt("version"));assertEquals(1,restored.artifacts().length());}
    }
    @Test public void traversalAndExecutableHostFilesAreRejected() {
        for(String name:new String[]{"../secret.txt","C:/secret.txt","tools/run.exe","run.bat","a/../../x.js","index.html?x"}) {
            try{WorkspaceStore.validFile(name);fail(name);}catch(IllegalArgumentException expected){}
        }
        WorkspaceStore.validFile("assets/app.js");
    }
    @Test public void interruptedUncommittedFilesArePreservedWhileTheNextEditSucceeds() throws Exception {
        Path root=Files.createTempDirectory("fraynix-interrupted-write");
        try(var store=new WorkspaceStore(root)) {
            String id=store.write(create()).getString("id");Path orphan=store.asset(id,2,"index.html");Files.createDirectories(orphan.getParent());Files.writeString(orphan,"unfinished edit");
            assertEquals(2,store.write(create().put("id",id).put("expectedVersion",1)).getInt("version"));
            try(var recovered=Files.walk(root.resolve("recovery"))){assertTrue(recovered.filter(Files::isRegularFile).anyMatch(p->{try{return Files.readString(p).equals("unfinished edit");}catch(Exception e){return false;}}));}
        }
    }
    @Test public void onlyCurrentPassingEvidenceCanBecomeReusableSkill() throws Exception {
        Path root=Files.createTempDirectory("fraynix-skill-test");
        try(var store=new WorkspaceStore(root)) {
            var artifact=store.write(create());String id=artifact.getString("id");
            var skill=new JSONObject().put("id",id).put("name","counter-recipe").put("description","A tested counter");
            try{store.saveSkill(skill);fail("Untested skill accepted");}catch(IllegalStateException expected){}
            var step=new JSONObject().put("selector","#count").put("expectText","0");
            var proof=new JSONObject().put("id",id).put("version",1).put("steps",new JSONArray().put(step)).put("checks",new JSONArray().put(new JSONObject().put("passed",true)));
            assertTrue(store.recordTest(proof).getBoolean("passed"));assertEquals(1,store.saveSkill(skill).getInt("version"));
            store.write(create().put("id",id).put("expectedVersion",1));
            try{store.saveSkill(skill);fail("Old test evidence was reused for new code");}catch(IllegalStateException expected){}
            assertEquals(1,store.skills().length());
        }
    }
}
