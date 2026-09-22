#include "mesh_loader.h"

#include <assimp/Importer.hpp>
#include <assimp/postprocess.h>
#include <assimp/scene.h>

MeshData* LoadMeshFromFile(const std::string& path, std::string* outError) {
    Assimp::Importer importer;
    const aiScene* scene = importer.ReadFile(
        path,
        aiProcess_Triangulate |
        aiProcess_JoinIdenticalVertices |
        aiProcess_GenSmoothNormals |
        aiProcess_GenUVCoords |
        aiProcess_ImproveCacheLocality |
        aiProcess_ValidateDataStructure
    );

    if (!scene || (scene->mFlags & AI_SCENE_FLAGS_INCOMPLETE) || !scene->mRootNode) {
        if (outError) *outError = importer.GetErrorString();
        return nullptr;
    }

    auto* mesh = new MeshData();
    uint32_t vertexOffset = 0;

    for (unsigned int m = 0; m < scene->mNumMeshes; ++m) {
        const aiMesh* aim = scene->mMeshes[m];

        for (unsigned int v = 0; v < aim->mNumVertices; ++v) {
            const aiVector3D& p = aim->mVertices[v];
            const aiVector3D n = aim->HasNormals() ? aim->mNormals[v] : aiVector3D(0, 1, 0);
            aiVector3D uv(0, 0, 0);
            if (aim->HasTextureCoords(0)) uv = aim->mTextureCoords[0][v];

            mesh->vertexData.insert(mesh->vertexData.end(),
                {p.x, p.y, p.z, n.x, n.y, n.z, uv.x, uv.y});
        }

        for (unsigned int f = 0; f < aim->mNumFaces; ++f) {
            const aiFace& face = aim->mFaces[f];
            for (unsigned int i = 0; i < face.mNumIndices; ++i) {
                mesh->indices.push_back(vertexOffset + face.mIndices[i]);
            }
        }

        vertexOffset += aim->mNumVertices;
    }

    return mesh;
}
