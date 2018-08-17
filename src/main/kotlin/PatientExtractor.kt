
import com.github.rothso.mass.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Observable
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface Athena {

  @GET("{id}/practiceinfo")
  fun getPracticeInfo(@Path("id") id: Int): Observable<PracticeResponse>
}

interface OAuthService {

  @FormUrlEncoded
  @POST("token")
  fun login(
      @Header("Authorization") basic: String,
      @Field("grant_type") grantType: String
  ): Observable<TokenResponse>
}

data class TokenResponse(
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "refresh_token") val refreshToken: String
)

data class PracticeResponse(
    @Json(name = "practiceinfo") val practiceInfos: List<PracticeInfo>
)

data class PracticeInfo(
    @Json(name = "iscoordinatorsender") val isCoordinatorSender: Boolean,
    @Json(name = "hasclinicals") val hasClinicals: Boolean,
    @Json(name = "name") val name: String,
    @Json(name = "golivedate") val goLiveDate: String, // mm/dd/yyyy
    @Json(name = "experiencemode") val experienceMode: String,
    @Json(name = "hascommunicator") val hasCommunicator: Boolean,
    @Json(name = "iscoordinatorreceiver") val isCoordinatorReceiver: Boolean,
    @Json(name = "hascollector") val hasCollector: Boolean,
    @Json(name = "practiceid") val practiceId: String
)

class OAuthAuthenticator(
    private val service: OAuthService,
    apiKey: String,
    apiSecret: String
) : Authenticator, Interceptor {
  private val credentials = Credentials.basic(apiKey, apiSecret)
  private var accessToken: String? = null
  private var refreshToken: String? = null // TODO use

  override fun authenticate(route: Route, response: Response): Request? {
    if (response.request().header("Authorization") != null) {
      return null; // Give up, we've already failed to authenticate.
    }

    // Get a token from the OAuth endpoint or die trying
    val tokenResponse = service.login(credentials, "client_credentials").blockingFirst()
    this.accessToken = tokenResponse.accessToken
    this.refreshToken = tokenResponse.refreshToken

    // Retry the previous request
    return response.request().newBuilder()
        .addHeader("Authorization", "Bearer ${accessToken!!}")
        .build()
  }

  override fun intercept(chain: Interceptor.Chain): Response {
    return with(chain.request()) {
      accessToken?.let {
        chain.proceed(this.newBuilder()
            .addHeader("Authorization", it)
            .build())
      }

      // Let the request 401 if we don't have a token yet
      chain.proceed(this)
    }
  }
}

class PatientExtractor {

  fun run() {
    val rxJavaAdapterFactory = RxJava2CallAdapterFactory.create() // TODO scheduling
    val moshiConverterFactory = MoshiConverterFactory.create(Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build())

    val authenticator = let {
      val oAuthService = Retrofit.Builder()
          .baseUrl("https://api.athenahealth.com/oauthpreview/")
          .addCallAdapterFactory(rxJavaAdapterFactory)
          .addConverterFactory(moshiConverterFactory)
          .build().create(OAuthService::class.java)

      OAuthAuthenticator(oAuthService, BuildConfig.ATHENA_KEY, BuildConfig.ATHENA_SECRET)
    }

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.athenahealth.com/preview1/")
        .addCallAdapterFactory(rxJavaAdapterFactory)
        .addConverterFactory(moshiConverterFactory)
        .client(OkHttpClient.Builder()
            .authenticator(authenticator)
            .addInterceptor(authenticator)
            .build())
        .build()

    val api = retrofit.create(Athena::class.java)

    api.getPracticeInfo(1)
        .flatMap { Observable.fromIterable(it.practiceInfos) }
        .subscribe(
            { info -> print(info) },
            { err -> err.printStackTrace() },
            { println("Done") }
        )
  }
}

fun main(args: Array<String>) {
  PatientExtractor().run()
}