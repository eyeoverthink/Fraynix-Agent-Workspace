# Copy a detailed prop with its own collider

Import a Blender prop as a detailed triangle surface with a separate box collider. Use Chat to review a transformed copy, verify contacts in two native worlds, and return it to Blender.

The original Blender beacon has 636 visual triangles. Two fresh native worlds each reported 1 contact inside the box, zero at a sampled point on the visual overhang, and support from the floor. Java refused an overlapping placement before committing the scene.

Edit a part of one copy and verify which visual asset and collider changed. Then place a prop measured from calibrated multi-view observations and review its collider before acting.

[Edition 217](../../README-217.md) · [Workflow map](225-detailed-prop.canvas) · [Reproduction](../publication217/plan.md)
