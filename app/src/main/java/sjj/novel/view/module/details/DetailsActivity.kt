package sjj.novel.view.module.details

import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import android.view.*
import android.widget.TextView
import androidx.core.view.GravityCompat
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_details.*
import org.jetbrains.anko.find
import org.jetbrains.anko.startActivity
import sjj.novel.BaseActivity
import sjj.novel.DISPOSABLE_ACTIVITY_DETAILS_REFRESH
import sjj.novel.R
import sjj.novel.databinding.ActivityDetailsBinding
import sjj.novel.model.Chapter
import sjj.novel.view.module.read.ReadActivity
import sjj.novel.util.getModel
import sjj.novel.util.observeOnMain
import sjj.novel.view.fragment.ChapterListFragment
import sjj.novel.view.fragment.ChooseBookSourceFragment

/**
 * Created by SJJ on 2017/10/10.
 */
class DetailsActivity : BaseActivity(),ChapterListFragment.ItemClickListener {
    companion object {
        const val BOOK_NAME = "book_name"
        const val BOOK_AUTHOR = "book_author"
    }

    private lateinit var model: DetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = getModel { arrayOf(intent.getStringExtra(BOOK_NAME), intent.getStringExtra(BOOK_AUTHOR)) }

        supportFragmentManager.beginTransaction()
                .replace(R.id.chapter_list,ChapterListFragment.create(model.name,model.author))
                .commitAllowingStateLoss()

        val bind: ActivityDetailsBinding = DataBindingUtil.setContentView(this, R.layout.activity_details)

        model.book.observeOn(AndroidSchedulers.mainThread()).subscribe { book ->
            bind.book = book
            originWebsite.text = book.origin?.sourceName
            originWebsite.setOnClickListener { v ->
                ChooseBookSourceFragment.newInstance(book.name, book.author).show(supportFragmentManager)
            }

            detailsRefreshLayout.setOnRefreshListener {
                model.refresh(book).observeOn(AndroidSchedulers.mainThread()).doOnTerminate {
                    detailsRefreshLayout.isRefreshing = false
                }.subscribe().destroy(DISPOSABLE_ACTIVITY_DETAILS_REFRESH)
            }
            reading.setOnClickListener { _ ->
                model.bookSourceRecord.firstElement().observeOn(AndroidSchedulers.mainThread()).subscribe {
                    if (it.isThrough && it.readIndex == book.chapterList.size - 2) {
                        //有更新点击阅读直接进入下一章
                        model.setReadIndex(book.chapterList.last()).observeOn(AndroidSchedulers.mainThread()).subscribe {
                            startActivity<ReadActivity>(ReadActivity.BOOK_NAME to model.name, ReadActivity.BOOK_AUTHOR to model.author)
                        }.destroy("read book")

                    } else {
                        startActivity<ReadActivity>(ReadActivity.BOOK_NAME to model.name, ReadActivity.BOOK_AUTHOR to model.author)
                    }
                }.destroy("load book source record")
            }
            intro.text = book.intro
            bookCover.setImageURI(book.bookCoverImgUrl)
            if (book.chapterList.isNotEmpty()) {
                latestChapter.text = book.chapterList.last().chapterName
                latestChapter.setOnClickListener { v ->
                    v.isEnabled = false
                    model.setReadIndex(book.chapterList.last()).observeOn(AndroidSchedulers.mainThread()).doOnTerminate {
                        v.isEnabled = true
                    }.subscribe {
                        startActivity<ReadActivity>(ReadActivity.BOOK_NAME to model.name, ReadActivity.BOOK_AUTHOR to model.author)
                    }
                }
            } else {
                latestChapter.text = "无章节信息"
                latestChapter.isClickable = false
            }
        }.destroy("book details activity")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_details_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.chapter_list -> {
                model.bookSourceRecord.firstElement().observeOn(AndroidSchedulers.mainThread()).subscribe { index ->

                }
                drawer_layout.openDrawer(GravityCompat.END)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.END)) {
            drawer_layout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    override fun onClick(chapter: Chapter) {
        model.setReadIndex(chapter).observeOnMain().subscribe {
            startActivity<ReadActivity>(ReadActivity.BOOK_NAME to model.name, ReadActivity.BOOK_AUTHOR to model.author)
        }.destroy("details activity start read novel")
    }
}