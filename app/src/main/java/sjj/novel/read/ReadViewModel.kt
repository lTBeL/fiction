package sjj.novel.read

import android.arch.lifecycle.ViewModel
import android.text.Html
import io.reactivex.Observable
import sjj.novel.data.repository.novelDataRepository
import sjj.novel.model.Chapter
import sjj.novel.util.lazyFromIterable
import sjj.novel.view.reader.page.TxtChapter
import java.util.concurrent.TimeUnit

class ReadViewModel(val name: String, val author: String) : ViewModel() {

    val book = novelDataRepository.getBookInBookSource(name, author)

    private var lastReadIndex = 0
    private var isThrough: Boolean? = null

    fun getChapters(bookUrl: String) = novelDataRepository.getChapters(bookUrl)

    fun loadChapter(chapter: Chapter): Observable<Chapter> {
        return novelDataRepository.loadChapter(chapter)
    }

    fun getChapter(url: String): Observable<Chapter> {
        return novelDataRepository.getChapter(url)
    }

    fun getChapter(requestChapters: List<TxtChapter>): Observable<List<TxtChapter>> {
        return Observable.just(requestChapters).lazyFromIterable { txtChapter ->
            novelDataRepository.getChapter(txtChapter.link).map { chapter ->
                txtChapter.content = Html.fromHtml(chapter.content).toString()
                txtChapter.title = chapter.chapterName
                txtChapter
            }.delay(500, TimeUnit.MILLISECONDS)
        }.flatMap { it }.reduce(requestChapters) { _, _ -> requestChapters }.toObservable()
    }

    val readIndex = novelDataRepository.getBookSourceRecord(name, author).doOnNext {
        lastReadIndex = it.readIndex
    }

    fun setReadIndex(index: Chapter, isThrough: Boolean = false): Observable<Int> {
        if (lastReadIndex == index.index && this.isThrough == isThrough) {
            return Observable.empty()
        }
        this.isThrough = isThrough
        lastReadIndex = index.index
        return novelDataRepository.setReadIndex(name, author, index, isThrough)
    }


    fun cachedBookChapter(bookUrl: String) = novelDataRepository.cachedBookChapter(bookUrl)

}