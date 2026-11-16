package com.example.data.repository

import android.util.Base64
import com.example.data.remote.GitHubApiService
import java.nio.charset.StandardCharsets

class GitHubMcpInspectorRepository(
    private val gitHubApi: GitHubApiService
) : EcosystemInspectorRepository {

    override suspend fun fetchRepos(owner: String): List<RepoSummary> {
        val cleanOwner = owner.trim().ifBlank { "yusi20006-max" }
        return gitHubApi.getUserRepos(cleanOwner).map { dto ->
            RepoSummary(
                id = dto.id,
                name = dto.name,
                fullName = dto.fullName,
                description = dto.description.orEmpty(),
                htmlUrl = dto.htmlUrl,
                language = dto.language ?: "نامشخص",
                stars = dto.stargazersCount,
                forks = dto.forksCount,
                updatedAt = dto.updatedAt.take(10)
            )
        }
    }

    override suspend fun fetchReadme(owner: String, repoName: String): String {
        val cleanOwner = owner.trim().ifBlank { "yusi20006-max" }
        val readmeDto = gitHubApi.getRepoReadme(cleanOwner, repoName)
        val raw = readmeDto.content ?: return "فایل README برای این مخزن در دسترس نیست."
        return if (readmeDto.encoding.equals("base64", ignoreCase = true)) {
            val cleaned = raw.replace("\n", "").replace("\r", "")
            val decodedBytes = Base64.decode(cleaned, Base64.DEFAULT)
            String(decodedBytes, StandardCharsets.UTF_8)
        } else {
            raw
        }
    }
}
