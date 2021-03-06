package com.github.k0kubun.gitstar_ranking.core

data class Repository(
    val id: Long,
    val ownerId: Long? = null,
    val name: String? = null, // TODO: can we remove this to save storage?
    val fullName: String,
    val description: String? = null,
    val fork: Boolean? = null,
    val homepage: String? = null,
    val stargazersCount: Long,
    val language: String? = null,
)
