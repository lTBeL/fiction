package sjj.novel.view.module.read

import androidx.lifecycle.Observer
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import android.view.*
import androidx.core.view.GravityCompat
import androidx.navigation.Navigation
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_read.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import sjj.novel.*
import sjj.novel.view.module.details.DetailsActivity
import sjj.novel.model.Book
import sjj.novel.model.Chapter
import sjj.novel.util.getModel
import sjj.novel.util.initScreenBrightness
import sjj.novel.util.observeOnMain
import sjj.novel.view.fragment.ChapterListFragment
import sjj.novel.view.reader.bean.BookBean
import sjj.novel.view.reader.bean.BookRecordBean
import sjj.novel.view.reader.page.PageLoader
import sjj.novel.view.reader.page.PageLoader.STATUS_LOADING
import sjj.novel.view.reader.page.PageMode
import sjj.novel.view.reader.page.PageView
import sjj.novel.view.reader.page.TxtChapter
import kotlin.math.max
import kotlin.math.min

class ReadActivity : BaseActivity(), ReaderSettingFragment.CallBack,ChapterListFragment.ItemClickListener,ChapterListFragment.ShowController {
    companion object {
        val BOOK_NAME = "BOOK_NAME"
        val BOOK_AUTHOR = "BOOK_AUTHOR"
    }

    private lateinit var model: ReadViewModel

    private val mPageLoader by lazy { chapterContent.pageLoader }

    private var controller: ReaderSettingFragment.CallBack.Controller? = null
    private var controllerChapterList: ChapterListFragment.Controller? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initScreenBrightness(this)
        setContentView(R.layout.activity_read)

        //关闭菜单栏
        toggleMenu()
        //禁止手势滑出
        drawer_layout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        chapterContent.setTouchListener(object : PageView.TouchListener {
            override fun intercept(event: MotionEvent?): Boolean {
                val showing = supportActionBar!!.isShowing
                if (showing && event?.action == MotionEvent.ACTION_DOWN) {
                    return true
                }
                if (showing && event?.action == MotionEvent.ACTION_UP) {
                    toggleMenu()
                    return true
                }
                return showing
            }

            override fun center() {
                toggleMenu()
            }

        })

        model = getModel { arrayOf(intent.getStringExtra(BOOK_NAME), intent.getStringExtra(BOOK_AUTHOR)) }

        supportFragmentManager.beginTransaction()
                .replace(R.id.chapter_list,ChapterListFragment.create(model.name,model.author))
                .commitAllowingStateLoss()

        model.book.firstElement().observeOn(AndroidSchedulers.mainThread()).subscribe { book ->
            title = book.name
            //阅读记录
            model.readIndex.firstElement().observeOn(AndroidSchedulers.mainThread()).subscribe {
                val index = min(max(book.chapterList.lastIndex, 0), it.readIndex)

                mPageLoader.setBookRecord(BookRecordBean().apply {
                    bookId = book.url
                    chapter = index
                    pagePos = it.pagePos
                    isThrough = it.isThrough
                })

                mPageLoader.setOnPageChangeListener(object : PageLoader.OnPageChangeListener {
                    override fun onBookRecordChange(bean: BookRecordBean) {
                        model.setReadIndex(model.chapterList[bean.chapter], bean.pagePos, bean.isThrough).subscribe()
                    }

                    override fun onChapterChange(pos: Int) {
                        controllerChapterList?.scrollToPosition(pos)
                    }

                    override fun requestChapters(requestChapters: MutableList<TxtChapter>) {
                        model.getChapter(requestChapters).observeOnMain().subscribe({
                            if (mPageLoader.pageStatus == STATUS_LOADING)
                                mPageLoader.openChapter()
                        }, {
                            if (mPageLoader.pageStatus == STATUS_LOADING)
                                mPageLoader.chapterError()
                        }).destroy("requestChapters")
                    }

                    override fun onCategoryFinish(chapters: MutableList<TxtChapter>?) {
                    }

                    override fun onPageCountChange(count: Int) {
                        controller?.setPageCount(count)
                    }

                    override fun onPageChange(pos: Int) {
                        controller?.setPagePos(pos)
                    }

                })

                initBookData(book)
            }.destroy(DISPOSABLE_ACTIVITY_READ_READ_INDEX)


        }.destroy()

        AppConfig.readerPageStyle.observe(this, Observer {
            mPageLoader.setPageStyle(it)
        })
        AppConfig.fontSize.observe(this, Observer {
            mPageLoader.setTextSize(it!!)
        })
    }

    private fun initBookData(book: Book) {
        mPageLoader.book = BookBean().apply {
            id = book.url
            title = book.name
            author = book.author
            shortIntro = book.intro
            cover = book.bookCoverImgUrl
            bookChapterList = book.chapterList.map { chapter ->
                TxtChapter().apply {
                    this.bookId = book.url
                    this.link = chapter.url
                    this.title = chapter.chapterName
                }
            }

        }
        mPageLoader.refreshChapterList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_read_menu, menu)
        val subMenu = menu.addSubMenu(0, R.id.menu_read_flip_page_mode, 1, "翻页模式")

        val mode = AppConfig.flipPageMode
        for (m in PageMode.values()) {
            val item = subMenu.add(0, m.ordinal, 0, m.des)
            item.isChecked = mode == m.name
        }
        subMenu.setGroupCheckable(0, true, true)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_read_flip_page_mode)?.also {
            val subMenu = it.subMenu
            val mode = PageMode.valueOf(AppConfig.flipPageMode)
            subMenu.getItem(mode.ordinal).isChecked = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_intro -> {
                startActivity<DetailsActivity>(DetailsActivity.BOOK_NAME to model.name, DetailsActivity.BOOK_AUTHOR to model.author)
                true
            }
            R.id.menu_refresh -> {
                mPageLoader.curChapter?.also {
                    model.getChapter(listOf(it), true).observeOnMain().subscribe({ txtChapter ->
                        mPageLoader.refreshChapter(txtChapter.first())
                    }, {
                        toast("刷新失败")
                    }).destroy("requestChapters menu_refresh")
                }
                true
            }
            R.id.menu_refresh_chapters -> {
                showSnackbar(chapterContent, "正在更新目录信息，请稍后……", Snackbar.LENGTH_INDEFINITE)
                model.refresh().observeOnMain().subscribe({
                    initBookData(it)
                    showSnackbar(chapterContent, "目录更新成功")
                }, {
                    showSnackbar(chapterContent, "目录更新失败：${it.message}")
                }).destroy("activity read refresh chapter list")
                true
            }
            else -> when {
                item.itemId < PageMode.values().size && item.itemId >= 0 -> {
                    val mode = PageMode.values()[item.itemId]
                    AppConfig.flipPageMode = mode.name
                    mPageLoader.setPageMode(mode)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onBackPressed() {
        when {
            drawer_layout?.isDrawerOpen(GravityCompat.END) == true -> drawer_layout?.closeDrawers()
            supportActionBar?.isShowing == true -> toggleMenu()
            else -> super.onBackPressed()
        }
    }

    private fun toggleMenu() {
        if (supportActionBar?.isShowing == true) {
            supportActionBar?.hide()
            Navigation.findNavController(this, R.id.nav_host_fragment_read).navigate(R.id.fragment_reader_setting)
            supportFragmentManager.beginTransaction()
                    .hide(nav_host_fragment_read)
                    .commitAllowingStateLoss()
        } else {
            supportFragmentManager.beginTransaction()
                    .show(nav_host_fragment_read)
                    .commit()
            supportActionBar?.show()
        }
    }

    override fun onPause() {
        super.onPause()
        mPageLoader.saveRecord()
    }

    /**
     * 底部菜单回调
     */
    override fun openChapterList() {
        drawer_layout.openDrawer(GravityCompat.END)
    }

    /**
     * 底部菜单回调
     */
    override fun getPageLoader(): PageLoader? {
        return mPageLoader
    }

    override fun setController(controller: ReaderSettingFragment.CallBack.Controller) {
        this.controller = controller
    }

    /**
     * 章节列表点击回调
     */
    override fun onClick(chapter: Chapter) {
        model.setReadIndex(chapter, 0).subscribe().destroy(DISPOSABLE_READ_INDEX)
        mPageLoader.skipToChapter(chapter.index)
    }

    /**
     * 章节列表控制回调
     */
    override fun set(controller: ChapterListFragment.Controller) {
        controllerChapterList = controller
    }
}
