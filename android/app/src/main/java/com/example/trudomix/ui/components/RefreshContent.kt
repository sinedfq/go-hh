package com.example.trudomix.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshContent(refreshing:Boolean,onRefresh:()->Unit,content:@Composable BoxScope.()->Unit){PullToRefreshBox(isRefreshing=refreshing,onRefresh=onRefresh,modifier=Modifier.fillMaxSize(),content=content)}
