package ru.dev.coroutinessample

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.net.URL

class MainActivity : AppCompatActivity() {
    //Описать разницу между SupervisorJob и Job
    private val parentJob = SupervisorJob()

    //Описать для чего нужен и как работет
    private val coroutineExceptionHandler: CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->

            //Обращение к основному UI потоку
            coroutineScope.launch(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "$throwable", Toast.LENGTH_LONG).show()
            }

            //Обращение к потку ввода/выводв
            coroutineScope.launch(Dispatchers.IO) {

            }
            //Обращение к default thread
            coroutineScope.launch(Dispatchers.Default) {

            }
            //Выполнение в глобальном скоупе(так делать нельзя!)
            GlobalScope.launch {
                println("Caught $throwable")
            }

        }

    //Описать зачем нужно объявлять scope
    private val coroutineScope = CoroutineScope(
        Dispatchers.Main + parentJob + coroutineExceptionHandler
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val path = this@MainActivity.getString(R.string.picture_url_path)

        buttonLoadPicture.setOnClickListener {
            imageViewContainer.setImageBitmap(null)
            //Описать процесс переключения между потоками и их диспечерами
            coroutineScope.launch(Dispatchers.Main) {
                val originalBitmap = getRemoteBitmap(path)
                imageViewContainer.setImageBitmap(originalBitmap)
            }
        }

    }


    private suspend fun getRemoteBitmap(path: String): Bitmap {
        withContext(Dispatchers.Main) {
            buttonLoadPicture.isEnabled = false
        }

        val image = withContext(Dispatchers.IO) {
            URL(path).openStream().use {
                return@withContext BitmapFactory.decodeStream(it)
            }
        }

        withContext(Dispatchers.Main) {
            buttonLoadPicture.isEnabled = true
        }

        return image
    }

    override fun onDestroy() {
        super.onDestroy()

        //Описать почему нужно закрыать детей работы и почему ответсвенность
        //за выпоение работы лежит на супервизоре
        parentJob.cancelChildren()
    }

}
