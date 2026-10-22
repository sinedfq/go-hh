package com.example.trudomix.ui.components

import kotlin.math.min

data class AvatarCropPose(val zoom:Float=1f,val centerX:Float=.5f,val centerY:Float=.5f)
data class AvatarCropSquare(val left:Float,val top:Float,val edge:Float)

fun avatarCropSquare(width:Int,height:Int,pose:AvatarCropPose):AvatarCropSquare {
    require(width>0&&height>0)
    val edge=min(width,height)/pose.zoom.coerceIn(1f,6f)
    val left=(pose.centerX*width-edge/2).coerceIn(0f,width-edge)
    val top=(pose.centerY*height-edge/2).coerceIn(0f,height-edge)
    return AvatarCropSquare(left,top,edge)
}

fun transformAvatarCrop(width:Int,height:Int,pose:AvatarCropPose,zoomFactor:Float,panX:Float=0f,panY:Float=0f,focalX:Float=.5f,focalY:Float=.5f):AvatarCropPose {
    val before=avatarCropSquare(width,height,pose)
    val zoom=(pose.zoom*zoomFactor).coerceIn(1f,6f)
    val edge=min(width,height)/zoom
    // Pan/focal coordinates are fractions of the square preview, not device pixels.
    val x=before.left+before.edge*focalX-edge*(focalX-.5f)-panX*edge
    val y=before.top+before.edge*focalY-edge*(focalY-.5f)-panY*edge
    val candidate=AvatarCropPose(zoom,x/width,y/height)
    val after=avatarCropSquare(width,height,candidate)
    return candidate.copy(centerX=(after.left+after.edge/2)/width,centerY=(after.top+after.edge/2)/height)
}
