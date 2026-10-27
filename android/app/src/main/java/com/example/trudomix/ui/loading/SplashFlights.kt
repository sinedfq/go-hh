package com.example.trudomix.ui.loading

import kotlin.math.*

/** Randomness is sampled while building a flight, never in a drawing frame. */
object SplashFlights {
    data class Point(val x:Float,val y:Float)
    data class Curve(val start:Point,val first:Point,val second:Point,val end:Point)
    fun curves(w:Float,h:Float,seed:Int,index:Int,margin:Float,step:Float):List<Curve>{
        val corner=index/3;val j=index%3;val short=min(w,h)
        val start=Point(if(corner%2==0)-margin-j*step else w+margin+j*step,if(corner<2)-margin-(2-j)*step else h+margin+(2-j)*step)
        val end=Point(w/2,h/2);val dx=end.x-start.x;val dy=end.y-start.y;val length=hypot(dx,dy);val nx=-dy/length;val ny=dx/length
        var bits=seed xor ((index+1)*0x9e3779b9.toInt())
        fun random():Float{bits=bits xor (bits shl 13);bits=bits xor (bits ushr 17);bits=bits xor (bits shl 5);return ((bits.toLong() and 0xffffffffL)/4294967296.0).toFloat()}
        val sign=if(random()<.5f)-1f else 1f
        val knots=mutableListOf(start)
        listOf(.20f,.43f,.66f,.85f).forEachIndexed{k,q->
            val base=Point(start.x+dx*q,start.y+dy*q)
            var low=Float.NEGATIVE_INFINITY;var high=Float.POSITIVE_INFINITY
            for((value,normal,edge) in listOf(Triple(base.x,nx,w),Triple(base.y,ny,h))){
                if(abs(normal)<1e-6f)continue
                val first=(8-value)/normal;val last=(edge-8-value)/normal
                low=max(low,min(first,last));high=min(high,max(first,last))
            }
            val requested=sign*(if(k%2==1)-1 else 1)*short*(.38f+random()*.42f)*sin(PI.toFloat()*q)
            val offset=(if(low<=high)requested.coerceIn(low,high)else 0f)*(.60f+random()*.36f)
            knots.add(Point(base.x+nx*offset,base.y+ny*offset))
        }
        knots.add(end)
        return (0 until knots.lastIndex).map{i->
            val a=knots[i];val d=knots[i+1];val before=knots[max(0,i-1)];val after=knots[min(knots.lastIndex,i+2)]
            Curve(a,Point(a.x+(d.x-before.x)/6,a.y+(d.y-before.y)/6),Point(d.x-(after.x-a.x)/6,d.y-(after.y-a.y)/6),d)
        }
    }
}
