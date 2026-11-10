package net.lds.online.network

import kotlinx.coroutines.CancellationException

/**
 * Исключение API, представляющее все возможные ошибки при работе с сетевыми запросами.
 * Предоставляет удобные фабричные методы для создания различных типов ошибок.
 *
 * @property code Код ошибки (HTTP статус или пользовательский код)
 * @property message Сообщение об ошибке
 * @property cause Исходное исключение, которое вызвало ошибку
 */
class ApiException private constructor(
    val code: Int,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Проверяет, является ли ошибка ошибкой клиента (4xx)
     */
    fun isClientError(): Boolean = code in 400..499

    /**
     * Проверяет, является ли ошибка ошибкой сервера (5xx)
     */
    fun isServerError(): Boolean = code in 500..599

    /**
     * Проверяет, является ли ошибка проблемой с аутентификацией
     */
    fun isAuthError(): Boolean = code == 401

    /**
     * Проверяет, является ли ошибка ошибкой доступа
     */
    fun isForbiddenError(): Boolean = code == 403

    /**
     * Проверяет, является ли ошибка ошибкой "не найдено"
     */
    fun isNotFoundError(): Boolean = code == 404

    /**
     * Проверяет, является ли ошибка ошибкой сети
     */
    fun isNetworkError(): Boolean = code == NETWORK_ERROR

    /**
     * Проверяет, является ли ошибка таймаутом
     */
    fun isTimeoutError(): Boolean = code == TIMEOUT_ERROR

    /**
     * Проверяет, является ли ошибка ошибкой парсинга
     */
    fun isParsingError(): Boolean = code == PARSING_ERROR

    /**
     * Возврает сообщение для отображения пользователю
     */
    fun getUserMessage(): String {
        return when {
            isNetworkError() -> "Проверьте подключение к интернету"
            isTimeoutError() -> "Сервер не отвечает, попробуйте позже"
            isAuthError() -> "Необходимо авторизоваться"
            isForbiddenError() -> "У вас нет доступа к этому ресурсу"
            isNotFoundError() -> "Запрашиваемая информация не найдена"
            isServerError() -> "Проблема с сервером, попробуйте позже"
            isParsingError() -> "Ошибка при обработке данных"
            else -> "Произошла ошибка: $message"
        }
    }

    companion object {
        // Коды пользовательских ошибок
        const val NETWORK_ERROR = -1
        const val TIMEOUT_ERROR = -2
        const val PARSING_ERROR = -3
        const val UNKNOWN_ERROR = -999

        /**
         * Создает исключение для ошибки сервера (5xx)
         */
        fun serverError(code: Int, message: String, cause: Throwable? = null): ApiException =
            ApiException(code, message, cause)//Ошибка сервера ($code): $message

        /**
         * Создает исключение для ошибки клиента (4xx)
         */
        fun clientError(code: Int, message: String, cause: Throwable? = null): ApiException {
            val msg = when (code) {
                401 -> "Требуется авторизация"
                403 -> "Доступ запрещен"
                404 -> "Ресурс не найден"
                else -> "Ошибка запроса"
            }
            return ApiException(code, "$msg ($code): $message", cause)
        }

        /**
         * Создает исключение для ошибки соединения/сети
         */
        fun networkError(cause: Throwable? = null): ApiException =
            ApiException(NETWORK_ERROR, "Проблема с сетевым подключением", cause)

        /**
         * Создает исключение для ошибки таймаута
         */
        fun timeoutError(cause: Throwable? = null): ApiException =
            ApiException(TIMEOUT_ERROR, "Превышено время ожидания ответа от сервера", cause)

        /**
         * Создает исключение для ошибки парсинга данных
         */
        fun parsingError(cause: Throwable? = null): ApiException =
            ApiException(PARSING_ERROR, "Ошибка при обработке данных, полученных от сервера", cause)

        /**
         * Создает исключение для непредвиденной ошибки
         */
        fun unknownError(message: String = "Неизвестная ошибка", cause: Throwable? = null): ApiException =
            ApiException(UNKNOWN_ERROR, message, cause)

        /**
         * Создает исключение на основе кода HTTP ответа
         */
        fun fromHttpStatusCode(code: Int, message: String, cause: Throwable? = null): ApiException =
            when {
                code in 400..499 -> clientError(code, message, cause)
                code in 500..599 -> serverError(code, message, cause)
                else -> ApiException(code, "HTTP ошибка ($code): $message", cause)
            }

        /**
         * Преобразует любое исключение в ApiException.
         * Автоматически определяет тип исключения и вызывает соответствующий фабричный метод.
         *
         * @param e Исходное исключение для преобразования
         * @return ApiException, соответствующий типу исходного исключения
         */
        fun from(e: Throwable): ApiException {
            // CancellationException должен пробрасываться напрямую, а не оборачиваться
            if (e is CancellationException) {
                throw e
            }

            // Если это уже ApiException, просто возвращаем его
            if (e is ApiException) {
                return e
            }

            e.printStackTrace()


            if(e::class.simpleName == "SocketTimeoutException") return timeoutError(e)
            if(e::class.simpleName == "ConnectTimeoutException") return timeoutError(e)
            if(e::class.simpleName == "ConnectException") return timeoutError(e)
            if(e::class.simpleName == "UnknownHostException") return networkError(e)
            if(e::class.simpleName == "IOException") return networkError(e)
            // Ktor / JS: UnresolvedAddressException и наследники IOException с другим simpleName
            if (e is kotlinx.io.IOException) return networkError(e)


            return when (e) {



//                // Ошибки сети (добавь импорты нужных классов в своем проекте)
//                is java.net.SocketTimeoutException,
//                is ConnectTimeoutException,
//                is java.net.ConnectException ->
//                    timeoutError(e)
//                is kotlinx.io.IOException ->
//                    networkError(e)

                // HTTP ошибки (добавь импорты нужных классов в своем проекте)
                // Пример для Ktor:
                // is io.ktor.client.plugins.ClientRequestException ->
                //     clientError(e.response.status.value, e.response.status.description, e)
                // is io.ktor.client.plugins.ServerResponseException ->
                //     serverError(e.response.status.value, e.response.status.description, e)

                // Ошибки парсинга (добавь импорты нужных классов в своем проекте)
                // is kotlinx.serialization.SerializationException ->
                //     parsingError(e)

                // Для всех остальных типов исключений
                else ->
                    unknownError("Непредвиденная ошибка: ${e.message}", e)
            }
        }

        /**
         * Универсальная функция для обработки исключений в API вызовах.
         * Упрощает преобразование различных типов исключений в ApiException.
         *
         * @param block Блок кода, который может выбросить исключение
         * @return Результат выполнения блока [block]
         * @throws ApiException если произошла ошибка внутри блока
         */
        inline fun <T> handleApiCall(block: () -> T): T {
            return try {
                block()
            } catch (e: CancellationException) {
                // CancellationException всегда пробрасываем как есть
                throw e
            } catch (e: Exception) {
                throw from(e)
            }
        }
    }
}