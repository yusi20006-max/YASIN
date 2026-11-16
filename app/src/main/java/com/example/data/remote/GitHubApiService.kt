package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class GitHubRepoDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "description") val description: String?,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "language") val language: String?,
    @Json(name = "stargazers_count") val stargazersCount: Int,
    @Json(name = "forks_count") val forksCount: Int,
    @Json(name = "updated_at") val updatedAt: String,
    @Json(name = "default_branch") val defaultBranch: String?
)

@JsonClass(generateAdapter = true)
data class GitHubReadmeDto(
    @Json(name = "name") val name: String,
    @Json(name = "content") val content: String?,
    @Json(name = "encoding") val encoding: String?,
    @Json(name = "html_url") val htmlUrl: String?
)

interface GitHubApiService {
    @Headers("Accept: application/vnd.github+json")
    @GET("users/{username}/repos")
    suspend fun getUserRepos(
        @Path("username") username: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50
    ): List<GitHubRepoDto>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/readme")
    suspend fun getRepoReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubReadmeDto
}
