package dev.devault.commonlib.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val code: String? = null,
){
    companion object {
        fun <T> ok(data: T) = ApiResponse(success = true, data = data)
        fun error(error: String, code: String? = null) = ApiResponse<Nothing>(success = false, error = error, code = code)
    }
}