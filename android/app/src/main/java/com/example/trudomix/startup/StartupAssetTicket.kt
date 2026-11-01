package com.example.trudomix.startup

/** Success may arrive from the memory cache before a Compose effect is committed. */
internal class StartupAssetTicket(private val onBegin:(Any)->Unit,private val onFinish:(Any)->Unit){
    private var started=false
    private var completed=false
    fun start(){if(!started&&!completed){started=true;onBegin(this)}}
    fun complete(){if(!completed){completed=true;if(started)onFinish(this)}}
}
