package sjj.fiction.data.source.remote

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Url

interface TestHttpInterface {
    @GET
    fun hello(@Url  url: String): Observable<String>
}