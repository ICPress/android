package net.crowdventures.storypop.paging

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import net.crowdventures.storypop.Constants
import net.crowdventures.storypop.room.StoryRoom
import net.crowdventures.storypop.util.ViewModelUtil
import net.crowdventures.storypop.viewmodels.StorySavedModel
import net.crowdventures.storypop.viewmodels.StorySavedViewModel
import java.io.IOException
import java.util.UUID


class StorySavedSource(val context: Context, val viewModel: StorySavedViewModel,val authorUserUUID: UUID) : PagingSource<Int, StorySavedModel>()  {
        override fun getRefreshKey(state: PagingState<Int, StorySavedModel>): Int?
        {
            return state.anchorPosition
        }

        private fun getPagerFlowStoryRoom(): Flow<PagingData<StoryRoom>> {
            val storyDB = Constants.getStoryDatabase(context)
            val getPagedSories = storyDB.storyDao().getAllStoriesForUserPagedFactory(authorUserUUID)
            val flow = Pager<Int,StoryRoom>(
                // Configure how data is loaded by passing additional properties to
                // PagingConfig, such as prefetchDistance.
                PagingConfig(pageSize = 5)
            ) {
                getPagedSories.asPagingSourceFactory().invoke()
            }.flow.cachedIn(viewModel.viewModelScope)
            return flow
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, StorySavedModel> {
            try {
                val nextPage = params.key ?: 1
                val resultList: MutableList<StorySavedModel> = mutableListOf()
                getPagerFlowStoryRoom().transform{ value ->
                    value.map {
                        val storyRoomAsSavedModel = ViewModelUtil.getStoryRoomAsStorySavedModel(it )
                        emit(storyRoomAsSavedModel)
                    }
                }.toList(resultList)
                return LoadResult.Page(
                    data = resultList,
                    prevKey = if (nextPage == 1) null else nextPage - 1,
                    nextKey = nextPage +1 //if (userList.data.isEmpty()) null else userList.page + 1
                )
            } catch (exception: IOException) {
                return LoadResult.Error(exception)
            }
//        catch (exception: HttpException) {
//            return LoadResult.Error(exception)
//        }
        }


}