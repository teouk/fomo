package io.github.teouk.fomo.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.teouk.fomo.FomoApplication
import io.github.teouk.fomo.R
import io.github.teouk.fomo.data.location.GeoPoint
import io.github.teouk.fomo.databinding.FragmentMapBinding
import io.github.teouk.fomo.databinding.SheetVenueEventsBinding
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.ui.common.EventRowAdapter
import io.github.teouk.fomo.ui.common.messageRes
import io.github.teouk.fomo.ui.details.EventDetailsActivity
import kotlinx.coroutines.launch

/**
 * Map tab: one Google Maps marker per venue with upcoming events.
 * Tapping a marker opens the event (or a list, if the venue has several).
 */
class MapFragment : Fragment() {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels { MapViewModel.Factory }

    /** Null until Google's map has loaded (getMapAsync), and again after the view is destroyed. */
    private var map: GoogleMap? = null

    /** The centre the camera was last moved to, so redraws don't fight the user's panning. */
    private var shownCentre: GeoPoint? = null

    /** Venue markers by key, so a tapped marker can be matched back to its events. */
    private var venuesByKey: Map<String, VenueMarker> = emptyMap()

    /**
     * Location is requested at the moment the map needs it, not when the app starts.
     * Whatever the answer, the map then starts: from the device position or the home town.
     */
    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) enableMyLocationLayer()
            viewModel.start()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonMapRetry.setOnClickListener { viewModel.retry() }
        binding.buttonSearchArea.setOnClickListener {
            map?.cameraPosition?.target?.let { viewModel.searchArea(GeoPoint(it.latitude, it.longitude)) }
            binding.buttonSearchArea.isVisible = false
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_container) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            if (_binding == null) return@getMapAsync
            map = googleMap
            setUpMap(googleMap)
            render(viewModel.uiState.value)
        }

        askForLocationOrStart()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun askForLocationOrStart() {
        val locationRepository = (requireActivity().application as FomoApplication).container.locationRepository
        val permission = Manifest.permission.ACCESS_COARSE_LOCATION
        when {
            locationRepository.hasPermission() -> viewModel.start()
            shouldShowRequestPermissionRationale(permission) ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.location_rationale_title)
                    .setMessage(R.string.location_rationale_body)
                    .setPositiveButton(R.string.action_allow) { _, _ -> locationPermission.launch(permission) }
                    .setNegativeButton(R.string.action_not_now) { _, _ -> viewModel.start() }
                    .setOnCancelListener { viewModel.start() }
                    .show()
            else -> locationPermission.launch(permission)
        }
    }

    private fun setUpMap(googleMap: GoogleMap) {
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = false
        enableMyLocationLayer()

        googleMap.setOnMarkerClickListener { marker ->
            venuesByKey[marker.tag as? String]?.let(::openVenue)
            true
        }
        // Offer "Search this area" only after the user moves the map themselves.
        googleMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                binding.buttonSearchArea.isVisible = true
            }
        }
    }

    /** Shows the blue "you are here" dot and button, which needs the location permission. */
    @SuppressLint("MissingPermission") // checked just below
    private fun enableMyLocationLayer() {
        val container = (requireActivity().application as FomoApplication).container
        map?.isMyLocationEnabled = container.locationRepository.hasPermission()
    }

    private fun render(state: MapUiState) {
        binding.textMapStatus.text = when (state.centreSource) {
            MapCentre.DEVICE_LOCATION -> getString(R.string.map_status_near_you)
            MapCentre.CHOSEN_AREA -> getString(R.string.map_status_near_area)
            MapCentre.HOME_TOWN, MapCentre.HOME_TOWN_OUTSIDE_UK ->
                getString(R.string.map_status_near_city, state.homeCity.displayName)
        }
        binding.textMapDetail.text = when {
            state.error != null -> getString(state.error.messageRes())
            state.isEmpty -> getString(R.string.map_empty)
            state.centreSource == MapCentre.HOME_TOWN && state.centre != null -> getString(R.string.map_location_off)
            state.centreSource == MapCentre.HOME_TOWN_OUTSIDE_UK -> getString(R.string.map_location_outside_uk)
            else -> getString(R.string.event_data_credit, state.providerName)
        }
        binding.progressMap.isVisible = state.isLoading
        binding.buttonMapRetry.isVisible = state.error != null

        val googleMap = map ?: return
        state.centre?.let { centre ->
            if (centre != shownCentre && state.centreSource != MapCentre.CHOSEN_AREA) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(centre.latitude, centre.longitude), CITY_ZOOM))
            }
            shownCentre = centre
        }

        googleMap.clear()
        venuesByKey = state.venues.associateBy { it.key }
        state.venues.forEach { venue ->
            googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(venue.latitude, venue.longitude))
                    .title(venue.name)
                    .snippet(resources.getQuantityString(R.plurals.venue_event_count, venue.events.size, venue.events.size)),
            )?.tag = venue.key
        }
        if (state.centreSource != MapCentre.CHOSEN_AREA && state.venues.size > 1 && !state.isLoading) {
            zoomToFit(googleMap, state.venues)
        }
    }

    private fun zoomToFit(googleMap: GoogleMap, venues: List<VenueMarker>) {
        val bounds = LatLngBounds.builder().apply { venues.forEach { include(LatLng(it.latitude, it.longitude)) } }.build()
        val padding = resources.getDimensionPixelSize(R.dimen.map_fit_padding)
        runCatching { googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding)) }
    }

    /** One event: go straight to its details. Several: list them in a bottom sheet. */
    private fun openVenue(venue: VenueMarker) {
        if (venue.events.size == 1) {
            openDetails(venue.events.first().event)
            return
        }
        val dialog = BottomSheetDialog(requireContext())
        val sheet = SheetVenueEventsBinding.inflate(layoutInflater)
        sheet.textVenueName.text = venue.name
        sheet.textVenueCount.text =
            resources.getQuantityString(R.plurals.venue_event_count, venue.events.size, venue.events.size)
        val adapter = EventRowAdapter(
            onEventClick = { event ->
                dialog.dismiss()
                openDetails(event)
            },
            onSaveClick = { viewModel.toggleSave(it) },
        )
        sheet.recyclerVenueEvents.adapter = adapter
        adapter.submitList(venue.events)
        dialog.setContentView(sheet.root)
        dialog.show()
    }

    private fun openDetails(event: Event) {
        startActivity(EventDetailsActivity.newIntent(requireContext(), event.id))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map = null
        shownCentre = null
        _binding = null
    }

    private companion object {
        const val CITY_ZOOM = 13f
    }
}
