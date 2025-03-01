# PDF Signature App Features

## Архитектура ✅
- Clean Architecture
- MVVM pattern
- Dependency Injection (Koin)
- Room Database

## Core Package ✅
- Интерфейс PDFRepository для загрузки документов
- Интерфейс PDFViewer для отображения PDF
- Интерфейс SignatureEditor для работы с подписями
- Константы и перечисления для источников PDF

## UI Components ✅
- Bottom Navigation с тремя вкладками
- Базовый UI для просмотра текущего документа
- Список документов с датами добавления
- Экран редактора нотаций

## База данных ✅
- Room DB для хранения информации о документах
- Таблица документов с датами и заголовками
- Таблица нотаций для подписей
- DAO для работы с документами и нотациями

## Функциональность
- Загрузка PDF из assets
- Просмотр PDF документов
- Добавление нотаций для подписи
- Рукописная подпись через SignaturePad

## В процессе разработки
- Реализация конкретных классов для интерфейсов
- Интеграция PDF библиотеки
- Добавление подписи через SignaturePad
- Реализация репозитория для работы с PDF

# Функциональность приложения

## Интерфейс
- Нижняя навигация с тремя вкладками
- Диалог для создания подписи
- Отображение PDF документов
- Индикаторы загрузки
- Информативные сообщения при отсутствии данных

## PDF функционал
- Просмотр PDF документов
- Добавление подписей в определенные места документа
- Отображение областей для подписи
- Интерактивное взаимодействие с областями подписи

## Архитектура
- Clean Architecture
- MVVM паттерн
- Dependency Injection через Koin
- Jetpack Compose для UI
- Отдельный интерфейс для PDF просмотрщика
- Репозиторий для работы с PDF документами

## Хранение данных
- Room для локального хранения
- Загрузка PDF из assets
- Сохранение подписей

## Навигация
- Текущий документ
- Список документов
- Редактор пометок

## Настройки приложения
- Добавлен экран настроек
- Добавлен переключатель темной темы
- Реализовано сохранение настроек через DataStore
- Добавлена кнопка быстрого доступа к настройкам в тулбар

## Улучшение интерфейса
- Добавлена поддержка темной темы
- Обновлен дизайн тулбара с добавлением кнопки настроек
- Реализовано автоматическое применение выбранной темы ко всем экранам

## Улучшение подписи
- Добавлен расширенный режим подписи
- Поддержка 4 цветов ручки (черный, синий, красный, зеленый)
- Настройка толщины линии (тонкая, средняя, толстая)
- Сохранение настроек подписи между сессиями
- Быстрое переключение между базовым и расширенным режимом

## Просмотр подписанных документов
- Отдельный экран для просмотра подписанных PDF
- Режим "только для чтения" без возможности редактирования
- Быстрый доступ к функции "Поделиться"
- Отображение названия документа в заголовке
- Удобная навигация по страницам

## Инструкции по запуску
1. Скопировать sample.pdf в assets
2. Синхронизировать Gradle
3. Запустить приложение 

# Список изменений

## Управление файлами
- Добавлено сохранение PDF файлов в постоянное хранилище приложения
- Реализовано использование относительных путей в базе данных
- Добавлена конвертация между абсолютными и относительными путями

## Исправления
- Исправлено сохранение путей к файлам в базе данных
- Добавлена обработка относительных путей при чтении файлов

## Навигация и обработка документов:
- Исправлена навигация между экранами для корректной передачи выбранного документа
- Добавлен метод setSelectedDocument в DocumentListViewModel для сохранения выбранного документа
- Обновлена логика загрузки документа в NotationEditorViewModel для использования правильного пути к файлу
- Добавлена проверка существования файла перед его загрузкой
- Улучшена обработка ошибок при загрузке документов

## Улучшения UI:
- Обновлены кнопки "Просмотр" и "Редактировать" для корректной работы с выбранным документом
- Добавлены сообщения об ошибках при отсутствии файла

## Исправления багов:
- Исправлена проблема с неправильной загрузкой документа в редакторе нотаций
- Исправлена проблема с навигацией при выборе документа для просмотра или редактирования 