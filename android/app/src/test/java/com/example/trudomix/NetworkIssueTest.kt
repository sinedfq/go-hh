package com.example.trudomix

import com.example.trudomix.startup.*
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException

class NetworkIssueTest{
    @Test fun connectionFailuresAreNotAllOffline(){
        assertEquals(StartupState.OFFLINE,NetworkIssue.from(offline=true).state)
        assertEquals(StartupState.SERVER_UNAVAILABLE,NetworkIssue.from().state)
        assertEquals("SERVER",NetworkIssue.from(offline=true,status=503).kind)
        assertEquals("TIMEOUT",NetworkIssue.from(SocketTimeoutException()).kind)
        assertEquals("AUTH",NetworkIssue.from(status=401).kind)
    }
}
