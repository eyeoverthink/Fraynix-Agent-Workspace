package fraymus.workspace;

import java.awt.image.BufferedImage;
import org.json.JSONObject;

interface ImageTracker {
    JSONObject update(BufferedImage image);
}
