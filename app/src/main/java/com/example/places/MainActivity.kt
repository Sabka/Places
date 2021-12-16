package com.example.places

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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


class MainActivity : AppCompatActivity()
{

    var points = mutableMapOf<Long, String>()
    lateinit var db: FirebaseFirestore
    lateinit var pointManager: PointAnnotationManager
    var last_point = Pair(17.7, 48.8) // lon, lat

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
            infoBox.visibility = View.INVISIBLE
            formLayuot.visibility = View.INVISIBLE
            true
        }

        mapboxMap.addOnMapLongClickListener()
        {
            formLayuot.visibility = View.VISIBLE
            last_point = Pair(it.longitude(), it.latitude())
            true
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
        addPlace(last_point.first, last_point.second)
        formLayuot.visibility = View.INVISIBLE
    }

    private fun addPlace(longitude: Double, latitude: Double)
    {
        val inputdata = hashMapOf("longitude" to longitude, "latitude" to latitude, "name" to nameIn.text.toString(), "remark" to remark.text.toString())

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
                            infoBox.visibility= View.VISIBLE
                            infoBox.text = "${document.data["name"] as String }, ${document.data["remark"] as String }"
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

                    var point = pointManager.create(pointOptions)
                    //Toast.makeText(this, "${point.id}",Toast.LENGTH_SHORT).show()
                    points[point.id] = document.id
                    //Toast.makeText(this, "${point.id}, ${document.id}", Toast.LENGTH_SHORT).show()

                }}
            .addOnFailureListener{ Toast.makeText(this, "data not reached", Toast.LENGTH_SHORT).show() }

        Toast.makeText(this, "places loaded",Toast.LENGTH_SHORT).show()
    }


}

