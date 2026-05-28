package net.crowdventures.storypop.map.editor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.crowdventures.storypop.map.models.*
import java.util.UUID

/**
 * MapEditorViewModel
 *
 * Single source of truth for the editor.  Handles:
 *   - Full element list (incident / city / ordinary markers + paths)
 *   - Undo / redo via an EditorCommand stack
 *   - GeoJSON serialization for storage / API submission
 *   - Active tool selection
 *   - Active element selection (for drag / edit)
 */
class MapEditorViewModel : ViewModel() {

    // ── Active tool ───────────────────────────────────────────────

    enum class Tool {
        SELECT,         // tap to select, drag to move
        ADD_INCIDENT,   // tap map → place red incident marker
        ADD_CITY,       // tap map → place white city marker
        ADD_ORDINARY,   // tap map → place white ordinary marker
        DRAW_PATH       // tap sequence of points → yellow path
    }

    private val _activeTool = MutableLiveData(Tool.SELECT)
    val activeTool: LiveData<Tool> = _activeTool

    fun setTool(tool: Tool) {
        _activeTool.value = tool
        // Cancel any in-progress path if switching away
        if (tool != Tool.DRAW_PATH) _pathInProgress.value = null
    }

    // ── Elements ──────────────────────────────────────────────────

    private val _elements = MutableLiveData<MutableList<MapElement>>(mutableListOf())
    val elements: LiveData<MutableList<MapElement>> = _elements

    private val _selectedElementId = MutableLiveData<String?>(null)
    val selectedElementId: LiveData<String?> = _selectedElementId

    fun selectElement(id: String?) {
        _selectedElementId.value = id
    }

    fun getElementById(id: String): MapElement? =
        _elements.value?.firstOrNull { it.id == id }

    // ── Path in progress (waypoints accumulating before commit) ───

    private val _pathInProgress = MutableLiveData<MutableList<Pair<Double, Double>>?>(null)
    val pathInProgress: LiveData<MutableList<Pair<Double, Double>>?> = _pathInProgress

    // ── Undo / Redo stacks ────────────────────────────────────────

    private val undoStack = ArrayDeque<EditorCommand>()
    private val redoStack = ArrayDeque<EditorCommand>()

    private val _canUndo = MutableLiveData(false)
    private val _canRedo = MutableLiveData(false)
    val canUndo: LiveData<Boolean> = _canUndo
    val canRedo: LiveData<Boolean> = _canRedo

    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    // ── Map tap handler ───────────────────────────────────────────

    /**
     * Called by the Activity whenever the user taps the map at (lat, lng).
     * Behaviour depends on the active tool.
     */
    fun onMapTap(lat: Double, lng: Double) {
        when (_activeTool.value) {
            Tool.ADD_INCIDENT -> addElement(
                IncidentMarker(id = newId(), latitude = lat, longitude = lng,
                    label = "Incident", radiusMeters = 5000.0)
            )
            Tool.ADD_CITY -> addElement(
                CityMarker(id = newId(), latitude = lat, longitude = lng, label = "City")
            )
            Tool.ADD_ORDINARY -> addElement(
                OrdinaryMarker(id = newId(), latitude = lat, longitude = lng, label = "")
            )
            Tool.DRAW_PATH -> {
                val pts = _pathInProgress.value ?: mutableListOf<Pair<Double, Double>>().also {
                    _pathInProgress.value = it
                }
                pts.add(lat to lng)
                _pathInProgress.value = pts   // trigger observers
            }
            else -> { /* SELECT – handled via marker click */ }
        }
    }

    /** Commit the in-progress path as a permanent PathElement. */
    fun commitPath(label: String = "Route") {
        val pts = _pathInProgress.value ?: return
        if (pts.size < 2) return
        val path = PathElement(
            id          = newId(),
            points      = pts.toList(),
            label       = label,
            color       = "#ffa502",
            dashPattern = "10,10"
        )
        addElement(path)
        _pathInProgress.value = null
    }

    fun cancelPath() {
        _pathInProgress.value = null
    }

    // ── CRUD with undo/redo ───────────────────────────────────────

    fun addElement(element: MapElement) {
        execute(EditorCommand.AddElement(element))
    }

    fun editDescription(id: String, newDescription: String) {
        val el = getElementById(id) ?: return
        val oldDescription = when (el) {
            is IncidentMarker -> el.description
            is CityMarker -> el.description
            is OrdinaryMarker -> el.description
            is PathElement -> el.description
            else -> ""
        }
        execute(EditorCommand.EditDescription(id, oldDescription, newDescription))
    }



    fun removeElement(id: String) {
        val el = getElementById(id) ?: return
        execute(EditorCommand.RemoveElement(el))
    }

    fun moveMarker(id: String, newLat: Double, newLng: Double) {
        val el = getElementById(id) ?: return
        val (oldLat, oldLng) = when (el) {
            is IncidentMarker -> el.latitude to el.longitude
            is CityMarker     -> el.latitude to el.longitude
            is OrdinaryMarker -> el.latitude to el.longitude
            else              -> return
        }
        execute(EditorCommand.MoveMarker(id, oldLat, oldLng, newLat, newLng))
    }

    fun editLabel(id: String, newLabel: String) {
        val el = getElementById(id) ?: return
        val oldLabel = when (el) {
            is IncidentMarker -> el.label
            is CityMarker     -> el.label
            is OrdinaryMarker -> el.label
            is PathElement    -> el.label
        }
        execute(EditorCommand.EditLabel(id, oldLabel, newLabel))
    }

    fun changeRadius(id: String, newRadius: Double) {
        val el = getElementById(id) as? IncidentMarker ?: return
        execute(EditorCommand.ChangeRadius(id, el.radiusMeters, newRadius))
    }

    // ── Undo / Redo ───────────────────────────────────────────────

    fun undo() {
        val cmd = undoStack.removeLastOrNull() ?: return
        applyReverse(cmd)
        redoStack.addLast(cmd)
        updateUndoRedoState()
        _elements.value = _elements.value   // trigger observer
    }

    fun redo() {
        val cmd = redoStack.removeLastOrNull() ?: return
        applyForward(cmd)
        undoStack.addLast(cmd)
        updateUndoRedoState()
        _elements.value = _elements.value
    }

    private fun execute(cmd: EditorCommand) {
        applyForward(cmd)
        undoStack.addLast(cmd)
        redoStack.clear()
        updateUndoRedoState()
        _elements.value = _elements.value   // trigger observer
    }

    private fun applyForward(cmd: EditorCommand) {
        val list = _elements.value ?: return
        when (cmd) {
            is EditorCommand.AddElement    -> list.add(cmd.element)
            is EditorCommand.RemoveElement -> list.removeAll { it.id == cmd.element.id }
            is EditorCommand.MoveMarker    -> applyMove(cmd.id, cmd.newLat, cmd.newLng, list)
            is EditorCommand.EditLabel     -> applyLabelChange(cmd.id, cmd.newLabel, list)
            is EditorCommand.EditDescription -> applyDescriptionChange(cmd.id, cmd.newDescription, list)
            is EditorCommand.ChangeRadius  -> applyRadiusChange(cmd.id, cmd.newRadius, list)
            is EditorCommand.AddPathPoint  -> { /* handled via commitPath */ }
        }
    }

    private fun applyReverse(cmd: EditorCommand) {
        val list = _elements.value ?: return
        when (cmd) {
            is EditorCommand.AddElement    -> list.removeAll { it.id == cmd.element.id }
            is EditorCommand.RemoveElement -> list.add(cmd.element)
            is EditorCommand.MoveMarker    -> applyMove(cmd.id, cmd.oldLat, cmd.oldLng, list)
            is EditorCommand.EditLabel     -> applyLabelChange(cmd.id, cmd.oldLabel, list)
            is EditorCommand.EditDescription -> applyDescriptionChange(cmd.id, cmd.oldDescription, list)
            is EditorCommand.ChangeRadius  -> applyRadiusChange(cmd.id, cmd.oldRadius, list)
            is EditorCommand.AddPathPoint  -> { /* handled via cancelPath */ }
        }
    }

    private fun applyDescriptionChange(id: String, description: String, list: MutableList<MapElement>) {
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        list[idx] = when (val el = list[idx]) {
            is IncidentMarker -> el.copy(description = description)
            is CityMarker -> el.copy(description = description)
            is OrdinaryMarker -> el.copy(description = description)
            is PathElement -> el.copy(description = description)
            else -> el
        }
    }

    private fun applyMove(id: String, lat: Double, lng: Double,
                          list: MutableList<MapElement>) {
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        list[idx] = when (val el = list[idx]) {
            is IncidentMarker -> el.copy(latitude = lat, longitude = lng)
            is CityMarker     -> el.copy(latitude = lat, longitude = lng)
            is OrdinaryMarker -> el.copy(latitude = lat, longitude = lng)
            else              -> el
        }
    }

    private fun applyLabelChange(id: String, label: String,
                                 list: MutableList<MapElement>) {
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        list[idx] = when (val el = list[idx]) {
            is IncidentMarker -> el.copy(label = label)
            is CityMarker     -> el.copy(label = label)
            is OrdinaryMarker -> el.copy(label = label)
            is PathElement    -> el.copy(label = label)
        }
    }

    private fun applyRadiusChange(id: String, radius: Double,
                                  list: MutableList<MapElement>) {
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val el = list[idx]
        if (el is IncidentMarker) list[idx] = el.copy(radiusMeters = radius)
    }

    // ── GeoJSON serialisation ─────────────────────────────────────

    fun toGeoJson(): StoryMapGeoJson {
        val features = _elements.value
            ?.map { it.toFeature() }
            ?.toMutableList()
            ?: mutableListOf()
        return StoryMapGeoJson(features = features)
    }

    fun toGeoJsonString(): String = toGeoJson().toJson()

    /**
     * Load a previously saved GeoJSON string into the editor.
     * Clears the undo/redo stack so it behaves like a fresh start.
     */
    fun loadFromGeoJson(json: String) {
        val geo = StoryMapGeoJson.fromJson(json)
        val parsed = mutableListOf<MapElement>()
        for (feature in geo.features) {
            val id = newId()
            val props = feature.properties
            when (props.markerType) {
                MarkerType.INCIDENT -> {
                    val pt = feature.geometry as? PointGeometry ?: continue
                    parsed.add(IncidentMarker(id, pt.latitude, pt.longitude,
                        props.label, props.description, props.radiusMeters))
                }
                MarkerType.CITY -> {
                    val pt = feature.geometry as? PointGeometry ?: continue
                    parsed.add(CityMarker(id, pt.latitude, pt.longitude,
                        props.label, props.description))
                }
                MarkerType.ORDINARY -> {
                    val pt = feature.geometry as? PointGeometry ?: continue
                    parsed.add(OrdinaryMarker(id, pt.latitude, pt.longitude,
                        props.label, props.description))
                }
                MarkerType.PATH -> {
                    val ls = feature.geometry as? LineStringGeometry ?: continue
                    parsed.add(PathElement(id, ls.latLngPairs(),
                        props.pathLabel, props.description, props.color, props.weight,
                        props.opacity, props.dashPattern))
                }
            }
        }
        _elements.value = parsed
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoState()
    }

    // ── Helpers ───────────────────────────────────────────────────

    private fun newId() = UUID.randomUUID().toString()

    // ── AI Suggestion Add Methods ───────────────────────────────────

    /**
     * Adds an incident marker at the specified coordinates with the given label and description
     */
    fun addIncident(latitude: Double, longitude: Double, label: String, description: String = "", radiusMeters: Double = 5000.0) {
        val incident = IncidentMarker(
            id = newId(),
            latitude = latitude,
            longitude = longitude,
            label = label.take(50),
            description = description.take(200),
            radiusMeters = radiusMeters
        )
        addElement(incident)
    }

    /**
     * Adds a city marker at the specified coordinates with the given label and description
     */
    fun addCity(latitude: Double, longitude: Double, label: String, description: String = "") {
        val city = CityMarker(
            id = newId(),
            latitude = latitude,
            longitude = longitude,
            label = label.take(50),
            description = description.take(200)
        )
        addElement(city)
    }

    /**
     * Adds an ordinary marker at the specified coordinates with the given label and description
     */
    fun addOrdinary(latitude: Double, longitude: Double, label: String, description: String = "") {
        val marker = OrdinaryMarker(
            id = newId(),
            latitude = latitude,
            longitude = longitude,
            label = label.take(50),
            description = description.take(200)
        )
        addElement(marker)
    }

    /**
     * Adds a path with the given points
     */
    fun addPath(points: List<Pair<Double, Double>>, label: String = "Route") {
        if (points.size < 2) return
        val path = PathElement(
            id = newId(),
            points = points,
            label = label,
            color = "#ffa502",
            weight = 3.0f,
            opacity = 0.8f,
            dashPattern = "2,2"
        )
        addElement(path)
    }
}