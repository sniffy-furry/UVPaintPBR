#pragma once
#include <string>
#include <vector>
#include <cstdint>

// Interleaved per-vertex layout: position(3) + normal(3) + uv(2) = 8 floats.
struct MeshData {
    std::vector<float> vertexData;
    std::vector<uint32_t> indices;
};

// Loads every mesh in the scene, flattened into one combined buffer.
// Caller owns the returned pointer (free with delete). Returns nullptr on failure.
// TODO: keep per-mesh/per-material splits once paint layers are per-material.
MeshData* LoadMeshFromFile(const std::string& path, std::string* outError);
