package components;

/** Explicit level component. Existing levels acquire no new fall rule. */
public final class FallBoundary extends Component {
    public float minimumY = -0.75f;

    @Override public void start() {
        if (!Float.isFinite(minimumY)) throw new IllegalArgumentException("Finite fall boundary required");
    }

    @Override public void update(float dt) {
        PlayerController player = gameObject.getComponent(PlayerController.class);
        if (player != null && !player.isDead() && !player.hasWon()
                && gameObject.transform.position.y < minimumY) player.die();
    }
}
