package sjj.fiction.details

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.paging.PagedListAdapter
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_details.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import sjj.fiction.BaseActivity
import sjj.fiction.Details
import sjj.fiction.R
import sjj.fiction.model.Book
import sjj.fiction.model.Chapter
import sjj.fiction.read.ReadActivity
import sjj.fiction.util.domain
import sjj.fiction.util.getModel

/**
 * Created by SJJ on 2017/10/10.
 */
class DetailsActivity : BaseActivity() {
    companion object {
        const val BOOK_NAME = "book_name"
        const val BOOK_AUTHOR = "book_author"
    }

    private val model by lazy {
        getModel<DetailsViewModel>(object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DetailsViewModel(intent.getStringExtra(BOOK_NAME), intent.getStringExtra(BOOK_AUTHOR)) as T
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bind = DataBindingUtil.setContentView<Details>(this, R.layout.activity_details)
        val adapter = ChapterListAdapter()
        model.book.observeOn(AndroidSchedulers.mainThread()).subscribe {
            bind.book = it
            originWebsite.text = it.url.domain()
            originWebsite.setOnClickListener { v ->
                v.isEnabled = false
                model.bookSource.doOnTerminate {
                    v.isEnabled = true
                }.observeOn(AndroidSchedulers.mainThread()).subscribe { bs ->
                    v.isEnabled = true
                    alert {
                        items(bs.map { it.domain() }) { dialog, index ->
                            dialog.dismiss()
                            model.setBookSource(bs[index]).subscribe()
                        }
                    }.show()
                }
            }
            chapterListButton.setOnClickListener { v ->
                if (chapterList.visibility != View.VISIBLE) {
                    chapterList.visibility = View.VISIBLE
                    model.getChapters(it.url).observe(this, Observer(adapter::submitList))
                    model.readIndex.firstElement().observeOn(AndroidSchedulers.mainThread()).subscribe { index ->
                        chapterList.scrollToPosition(index)
                    }
                } else {
                    chapterList.visibility = View.GONE
                }
            }
            refreshBtn.setOnClickListener { _ -> model.refresh(it) }

            intro.text = it.intro
            bookCover.setImageURI(it.bookCoverImgUrl)

            model.getLatestChapter(it.url).observeOn(AndroidSchedulers.mainThread()).subscribe {
                latestChapter.text = it.chapterName
                latestChapter.setOnClickListener { v ->
                    v.isEnabled = false
                    model.setReadIndex(it.index).doOnTerminate {
                        v.isEnabled = true
                    }.subscribe {
                        startActivity<ReadActivity>(ReadActivity.BOOK_NAME to model.name, ReadActivity.BOOK_AUTHOR to model.author)
                    }
                }
            }

        }.destroy()



        chapterList.layoutManager = LinearLayoutManager(this)
        chapterList.adapter = adapter
    }


    private inner class ChapterListAdapter : PagedListAdapter<Chapter, ViewHolder>(object : DiffUtil.ItemCallback<Chapter>() {
        override fun areItemsTheSame(oldItem: Chapter?, newItem: Chapter?): Boolean {
            return oldItem?.url == newItem?.url
        }

        override fun areContentsTheSame(oldItem: Chapter?, newItem: Chapter?): Boolean {
            return oldItem?.chapterName == newItem?.chapterName
        }
    }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return object : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_text_text, parent, false)) {}
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val book = getItem(position) ?: return
            holder.itemView.find<TextView>(R.id.text1).text = book.chapterName
            holder.itemView.setOnClickListener {
                it.isEnabled = false
                model.setReadIndex(position).observeOn(AndroidSchedulers.mainThread()).doOnTerminate {
                    it.isEnabled = true
                }.subscribe {
                    startActivity<ReadActivity>(ReadActivity.BOOK_NAME to model.name, ReadActivity.BOOK_AUTHOR to model.author)
                }
            }
        }
    }
}