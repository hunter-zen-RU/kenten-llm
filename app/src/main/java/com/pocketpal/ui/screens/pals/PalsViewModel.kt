package com.pocketpal.ui.screens.pals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketpal.data.model.Pal
import com.pocketpal.data.repository.PalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class PalsViewModel(private val palRepository: PalRepository) : ViewModel() {

    val pals: StateFlow<List<Pal>> = palRepository.allPals.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredPals: StateFlow<List<Pal>> = combine(pals, selectedCategory, searchQuery) { list, cat, query ->
        list.filter { pal ->
            (cat == "All" || pal.category.equals(cat, ignoreCase = true)) &&
            (query.isBlank() || pal.name.contains(query, ignoreCase = true) || pal.description.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveCustomPal(
        name: String,
        avatar: String,
        description: String,
        systemPrompt: String,
        category: String,
        temperature: Float
    ) {
        val newPal = Pal(
            id = "pal_custom_${UUID.randomUUID().toString().take(8)}",
            name = name,
            avatar = avatar.ifBlank { "🌟" },
            description = description,
            systemPrompt = systemPrompt,
            category = category,
            isCustom = true,
            temperature = temperature
        )
        viewModelScope.launch {
            palRepository.savePal(newPal)
        }
    }

    fun deletePal(pal: Pal) {
        viewModelScope.launch {
            palRepository.deletePal(pal)
        }
    }
}
