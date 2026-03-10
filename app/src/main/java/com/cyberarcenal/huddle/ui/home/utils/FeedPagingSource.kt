package com.cyberarcenal.huddle.ui.home.utils

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.network.ApiService
import retrofit2.HttpException
import java.io.IOException
import kotlin.collections.emptyList

class FeedPagingSource : PagingSource<Int, PostFeed>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PostFeed> {
        val page = params.key ?: 1
        return try {
            val response = ApiService.v1Api.v1FeedPostsRetrieve(
                feed = true,
                page = page,
                pageSize = params.loadSize
            )
            if (response.isSuccessful) {
                val posts = response.body()?.results ?: emptyList()
                LoadResult.Page(
                    data = posts,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (posts.isEmpty()) null else page + 1
                )
            } else {
                LoadResult.Error(HttpException(response))
            }
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PostFeed>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}