package renderer;

import components.*;
import jade.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class AnimationTextureBatchTest {
    private Texture texture(String path) {return new com.google.gson.Gson().fromJson("{\"filepath\":\""+path+"\"}",Texture.class);}
    private SpriteRenderer sprite(Texture texture) {
        GameObject go=new GameObject("Mario");go.addComponent(new Transform());go.transform=go.getComponent(Transform.class);
        SpriteRenderer sprite=new SpriteRenderer();sprite.setTexture(texture);go.addComponent(sprite);return sprite;
    }
    @Test public void animationSwitchRegistersNewTextureBeforeDrawing() {
        var batch=new RenderBatch(20,0,null);var mario=sprite(texture("small.png"));batch.addSprite(mario);
        Texture big=texture("big.png");mario.setTexture(big);var moved=new ArrayList<SpriteRenderer>();batch.prepareSprites(moved);
        assertTrue(batch.hasTexture(big));assertTrue(mario.isDirty());assertTrue(moved.isEmpty());
    }
    @Test public void fullSamplerTableMovesAnimationInsteadOfUsingSlotZeroOrEight() {
        var batch=new RenderBatch(20,0,null);SpriteRenderer mario=null;
        for(int i=0;i<7;i++) {var next=sprite(texture("sheet"+i));batch.addSprite(next);if(i==0)mario=next;}
        assertFalse(batch.hasTextureRoom());mario.setTexture(texture("big.png"));
        var moved=new ArrayList<SpriteRenderer>();batch.prepareSprites(moved);
        assertEquals(List.of(mario),moved);assertTrue(batch.hasRoom());
    }
}
