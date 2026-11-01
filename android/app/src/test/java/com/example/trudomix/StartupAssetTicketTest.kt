package com.example.trudomix

import com.example.trudomix.startup.StartupAssetTicket
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupAssetTicketTest{
 @Test fun memoryCacheSuccessBeforeMountDoesNotLeavePendingWork(){
  var pending=0
  val ticket=StartupAssetTicket({pending++},{pending--})
  ticket.complete();ticket.start()
  assertEquals(0,pending)
 }
 @Test fun loadErrorOrDisposalReleasesExactlyOneTicket(){
  var pending=0
  val ticket=StartupAssetTicket({pending++},{pending--})
  ticket.start();ticket.start()
  assertEquals(1,pending)
  ticket.complete();ticket.complete();ticket.start()
  assertEquals(0,pending)
 }
}
