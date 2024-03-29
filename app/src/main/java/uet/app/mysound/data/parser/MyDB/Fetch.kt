package uet.app.mysound.data.parser.MyDB
import uet.app.mysound.common.Config
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

fun fetchDataFromUrl(urlString: String): String? {
    var connection: HttpURLConnection? = null
    var reader: BufferedReader? = null
    var result: String? = null

    try {
        val url = URL(urlString)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        // Đọc dữ liệu từ InputStream
        val inputStream = connection.inputStream
        reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }

        result = stringBuilder.toString()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        // Đóng các resource sau khi sử dụng
        reader?.close()
        connection?.disconnect()
    }

    return result
}
enum class HttpMethod(val value: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    // Thêm các phương thức khác nếu cần
}

fun fetchDataWithJson(
    urlString: String,
    bearerToken: String,
    method: HttpMethod
): String? {
    var connection: HttpURLConnection? = null
    var reader: BufferedReader? = null
    var result: String? = null

    try {
        val url = URL(urlString)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method.value
        connection.setRequestProperty("Content-Type", "application/json")

        // Thêm "Bearer token" vào header
        connection.setRequestProperty("Authorization", "Bearer $bearerToken")

        if (method != HttpMethod.GET) {
            connection.doOutput = true
        }

        // Đọc dữ liệu từ InputStream
        val inputStream = connection.inputStream
        reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }

        result = stringBuilder.toString()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        // Đóng các resource sau khi sử dụng
        reader?.close()
        connection?.disconnect()
    }

    return result
}

fun postDataWithJson(urlString: String, jsonBody: String, bearerToken: String): String? {
    var connection: HttpURLConnection? = null
    var reader: BufferedReader? = null
    var result: String? = null

    try {
        val url = URL(urlString)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")

        // Thêm "Bearer token" vào header
        connection.setRequestProperty("Authorization", "Bearer $bearerToken")

        connection.doOutput = true

        // Ghi dữ liệu JSON vào body của yêu cầu
        val outputStream = connection.outputStream
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        writer.write(jsonBody)
        writer.flush()

        // Đọc dữ liệu từ InputStream
        val inputStream = connection.inputStream
        reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }

        result = stringBuilder.toString()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        // Đóng các resource sau khi sử dụng
        reader?.close()
        connection?.disconnect()
    }

    return result
}

fun main() {
    var baseUrl = Config.local_Url;
    val url = "$baseUrl/api/add_search_history"
    val jsonString: String = """{"query": "bai1"}"""
    val jsonData = postDataWithJson(url, jsonString, "11|gHdkpRmyAsQ00WLx9OcZ5Eym0hnb4F8hhdu5amfT");

    println(jsonData);
}
