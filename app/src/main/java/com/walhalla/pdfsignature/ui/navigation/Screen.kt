package com.walhalla.pdfsignature.ui.navigation

sealed class Screen(val route: String) {
    object CurrentDocument : Screen("current_document")
    object DocumentList : Screen("document_list")
    object NotationEditor : Screen("notation_editor")
    
    companion object {
        val bottomNavItems = listOf(
            CurrentDocument,
            DocumentList,
            NotationEditor
        )
    }
} 