# Reference implementation — network layer, repository and ViewModel pattern

This is the canonical code every project of ours follows. Copy it as-is; the only Android
adaptations allowed are: Ktor `OkHttp` engine, `android.util.Log` instead of `println`, and Koin
bindings.
Do NOT rename methods or classes (`makeNetworkRequest`, `ApiResult.Success/Failure`, `ApiData`).

## data/remote/NetworkUtil.kt
```kotlin
object NetworkUtil {
    const val BASE_URL = "https://example.com" // TODO real backend, if/when one exists
}
```

## data/remote/RequestType.kt
```kotlin
sealed class RequestType(val link: String) {
    data class Get(val url: String) : RequestType(link = url)
    data class Post(val url: String, val body: Any?) : RequestType(link = url)
    data class Delete(val url: String) : RequestType(link = url)
    data class DeleteWithBody(val url: String, val body: Any?) : RequestType(link = url)
    data class UploadFile(
        val url: String,
        val fileBytes: ByteArray,
        val fileName: String,
        val fileParamName: String = "file",
        val formData: Map<String, String> = emptyMap()
    ) : RequestType(link = url)
    data class Multipart(
        val url: String,
        val fields: Map<String, String>,
        val files: Map<String, Pair<ByteArray, String>?> = emptyMap()
    ) : RequestType(link = url)
}
```

## data/remote/SessionManager.kt
```kotlin
sealed interface SessionEvent {
    data object Unauthorized : SessionEvent
}

class SessionManager {
    private val _events = MutableSharedFlow<SessionEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    private var alreadyTriggered = false

    suspend fun notifyUnauthorized() {
        if (alreadyTriggered) return
        alreadyTriggered = true
        _events.emit(SessionEvent.Unauthorized)
    }

    fun reset() {
        alreadyTriggered = false
    }
}
```

## data/remote/ApiData.kt
```kotlin
@Serializable
data class ApiData<T : Any>(
    val status: Boolean = false,
    val message: String = "",
    val data: T? = null,
)
```

## data/remote/ApiResult.kt
```kotlin
sealed class ApiResult<T>(
    val data: T? = null,
    val message: String? = null,
) {
    class Success<T>(data: T) : ApiResult<T>(data = data)
    class Failure<T>(message: String) : ApiResult<T>(message = message)
}
```

## data/remote/NetworkClient.kt
```kotlin
class NetworkClient(val sessionManager: SessionManager) {
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(SerializingHelper.getJson())
        }
        val timeOut = 600_000L
        install(HttpTimeout) {
            requestTimeoutMillis = timeOut
            connectTimeoutMillis = timeOut
            socketTimeoutMillis = timeOut
        }
    }
    suspend inline fun <reified T> makeNetworkRequest(
        requestType: RequestType,
        headers: Map<String, String>? = null,
    ): ApiResult<T> {
        return withContext(Dispatchers.Default) {
            try {
                logIt("----------------------------------------------")
                logIt("Network Hit Url : ${requestType.link}")
                logIt("Network Hit Headers : $headers")
                val httpResponse = requestType.getHttpBuilder(headers)
                val statusCode = httpResponse.status.value
                val response: String = httpResponse.body()
                val newResponse: T = if (T::class == String::class) {
                    response as T
                } else {
                    SerializingHelper.getJson().decodeFromString(response)
                }
                logIt("Network Response-1:$response")
                logIt("Network Response-2:$newResponse")
                if (statusCode == 401) sessionManager.notifyUnauthorized()
                ApiResult.Success(newResponse)
            } catch (e: ClientRequestException) {
                val status = e.response.status.value
                if (status == 401) sessionManager.notifyUnauthorized()
                logEx("ClientRequestException: Status: ${e.response.status} Message: ${e.message}")
                ApiResult.Failure(e.message)
            } catch (e: ServerResponseException) {
                logEx("ServerResponseException:Status: ${e.response.status} Message: ${e.message}")
                ApiResult.Failure(e.message)
            } catch (e: Exception) {
                logEx("Network Exception:${e.message}")
                ApiResult.Failure(e.message ?: "Unknown error")
            }
        }
    }
    suspend fun RequestType.getHttpBuilder(
        headers: Map<String, String>? = null
    ): HttpResponse {
        return when (this) {
            is RequestType.Get -> {
                httpClient.get(url) {
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                }
            }
            is RequestType.Post -> {
                httpClient.post(url) {
                    header("Content-Type", "application/json")
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                    setBody((this@getHttpBuilder).body)
                }
            }
            is RequestType.Delete -> {
                httpClient.delete(url) {
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                }
            }
            is RequestType.DeleteWithBody -> {
                httpClient.delete(url) {
                    header("Content-Type", "application/json")
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                    setBody((this@getHttpBuilder).body)
                }
            }
            is RequestType.UploadFile -> {
                httpClient.post(url) {
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    key = fileParamName,
                                    value = fileBytes,
                                    headers = Headers.build {
                                        append(
                                            "Content-Disposition",
                                            "form-data; name=\"$fileParamName\"; filename=\"$fileName\""
                                        )
                                        append("Content-Type", "application/octet-stream")
                                    }
                                )
                                formData.forEach { (key, value) ->
                                    append(key, value)
                                }
                            }
                        ))
                }
            }
            is RequestType.Multipart -> {
                httpClient.post(url) {
                    headers?.forEach {
                        header(it.key, it.value)
                    }
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                fields.forEach { (key, value) ->
                                    append(key, value)
                                }
                                files.forEach { (key, pair) ->
                                    pair?.let { (bytes, name) ->
                                        append(
                                            key = key,
                                            value = bytes,
                                            headers = Headers.build {
                                                append(
                                                    "Content-Disposition",
                                                    "form-data; name=\"$key\"; filename=\"$name\""
                                                )
                                                append("Content-Type", "application/octet-stream")
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    )
                }
            }
        }
    }
    fun logIt(msg: String) {
        println("[NetworkClient] $msg")
    }
    fun logEx(msg: String) {
        println("[NetworkClient ERROR] $msg")
    }
}
```

## Repository pattern — domain/repository (interface) + data/repository (Imp)
```kotlin
interface SubResidentRepository {
    suspend fun getAllSubResidents(token: String): ApiResult<List<SubResident>>
    suspend fun addSubResident(token: String, subResident: SubResidentRequest): ApiResult<SubResident>
}

class SubResidentRepositoryImp(private val networkClient: NetworkClient) : SubResidentRepository {

    override suspend fun getAllSubResidents(token: String): ApiResult<List<SubResident>> {
        val url = "${NetworkUtil.BASE_URL}/api/manager/sub-residents"
        val header = mapOf("Authorization" to "Bearer $token")

        val response = networkClient.makeNetworkRequest<ApiData<List<SubResident>>>(
            requestType = RequestType.Get(url), headers = header
        )

        return when (response) {
            is ApiResult.Failure -> ApiResult.Failure(response.message ?: "Something went wrong")
            is ApiResult.Success -> {
                response.data?.let {
                    if (it.status) {
                        val data = it.data ?: return ApiResult.Failure("No data received, try again")
                        ApiResult.Success(data)
                    } else {
                        ApiResult.Failure(it.message)
                    }
                } ?: ApiResult.Failure("No data received, try again")
            }
        }
    }

    override suspend fun addSubResident(token: String, subResident: SubResidentRequest): ApiResult<SubResident> {
        val url = "${NetworkUtil.BASE_URL}/api/manager/sub-residents"
        val header = mapOf("Authorization" to "Bearer $token")

        val response = networkClient.makeNetworkRequest<ApiData<SubResident>>(
            requestType = RequestType.Post(url, subResident), headers = header
        )

        return when (response) {
            is ApiResult.Failure -> ApiResult.Failure(response.message ?: "Something went wrong")
            is ApiResult.Success -> {
                response.data?.let {
                    if (it.status) {
                        val data = it.data ?: return ApiResult.Failure("No data received, try again")
                        ApiResult.Success(data)
                    } else {
                        ApiResult.Failure(it.message)
                    }
                } ?: ApiResult.Failure("No data received, try again")
            }
        }
    }
}
```
The repeated `when (response)` unwrap may be extracted into a shared helper
(e.g. `ApiResult<ApiData<T>>.unwrap()`) — behaviour must stay identical.

PhoneCleaner has no backend today: there is no `data/remote` package yet. The network code above
is the reference for the day one is added, and is then copied as-is.

Repositories that talk to the device follow the same shape but wrap the feature library modules
(`:storage`, `:battery`, `:deviceinfo`, ...) and the Room DAOs instead of `NetworkClient`, and
return `ApiResult<T>` too:

```kotlin
interface BatteryRepository {
    suspend fun getBatteryInfo(): ApiResult<BatteryInfo>
}

class BatteryRepositoryImp(
    private val battery: ai.jhw.battery.domain.repository.BatteryRepository,
    private val sharedPref: SharedPref,
) : BatteryRepository {

    override suspend fun getBatteryInfo(): ApiResult<BatteryInfo> = try {
        ApiResult.Success(battery.read().toEntity(sharedPref.lastFullChargeAt))
    } catch (e: Exception) {
        Log.e(TAG, "Battery read failed", e)
        ApiResult.Failure(e.message ?: "Something went wrong")
    }

    private companion object {
        const val TAG = "BatteryRepositoryImp"
    }
}
```

Without `data/remote`, `ApiResult` lives in `domain/entities/ApiResult.kt`; it moves to
`data/remote/` unchanged when the network layer arrives.

## Room — data/local

```kotlin
@Entity(tableName = "ai_scan_results")
data class AiScanResultEntity(
    @PrimaryKey val path: String,
    val dateModified: Long,
    val isJunk: Boolean,
)

@Dao
interface AiScanResultDao {
    @Query("SELECT * FROM ai_scan_results")
    suspend fun getAll(): List<AiScanResultEntity>

    @Upsert
    suspend fun upsertAll(results: List<AiScanResultEntity>)
}

@Database(entities = [AiScanResultEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aiScanResultDao(): AiScanResultDao
}
```

The database and every DAO are `single { }` in `SharedModule`. DAOs are used by repositories only,
never by a ViewModel or an activity.

## ViewModel pattern — domain/view_models
```kotlin
data class SubResidentListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val subResidentList: List<SubResident> = emptyList(),
)

class SubResidentListViewModel(
    private val repository: SubResidentRepository,
    private val sharedPref: SharedPref,
) : ViewModel() {

    private val _state = MutableStateFlow(SubResidentListUiState())
    val state = _state.asStateFlow()

    private var job: Job? = null

    init {
        getAllSubResidents()
    }

    fun getAllSubResidents() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = state.value.copy(isLoading = true)
            val result = repository.getAllSubResidents(sharedPref.token)
            _state.value = state.value.copy(
                isLoading = false,
                error = result.message,
                subResidentList = result.data ?: emptyList()
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
```
One-shot effects (toast, navigation, show the delete confirmation) go through a
`SharedFlow<XUiEvent>` next to the state; activities collect both with `repeatOnLifecycle(STARTED)`.
