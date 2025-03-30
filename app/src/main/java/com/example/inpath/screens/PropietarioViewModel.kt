import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AreaSegura(
    val nombre: String,
    val latitud: Double,
    val longitud: Double
)

data class MascotaInfo(
    val nombre: String,
    val sexo: String,
    val tipo: String,
    var areaSegura: Boolean = false
)

class PropietarioViewModel : ViewModel() {
    private val _areasSeguras = MutableStateFlow(listOf(
        AreaSegura("EPS", 41.60831345075668, 0.6234935707600733),
        AreaSegura("Rectorat", 41.61493849521302, 0.6195960464809468),
        AreaSegura("ETSEA", 41.62781458688859, 0.5961549439884894)
    ))
    val areasSeguras: StateFlow<List<AreaSegura>> = _areasSeguras.asStateFlow()

    private val _listaMascotas = MutableStateFlow(mutableListOf<MascotaInfo>())
    val listaMascotas: StateFlow<MutableList<MascotaInfo>> = _listaMascotas.asStateFlow()

    fun agregarMascota(mascota: MascotaInfo) {
        _listaMascotas.update {
            it.apply { add(mascota) }
        }
    }

    fun eliminarMascota(mascota: MascotaInfo) {
        _listaMascotas.update {
            it.apply { remove(mascota) }
        }
    }

    fun actualizarLocalizacion(index: Int, areaSeguraActivada: Boolean) {
        _listaMascotas.value.getOrNull(index)?.areaSegura = areaSeguraActivada
    }

    fun agregarAreaSegura(area: AreaSegura) {
        _areasSeguras.update {
            it + area
        }
    }

    fun eliminarAreaSegura(area: AreaSegura) {
        _areasSeguras.value = _areasSeguras.value.toMutableList().apply {
            remove(area)
        }
    }
}