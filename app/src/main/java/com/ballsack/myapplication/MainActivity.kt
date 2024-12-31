package com.ballsack.myapplication

import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.ballsack.myapplication.ui.theme.MyApplicationTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.IOException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        //enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NavigationApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationApp() {
    var searchText by remember { mutableStateOf("") }
    val mapView = rememberMapViewWithLifecycle()
    var markers by remember { mutableStateOf(mutableListOf<GeoPoint>()) }
    var distance by remember { mutableStateOf("") }
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dio Cane") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AndroidView({ mapView }, modifier = Modifier.fillMaxSize()) { mapView ->
                    mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(10.0)
                        controller.setCenter(GeoPoint(40.7128, -74.0060)) // New York
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Divider(
                        modifier = Modifier
                            .width(20.dp)
                            .height(1.dp),
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                    Divider(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp),
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                }
                Button(
                    onClick = {
                        val mapCenter = mapView.mapCenter
                        val geoPoint = GeoPoint(mapCenter.latitude, mapCenter.longitude)
                        markers.add(geoPoint)
                        val marker = Marker(mapView)
                        marker.position = geoPoint
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        mapView.overlays.add(marker)
                        mapView.invalidate()

                        if (markers.size == 2) {
                            val distanceInMeters = calculateDistance(markers[0], markers[1])
                            val distanceInKilometers = (distanceInMeters / 1000).roundToInt()
                            distance = "$distanceInKilometers km"
                        }
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text("Add Marker")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search Location") },
                leadingIcon = {
                    IconButton(onClick = {
                        if (searchText.isNotBlank()) {
                            try {
                                val addresses = geocoder.getFromLocationName(searchText, 1)
                                if (addresses != null && addresses.isNotEmpty()) {
                                    val address = addresses[0]
                                    val geoPoint = GeoPoint(address.latitude, address.longitude)
                                    mapView.controller.animateTo(geoPoint)
                                } else {
                                    Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: IOException) {
                                Toast.makeText(context, "Geocoding failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            //Text("Additional UI elements can be added here")
            if (distance.isNotEmpty()) {
                Text("Distance: $distance")
            }
        }
    }
}

fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
    val earthRadius = 6371000.0 // Earth radius in meters

    val lat1 = Math.toRadians(point1.latitude)
    val lon1 = Math.toRadians(point1.longitude)
    val lat2 = Math.toRadians(point2.latitude)
    val lon2 = Math.toRadians(point2.longitude)

    val dLat = lat2 - lat1
    val dLon = lon2 - lon1

    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember {
        MapView(context).apply {
            id = R.id.map_view_id
        }
    }
}