package com.dimanakhratiants.picture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {


    val READ_EXTERNAL_STORAGE_PERISSION = 1
    val WRITE_EXTERNAL_STORAGE_PERISSION = 2
    val PICK_IMAGE_REQUEST_CODE = 1
    val REQUEST_IMAGE_CAPTURE = 2
    private var currentPhotoPath: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        modeMove.setOnClickListener { _ ->
            drawingView.tool = Tool.MOVE
            toolMenu.menuIconView.setImageResource(R.drawable.cursor_pointer)
            toolMenu.close(true)
        }
        modePath.setOnClickListener { _ ->
            drawingView.tool = Tool.PATH
            toolMenu.menuIconView.setImageResource(R.drawable.marker)
            toolMenu.close(true)
        }
        modeRect.setOnClickListener { _ ->
            drawingView.tool = Tool.RECT
            toolMenu.menuIconView.setImageResource(R.drawable.checkbox_blank_outline)
            toolMenu.close(true)
        }
        modeCircle.setOnClickListener { _ ->
            drawingView.tool = Tool.CIRCLE
            toolMenu.menuIconView.setImageResource(R.drawable.checkbox_blank_circle_outline)
            toolMenu.close(true)
        }

        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            val imageUri = intent.data
            imageUri?.let{
                val path = it.path
                val options = BitmapFactory.Options()
                options.inMutable = true
                val image = BitmapFactory.decodeFile(path, options)
                drawingView.drawImage(image)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId){
            R.id.actionPickImage -> {pickImage()}
            R.id.actionPickColor -> {pickColor()}
            R.id.actionPickSize -> {pickSize()}
            R.id.actionSave -> {saveImage()}
            R.id.actionTakePhoto -> { takePhoto() }
            R.id.actionShare -> {shareImage()}
        }
    return true
    }

    private fun shareImage() {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        val path = saveImage()
        if (path != null) {
            val uri = Uri.parse(path)
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/jpeg"
            startActivity(shareIntent)
        }
    }
    private fun pickSize() {
        WidthDialog.newInstance(drawingView.strokeWidth, {width -> drawingView.strokeWidth = width}).show(fragmentManager,"F")
    }

    private fun pickColor() {
        ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose color")
                .initialColor(drawingView.color)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton("ok") { dialog, selectedColor, allColors -> drawingView.color = selectedColor }
                .setNegativeButton("cancel") { dialog, which -> }
                .build()
                .show()
    }

    fun saveImage(): String? {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_EXTERNAL_STORAGE_PERISSION)
        } else {
            val name = "draw" + System.currentTimeMillis() + ".jpg"
            val image = drawingView.getImage()
            return MediaStore.Images.Media.insertImage(
                    contentResolver, image, name,
                    name)
            Toast.makeText(this,"Saved", Toast.LENGTH_LONG)
        }
        return null
    }

    override fun onBackPressed() {
        drawingView.undo()
    }

    private fun takePhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(this,"Error", Toast.LENGTH_LONG)
            }

            if (photoFile != null) {
                val photoURI = Uri.fromFile(photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun pickImage() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), READ_EXTERNAL_STORAGE_PERISSION)
        } else {
            showImagePickerActivity()
        }
    }

    private fun showImagePickerActivity() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == READ_EXTERNAL_STORAGE_PERISSION && permissions.size > 0) {
            showImagePickerActivity()
        }
        if (requestCode == WRITE_EXTERNAL_STORAGE_PERISSION && permissions.size > 0) {
            saveImage()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            // Let's read picked image data - its URI
            val pickedImage = data.data
            // Let's read picked image path using content resolver
            val filePath = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(pickedImage!!, filePath, null, null, null)
            cursor!!.moveToFirst()
            val imagePath = cursor.getString(cursor.getColumnIndex(filePath[0]))

            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bitmap = BitmapFactory.decodeFile(imagePath, options)
            drawingView.drawImage(bitmap)
            cursor.close()
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val o = BitmapFactory.Options()
            o.inMutable = true

            val imageBitmap = BitmapFactory.decodeFile(currentPhotoPath, o)

            drawingView.drawImage(imageBitmap)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )
        currentPhotoPath = image.getAbsolutePath()
        return image
    }
}
