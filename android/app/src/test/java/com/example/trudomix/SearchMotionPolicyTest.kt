package com.example.trudomix

import com.example.trudomix.ui.components.searchMotionAllowed
import org.junit.Assert.*
import org.junit.Test

class SearchMotionPolicyTest{
    @Test fun disablesContinuousMotionOnConstrainedDevicesAndWhenSystemRequestsIt(){
        assertTrue(searchMotionAllowed(false,256,false,1f))
        assertFalse(searchMotionAllowed(true,256,false,1f))
        assertFalse(searchMotionAllowed(false,128,false,1f))
        assertFalse(searchMotionAllowed(false,256,true,1f))
        assertFalse(searchMotionAllowed(false,256,false,0f))
    }
}
