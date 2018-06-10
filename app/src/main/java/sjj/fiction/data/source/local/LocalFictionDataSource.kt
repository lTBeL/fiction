package sjj.fiction.data.source.local

import com.google.gson.Gson
import io.reactivex.Observable
import sjj.fiction.data.repository.FictionDataRepository
import sjj.fiction.model.Book
import sjj.fiction.model.BookGroup
import sjj.fiction.model.Chapter
import sjj.fiction.util.def

/**
 * Created by SJJ on 2017/10/15.
 */
class LocalFictionDataSource : FictionDataRepository.SourceLocal {

    private val KEY_SEARCH_HISTORY = "LOCAL_FICTION_DATA_SOURCE_KEY_SEARCH_HISTORY"


    private val gson = Gson()

    override fun saveBookGroup(book: List<BookGroup>): Observable<List<BookGroup>> {
        return def {
            with(booksDataBase.bookDao()) {
                saveBookGroups(book)
                book.forEach {
                    saveBooks(it.books)
                    it.books.forEach {
                        saveChapter(it.chapterList)
                    }
                }
            }
            book
        }
    }

    override fun updateBookGroup(book: BookGroup): Observable<BookGroup> {
        return def {
            booksDataBase.bookDao().saveBookGroups(listOf(book))
            book
        }
    }

    override fun updateBook(book: Book): Observable<Book> {
        return def {
            booksDataBase.bookDao().saveBooks(listOf(book))
            booksDataBase.bookDao().saveChapter(book.chapterList)
            book
        }
    }

    override fun saveChapter(chapter: Chapter): Observable<Chapter> {
        return def {
            booksDataBase.bookDao().saveChapter(listOf(chapter))
            chapter
        }
    }

    override fun loadBookDetailsAndChapter(book: Book): Observable<Book> {
        return def {
            val r = booksDataBase.bookDao().getBook(book.id)
            r.chapterList = booksDataBase.bookDao().getChapterIntro(book.id)
            book.url = r.url
            book.name = r.name
            book.author = r.author
            book.bookCoverImgUrl = r.bookCoverImgUrl
            book.intro = r.intro
            book.chapterList = r.chapterList
            book
        }
    }

    override fun loadBookChapter(chapter: Chapter): Observable<Chapter> = def {
        val chapter1 = booksDataBase.bookDao().getChapter(chapter.url)
        chapter.url = chapter1.url
        chapter.bookId = chapter1.bookId
        chapter.index = chapter1.index
        chapter.chapterName = chapter1.chapterName
        chapter.isLoadSuccess = chapter1.isLoadSuccess
        chapter.content = chapter1.content
        chapter
    }

    override fun loadBookGroups(): Observable<List<BookGroup>> {
        return def {
            val group = booksDataBase.bookDao().getAllBookGroup()
            group.forEach {
                it.books = booksDataBase.bookDao().getBook(it.bookName, it.author).toMutableList()
                it.books.forEach { b ->
                    b.chapterList = booksDataBase.bookDao().getChapterIntro(b.id)
                    if (it.bookId == b.id) {
                        it.currentBook = b
                    }
                }
            }
            return@def group
        }
    }

    override fun loadBookGroup(bookName: String, author: String): Observable<BookGroup> {
        return def {
            val result = booksDataBase.bookDao().getBookGroup(bookName, author)
//            result.currentBook = (select from Book::class where (Book_Table.id eq result.bookId)).result!!
            result.books = booksDataBase.bookDao().getBook(bookName, author).toMutableList()
            result.books.forEach { b ->
                b.chapterList = booksDataBase.bookDao().getChapterIntro(b.id)
                if (result.bookId == b.id) {
                    result.currentBook = b
                }
            }
            result
        }
    }

    override fun deleteBookGroup(bookName: String, author: String): Observable<BookGroup> {
        return def {
            val bookGroup = booksDataBase.bookDao().getBookGroup(bookName, author)
            val list =booksDataBase.bookDao().getBook(bookName, author).toMutableList()
            val chapter = list.flatMap {
                booksDataBase.bookDao().getChapterIntro(it.id)
            }
            booksDataBase.bookDao().deleteChapter(chapter)
            booksDataBase.bookDao().deleteBooks(list)
            booksDataBase.bookDao().deleteBookGroup(bookGroup)
            bookGroup
        }
    }

}