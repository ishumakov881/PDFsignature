package com.pdfsignature.ui.navigation

sealed class Screen(val route: String) {
    object CurrentDocument : Screen("current_document")
    object DocumentList : Screen("document_list")
    object NotationEditor : Screen("notation_editor")
    object SignedDocuments : Screen("signed_documents")
    object Settings : Screen("settings")
    
    companion object {
        val bottomNavItems = listOf(
            CurrentDocument,
            DocumentList,
            NotationEditor,
            SignedDocuments
        )
    }
} 