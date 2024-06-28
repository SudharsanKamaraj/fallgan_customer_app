package wrteam.multivendor.customer.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.activity.MainActivity
import wrteam.multivendor.customer.adapter.AddressAdapter
import wrteam.multivendor.customer.databinding.FragmentAddressAddUpdateBinding
import wrteam.multivendor.customer.fragment.AddressListFragment.Companion.addressAdapter
import wrteam.multivendor.customer.fragment.AddressListFragment.Companion.addresses
import wrteam.multivendor.customer.fragment.AddressListFragment.Companion.recyclerView
import wrteam.multivendor.customer.helper.*
import wrteam.multivendor.customer.helper.ApiConfig.Companion.getAddress
import wrteam.multivendor.customer.model.Address
import wrteam.multivendor.customer.model.Area
import wrteam.multivendor.customer.model.City

@SuppressLint("NotifyDataSetChanged", "SetTextI18n", "ClickableViewAccessibility")
class AddressAddUpdateFragment : Fragment(),
    OnMapReadyCallback {
    lateinit var root: View
    lateinit var cityArrayList: ArrayList<City?>
    lateinit var areaArrayList: ArrayList<Area?>
    lateinit var name: String
    lateinit var mobile: String
    private lateinit var alternateMobile: String
    private lateinit var address2: String
    private lateinit var landmark: String
    lateinit var state: String
    private lateinit var country: String
    private lateinit var addressType: String
    lateinit var activity: Activity
    lateinit var For: String
    lateinit var cityAdapter: CityAdapter
    lateinit var areaAdapter: AreaAdapter
    lateinit var session: Session

    var isLoadMore = false
    private var isDefault = "0"
    var position = 0
    var total = 0
    var offset = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddressAddUpdateBinding.inflate(inflater, container, false)
        root = binding.root
        activity = requireActivity()
        session = Session(activity)
        setHasOptionsMenu(true)
        tvCurrent = root.findViewById(R.id.tvCurrent)
        pincodeId = session.getData(Constant.CITY_ID).toString()
        areaId = session.getData(Constant.AREA_ID).toString()
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val bundle = requireArguments()
        For = bundle.getString("for").toString()
        position = bundle.getInt("position")
        if (session.getData(Constant.SHIPPING_TYPE).equals("local")) {
            binding.lytLocalCityArea.visibility = View.VISIBLE
            binding.lytStandardCityArea.visibility = View.GONE
        } else {
            binding.lytLocalCityArea.visibility = View.GONE
            binding.lytStandardCityArea.visibility = View.VISIBLE
        }
        if (session.getData(Constant.SHIPPING_TYPE).equals("local")) {
            binding.edtPinCode.isEnabled = false
        }
        address1 = Address()
        if (For == "update") {
            binding.btnSubmit.text = getString(R.string.update)
            address1 = bundle.getSerializable("model") as Address
            pincodeId = address1.pincode_id
            areaId = address1.area_id
            cityId = address1.city_id
            binding.tvCity.setText(address1.city)
            binding.edtCity.setText(address1.city)
            binding.tvArea.setText(address1.area)
            binding.edtArea.setText(address1.area)
            binding.edtPinCode.setText(address1.pincode)
            latitude = address1.latitude.toDouble()
            longitude = address1.longitude.toDouble()
            tvCurrent.text = getString(R.string.location_1) + getAddress(
                latitude, longitude,
                activity
            )
            mapFragment.getMapAsync(this)
            SetData()

            binding.progressBar.visibility = View.GONE
        } else {
            binding.tvArea.isEnabled = false
            binding.progressBar.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE
            binding.btnSubmit.visibility = View.VISIBLE
            showKeyboard()
        }


        binding.edtName.requestFocus()

        mapReadyCallback = OnMapReadyCallback { googleMap: GoogleMap ->
            googleMap.clear()
            val latLng = LatLng(
                latitude,
                longitude
            )
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .title(getString(R.string.current_location))
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(18f))
        }
        binding.btnSubmit.setOnClickListener { AddUpdateAddress() }

        binding.tvUpdate.setOnClickListener {
            displayLocationSettingsRequest(activity)
        }
        binding.tvCity.setOnClickListener {
            OpenDialog(
                activity,
                "city"
            )
        }
        binding.tvArea.setOnClickListener {
            if (cityId != "0" && binding.tvArea.isEnabled) {
                OpenDialog(activity, "area")
            } else {
                binding.tvArea.error = getString(R.string.select_city_first)
            }
        }
        binding.chIsDefault.setOnClickListener {
            isDefault = if (isDefault.equals("0", ignoreCase = true)) {
                "1"
            } else {
                "0"
            }
        }
        return root
    }

    private fun SetData() {
        name = address1.name
        mobile = address1.mobile
        address2 = address1.address
        alternateMobile = address1.alternate_mobile
        landmark = address1.landmark
        state = address1.state
        country = address1.country
        isDefault = address1.is_default
        addressType = address1.type
        binding.progressBar.visibility = View.VISIBLE
        binding.edtName.setText(name)
        binding.edtMobile.setText(mobile)
        binding.edtAlternateMobile.setText(alternateMobile)
        binding.edtAddress.setText(address2)
        binding.edtLandmark.setText(landmark)
        binding.edtState.setText(state)
        binding.edtCountry.setText(country)
        binding.chIsDefault.isChecked = isDefault.equals("1", ignoreCase = true)
        if (addressType.equals("home", ignoreCase = true)) {
            binding.rdHome.isChecked = true
        } else if (addressType.equals("office", ignoreCase = true)) {
            binding.rdOffice.isChecked = true
        } else {
            binding.rdOther.isChecked = true
        }
        binding.progressBar.visibility = View.GONE
        binding.btnSubmit.visibility = View.VISIBLE
        showKeyboard()
        binding.edtName.requestFocus()
    }

    private fun AddUpdateAddress() {
        val isDefault = if (binding.chIsDefault.isChecked) "1" else "0"
        val type =
            if (binding.rdHome.isChecked) "Home" else if (binding.rdOffice.isChecked) "Office" else "Other"

        when {
            binding.edtName.text.toString().trim().isEmpty() -> {
                binding.edtName.requestFocus()
                binding.edtName.error = "Please enter name!"
            }
            binding.edtMobile.text.toString().trim().isEmpty() -> {
                binding.edtMobile.requestFocus()
                binding.edtMobile.error = "Please enter mobile!"
            }
            binding.edtAddress.text.toString().trim().isEmpty() -> {
                binding.edtAddress.requestFocus()
                binding.edtAddress.error = "Please enter address!"
            }
            binding.edtLandmark.text.toString().trim().isEmpty() -> {
                binding.edtLandmark.requestFocus()
                binding.edtLandmark.error = "Please enter landmark!"
            }
            binding.edtState.text.toString().trim().isEmpty() -> {
                binding.edtState.requestFocus()
                binding.edtState.error = "Please enter state!"
            }
            binding.edtCity.text.toString().trim()
                .isEmpty() -> {
                binding.edtCity.requestFocus()
                binding.edtCity.error = "Please enter city!"
            }
            binding.edtArea.text.toString().trim()
                .isEmpty() -> {
                binding.edtArea.requestFocus()
                binding.edtArea.error = "Please enter area!"
            }
            binding.edtCountry.text.toString().trim().isEmpty() -> {
                binding.edtCountry.requestFocus()
                binding.edtCountry.error = "Please enter country"
            }
            else -> {
                val params: MutableMap<String, String> = HashMap()
                if (For.equals("add", ignoreCase = true)) {
                    params[Constant.ADD_ADDRESS] = Constant.GetVal
                } else if (For.equals("update", ignoreCase = true)) {
                    params[Constant.UPDATE_ADDRESS] = Constant.GetVal
                    params[Constant.ID] = address1.id
                }
                params[Constant.USER_ID] = session.getData(Constant.ID).toString()
                params[Constant.TYPE] = type
                params[Constant.NAME] = binding.edtName.text.toString().trim()
                params[Constant.MOBILE] = binding.edtMobile.text.toString().trim()
                params[Constant.ADDRESS] = binding.edtAddress.text.toString().trim()
                params[Constant.LANDMARK] = binding.edtLandmark.text.toString().trim()
                if (session.getData(Constant.SHIPPING_TYPE).equals("local")) {
                    params[Constant.AREA_ID] = if (areaId == "") "0" else areaId
                    params[Constant.CITY_ID] = if (cityId == "") "0" else cityId
                    params[Constant.PINCODE_ID] = if (pincodeId == "") "0" else pincodeId
                } else {
                    params[Constant.AREA_NAME] =
                        binding.edtArea.text.toString().trim()
                    params[Constant.CITY_NAME] =
                        binding.edtCity.text.toString().trim()
                    params[Constant.PINCODE] =
                        binding.edtPinCode.text.toString().trim()
                }
                params[Constant.STATE] = binding.edtState.text.toString().trim()
                params[Constant.COUNTRY] = binding.edtCountry.text.toString().trim()
                params[Constant.ALTERNATE_MOBILE] =
                    binding.edtAlternateMobile.text.toString().trim()
                params[Constant.COUNTRY_CODE] = session.getData(Constant.COUNTRY_CODE).toString()
                try {
                    params[Constant.LONGITUDE] = address1.longitude
                    params[Constant.LATITUDE] = address1.latitude
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                params[Constant.IS_DEFAULT] = isDefault
                ApiConfig.requestToVolley(object : VolleyCallback {
                    override fun onSuccess(result: Boolean, response: String) {
                        if (result) {
                            try {
                                val msg: String
                                val jsonObject = JSONObject(response)
                                if (!jsonObject.getBoolean(Constant.ERROR)) {
                                    try {
                                        val g = Gson()
                                        val address: Address = g.fromJson(
                                            jsonObject.getJSONObject(Constant.DATA).toString(),
                                            Address::class.java
                                        )
                                        if (address.is_default == "1" && addresses.size > 0
                                        ) {
                                            for (i in 0 until addresses.size) {
                                                addresses[i].is_default = "0"
                                            }
                                        }

                                        AddressListFragment.tvAlert.visibility = View.GONE
                                        if (address.is_default == "1") {
                                            for (i in 0 until addressAdapter.itemCount) {
                                                addresses[i].is_default = "0"
                                            }

                                            if (For.equals("add", ignoreCase = true)) {
                                                if (addresses.size > 0) {
                                                    addresses[0].is_default = "1"
                                                }
                                                Constant.selectedAddressId = address.id
                                            } else if (For.equals("update", ignoreCase = true)) {
                                                addresses[position].is_default = "1"
                                                Constant.selectedAddressId =
                                                    addresses[position].id
                                            }
                                        }

                                        if (For.equals("add", ignoreCase = true)) {
                                            msg = "Address added."
                                            addresses.add(address)
                                        } else {
                                            addresses[position] = address
                                            msg = "Address updated."
                                        }

                                        addressAdapter = AddressAdapter(
                                            activity,
                                            addresses,
                                            R.layout.lyt_address_list
                                        )

                                        recyclerView.adapter = addressAdapter

                                        if (addresses.size > 0) {
                                            AddressListFragment.tvAlert.visibility = View.GONE
                                        } else {
                                            AddressListFragment.tvAlert.visibility = View.VISIBLE
                                        }


                                        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()

                                        MainActivity.fm.popBackStack()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }, activity, Constant.GET_ADDRESS_URL, params, true)
            }
        }
    }

    @Deprecated("")
    fun displayLocationSettingsRequest(activity: Activity) {

        val googleApiClient = GoogleApiClient.Builder(activity).addApi(LocationServices.API).build()
        googleApiClient.connect()
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 1000
        locationRequest.fastestInterval = (1000 / 2).toLong()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result =
            LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
        result.setResultCallback { result1: LocationSettingsResult ->
            val status = result1.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> if (ContextCompat.checkSelfPermission(
                        activity, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        activity, Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ), 110
                    )
                } else {
                    GPSTracker(activity)

                    Thread.sleep(500)

                    val fragment: Fragment = MapFragment()
                    val bundle1 = Bundle()
                    bundle1.putString(Constant.FROM, "address")
                    bundle1.putDouble("latitude", latitude)
                    bundle1.putDouble("longitude", longitude)
                    fragment.arguments = bundle1
                    MainActivity.fm.beginTransaction().add(R.id.container, fragment)
                        .addToBackStack(null).commit()
                }
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    status.startResolutionForResult(activity, 110)
                } catch (e: SendIntentException) {
                    Log.i("TAG", "PendingIntent unable to execute request.")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = requireActivity().getString(R.string.address)
        activity.invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        closeKeyboard()
    }

    private fun showKeyboard() {
        val inputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    private fun closeKeyboard() {
        val inputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val saveLatitude: Double
        val saveLongitude: Double
        if (For == "update") {
            binding.btnSubmit.text = getString(R.string.update)

            address1 = arguments?.getSerializable("model") as Address
            pincodeId = address1.pincode_id
            areaId = address1.area_id
            latitude = address1.latitude.toDouble()
            longitude = address1.longitude.toDouble()
        }
        if (latitude <= 0 || longitude <= 0) {
            saveLatitude = session.getCoordinates(Constant.LATITUDE).toString().toDouble()
            saveLongitude = session.getCoordinates(Constant.LONGITUDE).toString().toDouble()
        } else {
            saveLatitude = latitude
            saveLongitude = longitude
        }
        googleMap.clear()
        val latLng = LatLng(saveLatitude, saveLongitude)
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .draggable(true)
                .title(getString(R.string.current_location))
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(18f))
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.toolbar_cart).isVisible = false
        menu.findItem(R.id.toolbar_layout).isVisible = false
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = false
    }

    private fun OpenDialog(activity: Activity?, from: String) {
        val alertDialog = AlertDialog.Builder(requireActivity())
        val inflater1 =
            requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView: View = inflater1.inflate(R.layout.dialog_city_area_selection, null)
        alertDialog.setView(dialogView)
        alertDialog.setCancelable(true)
        val dialog = alertDialog.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val scrollView: NestedScrollView = dialogView.findViewById(R.id.scrollView)
        val tvSearch: TextView = dialogView.findViewById(R.id.tvSearch)
        val tvAlert: TextView = dialogView.findViewById(R.id.tvAlert)
        val searchView: EditText = dialogView.findViewById(R.id.searchView)
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.recyclerView)
        val linearLayoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = linearLayoutManager
        searchView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_search,
            0,
            R.drawable.ic_close_,
            0
        )
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (searchView.text.toString().trim().isNotEmpty()) {
                    searchView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_search,
                        0,
                        R.drawable.ic_close,
                        0
                    )
                } else {
                    searchView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_search,
                        0,
                        R.drawable.ic_close_,
                        0
                    )
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        if (from == "city") {
            tvAlert.text = getString(R.string.no_cities_found)
            GetCityData("", recyclerView, tvAlert, linearLayoutManager, scrollView, dialog)
            tvSearch.setOnClickListener {
                GetCityData(
                    searchView.text.toString().trim(),
                    recyclerView,
                    tvAlert,
                    linearLayoutManager,
                    scrollView,
                    dialog
                )
            }
            searchView.setOnTouchListener { v: View?, event: MotionEvent ->
                val DRAWABLE_RIGHT = 2
                if (event.action == MotionEvent.ACTION_UP) {
                    if (searchView.text.toString().trim().isNotEmpty()) {
                        if (event.rawX >= searchView.right - searchView.compoundDrawables[DRAWABLE_RIGHT].bounds.width()
                        ) {
                            searchView.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_search,
                                0,
                                R.drawable.ic_close_,
                                0
                            )
                            searchView.setText("")
                            GetCityData(
                                "",
                                recyclerView,
                                tvAlert,
                                linearLayoutManager,
                                scrollView,
                                dialog
                            )
                        }
                        return@setOnTouchListener true
                    }
                }
                false
            }
        } else {
            tvAlert.text = getString(R.string.no_areas_found)
            GetAreaData("", recyclerView, tvAlert, linearLayoutManager, scrollView, dialog)
            tvSearch.setOnClickListener {
                GetAreaData(
                    searchView.text.toString().trim(),
                    recyclerView,
                    tvAlert,
                    linearLayoutManager,
                    scrollView,
                    dialog
                )
            }
            searchView.setOnTouchListener { _: View?, event: MotionEvent ->
                val DRAWABLE_RIGHT = 2
                if (event.action == MotionEvent.ACTION_UP) {
                    if (searchView.text.toString().trim().isNotEmpty()) {
                        if (event.rawX >= searchView.right - searchView.compoundDrawables[DRAWABLE_RIGHT].bounds.width()
                        ) {
                            searchView.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_search,
                                0,
                                R.drawable.ic_close_,
                                0
                            )
                            searchView.setText("")
                            GetAreaData(
                                "",
                                recyclerView,
                                tvAlert,
                                linearLayoutManager,
                                scrollView,
                                dialog
                            )
                        }
                        return@setOnTouchListener true
                    }
                }
                false
            }
        }
        dialog.show()
    }

    private fun GetCityData(
        search: String,
        recyclerView: RecyclerView,
        tvAlert: TextView,
        linearLayoutManager: LinearLayoutManager,
        scrollView: NestedScrollView,
        dialog: AlertDialog
    ) {
        cityArrayList = ArrayList()
        binding.progressBar.visibility = View.VISIBLE
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_CITIES] = Constant.GetVal
        params[Constant.SEARCH] = search
        params[Constant.OFFSET] = "" + offset
        params[Constant.LIMIT] = "" + (Constant.LOAD_ITEM_LIMIT + 20)
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                Log.i("CATEGORY_RES", response)
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            try {
                                total = jsonObject.getString(Constant.TOTAL).toInt()
                                val `object` = JSONObject(response)
                                val jsonArray: JSONArray = `object`.getJSONArray(Constant.DATA)
                                val g = Gson()
                                for (i in 0 until jsonArray.length()) {
                                    val jsonObject1 = jsonArray.getJSONObject(i)
                                    val city = g.fromJson(jsonObject1.toString(), City::class.java)
                                    cityArrayList.add(city)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (offset == 0) {
                                binding.progressBar.visibility = View.GONE
                                tvAlert.visibility = View.GONE
                                cityAdapter = CityAdapter(
                                    activity,
                                    cityArrayList, dialog
                                )
                                cityAdapter.setHasStableIds(true)
                                recyclerView.adapter = cityAdapter
                                scrollView.setOnScrollChangeListener { v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->

                                    // if (diff == 0) {
                                    if (scrollY == v.getChildAt(0)
                                            .measuredHeight - v.measuredHeight
                                    ) {
                                        if (cityArrayList.size < total) {
                                            if (!isLoadMore) {
                                                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == cityArrayList.size - 1) {
                                                    //bottom of list!
                                                    cityArrayList.add(null)
                                                    cityAdapter.notifyItemInserted(cityArrayList.size - 1)
                                                    offset += Constant.LOAD_ITEM_LIMIT + 20
                                                    val params1: MutableMap<String, String> =
                                                        HashMap()
                                                    params1[Constant.GET_CITIES] =
                                                        Constant.GetVal
                                                    params1[Constant.SEARCH] =
                                                        search
                                                    params1[Constant.OFFSET] = "" + offset
                                                    params1[Constant.LIMIT] =
                                                        "" + (Constant.LOAD_ITEM_LIMIT + 20)
                                                    ApiConfig.requestToVolley(
                                                        object : VolleyCallback {
                                                            override fun onSuccess(
                                                                result: Boolean,
                                                                response: String
                                                            ) {
                                                                if (result) {
                                                                    try {
                                                                        val jsonObject1 =
                                                                            JSONObject(response)
                                                                        if (!jsonObject1.getBoolean(
                                                                                Constant.ERROR
                                                                            )
                                                                        ) {
                                                                            cityArrayList.removeAt(
                                                                                cityArrayList.size - 1
                                                                            )
                                                                            cityAdapter.notifyItemRemoved(
                                                                                cityArrayList.size
                                                                            )
                                                                            val `object` =
                                                                                JSONObject(response)
                                                                            val jsonArray: JSONArray =
                                                                                `object`.getJSONArray(
                                                                                    Constant.DATA
                                                                                )
                                                                            val g = Gson()
                                                                            for (i in 0 until jsonArray.length()) {
                                                                                val jsonObject2 =
                                                                                    jsonArray.getJSONObject(
                                                                                        i
                                                                                    )
                                                                                val city =
                                                                                    g.fromJson(
                                                                                        jsonObject2.toString(),
                                                                                        City::class.java
                                                                                    )
                                                                                cityArrayList.add(
                                                                                    city
                                                                                )
                                                                            }
                                                                            cityAdapter.notifyDataSetChanged()
                                                                            cityAdapter.setLoaded()
                                                                            isLoadMore = false
                                                                        }
                                                                    } catch (e: JSONException) {
                                                                        e.printStackTrace()
                                                                        e.printStackTrace()
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        activity,
                                                        Constant.GET_LOCATIONS_URL,
                                                        params1,
                                                        false
                                                    )
                                                }
                                                isLoadMore = true
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            binding.progressBar.visibility = View.GONE
                            tvAlert.visibility = View.VISIBLE
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.progressBar.visibility = View.GONE
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.GET_LOCATIONS_URL, params, false)
    }

    private fun GetAreaData(
        search: String,
        recyclerView: RecyclerView,
        tvAlert: TextView,
        linearLayoutManager: LinearLayoutManager,
        scrollView: NestedScrollView,
        dialog: AlertDialog
    ) {
        areaArrayList = ArrayList()
        binding.progressBar.visibility = View.VISIBLE
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_AREAS] = Constant.GetVal
        params[Constant.CITY_ID] = cityId
        params[Constant.SEARCH] = search
        params[Constant.OFFSET] = "" + offset
        params[Constant.LIMIT] = "" + (Constant.LOAD_ITEM_LIMIT + 20)
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            try {
                                total = jsonObject.getString(Constant.TOTAL).toInt()
                                val `object` = JSONObject(response)
                                val jsonArray: JSONArray = `object`.getJSONArray(Constant.DATA)
                                val g = Gson()
                                for (i in 0 until jsonArray.length()) {
                                    val jsonObject1 = jsonArray.getJSONObject(i)
                                    val area = g.fromJson(jsonObject1.toString(), Area::class.java)
                                    areaArrayList.add(area)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (offset == 0) {
                                binding.progressBar.visibility = View.GONE
                                tvAlert.visibility = View.GONE
                                areaAdapter = AreaAdapter(
                                    activity,
                                    areaArrayList, dialog
                                )
                                areaAdapter.setHasStableIds(true)
                                recyclerView.adapter = areaAdapter
                                scrollView.setOnScrollChangeListener { v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->

                                    // if (diff == 0) {
                                    if (scrollY == v.getChildAt(0)
                                            .measuredHeight - v.measuredHeight
                                    ) {
                                        if (areaArrayList.size < total) {
                                            if (!isLoadMore) {
                                                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == areaArrayList.size - 1) {
                                                    //bottom of list!
                                                    areaArrayList.add(null)
                                                    areaAdapter.notifyItemInserted(areaArrayList.size - 1)
                                                    offset += Constant.LOAD_ITEM_LIMIT + 20
                                                    val params1: MutableMap<String, String> =
                                                        HashMap()
                                                    params1[Constant.GET_AREAS] =
                                                        Constant.GetVal
                                                    params1[Constant.CITY_ID] =
                                                        cityId
                                                    params1[Constant.SEARCH] =
                                                        search
                                                    params1[Constant.OFFSET] = "" + offset
                                                    params1[Constant.LIMIT] =
                                                        "" + (Constant.LOAD_ITEM_LIMIT + 20)
                                                    ApiConfig.requestToVolley(
                                                        object : VolleyCallback {
                                                            override fun onSuccess(
                                                                result: Boolean,
                                                                response: String
                                                            ) {
                                                                if (result) {
                                                                    try {
                                                                        val jsonObject1 =
                                                                            JSONObject(response)
                                                                        if (!jsonObject1.getBoolean(
                                                                                Constant.ERROR
                                                                            )
                                                                        ) {
                                                                            areaArrayList.removeAt(
                                                                                areaArrayList.size - 1
                                                                            )
                                                                            areaAdapter.notifyItemRemoved(
                                                                                areaArrayList.size
                                                                            )
                                                                            val `object` =
                                                                                JSONObject(response)
                                                                            val jsonArray: JSONArray =
                                                                                `object`.getJSONArray(
                                                                                    Constant.DATA
                                                                                )
                                                                            val g = Gson()
                                                                            for (i in 0 until jsonArray.length()) {
                                                                                val jsonObject2 =
                                                                                    jsonArray.getJSONObject(
                                                                                        i
                                                                                    )
                                                                                val area =
                                                                                    g.fromJson(
                                                                                        jsonObject2.toString(),
                                                                                        Area::class.java
                                                                                    )
                                                                                areaArrayList.add(
                                                                                    area
                                                                                )
                                                                            }
                                                                            areaAdapter.notifyDataSetChanged()
                                                                            areaAdapter.setLoaded()
                                                                            isLoadMore = false
                                                                        }
                                                                    } catch (e: JSONException) {
                                                                        e.printStackTrace()
                                                                        e.printStackTrace()
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        activity,
                                                        Constant.GET_LOCATIONS_URL,
                                                        params1,
                                                        false
                                                    )
                                                }
                                                isLoadMore = true
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            binding.progressBar.visibility = View.GONE
                            tvAlert.visibility = View.VISIBLE
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.progressBar.visibility = View.GONE
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.GET_LOCATIONS_URL, params, false)
    }

    class CityAdapter(
        val activity: Activity,
        private val cities: ArrayList<City?>,
        val dialog: AlertDialog
    ) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        // for load more
        private val VIEW_TYPE_ITEM = 0
        private val VIEW_TYPE_LOADING = 1
        var isLoading = false
        val session: Session = Session(activity)
        fun add(position: Int, city: City?) {
            cities.add(position, city)
            notifyItemInserted(position)
        }

        fun setLoaded() {
            isLoading = false
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view: View
            return when (viewType) {
                VIEW_TYPE_ITEM -> {
                    view = LayoutInflater.from(activity)
                        .inflate(R.layout.lyt_pin_code_list, parent, false)
                    HolderItems(view)
                }
                VIEW_TYPE_LOADING -> {
                    view = LayoutInflater.from(activity)
                        .inflate(R.layout.item_progressbar, parent, false)
                    ViewHolderLoading(view)
                }
                else -> throw IllegalArgumentException("unexpected viewType: $viewType")
            }
        }

        override fun onBindViewHolder(holderParent: RecyclerView.ViewHolder, position: Int) {
            if (holderParent is HolderItems) {
                try {
                    val city = cities[position]
                    holderParent.tvPinCode.text = city!!.city_name

                    holderParent.tvPinCode.setOnClickListener {
                        binding.tvCity.setText(city.city_name)
                        binding.edtCity.setText(city.city_name)
                        cityId = city.id
                        areaId = "0"
                        binding.tvArea.text.clear()
                        binding.tvArea.isEnabled = true
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (holderParent is ViewHolderLoading) {
                holderParent.progressBar.isIndeterminate = true
            }
        }

        override fun getItemCount(): Int {
            return cities.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (cities[position] == null) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        internal class ViewHolderLoading(view: View) :
            RecyclerView.ViewHolder(view) {
            val progressBar: ProgressBar = view.findViewById(R.id.itemProgressbar)

        }

        internal class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvPinCode: TextView = itemView.findViewById(R.id.tvPinCode)

        }

    }

    class AreaAdapter(
        val activity: Activity?,
        private val areas: ArrayList<Area?>,
        val dialog: AlertDialog
    ) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        // for load more
        private val VIEW_TYPE_ITEM = 0
        private val VIEW_TYPE_LOADING = 1
        var isLoading = false
        val session: Session = Session(activity!!)
        fun add(position: Int, area: Area?) {
            areas.add(position, area)
            notifyItemInserted(position)
        }

        fun setLoaded() {
            isLoading = false
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view: View
            return when (viewType) {
                VIEW_TYPE_ITEM -> {
                    view = LayoutInflater.from(activity)
                        .inflate(R.layout.lyt_pin_code_list, parent, false)
                    HolderItems(view)
                }
                VIEW_TYPE_LOADING -> {
                    view = LayoutInflater.from(activity)
                        .inflate(R.layout.item_progressbar, parent, false)
                    ViewHolderLoading(view)
                }
                else -> throw IllegalArgumentException("unexpected viewType: $viewType")
            }
        }

        override fun onBindViewHolder(holderParent: RecyclerView.ViewHolder, position: Int) {
            if (holderParent is HolderItems) {
                try {
                    val area = areas[position]
                    holderParent.tvPinCode.text = area!!.name
                    holderParent.tvPinCode.setOnClickListener {
                        binding.tvArea.setText(area.name)
                        binding.edtArea.setText(area.name)
                        areaId = area.id
                        pincodeId = area.pincode_id
                        binding.edtPinCode.setText(area.pincode)
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (holderParent is ViewHolderLoading) {
                holderParent.progressBar.isIndeterminate = true
            }
        }

        override fun getItemCount(): Int {
            return areas.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (areas[position] == null) VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        internal class ViewHolderLoading(view: View) :
            RecyclerView.ViewHolder(view) {
            val progressBar: ProgressBar = view.findViewById(R.id.itemProgressbar)

        }

        internal class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvPinCode: TextView = itemView.findViewById(R.id.tvPinCode)

        }

    }

    @SuppressLint("StaticFieldLeak")
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var tvCurrent: TextView
        lateinit var binding: FragmentAddressAddUpdateBinding
        var latitude = 0.00
        var longitude = 0.00
        lateinit var address1: Address
        lateinit var mapFragment: SupportMapFragment
        lateinit var mapReadyCallback: OnMapReadyCallback
        var pincodeId = "0"
        var areaId = "0"
        var cityId = "0"
    }
}