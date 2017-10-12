package sjj.fiction.search

import io.reactivex.Observable
import sjj.fiction.data.Repository.FictionDataRepository
import sjj.fiction.data.source.remote.dhzw.DhzwDataSource
import sjj.fiction.data.source.remote.yunlaige.YunlaigeDataSource
import sjj.fiction.model.Book
import sjj.fiction.model.SearchResultBook
import sjj.fiction.model.Url
import sjj.fiction.util.DATA_REPOSITORY_FICTION
import sjj.fiction.util.DataRepository
import sjj.fiction.util.errorObservable

/**
 * Created by SJJ on 2017/10/8.
 */
class SearchPresenter(private val view: SearchContract.view) : SearchContract.presenter {
    private val sources = arrayOf<FictionDataRepository.Source>(DhzwDataSource(),YunlaigeDataSource())
    private var data: FictionDataRepository? = null
    override fun start() {
        data = DataRepository[DATA_REPOSITORY_FICTION]
    }

    override fun stop() {
        data = null
    }

    override fun search(text: String): Observable<List<SearchResultBook>> = data?.search(text) ?: errorObservable<List<SearchResultBook>>("this presenter not start")

    override fun onSelect(book: SearchResultBook):Observable<Book> = data?.loadBookDetailsAndChapter(book)?: errorObservable<Book>("this presenter not start")

    override fun onSelect(url: Url) {

    }

    fun setSource(toInt: Int) {
        data?.source = sources[toInt]
    }

}