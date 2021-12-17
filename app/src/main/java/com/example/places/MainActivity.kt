package com.example.places

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity()
{

    private var points = mutableMapOf<Long, String>()
    private lateinit var db: FirebaseFirestore
    private lateinit var pointManager: PointAnnotationManager
    private var lastPoint = Pair(17.7, 48.8) // lon, lat
    private var PICK_IMAGE = 1
    lateinit var storageRef: StorageReference
    lateinit var chosenPhoto: Uri

    override fun onCreate(savedInstanceState: Bundle?)
    {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapView = findViewById<MapView>(R.id.mapView)
        val mapboxMap = mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS)


        db = Firebase.firestore

        val annotationPlugin = mapView.annotations
        pointManager = annotationPlugin.createPointAnnotationManager(mapView)
        pointManager.addClickListener {
                    //Toast.makeText(this, "${points[it.id]}",Toast.LENGTH_SHORT).show()
                    //it.iconSize = 2.0
                    //pointManager.update(it)
                    displayPlace(points[it.id])
                    true
        }

        pointManager.addLongClickListener()
        {
            deletePlace(points[it.id])
            pointManager.delete(it)
            true
        }

        loadAllPlaces()

        mapboxMap.addOnMapClickListener()
        {
            //Toast.makeText(this, "map clicked", Toast.LENGTH_SHORT).show()
            infoLayout.visibility = View.INVISIBLE
            formLayuot.visibility = View.INVISIBLE
            true
        }

        mapboxMap.addOnMapLongClickListener()
        {
            formLayuot.visibility = View.VISIBLE
            lastPoint = Pair(it.longitude(), it.latitude())
            true
        }

        val storage = Firebase.storage
        storageRef = storage.reference

    }

    fun loadPhoto(name : String)
    {
        val localFile = File.createTempFile("images", "tmp.jpg")

        val dispPhoto = storageRef.child("images/${name}")

        dispPhoto.getFile(localFile).addOnSuccessListener {
            val drawable = Drawable.createFromPath(localFile.path)
            photo.setImageDrawable(drawable)
            //Toast.makeText(this, "succes: ", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            //Toast.makeText(this, "error: $it", Toast.LENGTH_SHORT).show()
        }
    }

    fun pickImage(view: View)
    {
        val intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, "Choose picture"), PICK_IMAGE)
    }

    fun uploadImage()
    {
        val selectedImageUri = chosenPhoto
        val riversRef = storageRef.child("images/${selectedImageUri?.lastPathSegment}")
        val uploadTask = selectedImageUri?.let { riversRef.putFile(it) }

        if (uploadTask != null)
        {
            uploadTask.addOnFailureListener {
                // Handle unsuccessful uploads
                Toast.makeText(this, "error: $it", Toast.LENGTH_LONG).show()
            }.addOnSuccessListener { taskSnapshot ->
                // taskSnapshot.metadata contains file metadata such as size, content-type, etc.

                Toast.makeText(this, "succes: ", Toast.LENGTH_SHORT).show()
                // ...
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE)
        {
            Toast.makeText(this, "picture picked, ${data.toString()}", Toast.LENGTH_SHORT).show()
            if (data != null) {
                if(data.getData() != null)
                {
                    chosenPhoto = data.getData()!!
                }
            }
        }
    }

    private fun deletePlace(id: String?)
    {
        if (id != null) {
            db.collection("places").document(id)
                .delete()
                .addOnSuccessListener {  Toast.makeText(this, "place deletion succesful :)", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { Toast.makeText(this, "place deletion unsuccesful :(", Toast.LENGTH_SHORT).show() }
        }

        loadAllPlaces()

    }

    fun onSaveBtnClicked(view : View)
    {
        addPlace(lastPoint.first, lastPoint.second)
        formLayuot.visibility = View.INVISIBLE
    }

    private fun addPlace(longitude: Double, latitude: Double)
    {
        uploadImage()

        val inputdata = hashMapOf(
            "longitude" to longitude,
            "latitude" to latitude,
            "name" to nameIn.text.toString(),
            "remark" to remark.text.toString(),
            "date" to date.text.toString(),
            "photo_name" to chosenPhoto.lastPathSegment)

        db.collection("places")
            .add(inputdata)
            .addOnSuccessListener { Toast.makeText(this, "place added :)", Toast.LENGTH_SHORT).show()
                                    loadAllPlaces()}
            .addOnFailureListener{  Toast.makeText(this, "failed to add place :(", Toast.LENGTH_SHORT).show() }

    }

    private fun displayPlace(id: String?)
    {
        db.collection("places")
            .get()
            .addOnSuccessListener{ data ->

                if(data.isEmpty)
                {
                    Toast.makeText(this, "place not found", Toast.LENGTH_SHORT).show()
                }
                else
                {
                    for(document in data)
                    {
                        if(document.id == id)
                        {
                            formLayuot.visibility = View.INVISIBLE
                            infoLayout.visibility= View.VISIBLE
                            infoBox.text = "${document.data["name"] as String } \n${document.data["remark"] as String } \n${document.data["date"].toString()}"
                            if(document.data["photo_name"] != null) loadPhoto(document.data["photo_name"] as String)
                        }
                    }
                }
            }
            .addOnFailureListener{ Toast.makeText(this, "places with set id not found", Toast.LENGTH_SHORT).show() }
    }

    private fun loadAllPlaces()
    {
        val icon = (resources.getDrawable(R.drawable.red_marker) as BitmapDrawable).bitmap

        db.collection("places")
            .get()
            .addOnSuccessListener{data ->
                for(document in data)
                {
                    //Toast.makeText(this, "${document.data["longitude"]}", Toast.LENGTH_SHORT).show()

                    val pointOptions = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(
                            document.data["longitude"] as Double,
                            document.data["latitude"] as Double
                        ))
                        .withIconImage(icon)

                    val point = pointManager.create(pointOptions)
                    //Toast.makeText(this, "${point.id}",Toast.LENGTH_SHORT).show()
                    points[point.id] = document.id
                    //Toast.makeText(this, "${point.id}, ${document.id}", Toast.LENGTH_SHORT).show()

                }}
            .addOnFailureListener{ Toast.makeText(this, "data not reached", Toast.LENGTH_SHORT).show() }

        Toast.makeText(this, "places loaded",Toast.LENGTH_SHORT).show()
    }


}

