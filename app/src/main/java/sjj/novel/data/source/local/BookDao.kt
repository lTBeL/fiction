package sjj.novel.data.source.local

import androidx.paging.DataSource
import androidx.room.*
import io.reactivex.Flowable
import sjj.novel.model.Book
import sjj.novel.model.BookSourceRecord
import sjj.novel.model.Chapter


@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecordAndBooks(bookSource: BookSourceRecord, books: List<Book>)

    @Query("update BookSourceRecord set sequence=:sequence where bookName=:bookName and author=:author")
    fun updateBookSourceRecordSeq(sequence: Int, bookName: String, author: String)

    @Query("SELECT MAX(sequence) FROM BookSourceRecord")
    fun getBookSourceRecordMaxSeq(): Int


    @Query("select * from Book where url in (select bookUrl from BookSourceRecord)")
    fun getBooksInRecord(): Flowable<List<Book>>

    @Query("select * from Book where url=:url")
    fun getBook(url: String): Flowable<Book>

    @Query("select * from Book where name=:name and author=:author")
    fun getBookSource(name: String, author: String): Flowable<List<Book>>

    @Query("SELECT * FROM Book WHERE url = (select bookUrl from BookSourceRecord where bookName=:name and author=:author)")
    fun getBookInBookSource(name: String, author: String): Flowable<Book>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateBook(book: Book)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertChapters(book: List<Chapter>)

    @Query("update Chapter set `index`=:index where url=:chapterId")
    fun updateChapterIndex(index: Int, chapterId: String)

    @Query("select * from Chapter where bookUrl=:bookUrl order by `index`")
    fun getChapters(bookUrl: String): DataSource.Factory<Int, Chapter>

    @Query("select * from Chapter where url=:url")
    fun getChapter(url: String): Chapter

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateChapter(chapter: Chapter)

    @Query("delete from BookSourceRecord where bookName=:bookName and author=:author")
    fun deleteBook(bookName: String, author: String): Int

    @Query("update BookSourceRecord set bookUrl=:url where bookName=:name and author=:author")
    fun updateBookSource(name: String, author: String, url: String): Int

    @Query("select * from BookSourceRecord where bookName=:name and author=:author")
    fun getBookSourceRecord(name: String, author: String): Flowable<BookSourceRecord>

    @Query("update BookSourceRecord set readIndex=:index,chapterName=:chapterName,isThrough=:isThrough,pagePos=:pagePos where bookName=:name and author=:author")
    fun setReadIndex(name: String, author: String, index: Int, chapterName: String, pagePos: Int, isThrough: Boolean): Int

    @Query("select * from Chapter where bookUrl=:bookUrl order by `index` desc limit 1")
    fun getLatestChapter(bookUrl: String): Chapter

    @Query("select url,bookUrl,`index`,chapterName,isLoadSuccess from Chapter where bookUrl=:bookUrl and isLoadSuccess=0 order by `index`")
    fun getUnLoadChapters(bookUrl: String): List<Chapter>

    @Query("select url,bookUrl,`index`,chapterName,isLoadSuccess from Chapter where bookUrl=:bookUrl order by `index`")
    fun getChapterIntro(bookUrl: String): Flowable<List<Chapter>>

    @Query("select url from Chapter where bookUrl=:bookUrl")
    fun getChapterIds(bookUrl: String): List<String>

    @Query("delete from chapter where url=:url")
    fun deleteChapter(url: String)
}