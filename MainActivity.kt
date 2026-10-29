package com.example.smarttrip

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import java.util.UUID

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: DatabaseReference

    private lateinit var etTripCode: EditText
    private lateinit var btnJoinTrip: Button
    private lateinit var btnCreateShare: Button

    private var currentTripCode: String? = null
    private val userId = UUID.randomUUID().toString().substring(0, 8)
    private val userMarkers = HashMap<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = FirebaseDatabase.getInstance().reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        etTripCode = findViewById(R.id.etTripCode)
        btnJoinTrip = findViewById(R.id.btnJoinTrip)
        btnCreateShare = findViewById(R.id.btnCreateShare)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
            ?: SupportMapFragment.newInstance().also {
                supportFragmentManager.beginTransaction().replace(R.id.map, it).commit()
            }
        mapFragment.getMapAsync(this)

        // Handle Deep Link if opened via URL (smarttrip://join?code=TRIP1234)
        handleDeepLink(intent)

        btnCreateShare.setOnClickListener {
            val code = "TRIP" + (1000..9999).random()
            currentTripCode = code
            etTripCode.setText(code)
            shareToWhatsApp(code)
            listenToTripLocations(code)
            startLocationUpdates()
        }

        btnJoinTrip.setOnClickListener {
            val code = etTripCode.text.toString().trim()
            if (code.isNotEmpty()) {
                currentTripCode = code
                listenToTripLocations(code)
                startLocationUpdates()
                Toast.makeText(this, "Joined trip: $code", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid trip code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val appLinkData: Uri? = intent?.data
        if (appLinkData != null && appLinkData.host == "join") {
            val code = appLinkData.getQueryParameter("code")
            if (!code.isNullOrEmpty()) {
                currentTripCode = code
                etTripCode.setText(code)
                listenToTripLocations(code)
                startLocationUpdates()
                Toast.makeText(this, "Auto-joined trip: $code via link", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun shareToWhatsApp(tripCode: String) {
        val shareLink = "smarttrip://join?code=$tripCode"
        val message = "Join my SmartTrip live location tracking group!\n\nTap this link to join: $shareLink"
        
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
            setPackage("com.whatsapp")
        }

        try {
            startActivity(sendIntent)
        } catch (e: Exception) {
            // Fallback to general intent chooser if WhatsApp isn't installed
            val chooserIntent = Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, message)
                type = "text/plain"
            }, "Share Trip Code")
            startActivity(chooserIntent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        } else {
            mMap.isMyLocationEnabled = true
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        currentTripCode?.let { code ->
                            val userLoc = HashMap<String, Any>()
                            userLoc["latitude"] = location.latitude
                            userLoc["longitude"] = location.longitude
                            userLoc["timestamp"] = System.currentTimeMillis()

                            database.child("trips").child(code).child("users").child(userId).setValue(userLoc)
                        }
                    }
                }
            }, mainLooper)
        }
    }

    private fun listenToTripLocations(tripCode: String) {
        database.child("trips").child(tripCode).child("users")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (userSnapshot in snapshot.children) {
                        val uid = userSnapshot.key ?: continue
                        val lat = userSnapshot.child("latitude").getValue(Double::class.java) ?: continue
                        val lng = userSnapshot.child("longitude").getValue(Double::class.java) ?: continue

                        val userPos = LatLng(lat, lng)

                        if (userMarkers.containsKey(uid)) {
                            userMarkers[uid]?.position = userPos
                        } else {
                            val markerLabel = if (uid == userId) "You" else "Member $uid"
                            val marker = mMap.addMarker(MarkerOptions().position(userPos).title(markerLabel))
                            if (marker != null) {
                                userMarkers[uid] = marker
                            }
                        }
                        
                        if (uid == userId) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userPos, 15f))
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
