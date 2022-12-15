package com.rchugunov.dynamicpinsview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import net.coobird.thumbnailator.Thumbnails
import net.coobird.thumbnailator.name.Rename
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runners.Parameterized.Parameters
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.random.Random


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val minLocation = 59.979295 to 30.212623
        val maxLocation = 59.850753 to 30.487056
        val random = Random(0)
        val values = (0..1000).map { index ->
            val lat = random.nextDouble(maxLocation.first, minLocation.first)
            val lon = random.nextDouble(minLocation.second, maxLocation.second)
            mapOf("lat" to lat, "lon" to lon)
        }

        val str = Gson().toJson(values)
        assertEquals("", str)
    }

    @Test
    @Parameters
    fun downloadImage() {
        (0..10).forEach { groupIndex ->
            (0..100).forEach { itemIndex ->
                try {
                    doInBackground(
                        "https://tile.loc.gov/storage-services/service/pnp/ppbd/${
                            (groupIndex * 100).toString().padStart(5, '0')
                        }/${
                            ((groupIndex * 100) + itemIndex).toString().padStart(5, '0')
                        }r.jpg"
                    )
                } catch (_: Exception) {

                }
            }
        }
    }

    @Test
    fun descaleImages() {
        val f = File(".").canonicalPath
        Files.list(Path("asas")).forEach { path ->
            val bmp = resizeImage(path.toString())
            try {
                FileOutputStream("compressed/${path.fileName}").use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 75, out)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Test
    fun descaleImages2() {
        Thumbnails.of(*File("asas").listFiles())
            .size(72, 72)
            .outputFormat("jpg")
            .toFiles(File("compressed"), Rename.NO_CHANGE);
    }

    private fun doInBackground(urlStr: String) {
        var input: InputStream? = null
        var output: OutputStream? = null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            val regex = ".*(\\.\\w*)".toRegex()
            val resourceName = regex.find(urlStr)!!.groupValues[1]
            val filePath = "asas/${url.hashCode()}$resourceName"
            if (File(filePath).exists()) {
                return
            }
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                error(connection.responseMessage)
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            val fileLength = connection.contentLength

            // download the file
            input = connection.inputStream
            output = FileOutputStream(filePath)
            val data = ByteArray(4096)
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                output.write(data, 0, count)
            }
        } catch (e: Exception) {
            throw e
        } finally {
            try {
                output?.close()
                input?.close()
            } catch (ignored: IOException) {
            }
            connection?.disconnect()
        }
    }

    fun resizeImage(filePath: String): Bitmap {
        val b = BitmapFactory.decodeFile(filePath)
        return Bitmap.createScaledBitmap(b, 72, 72, false)
    }
}