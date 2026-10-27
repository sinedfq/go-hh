package com.example.trudomix.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.trudomix.ui.loading.AppLoader
import com.example.trudomix.ui.loading.StaticLoaderLogo
import androidx.compose.ui.graphics.luminance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshContent(refreshing:Boolean,onRefresh:()->Unit,content:@Composable BoxScope.()->Unit){
    val state=rememberPullToRefreshState()
    PullToRefreshBox(isRefreshing=refreshing,onRefresh=onRefresh,state=state,modifier=Modifier.fillMaxSize(),indicator={
        if(refreshing||state.distanceFraction>0f){
            Box(Modifier.align(Alignment.TopCenter).pullToRefreshIndicator(state,refreshing,containerColor=MaterialTheme.colorScheme.surface).semantics{contentDescription=if(refreshing)"Обновление"else "Потяните для обновления"},contentAlignment=Alignment.Center){
                if(refreshing)AppLoader(Modifier.size(36.dp))
                else StaticLoaderLogo(MaterialTheme.colorScheme.background.luminance()<.5f,Modifier.size(26.dp,22.1.dp).graphicsLayer{
                    val scale=.6f+.4f*state.distanceFraction.coerceIn(0f,1f);scaleX=scale;scaleY=scale
                })
            }
        }
    },content=content)
}
