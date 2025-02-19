package com.pdfsignature.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object CurrentDocument : Screen("current_document", "Текущий документ")
    object DocumentList : Screen("document_list", "Документы")
    object NotationEditor : Screen("notation_editor", "Редактор")
    object SignedDocuments : Screen("signed_documents", "История")
    object Settings : Screen("settings", "Настройки")
    object HistoryViewer : Screen("history_viewer/{documentId}", "Просмотр документа") {
        fun createRoute(documentId: String) = "history_viewer/$documentId"
    }
    
    companion object {
        val bottomNavItems = listOf(
            CurrentDocument,
            DocumentList,
            NotationEditor,
            SignedDocuments
        )
    }
} 