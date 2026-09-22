# UV Paint PBR (Android)

Blender-style UV unwrapping + layered PBR texture painting, native on Android.

## Status: model import + UV-checker preview working
Pick a model (Import Model button) — it's loaded via assimp (native, JNI),
centered/normalized, and rendered with a UV checker + basic lighting shader,
which doubles as a correctness check for both mesh and UV import.

**Known limitation:** only the single picked file is copied in — companion
files aren't resolved yet. `.glb`, `.fbx`, and `.obj` without materials work
fully; multi-file `.gltf` (separate `.bin`/textures) needs folder import,
which is a later step. Materials/textures aren't parsed at all yet — this
stage is geometry + UVs only.

## Roadmap
1. ~~Scaffold~~ — Gradle + CMake + CI producing an installable APK.
2. ~~Model import~~ — assimp (BSD license, fetched via CMake FetchContent) for
   OBJ / glTF / FBX geometry + UVs, rendered with a UV-checker shader.
3. **UV editor** *(next)* — seam marking tool + Smart UV unwrap, implemented
   from the published LSCM (Least Squares Conformal Maps) algorithm — *not*
   ported from Blender's GPL source, so this repo can stay under whatever
   license you pick.
4. **Paint engine** — brush strokes rasterized into per-channel texture
   layers: base color, normal, roughness, metallic, AO, height. Layers
   composited in the fragment shader for live PBR preview.
5. **Export** — flatten layers, write PNG/KTX2 per channel.

## Building
Push to `main` (or open a PR) — GitHub Actions builds `app-debug.apk` and
attaches it as a workflow artifact. No local Gradle/NDK install required.
First build is slow (assimp compiles from source); cached after that.

Package name `com.sniffy.uvpaint` is a placeholder — rename before it matters.
