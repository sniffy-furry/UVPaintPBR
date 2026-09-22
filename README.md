# UV Paint PBR (Android)

Blender-style UV unwrapping + layered PBR texture painting, native on Android.

## Status: scaffold
This is stage 1 — an empty GLSurfaceView + NDK stub, wired to a GitHub Actions
workflow that produces a debug APK on every push. Nothing is drawn yet.

## Roadmap
1. **Scaffold** (this commit) — Gradle + CMake + CI producing an installable APK.
2. **Model import** — link assimp (BSD license) in `app/src/main/cpp/CMakeLists.txt`
   for OBJ / glTF / FBX loading into a shared mesh format.
3. **UV editor** — seam marking tool + Smart UV unwrap, implemented from the
   published LSCM (Least Squares Conformal Maps) algorithm — *not* ported from
   Blender's GPL source, so this repo can stay under whatever license you pick.
4. **Paint engine** — brush strokes rasterized into per-channel texture layers:
   base color, normal, roughness, metallic, AO, height. Layers composited in
   the fragment shader for live PBR preview.
5. **Export** — flatten layers, write PNG/KTX2 per channel.

## Building
Push to `main` (or open a PR) — GitHub Actions builds `app-debug.apk` and
attaches it as a workflow artifact. No local Gradle/NDK install required.

Package name `com.sniffy.uvpaint` is a placeholder — rename before it matters.
