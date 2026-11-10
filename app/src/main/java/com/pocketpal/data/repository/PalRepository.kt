package com.pocketpal.data.repository

import com.pocketpal.data.local.PalDao
import com.pocketpal.data.local.PalEntity
import com.pocketpal.data.model.Pal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PalRepository(private val palDao: PalDao) {

    val allPals: Flow<List<Pal>> = palDao.getAllPals().map { entities ->
        if (entities.isEmpty()) {
            val defaults = getDefaultPals()
            palDao.insertPals(defaults.map { it.toEntity() })
            defaults
        } else {
            entities.map { it.toPal() }
        }
    }

    suspend fun savePal(pal: Pal) {
        palDao.insertPal(pal.toEntity())
    }

    suspend fun deletePal(pal: Pal) {
        palDao.deletePal(pal.toEntity())
    }

    fun getDefaultPals(): List<Pal> {
        return listOf(
            Pal(
                id = "pal_default_assistant",
                name = "PocketPal",
                avatar = "bot",
                description = "Friendly, versatile AI assistant for general questions and daily tasks.",
                systemPrompt = "You are PocketPal, a helpful, polite, and knowledgeable AI assistant.",
                category = "Smart Chat",
                isCustom = false,
                iconName = "bot"
            ),
            Pal(
                id = "pal_default_coder",
                name = "Code Companion",
                avatar = "code",
                description = "Expert software engineer skilled in Kotlin, Jetpack Compose, Python, and web tech.",
                systemPrompt = "You are an expert software engineer. Provide clean, efficient, bug-free code with concise explanation.",
                category = "Coding",
                isCustom = false,
                temperature = 0.2f,
                iconName = "code"
            ),
            Pal(
                id = "pal_default_writer",
                name = "Creative Writer",
                avatar = "writer",
                description = "Imaginative author for storytelling, essays, copy, and creative poetry.",
                systemPrompt = "You are a master creative writer. Crafts engaging, vivid stories and elegant prose.",
                category = "Writing",
                isCustom = false,
                temperature = 0.9f,
                iconName = "writer"
            ),
            Pal(
                id = "pal_default_tutor",
                name = "STEM Tutor",
                avatar = "tutor",
                description = "Step-by-step problem solver for physics, calculus, chemistry, and logic.",
                systemPrompt = "You are a patient STEM tutor. Explain concepts clearly step-by-step with formulas.",
                category = "Education",
                isCustom = false,
                temperature = 0.3f,
                iconName = "tutor"
            ),
            Pal(
                id = "pal_default_roleplay",
                name = "Cyberpunk Detective",
                avatar = "detective",
                description = "Interactive roleplay character in a neon noir futuristic city.",
                systemPrompt = "You are Detective Vance in a rainy 2099 neo-city. Respond in character with gritty cyberpunk flair.",
                category = "Roleplay",
                isCustom = false,
                temperature = 0.85f,
                iconName = "detective"
            )
        )
    }

    private fun PalEntity.toPal() = Pal(
        id = id,
        name = name,
        avatar = avatar,
        description = description,
        systemPrompt = systemPrompt,
        category = category,
        isCustom = isCustom,
        temperature = temperature,
        topP = topP,
        topK = topK,
        iconName = iconName
    )

    private fun Pal.toEntity() = PalEntity(
        id = id,
        name = name,
        avatar = avatar,
        description = description,
        systemPrompt = systemPrompt,
        category = category,
        isCustom = isCustom,
        temperature = temperature,
        topP = topP,
        topK = topK,
        iconName = iconName
    )
}
