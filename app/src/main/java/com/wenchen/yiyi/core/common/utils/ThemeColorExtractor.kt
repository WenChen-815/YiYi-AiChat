package com.wenchen.yiyi.core.common.utils

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import kotlin.math.*
import androidx.core.graphics.scale
import androidx.core.graphics.get

class ThemeColorExtractor {

    /**
     * 提取图片的主题颜色，返回Compose Color对象列表
     *
     * @param bitmap 输入的图片Bitmap对象
     * @param maxDimension 处理前图片的最大尺寸（宽或高），超过此值会等比例缩小
     *                     作用：控制图片处理规模，值越小性能越好（计算量少），但可能损失精度；默认600像素
     * @param sampleSize 像素采样间隔，每隔多少个像素取一个样本
     *                   作用：减少参与计算的像素数量，值越大性能越好但可能丢失细节；默认4（每4x4像素取1个）
     * @param maxColors 最多提取的主题色数量
     *                  作用：限制结果数量，值越大返回的颜色种类越多，但计算成本越高；默认5种
     * @param minSaturation 最小饱和度过滤阈值（范围0-1）
     *                      作用：过滤低饱和度颜色（接近灰色），值越高过滤越严格；默认0.1（保留10%以上饱和度）
     * @param minValue 最小明度过滤阈值（范围0-1）
     *                 作用：过滤过暗的颜色，值越高过滤掉的暗色越多；默认0.1（保留10%以上明度）
     * @param maxValue 最大明度过滤阈值（范围0-1）
     *                 作用：过滤过亮的颜色（接近白色），值越低过滤掉的亮色越多；默认0.95（保留95%以下明度）
     *
     * @return 提取出的主题色列表，按颜色在图片中的占比从高到低排序，每个元素为Compose的Color对象
     */
    fun extract(
        bitmap: Bitmap,
        maxDimension: Int = 600,
        sampleSize: Int = 4,
        maxColors: Int = 1,
        minSaturation: Float = 0.1f,
        minValue: Float = 0.1f,
        maxValue: Float = 0.95f
    ): List<Color> {
        val scaledBitmap = scaleBitmap(bitmap, maxDimension)

        val pixels = sampleAndFilterPixels(
            scaledBitmap,
            sampleSize,
            minSaturation,
            minValue,
            maxValue
        )

        if (pixels.isEmpty()) {
            return listOf(Color.White)
        }

        val clusters = kMeansHsv(pixels, maxColors.coerceAtMost(pixels.size))

        return clusters.sortedByDescending { it.pixels.size }
            .map { hsvToComposeColor(it.center) }
    }

    /**
     * 缩小图片尺寸
     */
    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            return bitmap
        }

        val scaleFactor = maxDimension.toFloat() / maxOf(originalWidth, originalHeight)
        val newWidth = (originalWidth * scaleFactor).toInt()
        val newHeight = (originalHeight * scaleFactor).toInt()

        return bitmap.scale(newWidth, newHeight)
    }

    /**
     * 采样图片像素并过滤
     */
    private fun sampleAndFilterPixels(
        bitmap: Bitmap,
        sampleSize: Int,
        minSaturation: Float,
        minValue: Float,
        maxValue: Float
    ): List<FloatArray> {
        val hsvPixels = mutableListOf<FloatArray>()
        val width = bitmap.width
        val height = bitmap.height
        val hsv = FloatArray(3)

        for (y in 0 until height step sampleSize) {
            for (x in 0 until width step sampleSize) {
                val pixel = bitmap[x, y]
                AndroidColor.colorToHSV(pixel, hsv)

                if (hsv[1] >= minSaturation && hsv[2] >= minValue && hsv[2] <= maxValue) {
                    hsvPixels.add(hsv.copyOf())
                }
            }
        }

        if (hsvPixels.isEmpty()) {
            for (y in 0 until height step sampleSize) {
                for (x in 0 until width step sampleSize) {
                    val pixel = bitmap[x, y]
                    AndroidColor.colorToHSV(pixel, hsv)
                    hsvPixels.add(hsv.copyOf())
                }
            }
        }

        return hsvPixels
    }

    /**
     * 在HSV颜色空间执行K-Means聚类
     */
    private fun kMeansHsv(pixels: List<FloatArray>, k: Int): List<HsvCluster> {
        if (k <= 0) return emptyList()

        val clusters = initializeHsvCenters(pixels, k)

        var iterations = 0
        var changed: Boolean

        do {
            changed = false
            clusters.forEach { it.pixels.clear() }

            pixels.forEach { pixel ->
                val nearestCluster = findNearestHsvCluster(pixel, clusters)
                nearestCluster.pixels.add(pixel)
            }

            clusters.forEach { cluster ->
                if (cluster.pixels.isNotEmpty()) {
                    val newCenter = calculateNewHsvCenter(cluster.pixels)
                    if (!centersEqual(newCenter, cluster.center)) {
                        cluster.center = newCenter
                        changed = true
                    }
                }
            }

            iterations++
        } while (changed && iterations < 100)

        return clusters.filter { it.pixels.isNotEmpty() }
    }

    /**
     * 改进的HSV聚类中心初始化（K-Means++算法）
     */
    private fun initializeHsvCenters(pixels: List<FloatArray>, k: Int): List<HsvCluster> {
        if (pixels.isEmpty()) return emptyList()

        val centers = mutableListOf(pixels.random().copyOf())

        while (centers.size < k) {
            val distances = pixels.map { pixel ->
                centers.minOf { center -> calculateHsvDistanceSquared(pixel, center) }
            }

            val sumDistances = distances.sum()
            if (sumDistances == 0.0) {
                centers.add(centers.random().copyOf())
            } else {
                var randomValue = Math.random() * sumDistances
                var selectedPixel: FloatArray? = null

                for (i in pixels.indices) {
                    randomValue -= distances[i]
                    if (randomValue <= 0) {
                        selectedPixel = pixels[i]
                        break
                    }
                }

                selectedPixel?.let { centers.add(it.copyOf()) }
            }
        }

        return centers.map { HsvCluster(it) }
    }

    /**
     * 找到离HSV像素最近的聚类
     */
    private fun findNearestHsvCluster(pixel: FloatArray, clusters: List<HsvCluster>): HsvCluster {
        var nearestCluster = clusters[0]
        var minDistance = calculateHsvDistance(pixel, nearestCluster.center)

        for (i in 1 until clusters.size) {
            val distance = calculateHsvDistance(pixel, clusters[i].center)
            if (distance < minDistance) {
                minDistance = distance
                nearestCluster = clusters[i]
            }
        }

        return nearestCluster
    }

    /**
     * 计算两个HSV颜色之间的距离
     */
    private fun calculateHsvDistance(color1: FloatArray, color2: FloatArray): Double {
        val h1 = color1[0]
        val s1 = color1[1] * 100
        val v1 = color1[2] * 100

        val h2 = color2[0]
        val s2 = color2[1] * 100
        val v2 = color2[2] * 100

        val hDiff = abs(h1 - h2)
        val hDistance = if (hDiff > 180) 360 - hDiff else hDiff

        val sDistance = s1 - s2
        val vDistance = v1 - v2

        return sqrt(
            (hDistance * hDistance * 0.5) +
                    (sDistance * sDistance * 0.25) +
                    (vDistance * vDistance * 0.25)
        )
    }

    /**
     * 计算HSV距离的平方
     */
    private fun calculateHsvDistanceSquared(color1: FloatArray, color2: FloatArray): Double {
        val distance = calculateHsvDistance(color1, color2)
        return distance * distance
    }

    /**
     * 计算新的HSV聚类中心
     */
    private fun calculateNewHsvCenter(pixels: List<FloatArray>): FloatArray {
        val center = FloatArray(3)
        val count = pixels.size.toFloat()

        var sSum = 0f
        var vSum = 0f

        pixels.forEach {
            sSum += it[1]
            vSum += it[2]
        }

        center[1] = sSum / count
        center[2] = vSum / count

        var sinSum = 0.0
        var cosSum = 0.0

        pixels.forEach {
            val radians = Math.toRadians(it[0].toDouble())
            sinSum += sin(radians)
            cosSum += cos(radians)
        }

        val avgSin = sinSum / count
        val avgCos = cosSum / count
        val avgHueRadians = atan2(avgSin, avgCos)

        center[0] = ((Math.toDegrees(avgHueRadians) + 360) % 360.toFloat()).toFloat()

        return center
    }

    /**
     * 检查两个HSV中心是否相等
     */
    private fun centersEqual(center1: FloatArray, center2: FloatArray): Boolean {
        return abs(center1[0] - center2[0]) < 1 &&
                abs(center1[1] - center2[1]) < 0.01 &&
                abs(center1[2] - center2[2]) < 0.01
    }

    /**
     * 将HSV颜色转换为Compose的Color对象
     */
    private fun hsvToComposeColor(hsv: FloatArray): Color {
        val rgb = AndroidColor.HSVToColor(hsv)
        return Color(
            red = AndroidColor.red(rgb) / 255f,
            green = AndroidColor.green(rgb) / 255f,
            blue = AndroidColor.blue(rgb) / 255f,
            alpha = 1f
        )
    }

    /**
     * HSV聚类数据类
     */
    private inner class HsvCluster(
        var center: FloatArray,
        val pixels: MutableList<FloatArray> = mutableListOf()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is HsvCluster) return false
            return centersEqual(center, other.center)
        }

        override fun hashCode(): Int {
            return center.contentHashCode()
        }
    }
}
