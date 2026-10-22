package com.example.trudomix

import com.example.trudomix.ui.components.*
import org.junit.Assert.*
import org.junit.Test

class AvatarCropGeometryTest {
    @Test fun portraitAndLandscapeStartCentered() {
        assertEquals(AvatarCropSquare(500f,0f,1000f),avatarCropSquare(2000,1000,AvatarCropPose()))
        assertEquals(AvatarCropSquare(0f,500f,1000f),avatarCropSquare(1000,2000,AvatarCropPose()))
    }
    @Test fun extremePanCannotExposeOutsideOfImage() {
        for((w,h) in listOf(2000 to 1000,1000 to 2000)) {
            for(zoom in listOf(1f,2f,6f))for(dx in listOf(-100f,100f))for(dy in listOf(-100f,100f)) {
                val pose=transformAvatarCrop(w,h,AvatarCropPose(zoom),1f,dx,dy)
                val rect=avatarCropSquare(w,h,pose)
                assertTrue(rect.left>=0f&&rect.top>=0f)
                assertTrue(rect.left+rect.edge<=w+.001f&&rect.top+rect.edge<=h+.001f)
            }
        }
    }
    @Test fun zoomRetainsFocalPointAndLimitsMagnification() {
        val before=avatarCropSquare(2000,2000,AvatarCropPose())
        val pose=transformAvatarCrop(2000,2000,AvatarCropPose(),2f,focalX=.75f,focalY=.25f)
        val after=avatarCropSquare(2000,2000,pose)
        assertEquals(before.left+before.edge*.75f,after.left+after.edge*.75f,.001f)
        assertEquals(before.top+before.edge*.25f,after.top+after.edge*.25f,.001f)
        assertEquals(6f,transformAvatarCrop(2000,2000,pose,100f).zoom)
        assertEquals(1f,transformAvatarCrop(2000,2000,pose,.0001f).zoom)
    }
}
