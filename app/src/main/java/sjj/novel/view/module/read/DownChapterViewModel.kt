package sjj.novel.view.module.read

import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import sjj.novel.data.repository.novelDataRepository
import sjj.novel.model.Book
import sjj.novel.model.BookSourceRecord
import sjj.novel.model.Chapter
import sjj.novel.util.SafeLiveData
import sjj.novel.util.ViewModelDispose

class DownChapterViewModel(var bookName: String, var bookAuthor: String) : ViewModelDispose() {

    val book = SafeLiveData<Book>()

    val chapterList = SafeLiveData<List<Chapter>>()

    val bookSourceRecord = SafeLiveData<BookSourceRecord>()

    /**
     * 开始下载的起始章节
     */
    val startChapter = SafeLiveData<Chapter?>()

    val endChapter = SafeLiveData<Chapter?>()

    fun initData(): Flowable<Book?> {
        val chapterIntroUntil = PublishProcessor.create<Unit>()
        val bookSourceRecordUntil = PublishProcessor.create<Unit>()
        return novelDataRepository.getBookInBookSource(bookName, bookAuthor).flatMap { bk ->
            //停止章节简介的订阅
            chapterIntroUntil.offer(Unit)
            bookSourceRecordUntil.offer(Unit)
            book.setValue(bk)
            novelDataRepository.getChapterIntro(bk.url).takeUntil(chapterIntroUntil).flatMap { list ->
                bookSourceRecordUntil.offer(Unit)
                chapterList.setValue(list)
                endChapter.setValue(list.lastOrNull())
                novelDataRepository.getBookSourceRecord(bookName, bookAuthor).takeUntil(bookSourceRecordUntil).map {
                    bookSourceRecord.setValue(it)
                    startChapter.setValue(list.getOrElse(it.readIndex) { list.lastOrNull() })
                    bk
                }
            }
        }
    }

    init {
        initData()
                .subscribe()
                .autoDispose("view model Download Chapter init")
    }

    fun cachedBookChapter(): Flowable<Pair<Int, Int>> {
        val start = startChapter.value ?: return Flowable.error(Exception("请设置起始章节"))
        val end = endChapter.value ?: return Flowable.error(Exception("请设置结束章节"))
        return novelDataRepository.cachedBookChapter(start, end)
    }

}